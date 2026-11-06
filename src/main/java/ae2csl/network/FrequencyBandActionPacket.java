/*
 * AE2 Crystal Science - FrequencyBandActionPacket (1.12.2)
 * Client -> Server actions for the Ender Broadcaster sub-GUIs (band list / create / link /
 * manager / whitelist) plus pull-style data requests.
 *
 * Mirrors FrequencyBandMenu / FrequencyBandCreateMenu / FrequencyBandLinkMenu /
 * FrequencyBandManagerMenu / BandWhiteListManagerMenu server handlers.
 */
package ae2csl.network;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.api.linker.broadcast.BroadcastFrequencyBand;
import ae2csl.api.linker.broadcast.FrequencyBandManager;
import ae2csl.api.linker.broadcast.networking.BroadcastBandsField;
import ae2csl.api.linker.broadcast.networking.FrequencyBandDetailInfo;
import ae2csl.api.networking.ServerPlayerInfo;
import ae2csl.api.util.GlobalPos;
import ae2csl.client.gui.GuiHandler;
import ae2csl.tile.ender.TileEnderBroadcaster;
import ae2csl.util.ServerUtil;

import ae2.api.networking.IGrid;
import ae2.api.networking.pathing.ControllerState;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class FrequencyBandActionPacket implements IMessage {

    public static final byte TRY_CONNECT          = 0;  // a = band name
    public static final byte CREATE               = 1;  // a=name b=password f1=public f2=allowMemCard
    public static final byte LINK                 = 2;  // a=bandName b=password f1=asSender
    public static final byte CHANGE_PASSWORD      = 3;  // a = new password
    public static final byte CHANGE_PUBLIC        = 4;  // f1 = public
    public static final byte CHANGE_MEMORY_CARD   = 5;  // f1 = allow
    public static final byte DISCONNECT_BROADCASTER = 6; // n=dim, target=pos
    public static final byte TAP_BROADCASTER      = 7;  // n=dim, target=pos
    public static final byte REMOVE_BAND          = 8;
    public static final byte WHITELIST_TOGGLE     = 9;  // uuid
    public static final byte REQUEST_BANDS_LIST   = 10;
    public static final byte REQUEST_BAND_DETAIL  = 11;
    public static final byte REQUEST_WHITELIST    = 12;

    private BlockPos pos = BlockPos.ORIGIN;
    private byte action;
    private String a = "";
    private String b = "";
    private boolean f1;
    private boolean f2;
    private int n;               // dimension for disconnect/tap
    private BlockPos target = BlockPos.ORIGIN;
    private UUID uuid;

    public FrequencyBandActionPacket() {}

    public FrequencyBandActionPacket(BlockPos pos, byte action) {
        this.pos = pos;
        this.action = action;
    }

    public static FrequencyBandActionPacket withStrings(BlockPos pos, byte action, String a, String b) {
        FrequencyBandActionPacket p = new FrequencyBandActionPacket(pos, action);
        p.a = a != null ? a : "";
        p.b = b != null ? b : "";
        return p;
    }

    public static FrequencyBandActionPacket withLink(BlockPos pos, String band, String password, boolean asSender) {
        FrequencyBandActionPacket p = new FrequencyBandActionPacket(pos, LINK);
        p.a = band != null ? band : "";
        p.b = password != null ? password : "";
        p.f1 = asSender;
        return p;
    }

    public static FrequencyBandActionPacket withCreate(BlockPos pos, String name, String password, boolean isPublic, boolean allowMem) {
        FrequencyBandActionPacket p = new FrequencyBandActionPacket(pos, CREATE);
        p.a = name != null ? name : "";
        p.b = password != null ? password : "";
        p.f1 = isPublic;
        p.f2 = allowMem;
        return p;
    }

    public static FrequencyBandActionPacket withFlag(BlockPos pos, byte action, boolean flag) {
        FrequencyBandActionPacket p = new FrequencyBandActionPacket(pos, action);
        p.f1 = flag;
        return p;
    }

    public static FrequencyBandActionPacket withTarget(BlockPos pos, byte action, int dim, BlockPos target) {
        FrequencyBandActionPacket p = new FrequencyBandActionPacket(pos, action);
        p.n = dim;
        p.target = target;
        return p;
    }

    public static FrequencyBandActionPacket withUuid(BlockPos pos, byte action, UUID uuid) {
        FrequencyBandActionPacket p = new FrequencyBandActionPacket(pos, action);
        p.uuid = uuid;
        return p;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        action = buf.readByte();
        a = readStr(buf);
        b = readStr(buf);
        f1 = buf.readBoolean();
        f2 = buf.readBoolean();
        n = buf.readInt();
        target = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        if (buf.readBoolean()) uuid = new UUID(buf.readLong(), buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeByte(action);
        writeStr(buf, a);
        writeStr(buf, b);
        buf.writeBoolean(f1);
        buf.writeBoolean(f2);
        buf.writeInt(n);
        buf.writeInt(target.getX());
        buf.writeInt(target.getY());
        buf.writeInt(target.getZ());
        buf.writeBoolean(uuid != null);
        if (uuid != null) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    }

    private static void writeStr(ByteBuf buf, String s) {
        buf.writeShort(s != null ? s.length() : 0);
        if (s != null && !s.isEmpty()) {
            for (char c : s.toCharArray()) buf.writeChar(c);
        }
    }

    private static String readStr(ByteBuf buf) {
        int len = buf.readShort();
        if (len <= 0) return "";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(buf.readChar());
        return sb.toString();
    }

    public static class Handler implements IMessageHandler<FrequencyBandActionPacket, IMessage> {
        @Override
        public IMessage onMessage(FrequencyBandActionPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            ((WorldServer) player.world).addScheduledTask(() -> handle(player, msg));
            return null;
        }

        private static void handle(EntityPlayerMP player, FrequencyBandActionPacket msg) {
            TileEntity te = player.world.getTileEntity(msg.pos);
            TileEnderBroadcaster tile = te instanceof TileEnderBroadcaster ? (TileEnderBroadcaster) te : null;

            switch (msg.action) {
                case TRY_CONNECT: {
                    BroadcastFrequencyBand band = FrequencyBandManager.getBand(msg.a);
                    if (band == null) break;
                    player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_FREQUENCY_BAND_LINK, player.world,
                            msg.pos.getX(), msg.pos.getY(), msg.pos.getZ());
                    break;
                }
                case CREATE: {
                    if (msg.a.isEmpty()) {
                        player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.band_create.no_name"), true);
                        break;
                    }
                    if (FrequencyBandManager.isBandPresent(msg.a)) {
                        player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.band_create.band_present"), true);
                        break;
                    }
                    if (FrequencyBandManager.tryCreateBand(msg.a, msg.b, player.getUniqueID(), msg.f1, msg.f2) != null) {
                        player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.band_create.success"), true);
                        player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_ENDER_BROADCASTER, player.world,
                                msg.pos.getX(), msg.pos.getY(), msg.pos.getZ());
                    }
                    break;
                }
                case LINK: {
                    if (tile == null) break;
                    linkToBand(player, tile, msg.a, msg.b, msg.f1, msg.pos);
                    break;
                }
                case CHANGE_PASSWORD:
                    if (tile != null) changePassword(player, tile, msg.a);
                    break;
                case CHANGE_PUBLIC:
                    if (tile != null) changePublic(player, tile, msg.f1);
                    break;
                case CHANGE_MEMORY_CARD:
                    if (tile != null) changeMemoryCard(player, tile, msg.f1);
                    break;
                case DISCONNECT_BROADCASTER:
                    applyToBroadcaster(player, msg.n, msg.target, true);
                    break;
                case TAP_BROADCASTER:
                    applyToBroadcaster(player, msg.n, msg.target, false);
                    break;
                case REMOVE_BAND:
                    if (tile != null) removeBand(player, tile);
                    break;
                case WHITELIST_TOGGLE:
                    if (tile != null) toggleWhitelist(player, tile, msg.uuid);
                    break;
                case REQUEST_BANDS_LIST: {
                    BroadcastBandsField field = FrequencyBandManager.getBandsInfoByPlayer(player);
                    if (field != null) {
                        AE2CrystalScienceLegacy.NETWORK.sendTo(
                                FrequencyBandSyncPacket.bands(field), player);
                    }
                    break;
                }
                case REQUEST_BAND_DETAIL: {
                    if (tile == null) break;
                    BroadcastFrequencyBand band = FrequencyBandManager.getBand(tile.getBandName());
                    if (band == null) {
                        // The broadcaster is no longer linked to any band (e.g. it was just
                        // disconnected from this very menu). Send an empty detail so the client
                        // clears its stale sender/receiver list instead of keeping old entries.
                        FrequencyBandDetailInfo empty = new FrequencyBandDetailInfo("", false, false, false,
                                BroadcastFrequencyBand.BandError.NO_SENDER,
                                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                        AE2CrystalScienceLegacy.NETWORK.sendTo(FrequencyBandSyncPacket.detail(empty, 0, 0), player);
                        break;
                    }
                    List<GlobalPos> senders = new ArrayList<>(band.getDeclaredSenders());
                    List<GlobalPos> receivers = new ArrayList<>(band.getDeclaredReceivers());
                    FrequencyBandDetailInfo detail = new FrequencyBandDetailInfo(
                            band.getName(),
                            !band.getPassword().isEmpty(),
                            band.isPublic(),
                            band.isAllowedMemoryCardCopy(),
                            band.getErrorState(),
                            new ArrayList<>(),
                            senders,
                            receivers);
                    AE2CrystalScienceLegacy.NETWORK.sendTo(
                            FrequencyBandSyncPacket.detail(detail, band.getUsableChannels(), band.getUsedChannels()),
                            player);
                    break;
                }
                case REQUEST_WHITELIST: {
                    if (tile == null) break;
                    BroadcastFrequencyBand band = FrequencyBandManager.getBand(tile.getBandName());
                    if (band == null) break;
                    MinecraftServer server = player.getServer();
                    if (server == null) break;

                    LinkedHashMap<UUID, String> wlMap = new LinkedHashMap<>();
                    for (UUID id : band.getWhiteList()) {
                        wlMap.put(id, ServerUtil.getPlayerNameByUUID(id, server));
                    }
                    LinkedHashMap<UUID, String> otherMap = new LinkedHashMap<>();
                    UUID owner = band.getOwner();
                    for (EntityPlayerMP sp : server.getPlayerList().getPlayers()) {
                        UUID id = sp.getUniqueID();
                        if (id.equals(owner)) continue;
                        if (band.getWhiteList().contains(id)) continue;
                        otherMap.put(id, sp.getGameProfile().getName());
                    }
                    AE2CrystalScienceLegacy.NETWORK.sendTo(
                            FrequencyBandSyncPacket.whitelist(
                                    new ServerPlayerInfo(wlMap), new ServerPlayerInfo(otherMap)),
                            player);
                    break;
                }
            }
        }

        private static void linkToBand(EntityPlayerMP player, TileEnderBroadcaster tile, String bandName,
                                       String password, boolean asSender, BlockPos pos) {
            if (bandName == null || bandName.isEmpty()) return;
            BroadcastFrequencyBand band = FrequencyBandManager.getBand(bandName);
            if (band == null) return;

            boolean permissionValid = band.validWhiteList(player.getUniqueID()) || band.validPassword(password);
            if (!permissionValid) {
                player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.band_link.password_error"), true);
                return;
            }

            IGrid bandGrid = band.getBindGrid();
            IGrid hostGrid = tile.getMainNode().getGrid();

            if (asSender) {
                if (hostGrid == null) return;
                if (hostGrid.getPathingService().getControllerState() != ControllerState.CONTROLLER_ONLINE) {
                    player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.band_link.sender.grid_must_has_control"), true);
                    return;
                }
                if (bandGrid != null && bandGrid != hostGrid) {
                    player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.band_link.sender.grid_conflict"), true);
                    return;
                }
            } else {
                if (hostGrid != null && hostGrid.getPathingService().getControllerState() == ControllerState.CONTROLLER_ONLINE
                        && hostGrid != bandGrid) {
                    player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.band_link.receiver.grid_conflict"), true);
                    return;
                }
            }

            tile.connectToBand(bandName, asSender);
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_ENDER_BROADCASTER, player.world,
                    pos.getX(), pos.getY(), pos.getZ());
        }

        private static void changePassword(EntityPlayerMP player, TileEnderBroadcaster tile, String newPassword) {
            BroadcastFrequencyBand band = FrequencyBandManager.getBand(tile.getBandName());
            if (band == null) return;
            if (band.getOwner().equals(player.getUniqueID())) {
                band.setPassword(newPassword != null ? newPassword : "");
                player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.frequency_manager.password_changed"), true);
            } else {
                player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.frequency_manager.you_not_owner"), true);
            }
        }

        private static void changePublic(EntityPlayerMP player, TileEnderBroadcaster tile, boolean publicMode) {
            BroadcastFrequencyBand band = FrequencyBandManager.getBand(tile.getBandName());
            if (band == null) return;
            if (band.getOwner().equals(player.getUniqueID())) {
                band.setPublic(publicMode);
            } else {
                player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.frequency_manager.you_not_owner"), true);
            }
        }

        private static void changeMemoryCard(EntityPlayerMP player, TileEnderBroadcaster tile, boolean allow) {
            BroadcastFrequencyBand band = FrequencyBandManager.getBand(tile.getBandName());
            if (band == null) return;
            if (band.getOwner().equals(player.getUniqueID())) {
                band.setAllowedMemoryCardCopy(allow);
            } else {
                player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.frequency_manager.you_not_owner"), true);
            }
        }

        private static void removeBand(EntityPlayerMP player, TileEnderBroadcaster tile) {
            BroadcastFrequencyBand band = FrequencyBandManager.getBand(tile.getBandName());
            if (band == null) return;
            if (band.getOwner().equals(player.getUniqueID())) {
                player.closeContainer();
                FrequencyBandManager.deleteBand(band.getName());
            } else {
                player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.frequency_manager.you_not_owner"), true);
            }
        }

        private static void toggleWhitelist(EntityPlayerMP player, TileEnderBroadcaster tile, UUID target) {
            if (target == null) return;
            BroadcastFrequencyBand band = FrequencyBandManager.getBand(tile.getBandName());
            if (band == null) return;
            UUID owner = band.getOwner();
            if (!owner.equals(player.getUniqueID())) {
                player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.frequency_manager.you_not_owner"), true);
                return;
            }
            if (owner.equals(target)) return;
            if (band.validWhiteList(target)) {
                band.removeFromWhiteList(target);
            } else if (player.getServer() != null && player.getServer().getPlayerList().getPlayerByUUID(target) != null) {
                band.addToWhiteList(target);
            }
        }

        private static void applyToBroadcaster(EntityPlayerMP player, int dim, BlockPos target, boolean disconnect) {
            WorldServer level = DimensionManager.getWorld(dim);
            if (level == null || target == null) return;
            if (!level.isBlockLoaded(target)) {
                level.getChunkProvider().provideChunk(target.getX() >> 4, target.getZ() >> 4);
            }
            TileEntity te = level.getTileEntity(target);
            if (!(te instanceof TileEnderBroadcaster)) return;
            TileEnderBroadcaster be = (TileEnderBroadcaster) te;

            if (disconnect) {
                be.cleanConnectionPermanent();
            } else {
                if (player.getServer() == null || !player.getServer().getPlayerList().canSendCommands(player.getGameProfile())) {
                    return;
                }
                player.closeContainer();
                player.connection.setPlayerLocation(target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                        player.rotationYaw, player.rotationPitch);
            }
        }
    }
}
