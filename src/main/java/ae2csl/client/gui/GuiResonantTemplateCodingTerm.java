/*
 * AE2 Crystal Science - GuiResonantTemplateCodingTerm (1.12.2)
 *
 * 复用 AE2 样板编码终端的界面（编码网格 / 样板槽 / 模式切换 / 自动填充），
 * 外观暂时沿用 AE2 的编码终端样式；本模组的额外开关在后续阶段接入。
 */
package ae2csl.client.gui;

import ae2.client.gui.me.patternencode.GuiPatternEncodingTerm;
import ae2.client.gui.style.GuiStyleManager;
import ae2csl.container.ContainerResonantTemplateCodingTerm;

import net.minecraft.entity.player.InventoryPlayer;

public class GuiResonantTemplateCodingTerm extends GuiPatternEncodingTerm {

    private static final String STYLE_PATH = "/screens/terminals/pattern_encoding_terminal.json";

    public GuiResonantTemplateCodingTerm(ContainerResonantTemplateCodingTerm container,
                                         InventoryPlayer playerInventory) {
        super(container, playerInventory, null, GuiStyleManager.loadStyleDoc(STYLE_PATH));
    }
}
