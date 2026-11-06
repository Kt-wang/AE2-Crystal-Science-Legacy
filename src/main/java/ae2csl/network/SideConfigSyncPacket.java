/*
 * AE2 Crystal Science - SideConfigSyncPacket (1.12.2)
 * 服务端 → 客户端：同步机器侧面配置位掩码（打开侧配界面时 + 每次操作后）。
 */
package ae2csl.network;

import ae2csl.machine.ISideConfigHost;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class SideConfigSyncPacket implements IMessage {

    private BlockPos pos;
    private int mask;

    public SideConfigSyncPacket() {}
    public SideConfigSyncPacket(BlockPos pos, int mask) { this.pos = pos; this.mask = mask; }

    @Override public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        mask = buf.readInt();
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX()); buf.writeInt(pos.getY()); buf.writeInt(pos.getZ());
        buf.writeInt(mask);
    }

    public static class Handler implements IMessageHandler<SideConfigSyncPacket, IMessage> {
        @Override
        public IMessage onMessage(SideConfigSyncPacket msg, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> apply(msg));
            return null;
        }

        @SideOnly(Side.CLIENT)
        private static void apply(SideConfigSyncPacket msg) {
            if (Minecraft.getMinecraft().world == null) return;
            TileEntity te = Minecraft.getMinecraft().world.getTileEntity(msg.pos);
            if (te instanceof ISideConfigHost) {
                ((ISideConfigHost) te).getSideConfig().decode(msg.mask);
            }
        }
    }
}
