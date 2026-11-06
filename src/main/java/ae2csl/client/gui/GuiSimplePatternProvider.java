/*
 * AE2 Crystal Science - GuiSimplePatternProvider (1.12.2)
 * 简易（初级）样板供应器 GUI。
 * 复用 fork GuiPatternProvider 的全部功能，仅修正当其翻页重定位把样板槽
 * 硬编码到 (8+,42+) 的问题——本界面布局是 44,45 横向 5 槽（源码样式），
 * 在每帧刷新后重新应用样式坐标。
 */
package ae2csl.client.gui;

import ae2.client.gui.implementations.GuiPatternProvider;
import ae2.client.gui.style.GuiStyleManager;
import ae2.container.SlotSemantics;
import ae2.container.implementations.ContainerPatternProvider;

import net.minecraft.entity.player.InventoryPlayer;

public class GuiSimplePatternProvider extends GuiPatternProvider {

    private static final String STYLE_PATH = "/screens/simple_pattern_provider_menu.json";

    public GuiSimplePatternProvider(ContainerPatternProvider container, InventoryPlayer playerInventory) {
        super(container, playerInventory, null, GuiStyleManager.loadStyleDoc(STYLE_PATH));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        // fork 的 repositionPatternPageSlots 把样板槽硬编码到 (8+,42+)，与本界面(44,45)不符，
        // 重新应用样式 JSON 中的槽位坐标
        repositionSlots(SlotSemantics.ENCODED_PATTERN);
    }
}
