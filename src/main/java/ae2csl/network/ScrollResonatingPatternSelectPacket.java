/*
 * AE2 Crystal Science - ScrollResonatingPatternSelectPacket (1.12.2)
 */
package ae2csl.network;

import ae2csl.item.IResonatingTargetModeItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ScrollResonatingPatternSelectPacket implements IMessage {

    private boolean next;
    /** 发送时玩家所在的快捷栏槽位（0-8），避免滚轮同时切换物品时服务端取错手持物 */
    private int slot;

    public ScrollResonatingPatternSelectPacket() {}
    public ScrollResonatingPatternSelectPacket(boolean next, int slot) {
        this.next = next;
        this.slot = slot;
    }

    @Override public void fromBytes(ByteBuf buf) {
        next = buf.readBoolean();
        slot = buf.readByte();
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeBoolean(next);
        buf.writeByte(slot);
    }

    public static class Handler implements IMessageHandler<ScrollResonatingPatternSelectPacket, IMessage> {
        @Override
        public IMessage onMessage(ScrollResonatingPatternSelectPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            ((WorldServer) player.world).addScheduledTask(() -> {
                if (msg.slot < 0 || msg.slot >= 9) {
                    return;
                }
                ItemStack stack = player.inventory.getStackInSlot(msg.slot);
                if (!stack.isEmpty() && stack.getItem() instanceof IResonatingTargetModeItem item) {
                    item.scrollSelectedInputAndToast(player, stack, msg.next);
                }
            });
            return null;
        }
    }
}
