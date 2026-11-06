/*
 * AE2 Crystal Science - OpenGuiPacket (1.12.2)
 * 客户端请求在服务端打开一个机器 GUI（机器 GUI ↔ 侧配界面互相跳转用，
 * 保证服务端容器与客户端界面成对创建）。
 */
package ae2csl.network;

import ae2csl.AE2CrystalScienceLegacy;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class OpenGuiPacket implements IMessage {

    private BlockPos pos;
    private int guiId;

    public OpenGuiPacket() {}
    public OpenGuiPacket(BlockPos pos, int guiId) { this.pos = pos; this.guiId = guiId; }

    @Override public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        guiId = buf.readInt();
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX()); buf.writeInt(pos.getY()); buf.writeInt(pos.getZ());
        buf.writeInt(guiId);
    }

    public static class Handler implements IMessageHandler<OpenGuiPacket, IMessage> {
        @Override
        public IMessage onMessage(OpenGuiPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            ((WorldServer) player.world).addScheduledTask(() ->
                player.openGui(AE2CrystalScienceLegacy.INSTANCE, msg.guiId,
                    player.world, msg.pos.getX(), msg.pos.getY(), msg.pos.getZ()));
            return null;
        }
    }
}
