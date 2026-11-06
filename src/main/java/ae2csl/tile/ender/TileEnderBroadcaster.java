/*
 * AE2 Crystal Science - TileEnderBroadcaster (1.12.2)
 * Full port from 1.21.1 EnderBroadcasterBlockEntity (644 lines)
 *
 * Core of the frequency band broadcast system:
 * - As sender: creates virtual grid nodes that consume channels, providing bandwidth to the band
 * - As receiver: uses CustomChannelProviderHost to receive allocated channels from the band
 * - Memory card import/export for band connection info
 */
package ae2csl.tile.ender;

import ae2.api.networking.*;
import ae2.api.networking.pathing.ChannelMode;
import ae2.core.AEConfig;
import ae2.core.definitions.AEBlocks;
import ae2.api.networking.IManagedGridNode;
import ae2.util.SettingsFrom;
import ae2csl.api.CustomChannelProviderHost;
import ae2csl.api.linker.broadcast.*;
import ae2csl.api.util.GlobalPos;
import ae2csl.init.AE2CSBlocks;
import ae2csl.init.AE2CSDataComponents;
import ae2csl.tile.base.AENetworkedTileBase;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileEnderBroadcaster extends AENetworkedTileBase
        implements CustomChannelProviderHost, BroadcastSenderHost, BroadcastReceiverHost {

    private static final int VIRTUAL_SENDER_NODE_HARD_CAP = 768;

    private static int getMaxReceiverChannels() {
        ChannelMode mode = AEConfig.instance().getChannelMode();
        if (mode == ChannelMode.INFINITE) return Integer.MAX_VALUE;
        return 32 * mode.getCableCapacityFactor();
    }

    private String bandId = "";
    private ConnectionType connectionType = ConnectionType.NO_CONNECTION;
    private int expectedChannels = 32;

    // CustomChannelProviderHost
    private int customMaxChannels = 0;
    private boolean enabledCustomChannel = false;

    // Sender virtual nodes
    private final List<IManagedGridNode> virtualSenderNodes = new ArrayList<>();
    private int virtualSenderNodeTarget = 0;
    private int succeedVirtualSenderNodes = 0;
    private boolean needRecountVirtualSenderNodes = true;

    // Client render state
    private boolean activeForClient = false;
    private boolean asSenderForClient = false;

    public TileEnderBroadcaster() {
        super();
    }

    @Override protected ItemStack getMachineRepresentation() {
        return new ItemStack(AE2CSBlocks.ENDER_BROADCASTER);
    }

    @Override protected double getIdlePowerUsage() { return 10.0; }

    // The broadcaster's main (anchor) node must be a dense-capacity carrier, not a normal
    // channel-consuming device: the 32 virtual sender nodes hang off it and each needs a real
    // channel. A normal (REQUIRE_CHANNEL) anchor would consume 1 channel itself and cap at
    // 8-1=7 channels on a standard 8-channel cable. DENSE_CAPACITY (matches 1.21.1 source
    // `getMainNode().setFlags(GridFlags.DENSE_CAPACITY)`) lets it carry 32 channels like a dense
    // cable, so up to 32 virtual nodes can be powered.
    @Override protected GridFlags[] getMainNodeFlags() { return new GridFlags[]{GridFlags.DENSE_CAPACITY}; }

    // ---- Tick ----
    public void update() {
        if (world == null || world.isRemote) return;

        if (needRecountVirtualSenderNodes) {
            needRecountVirtualSenderNodes = false;
            int count = countSucceedVirtualSenderNodes();
            if (succeedVirtualSenderNodes != count) {
                succeedVirtualSenderNodes = count;
                if (connectionType == ConnectionType.AS_SENDER) markBandRuntimeDirty();
                markForClientUpdate();
            }
        }

        boolean asSender = connectionType == ConnectionType.AS_SENDER;
        boolean active = asSender ? getCouldSendChannels() > 0
                : (connectionType == ConnectionType.AS_RECEIVER && getMainNode().getNode() != null
                        && getMainNode().getNode().isPowered());
        if (active != activeForClient) { activeForClient = active; markForClientUpdate(); }
        if (asSender != asSenderForClient) { asSenderForClient = asSender; markForClientUpdate(); }
    }

    private void markForClientUpdate() {
        World w = getWorld(); BlockPos p = getPos();
        if (w != null && !w.isRemote) { IBlockState s = w.getBlockState(p); w.notifyBlockUpdate(p, s, s, 3); }
    }

    // ---- Getters ----
    public String getBandName() { return bandId; }
    public ConnectionType getConnectionType() { return connectionType; }
    public boolean isActiveForClient() { return activeForClient; }
    public boolean isAsSenderForClient() { return asSenderForClient; }

    // ---- BroadcastSenderHost ----
    @Override public int getCouldSendChannels() {
        if (world == null || connectionType != ConnectionType.AS_SENDER) return 0;
        return succeedVirtualSenderNodes;
    }

    // ---- BroadcastReceiverHost ----
    @Override public int getExpectedChannels() { return expectedChannels; }

    public void setExpectedChannels(int v) {
        int n = Math.max(0, Math.min(v, getMaxExpectedChannels()));
        if (n == expectedChannels) return;
        expectedChannels = n;
        markBandRuntimeDirty();
        markDirty();
        markForClientUpdate();
    }

    private int getMaxExpectedChannels() {
        if (bandId != null && !bandId.isEmpty()) {
            BroadcastFrequencyBand band = FrequencyBandManager.getBand(bandId);
            if (band != null) return clampChannelCount(band.getUsableChannels());
        }
        return getMaxReceiverChannels();
    }

    private static int clampChannelCount(long c) { return c <= 0 ? 0 : c >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) c; }

    // ---- CustomChannelProviderHost ----
    @Override public int getMaxChannels() { return Math.max(0, customMaxChannels); }
    @Override public void setMaxChannels(int v) {
        v = Math.max(0, v);
        if (v == customMaxChannels) return;
        customMaxChannels = v;
        markDirty();
        markForClientUpdate();
    }
    @Override public boolean isEnabledCustomChannel() { return enabledCustomChannel; }
    @Override public void setEnabledCustomChannel(boolean v) {
        if (v == enabledCustomChannel) return;
        enabledCustomChannel = v;
        markDirty();
        markForClientUpdate();
    }

    // ---- Sender virtual nodes ----
    private int computeSenderVirtualNodeTarget() {
        ChannelMode mode = AEConfig.instance().getChannelMode();
        if (mode == ChannelMode.INFINITE) return 0;
        int desired = Math.max(0, 32 * mode.getCableCapacityFactor());
        if (world != null) {
            int controlSide = 0;
            for (EnumFacing d : EnumFacing.VALUES) {
                if (world.getBlockState(pos.offset(d)).getBlock() == AEBlocks.CONTROLLER.block()) controlSide++;
            }
            desired *= Math.max(1, controlSide);
        }
        return Math.min(desired, VIRTUAL_SENDER_NODE_HARD_CAP);
    }

    private void ensureSenderVirtualNodes() {
        if (world == null || world.isRemote) return;
        if (connectionType != ConnectionType.AS_SENDER) { destroySenderVirtualNodes(); return; }
        int target = computeSenderVirtualNodeTarget();
        if (target <= 0) { destroySenderVirtualNodes(); return; }
        if (target == virtualSenderNodeTarget && virtualSenderNodes.size() == target) {
            boolean allReady = true;
            for (IManagedGridNode n : virtualSenderNodes) {
                if (!n.isReady()) { allReady = false; break; }
            }
            if (allReady) return;
        }

        destroySenderVirtualNodes();
        virtualSenderNodeTarget = target;
        setEnabledCustomChannel(true);
        setMaxChannels(target);

        IGridNode anchor = getMainNode().getNode();
        if (anchor == null) return;

        for (int i = 0; i < target; i++) {
            IManagedGridNode vm = GridHelper.createManagedNode(this, new IGridNodeListener<TileEnderBroadcaster>() {
                @Override public void onSaveChanges(TileEnderBroadcaster o, IGridNode n) {}
                @Override public void onStateChanged(TileEnderBroadcaster o, IGridNode n, State r) {
                    if (o == null || o.connectionType != ConnectionType.AS_SENDER) return;
                    if (o.bandId == null || o.bandId.isEmpty()) return;
                    if (r == State.CHANNEL || r == State.GRID_BOOT || r == State.POWER) o.markNeedRecountVirtualSenderNodes();
                }
            });
            vm.setInWorldNode(false);
            vm.setIdlePowerUsage(10.0);
            vm.setTagName("sender_virtual_" + i);
            vm.setFlags(GridFlags.REQUIRE_CHANNEL);
            vm.create(world, pos);

            IGridNode vn = vm.getNode();
            if (vn != null) GridHelper.createConnection(anchor, vn);
            virtualSenderNodes.add(vm);
        }
    }

    private void destroySenderVirtualNodes() {
        for (IManagedGridNode m : virtualSenderNodes) { try { m.destroy(); } catch (Throwable ignored) {} }
        virtualSenderNodes.clear();
        virtualSenderNodeTarget = 0;
    }

    private int countSucceedVirtualSenderNodes() {
        int c = 0;
        for (IManagedGridNode m : virtualSenderNodes) {
            IGridNode n = m.getNode();
            if (n != null && n.isOnline()) c++;
        }
        return c;
    }

    private void markNeedRecountVirtualSenderNodes() { needRecountVirtualSenderNodes = true; }

    // ---- Connection logic ----
    public void connectToBand(String newBandId, boolean asSender) {
        if (world == null || world.isRemote) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        BroadcastFrequencyBand newBand = FrequencyBandManager.getBand(newBandId);
        if (newBand == null) return;

        GlobalPos gp = new GlobalPos(world.provider.getDimension(), pos);

        if (!bandId.isEmpty()) {
            BroadcastFrequencyBand oldBand = FrequencyBandManager.getBand(bandId);
            if (oldBand != null) {
                if (connectionType == ConnectionType.AS_RECEIVER) oldBand.onReceiverOffline(server, gp);
                else if (connectionType == ConnectionType.AS_SENDER) oldBand.onSenderOffline(server, gp);
                if (oldBand != newBand) { oldBand.undeclareSender(gp); oldBand.undeclareReceiver(gp); }
            }
        }

        if (asSender) {
            newBand.declareSender(gp); newBand.undeclareReceiver(gp);
            connectionType = ConnectionType.AS_SENDER;
            ensureSenderVirtualNodes();
            IGridNode node = getMainNode().getNode();
            if (node != null) newBand.onSenderOnline(server, gp, node);
        } else {
            newBand.declareReceiver(gp); newBand.undeclareSender(gp);
            destroySenderVirtualNodes();
            setEnabledCustomChannel(true); setMaxChannels(0);
            connectionType = ConnectionType.AS_RECEIVER;
            IGridNode node = getMainNode().getNode();
            if (node != null) newBand.onReceiverOnline(server, gp, node, this);
        }

        bandId = newBand.getName();
        markForClientUpdate(); markDirty();
    }

    @Override public void cleanConnectionPermanent() {
        if (world == null || world.isRemote) return;
        destroySenderVirtualNodes();
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        if (bandId.isEmpty() || connectionType == ConnectionType.NO_CONNECTION) { setEnabledCustomChannel(false); return; }

        BroadcastFrequencyBand band = FrequencyBandManager.getBand(bandId);
        GlobalPos gp = new GlobalPos(world.provider.getDimension(), pos);
        if (band != null) {
            if (connectionType == ConnectionType.AS_RECEIVER) band.onReceiverOffline(server, gp);
            else band.onSenderOffline(server, gp);
            band.undeclareSender(gp); band.undeclareReceiver(gp);
        }
        bandId = ""; connectionType = ConnectionType.NO_CONNECTION;
        setEnabledCustomChannel(false); markForClientUpdate(); markDirty();
    }

    private void markBandRuntimeDirty() {
        if (world == null || world.isRemote || bandId.isEmpty()) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        FrequencyBandManager.markRuntimeDirty(server, bandId);
    }

    @Override public void onChunkUnload() {
        destroySenderVirtualNodes();
        if (world != null && !world.isRemote && !bandId.isEmpty() && connectionType != ConnectionType.NO_CONNECTION) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                BroadcastFrequencyBand band = FrequencyBandManager.getBand(bandId);
                if (band != null) {
                    GlobalPos gp = new GlobalPos(world.provider.getDimension(), pos);
                    if (connectionType == ConnectionType.AS_RECEIVER) band.onReceiverOffline(server, gp);
                    else band.onSenderOffline(server, gp);
                }
            }
        }
        super.onChunkUnload();
    }

    @Override protected void onGridReady() {
        if (world == null || world.isRemote || bandId.isEmpty() || connectionType == ConnectionType.NO_CONNECTION) {
            setEnabledCustomChannel(false); destroySenderVirtualNodes(); return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        BroadcastFrequencyBand band = FrequencyBandManager.getBand(bandId);
        if (band == null) { setEnabledCustomChannel(false); destroySenderVirtualNodes(); return; }
        GlobalPos gp = new GlobalPos(world.provider.getDimension(), pos);
        IGridNode node = getMainNode().getNode();
        if (node == null) return;

        if (connectionType == ConnectionType.AS_RECEIVER) {
            band.declareReceiver(gp); band.undeclareSender(gp);
            destroySenderVirtualNodes(); setEnabledCustomChannel(true);
            band.onReceiverOnline(server, gp, node, this);
        } else {
            band.declareSender(gp); band.undeclareReceiver(gp);
            ensureSenderVirtualNodes();
            band.onSenderOnline(server, gp, node);
        }
    }

    @Override public void onStateChanged(AENetworkedTileBase owner, IGridNode node, State state) {
        super.onStateChanged(owner, node, state);
    }

    // ---- Memory card ----
    public void importSettings(SettingsFrom mode, NBTTagCompound input, @Nullable net.minecraft.entity.player.EntityPlayer player) {
        if (mode == SettingsFrom.MEMORY_CARD) {
            MemoryCardBandInfo info = AE2CSDataComponents.getMemoryCardBandInfo(new ItemStack(net.minecraft.init.Items.PAPER) /* dummy */);
            NBTTagCompound tag = input.getCompoundTag("band_info");
            if (tag.hasKey("band_name")) info = MemoryCardBandInfo.readFromNBT(tag);
            if (info == null) return;
            BroadcastFrequencyBand band = FrequencyBandManager.getBand(info.getBandName());
            if (band == null || !band.isAllowedMemoryCardCopy()) return;
            cleanConnectionPermanent();
            connectToBand(info.getBandName(), info.isAsSender());
            if (!info.isAsSender() && input.hasKey("expect_channels")) setExpectedChannels(input.getInteger("expect_channels"));
        }
    }

    public void exportSettings(SettingsFrom mode, NBTTagCompound output, @Nullable net.minecraft.entity.player.EntityPlayer player) {
        if (mode == SettingsFrom.MEMORY_CARD) {
            if (bandId == null || bandId.isEmpty()) return;
            BroadcastFrequencyBand band = FrequencyBandManager.getBand(bandId);
            if (band == null || !band.isAllowedMemoryCardCopy()) return;
            boolean asSender = connectionType == ConnectionType.AS_SENDER;
            NBTTagCompound infoTag = new MemoryCardBandInfo(bandId, asSender).writeToNBT();
            output.setTag("band_info", infoTag);
            if (!asSender) output.setInteger("expect_channels", expectedChannels);
        }
    }

    // ---- NBT ----
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        bandId = tag.getString("band_id");
        String t = tag.getString("connection_type");
        connectionType = t.isEmpty() ? ConnectionType.NO_CONNECTION : ConnectionType.valueOf(t);
        enabledCustomChannel = tag.getBoolean("enabled_custom_channel");
        customMaxChannels = tag.getInteger("custom_max_channels");
        expectedChannels = tag.getInteger("expected_channels");
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString("band_id", bandId);
        tag.setString("connection_type", connectionType.name());
        tag.setBoolean("enabled_custom_channel", enabledCustomChannel);
        tag.setInteger("custom_max_channels", customMaxChannels);
        tag.setInteger("expected_channels", expectedChannels);
        return tag;
    }

    // ---- Stream ----
    public void writeToStream(PacketBuffer data) {
        data.writeString(bandId);
        data.writeBoolean(activeForClient);
        data.writeBoolean(asSenderForClient);
    }

    public boolean readFromStream(PacketBuffer data) {
        bandId = data.readString(64);
        activeForClient = data.readBoolean();
        asSenderForClient = data.readBoolean();
        return true;
    }

    // ---- Client sync (1.12.2) ----

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setString("band_id", bandId);
        tag.setString("connection_type", connectionType.name());
        tag.setInteger("expected_channels", expectedChannels);
        tag.setInteger("sent_channels", succeedVirtualSenderNodes);
        tag.setInteger("custom_max_channels", customMaxChannels);
        tag.setBoolean("enabled_custom_channel", enabledCustomChannel);
        tag.setBoolean("active_for_client", activeForClient);
        tag.setBoolean("as_sender_for_client", asSenderForClient);
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        applyClientState(tag);
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        applyClientState(pkt.getNbtCompound());
    }

    private void applyClientState(NBTTagCompound tag) {
        bandId = tag.getString("band_id");
        String t = tag.getString("connection_type");
        connectionType = t.isEmpty() ? ConnectionType.NO_CONNECTION : ConnectionType.valueOf(t);
        expectedChannels = tag.getInteger("expected_channels");
        succeedVirtualSenderNodes = tag.getInteger("sent_channels");
        customMaxChannels = tag.getInteger("custom_max_channels");
        enabledCustomChannel = tag.getBoolean("enabled_custom_channel");
        activeForClient = tag.getBoolean("active_for_client");
        asSenderForClient = tag.getBoolean("as_sender_for_client");
    }

    // ---- Misc ----
    public ItemStack getMainMenuIcon() { return new ItemStack(AE2CSBlocks.ENDER_BROADCASTER); }

    public enum ConnectionType { AS_SENDER, AS_RECEIVER, NO_CONNECTION }
}
