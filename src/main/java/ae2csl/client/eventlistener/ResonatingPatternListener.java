/*
 * AE2 Crystal Science - ResonatingPatternListener (1.12.2, client)
 *
 * 潜行 + 滚轮切换谐振样板/链接器/存储卡的槽位。
 *
 * 快捷栏锁定由 InventoryPlayerMixin 在 changeCurrentItem 处直接取消（双方法名、require=0）。
 * 这里保留 tick 级兜底：若 mixin 未生效，则在同 tick 的 END（输入处理之后、渲染之前）恢复滚轮前
 * 的槽位，避免可见闪动；同时把槽位随包发给服务端，保证服务端操作正确的物品。
 */
package ae2csl.client.eventlistener;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.api.ids.AE2CSConstants;
import ae2csl.item.IResonatingTargetModeItem;
import ae2csl.network.ScrollResonatingPatternSelectPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import org.lwjgl.input.Mouse;

@Mod.EventBusSubscriber(modid = AE2CSConstants.MODID, value = Side.CLIENT)
public class ResonatingPatternListener {

    /** 本 tick 开始时的快捷栏槽位（滚轮前的值） */
    private static int tickStartSlot = -1;
    /** 需要在下个 tick 恢复快捷栏槽位 */
    private static boolean restorePending = false;

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        if (!mc.player.isSneaking()) {
            return;
        }

        ItemStack stack = mc.player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof IResonatingTargetModeItem)) {
            return;
        }

        int dw = Mouse.getDWheel();
        if (dw == 0) {
            return;
        }

        int slot = tickStartSlot >= 0 ? tickStartSlot : mc.player.inventory.currentItem;
        boolean next = dw < 0;

        AE2CrystalScienceLegacy.NETWORK.sendToServer(new ScrollResonatingPatternSelectPacket(next, slot));

        // 原版会在本 tick 内把滚轮应用为切槽：记录需要恢复的槽位
        restorePending = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            tickStartSlot = -1;
            restorePending = false;
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            tickStartSlot = mc.player.inventory.currentItem;
            return;
        }

        // END：本 tick 的输入（含原版滚轮切槽）已处理，但帧还没渲染 —— 立即锁回，玩家看不到闪动
        if (restorePending) {
            restorePending = false;
            if (tickStartSlot >= 0 && tickStartSlot < 9
                && mc.player.inventory.currentItem != tickStartSlot) {
                mc.player.inventory.currentItem = tickStartSlot;
            }
        }
    }
}
