/*
 * AE2 Crystal Science - API BroadcastFrequencyBand
 * Ported from 1.21.1 source
 *
 * A frequency band: persistent data + runtime online endpoints/connections/allocations.
 * - declaredSenders/declaredReceivers: pure data (survives chunk unload/load)
 * - online*: runtime (reported by endpoints)
 * - recomputeRuntime: triggered by FrequencyBandManager
 */
package ae2csl.api.linker.broadcast;

import ae2.api.networking.GridHelper;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridConnection;
import ae2.api.networking.IGridNode;
import ae2.api.networking.pathing.ChannelMode;
import ae2.api.networking.pathing.ControllerState;
import ae2.core.AEConfig;
import ae2csl.api.CustomChannelProviderHost;
import ae2csl.api.util.AE2CSGridHelper;
import ae2csl.api.util.GlobalPos;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class BroadcastFrequencyBand {

    /**
     * When AE uses wireless bands, return this for infinite usable channels.
     */
    private static final long INFINITE_USABLE_CHANNELS = 1L << 60;

    // ---- Persistent (pure data) ----
    private String name;
    private String password;
    private UUID owner;
    private boolean isPublic;
    private boolean allowedMemoryCardCopy;
    private final Set<UUID> whiteList = new LinkedHashSet<>();
    private final Set<GlobalPos> declaredSenders = new LinkedHashSet<>();
    private final Set<GlobalPos> declaredReceivers = new LinkedHashSet<>();

    // ---- Runtime (not persisted) ----
    private final transient Object2ObjectOpenHashMap<GlobalPos, IGridNode> onlineSenderNodes = new Object2ObjectOpenHashMap<>();
    private final transient Object2ObjectOpenHashMap<GlobalPos, IGridNode> onlineReceiverNodes = new Object2ObjectOpenHashMap<>();
    private final transient Object2ObjectOpenHashMap<GlobalPos, CustomChannelProviderHost> onlineReceiverHosts = new Object2ObjectOpenHashMap<>();
    private final transient Object2IntOpenHashMap<GlobalPos> receiverAllocated = new Object2IntOpenHashMap<>();
    private final transient Object2ObjectOpenHashMap<GlobalPos, IGridConnection> receiverConnections = new Object2ObjectOpenHashMap<>();

    private transient @Nullable IGrid bindGrid;
    private transient @Nullable IGridNode controllerNode;
    private long usableChannels;
    private long usedChannels;
    private BandError errorState = BandError.FINE;

    public BroadcastFrequencyBand(String name, String password, UUID owner, boolean isPublic, boolean allowedMemoryCardCopy) {
        this.name = name;
        this.password = password;
        this.owner = owner;
        this.isPublic = isPublic;
        this.allowedMemoryCardCopy = allowedMemoryCardCopy;
        receiverAllocated.defaultReturnValue(0);
    }

    // ---- Getters & Setters ----

    public String getName() { return name; }

    public boolean validPassword(String password) {
        if (this.password.isEmpty()) return true;
        return this.password.equals(password);
    }

    public String getPassword() { return password; }
    public void setPassword(String password) {
        if (this.password.equals(password)) return;
        this.password = password;
        FrequencyBandManager.markSavedDataDirty();
    }

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) {
        if (this.owner.equals(owner)) return;
        this.owner = owner;
        FrequencyBandManager.markSavedDataDirty();
    }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) {
        if (this.isPublic == isPublic) return;
        this.isPublic = isPublic;
        FrequencyBandManager.markSavedDataDirty();
    }

    public boolean isAllowedMemoryCardCopy() { return allowedMemoryCardCopy; }
    public void setAllowedMemoryCardCopy(boolean allowedMemoryCardCopy) {
        if (this.allowedMemoryCardCopy == allowedMemoryCardCopy) return;
        this.allowedMemoryCardCopy = allowedMemoryCardCopy;
        FrequencyBandManager.markSavedDataDirty();
    }

    public boolean validWhiteList(UUID uuid) {
        return owner.equals(uuid) || whiteList.contains(uuid);
    }

    /** Read-only! */
    public Set<UUID> getWhiteList() { return whiteList; }

    public void addToWhiteList(UUID uuid) {
        if (whiteList.contains(uuid)) return;
        whiteList.add(uuid);
        FrequencyBandManager.markSavedDataDirty();
    }

    public void removeFromWhiteList(UUID uuid) {
        if (!whiteList.contains(uuid)) return;
        whiteList.remove(uuid);
        FrequencyBandManager.markSavedDataDirty();
    }

    public Set<GlobalPos> getDeclaredReceivers() { return declaredReceivers; }
    public Set<GlobalPos> getDeclaredSenders() { return declaredSenders; }
    public long getUsableChannels() { return usableChannels; }
    public long getUsedChannels() { return usedChannels; }
    public @Nullable IGrid getBindGrid() { return bindGrid; }
    public BandError getErrorState() { return errorState; }

    // ---- Pure Data API ----

    public boolean declareSender(GlobalPos pos) {
        boolean changed = declaredSenders.add(pos);
        if (changed) FrequencyBandManager.markSavedDataDirty();
        return changed;
    }

    public boolean undeclareSender(GlobalPos pos) {
        boolean changed = declaredSenders.remove(pos);
        if (changed) FrequencyBandManager.markSavedDataDirty();
        return changed;
    }

    public boolean declareReceiver(GlobalPos pos) {
        boolean changed = declaredReceivers.add(pos);
        if (changed) FrequencyBandManager.markSavedDataDirty();
        return changed;
    }

    public boolean undeclareReceiver(GlobalPos pos) {
        boolean changed = declaredReceivers.remove(pos);
        if (changed) FrequencyBandManager.markSavedDataDirty();
        return changed;
    }

    // ---- Runtime API ----

    public void onSenderOnline(MinecraftServer server, GlobalPos pos, IGridNode node) {
        if (!declaredSenders.contains(pos)) return;
        onlineSenderNodes.put(pos, node);
        FrequencyBandManager.markRuntimeDirty(server, name);
    }

    public void onSenderOffline(MinecraftServer server, GlobalPos pos) {
        onlineSenderNodes.remove(pos);
        FrequencyBandManager.markRuntimeDirty(server, name);
    }

    public void onReceiverOnline(MinecraftServer server, GlobalPos pos, IGridNode node, CustomChannelProviderHost host) {
        if (!declaredReceivers.contains(pos)) return;
        onlineReceiverNodes.put(pos, node);
        onlineReceiverHosts.put(pos, host);
        FrequencyBandManager.markRuntimeDirty(server, name);
    }

    public void onReceiverOffline(MinecraftServer server, GlobalPos pos) {
        onlineReceiverNodes.remove(pos);

        CustomChannelProviderHost host = onlineReceiverHosts.remove(pos);
        if (host != null) {
            host.setEnabledCustomChannel(false);
        }

        receiverAllocated.removeInt(pos);

        IGridConnection conn = receiverConnections.remove(pos);
        if (conn != null) conn.destroy();

        FrequencyBandManager.markRuntimeDirty(server, name);
    }

    /**
     * Recompute all sender channels and allocate to receivers.
     * Called by FrequencyBandManager#tick.
     */
    void recomputeRuntime() {
        // 1) Choose bindGrid: all online senders must be in the same grid with a controller
        IGrid newBindGrid = null;
        this.errorState = BandError.FINE;

        for (Object2ObjectMap.Entry<GlobalPos, IGridNode> entry : onlineSenderNodes.object2ObjectEntrySet()) {
            IGridNode senderNode = entry.getValue();
            if (senderNode == null) {
                this.errorState = BandError.SENDER_ERROR;
                break;
            }

            IGrid grid;
            try {
                grid = senderNode.grid();
            } catch (IllegalStateException destroyed) {
                // Node was destroyed without onSenderOffline being called; treat as sender error.
                this.errorState = BandError.SENDER_ERROR;
                break;
            }
            if (grid == null) {
                this.errorState = BandError.SENDER_ERROR;
                break;
            }

            if (grid.getPathingService().getControllerState() != ControllerState.CONTROLLER_ONLINE) {
                this.errorState = BandError.SENDER_ERROR;
                break;
            }

            if (newBindGrid == null) {
                newBindGrid = grid;
            } else if (newBindGrid != grid) {
                this.errorState = BandError.CONTROLLER_CONFLICT;
                break;
            }
        }

        if (this.errorState != BandError.FINE || newBindGrid == null) {
            if (newBindGrid == null)
                this.errorState = BandError.NO_SENDER;

            bindGrid = null;
            controllerNode = null;
            dropAllReceiversToVanillaAndDisconnect();
            return;
        }

        // Find controller node
        IGridNode newController = AE2CSGridHelper.getControlNode(newBindGrid);
        if (newController == null) {
            this.errorState = BandError.MISSING_CONTROLLER;
            bindGrid = null;
            controllerNode = null;
            dropAllReceiversToVanillaAndDisconnect();
            return;
        }

        boolean controllerChanged = (controllerNode != newController);
        bindGrid = newBindGrid;
        controllerNode = newController;

        if (controllerChanged) {
            for (IGridConnection conn : receiverConnections.values()) {
                if (conn != null) conn.destroy();
            }
            receiverConnections.clear();
        }

        // 2) Count total usable channels from senders
        long totalUsable = computeTotalUsableChannels();
        this.usableChannels = totalUsable;

        // 3) Allocate
        allocateToAllReceivers(totalUsable);
    }

    private long computeTotalUsableChannels() {
        boolean infinite = (AEConfig.instance().getChannelMode() == ChannelMode.INFINITE);
        if (infinite) {
            return INFINITE_USABLE_CHANNELS;
        }

        long totalUsable = 0;
        long maxNeed = INFINITE_USABLE_CHANNELS;

        for (var entry : onlineSenderNodes.object2ObjectEntrySet()) {
            IGridNode senderNode = entry.getValue();
            if (senderNode == null) continue;

            int usable = 0;
            if (senderNode.getOwner() instanceof BroadcastSenderHost senderHost) {
                usable = senderHost.getCouldSendChannels();
            }

            if (usable < 0) usable = 0;

            totalUsable += usable;
            if (totalUsable >= maxNeed) {
                totalUsable = maxNeed;
                break;
            }
        }

        return totalUsable;
    }

    private void dropAllReceiversToVanillaAndDisconnect() {
        for (Object2ObjectMap.Entry<GlobalPos, CustomChannelProviderHost> entry : onlineReceiverHosts.object2ObjectEntrySet()) {
            CustomChannelProviderHost host = entry.getValue();
            if (host != null) host.setEnabledCustomChannel(false);
        }

        receiverAllocated.clear();

        for (IGridConnection conn : receiverConnections.values()) {
            if (conn != null) conn.destroy();
        }
        receiverConnections.clear();
    }

    private void allocateToAllReceivers(long totalUsable) {
        // 共享频段 + 先到先得：按声明顺序（declaredReceivers 为 LinkedHashSet，即 declare 顺序）
        // 贪婪分配。每个接收端拿到 min(剩余, 实际需求)，需求即其「已链接 + 待链接」的机器数，
        // 因此未使用满时每个接收端都能拿到所需额度，满时先声明的接收端优先、后来的拿剩余。
        // 总额度之和不会超过 totalUsable（控制器真实容量），保证总 active ≤ 32。
        long remaining = totalUsable;

        for (GlobalPos rp : declaredReceivers) {
            IGridNode receiverNode = onlineReceiverNodes.get(rp);
            CustomChannelProviderHost host = onlineReceiverHosts.get(rp);
            if (receiverNode == null || host == null) continue;
            if (!(receiverNode.getOwner() instanceof BroadcastReceiverHost receiverHost)) continue;

            int expected = receiverHost.getExpectedChannels();
            int alloc = computeAllocChannels(remaining, expected);
            remaining -= alloc;
            applyReceiver(rp, receiverNode, host, alloc);
        }

        this.usedChannels = totalUsable - remaining;

        // Online but undeclared receivers: restore to vanilla and disconnect
        for (Object2ObjectMap.Entry<GlobalPos, CustomChannelProviderHost> entry : onlineReceiverHosts.object2ObjectEntrySet()) {
            GlobalPos rp = entry.getKey();
            if (!declaredReceivers.contains(rp)) {
                CustomChannelProviderHost host = entry.getValue();
                if (host != null) host.setEnabledCustomChannel(false);

                receiverAllocated.removeInt(rp);
                IGridConnection conn = receiverConnections.remove(rp);
                if (conn != null) conn.destroy();
            }
        }
    }

    private int computeAllocChannels(long remaining, int expectedChannels) {
        if (expectedChannels <= 0) return 0;
        if (remaining <= 0) return 0;
        long give = Math.min(remaining, expectedChannels);
        return (int) give;
    }

    private void applyReceiver(GlobalPos receiverPos, IGridNode receiverNode, CustomChannelProviderHost host, int newAlloc) {
        int oldAlloc = receiverAllocated.getInt(receiverPos);
        boolean allocChanged = (oldAlloc != newAlloc);

        if (newAlloc <= 0) {
            receiverAllocated.removeInt(receiverPos);
            host.setEnabledCustomChannel(false);

            IGridConnection oldConn = receiverConnections.remove(receiverPos);
            if (oldConn != null) oldConn.destroy();
            return;
        }

        if (!host.isEnabledCustomChannel()) {
            host.setEnabledCustomChannel(true);
            allocChanged = true;
        }

        if (allocChanged) {
            receiverAllocated.put(receiverPos, newAlloc);
            host.setMaxChannels(newAlloc);
        }

        if (controllerNode == null) {
            host.setEnabledCustomChannel(false);
            IGridConnection oldConn = receiverConnections.remove(receiverPos);
            if (oldConn != null) oldConn.destroy();
            return;
        }

        IGridConnection conn = receiverConnections.get(receiverPos);
        boolean needRebuild = conn == null || allocChanged;

        if (needRebuild) {
            if (conn != null) conn.destroy();
            if (!(controllerNode.getOwner() instanceof TileEntity)) return;
            TileEntity controllerBE = (TileEntity) controllerNode.getOwner();
            if (!(controllerBE.getWorld() instanceof WorldServer)) return;

            WorldServer level = (WorldServer) controllerBE.getWorld();
            // Prevent direct adjacent duplicate connections
            if (level.provider.getDimension() == receiverPos.getDimension() &&
                    areAdjacent(receiverPos.getPos(), controllerBE.getPos())) {
                return;
            }

            receiverConnections.put(receiverPos, GridHelper.createConnection(controllerNode, receiverNode));
        }
    }

    private static boolean areAdjacent(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        return (dx + dy + dz) == 1;
    }

    void onRemoved() {
        MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            cleanupDeclaredBlockEntities(server);
        }

        onlineSenderNodes.clear();
        onlineReceiverNodes.clear();
        onlineReceiverHosts.clear();
        receiverAllocated.clear();

        bindGrid = null;
        controllerNode = null;

        usableChannels = 0;
        usedChannels = 0;
        errorState = BandError.FINE;
    }

    private void cleanupDeclaredBlockEntities(MinecraftServer server) {
        Set<GlobalPos> targets = new LinkedHashSet<>();
        targets.addAll(declaredSenders);
        targets.addAll(declaredReceivers);

        for (GlobalPos gp : targets) {
            if (gp == null) continue;

            WorldServer level = DimensionManager.getWorld(gp.getDimension());
            if (level == null) continue;

            BlockPos pos = gp.getPos();
            try {
                level.getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
            } catch (Throwable ignored) {
                continue;
            }

            TileEntity be = level.getTileEntity(pos);
            if (be instanceof BandLinkHost) {
                try {
                    ((BandLinkHost) be).cleanConnectionPermanent();
                } catch (Throwable ignored) {}
            }
        }
    }

    // ---- NBT Serialization ----

    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("name", name);
        tag.setString("password", password);
        tag.setString("owner", owner.toString());
        tag.setBoolean("is_public", isPublic);
        tag.setBoolean("allowed_memory_card_copy", allowedMemoryCardCopy);

        NBTTagList whiteListTag = new NBTTagList();
        for (UUID id : whiteList) {
            if (id != null) whiteListTag.appendTag(new NBTTagString(id.toString()));
        }
        tag.setTag("white_list", whiteListTag);

        NBTTagList senderListTag = new NBTTagList();
        for (GlobalPos sender : declaredSenders) {
            if (sender == null) continue;
            NBTTagCompound posTag = writeGlobalPos(sender);
            senderListTag.appendTag(posTag);
        }
        tag.setTag("sender_list", senderListTag);

        NBTTagList receiverListTag = new NBTTagList();
        for (GlobalPos receiver : declaredReceivers) {
            if (receiver == null) continue;
            NBTTagCompound posTag = writeGlobalPos(receiver);
            receiverListTag.appendTag(posTag);
        }
        tag.setTag("receiver_list", receiverListTag);

        return tag;
    }

    public void deserializeNBT(NBTTagCompound compoundTag) {
        this.name = compoundTag.getString("name");
        this.password = compoundTag.getString("password");
        this.owner = UUID.fromString(compoundTag.getString("owner"));
        this.isPublic = compoundTag.getBoolean("is_public");
        this.allowedMemoryCardCopy = compoundTag.getBoolean("allowed_memory_card_copy");

        this.whiteList.clear();
        this.declaredSenders.clear();
        this.declaredReceivers.clear();

        this.onlineSenderNodes.clear();
        this.onlineReceiverNodes.clear();
        this.onlineReceiverHosts.clear();
        this.receiverAllocated.clear();
        for (IGridConnection conn : receiverConnections.values()) {
            if (conn != null) conn.destroy();
        }
        this.receiverConnections.clear();
        this.bindGrid = null;
        this.controllerNode = null;

        NBTTagList whiteListTag = compoundTag.getTagList("white_list", 8);
        for (int i = 0; i < whiteListTag.tagCount(); i++) {
            try {
                UUID id = UUID.fromString(whiteListTag.getStringTagAt(i));
                this.whiteList.add(id);
            } catch (Throwable ignored) {}
        }

        NBTTagList senderListTag = compoundTag.getTagList("sender_list", 10);
        for (int i = 0; i < senderListTag.tagCount(); i++) {
            NBTTagCompound t = senderListTag.getCompoundTagAt(i);
            GlobalPos pos = readGlobalPos(t);
            if (pos != null) this.declaredSenders.add(pos);
        }

        NBTTagList receiverListTag = compoundTag.getTagList("receiver_list", 10);
        for (int i = 0; i < receiverListTag.tagCount(); i++) {
            NBTTagCompound t = receiverListTag.getCompoundTagAt(i);
            GlobalPos pos = readGlobalPos(t);
            if (pos != null) this.declaredReceivers.add(pos);
        }
    }

    private static NBTTagCompound writeGlobalPos(GlobalPos pos) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("dim", pos.getDimension());
        tag.setInteger("x", pos.getPos().getX());
        tag.setInteger("y", pos.getPos().getY());
        tag.setInteger("z", pos.getPos().getZ());
        return tag;
    }

    @Nullable
    private static GlobalPos readGlobalPos(NBTTagCompound tag) {
        if (!tag.hasKey("dim") || !tag.hasKey("x") || !tag.hasKey("y") || !tag.hasKey("z")) return null;
        int dim = tag.getInteger("dim");
        BlockPos bp = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
        return new GlobalPos(dim, bp);
    }

    // ---- BandError enum ----

    public enum BandError {
        FINE,                // Working normally
        MISSING_CONTROLLER,  // Missing controller
        CONTROLLER_CONFLICT, // Senders connected to different controller networks
        SENDER_ERROR,        // A sender is in a grid without controller or not ready
        NO_SENDER            // No senders in the network
    }
}
