/*
 * AE2 Crystal Science - EnderEmitterActionPacket (1.12.2)
 * Client -> Server actions for the Ender Emitter main GUI and its band sub-GUIs.
 *
 * Mirrors EnderEmitterMenu's client actions:
 *   change_distance / try_scan_all / destroy_all / open_band_menu / clean_band_connection
 * plus the setting toggles and the band link flow.
 */
package ae2csl.network;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.api.linker.broadcast.BroadcastFrequencyBand;
import ae2csl.api.linker.broadcast.FrequencyBandManager;
import ae2csl.client.gui.GuiHandler;
import ae2csl.tile.ender.TileEnderEmitter;

import ae2.api.networking.IGrid;
import ae2.api.networking.pathing.ControllerState;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class EnderEmitterActionPacket implements IMessage {

    public static final byte CHANGE_DISTANCE        = 0;  // delta
    public static final byte SCAN_ALL               = 1;
    public static final byte DESTROY_ALL            = 2;
    public static final byte OPEN_BAND_MENU         = 3;
    public static final byte CLEAN_BAND_CONNECTION  = 4;
    public static final byte TOGGLE_AUTO_MODE       = 5;
    public static final byte TOGGLE_SHOW_RANGE      = 6;
    public static final byte TOGGLE_AUTO_LINK_CABLE = 7;
    public static final byte OPEN_LINK_MENU         = 8;  // a = band name
    public static final byte CONNECT_BAND           = 9;  // a = band name, b = password

    private BlockPos pos = BlockPos.ORIGIN;
    private byte action;
    private int delta;
    private String a = "";
    private String b = "";

    public EnderEmitterActionPacket() {}

    public EnderEmitterActionPacket(BlockPos pos, byte action) {
        this(pos, action, 0, "", "");
    }

    public EnderEmitterActionPacket(BlockPos pos, byte action, int delta) {
        this(pos, action, delta, "", "");
    }

    public EnderEmitterActionPacket(BlockPos pos, byte action, String a) {
        this(pos, action, 0, a, "");
    }

    public EnderEmitterActionPacket(BlockPos pos, byte action, String a, String b) {
        this(pos, action, 0, a, b);
    }

    private EnderEmitterActionPacket(BlockPos pos, byte action, int delta, String a, String b) {
        this.pos = pos;
        this.action = action;
        this.delta = delta;
        this.a = a != null ? a : "";
        this.b = b != null ? b : "";
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        action = buf.readByte();
        delta = buf.readInt();
        a = readStr(buf);
        b = readStr(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeByte(action);
        buf.writeInt(delta);
        writeStr(buf, a);
        writeStr(buf, b);
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

    public static class Handler implements IMessageHandler<EnderEmitterActionPacket, IMessage> {
        @Override
        public IMessage onMessage(EnderEmitterActionPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            ((WorldServer) player.world).addScheduledTask(() -> handle(player, msg));
            return null;
        }

        private static void handle(EntityPlayerMP player, EnderEmitterActionPacket msg) {
            TileEntity te = player.world.getTileEntity(msg.pos);
            TileEnderEmitter tile = te instanceof TileEnderEmitter ? (TileEnderEmitter) te : null;

            switch (msg.action) {
                case CHANGE_DISTANCE:
                    if (tile != null) tile.setLinkDistance(tile.getLinkDistance() + msg.delta);
                    break;
                case SCAN_ALL:
                    if (tile != null) TileEnderEmitter.addAllRecentBEtoEmitter(tile);
                    break;
                case DESTROY_ALL:
                    if (tile != null) TileEnderEmitter.removeAllLinkedFromEmitter(tile);
                    break;
                case OPEN_BAND_MENU:
                    player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_ENDER_EMITTER_FREQUENCY_BAND, player.world,
                            msg.pos.getX(), msg.pos.getY(), msg.pos.getZ());
                    break;
                case OPEN_LINK_MENU:
                    player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_ENDER_EMITTER_FREQUENCY_BAND_LINK, player.world,
                            msg.pos.getX(), msg.pos.getY(), msg.pos.getZ());
                    break;
                case CLEAN_BAND_CONNECTION:
                    if (tile != null) tile.cleanConnectionPermanent();
                    break;
                case TOGGLE_AUTO_MODE:
                    if (tile != null) tile.toggleAutoMode();
                    break;
                case TOGGLE_SHOW_RANGE:
                    if (tile != null) tile.toggleShowRange();
                    break;
                case TOGGLE_AUTO_LINK_CABLE:
                    if (tile != null) tile.toggleAutoLinkCableLike();
                    break;
                case CONNECT_BAND:
                    if (tile != null) connectBand(player, tile, msg.a, msg.b);
                    break;
            }
        }

        private static void connectBand(EntityPlayerMP player, TileEnderEmitter tile, String bandName, String password) {
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
            if (hostGrid != null && hostGrid.getPathingService().getControllerState() == ControllerState.CONTROLLER_ONLINE
                    && hostGrid != bandGrid) {
                player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.band_link.receiver.grid_conflict"), true);
                return;
            }

            tile.connectToBand(bandName);
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_ENDER_EMITTER, player.world,
                    tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
        }
    }
}
