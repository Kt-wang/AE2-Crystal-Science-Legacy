/*
 * AE2 Crystal Science - EnderBroadcasterActionPacket (1.12.2)
 * Client -> Server actions for the Ender Broadcaster main GUI.
 *
 * Mirrors EnderBroadcasterMenu's client actions:
 *   change_expected_channels / toggle_linker_side / clean_linker_connection
 *   open_frequency_band_menu / open_frequency_band_create_menu / open_frequency_band_manager_menu
 */
package ae2csl.network;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.tile.ender.TileEnderBroadcaster;
import ae2csl.tile.ender.TileEnderBroadcaster.ConnectionType;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class EnderBroadcasterActionPacket implements IMessage {

    public static final byte CHANGE_CHANNELS   = 0;
    public static final byte TOGGLE_SIDE       = 1;
    public static final byte DISCONNECT        = 2;
    public static final byte OPEN_BAND_VIEW    = 3;
    public static final byte OPEN_BAND_CREATE  = 4;
    public static final byte OPEN_BAND_MANAGER = 5;
    public static final byte OPEN_MAIN         = 6;
    public static final byte OPEN_WHITELIST    = 7;

    private BlockPos pos = BlockPos.ORIGIN;
    private byte action;
    private int delta;

    public EnderBroadcasterActionPacket() {}

    public EnderBroadcasterActionPacket(BlockPos pos, byte action) {
        this(pos, action, 0);
    }

    public EnderBroadcasterActionPacket(BlockPos pos, byte action, int delta) {
        this.pos = pos;
        this.action = action;
        this.delta = delta;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        action = buf.readByte();
        delta = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeByte(action);
        buf.writeInt(delta);
    }

    public static class Handler implements IMessageHandler<EnderBroadcasterActionPacket, IMessage> {
        @Override
        public IMessage onMessage(EnderBroadcasterActionPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            ((WorldServer) player.world).addScheduledTask(() -> handle(player, msg));
            return null;
        }

        private static void handle(EntityPlayerMP player, EnderBroadcasterActionPacket msg) {
            TileEntity te = player.world.getTileEntity(msg.pos);
            TileEnderBroadcaster tile = te instanceof TileEnderBroadcaster ? (TileEnderBroadcaster) te : null;

            switch (msg.action) {
                case CHANGE_CHANNELS:
                    if (tile != null) {
                        tile.setExpectedChannels(tile.getExpectedChannels() + msg.delta);
                    }
                    break;
                case TOGGLE_SIDE:
                    if (tile != null) {
                        toggleLinkerSide(tile);
                    }
                    break;
                case DISCONNECT:
                    if (tile != null) {
                        tile.cleanConnectionPermanent();
                    }
                    break;
                case OPEN_BAND_VIEW:
                    player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_FREQUENCY_BAND, player.world,
                            msg.pos.getX(), msg.pos.getY(), msg.pos.getZ());
                    break;
                case OPEN_BAND_CREATE:
                    player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_FREQUENCY_BAND_CREATE, player.world,
                            msg.pos.getX(), msg.pos.getY(), msg.pos.getZ());
                    break;
                case OPEN_BAND_MANAGER:
                    if (tile != null && tile.getBandName() != null && !tile.getBandName().isEmpty()) {
                        player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_FREQUENCY_BAND_MANAGER, player.world,
                                msg.pos.getX(), msg.pos.getY(), msg.pos.getZ());
                    } else {
                        player.sendStatusMessage(new TextComponentTranslation(
                                "ae2csl.msg.broadcaster.sender.not_connect_any_band"), true);
                    }
                    break;
                case OPEN_MAIN:
                    player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_ENDER_BROADCASTER, player.world,
                            msg.pos.getX(), msg.pos.getY(), msg.pos.getZ());
                    break;
                case OPEN_WHITELIST:
                    if (tile != null && tile.getBandName() != null && !tile.getBandName().isEmpty()) {
                        player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_BAND_WHITELIST, player.world,
                                msg.pos.getX(), msg.pos.getY(), msg.pos.getZ());
                    }
                    break;
            }
        }

        /**
         * Toggle between sender and receiver, mirroring EnderBroadcasterMenu.toggleLinkerSideAction().
         */
        private static void toggleLinkerSide(TileEnderBroadcaster tile) {
            String bandId = tile.getBandName();
            if (bandId == null || bandId.isEmpty()) return;
            if (tile.getConnectionType() == ConnectionType.NO_CONNECTION) return;

            // Preserve the band; only flip the side.
            if (tile.getConnectionType() == ConnectionType.AS_RECEIVER) {
                tile.cleanConnectionPermanent();
                tile.connectToBand(bandId, true);
            } else if (tile.getConnectionType() == ConnectionType.AS_SENDER) {
                tile.cleanConnectionPermanent();
                tile.connectToBand(bandId, false);
            }
        }
    }
}
