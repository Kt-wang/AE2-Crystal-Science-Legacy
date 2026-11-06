/*
 * AE2 Crystal Science - EntropyModeTogglePacket (1.12.2)
 * Toggles the entropy mode (HEAT/COOL) of an Entropy Variation Chamber.
 */
package ae2csl.network;

import ae2csl.tile.entropy.TileEntropyVariationChamber;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class EntropyModeTogglePacket implements IMessage {

    private BlockPos pos;

    public EntropyModeTogglePacket() {}
    public EntropyModeTogglePacket(BlockPos pos) { this.pos = pos; }

    @Override public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX()); buf.writeInt(pos.getY()); buf.writeInt(pos.getZ());
    }

    public static class Handler implements IMessageHandler<EntropyModeTogglePacket, IMessage> {
        @Override
        public IMessage onMessage(EntropyModeTogglePacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            ((WorldServer) player.world).addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(msg.pos);
                if (te instanceof TileEntropyVariationChamber) {
                    ((TileEntropyVariationChamber) te).toggleMode();
                }
            });
            return null;
        }
    }
}
