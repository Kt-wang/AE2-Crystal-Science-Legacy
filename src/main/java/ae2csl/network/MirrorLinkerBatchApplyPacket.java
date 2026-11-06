/*
 * AE2 Crystal Science - MirrorLinkerBatchApplyPacket (1.12.2)
 */
package ae2csl.network;

import ae2csl.item.linker.MirrorLinkerItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MirrorLinkerBatchApplyPacket implements IMessage {

    private BlockPos pos;
    private boolean mainHand;
    private double hitX, hitY, hitZ;

    public MirrorLinkerBatchApplyPacket() {}

    public MirrorLinkerBatchApplyPacket(BlockPos pos, boolean mainHand, Vec3d hitVec) {
        this.pos = pos; this.mainHand = mainHand;
        this.hitX = hitVec.x; this.hitY = hitVec.y; this.hitZ = hitVec.z;
    }

    @Override public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        mainHand = buf.readBoolean();
        hitX = buf.readDouble(); hitY = buf.readDouble(); hitZ = buf.readDouble();
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX()); buf.writeInt(pos.getY()); buf.writeInt(pos.getZ());
        buf.writeBoolean(mainHand);
        buf.writeDouble(hitX); buf.writeDouble(hitY); buf.writeDouble(hitZ);
    }

    public static class Handler implements IMessageHandler<MirrorLinkerBatchApplyPacket, IMessage> {
        @Override
        public IMessage onMessage(MirrorLinkerBatchApplyPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            ((WorldServer) player.world).addScheduledTask(() -> {
                EnumHand hand = msg.mainHand ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
                ItemStack stack = player.getHeldItem(hand);
                if (stack.getItem() instanceof MirrorLinkerItem) {
                    MirrorLinkerItem.applyStoredTargetToCluster(stack, player, player.world, msg.pos,
                            new Vec3d(msg.hitX, msg.hitY, msg.hitZ));
                }
            });
            return null;
        }
    }
}
