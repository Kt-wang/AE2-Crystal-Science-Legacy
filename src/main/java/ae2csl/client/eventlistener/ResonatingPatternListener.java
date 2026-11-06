/*
 * AE2 Crystal Science - ResonatingPatternListener (1.12.2, client)
 *
 * 潜行 + 滚轮切换谐振样板/链接器/存储卡的槽位。
 *
 * 从 Forge 的鼠标事件读取本次滚轮增量，并取消原版快捷栏切换。
 * 不查询或消费底层输入实现的全局滚轮状态，兼容 Cleanroom 的不同输入后端。
 */
package ae2csl.client.eventlistener;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.api.ids.AE2CSConstants;
import ae2csl.item.IResonatingTargetModeItem;
import ae2csl.network.ScrollResonatingPatternSelectPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = AE2CSConstants.MODID, value = Side.CLIENT)
public class ResonatingPatternListener {

    @SubscribeEvent
    public static void onMouseScroll(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || mc.currentScreen != null) {
            return;
        }
        if (!mc.player.isSneaking()) {
            return;
        }

        ItemStack stack = mc.player.getHeldItemMainhand();
        if (!(stack.getItem() instanceof IResonatingTargetModeItem)) {
            return;
        }

        int dw = event.getDwheel();
        if (dw == 0) {
            return;
        }

        int slot = mc.player.inventory.currentItem;
        boolean next = dw < 0;

        AE2CrystalScienceLegacy.NETWORK.sendToServer(new ScrollResonatingPatternSelectPacket(next, slot));

        event.setCanceled(true);
    }
}
