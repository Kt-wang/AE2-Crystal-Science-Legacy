/*
 * AE2 Crystal Science - GuiResonantTemplateCodingTerm (1.12.2)
 *
 * 复用 AE2 样板编码终端的界面。谐振编码默认开启，界面不再提供开关，
 * 编码时由容器决定产出「谐振样板」。
 */
package ae2csl.client.gui;

import ae2.client.gui.me.patternencode.GuiPatternEncodingTerm;
import ae2.client.gui.style.GuiStyleManager;
import ae2csl.container.ContainerResonantTemplateCodingTerm;

import net.minecraft.entity.player.InventoryPlayer;

public class GuiResonantTemplateCodingTerm extends GuiPatternEncodingTerm {

    private static final String STYLE_PATH = "/screens/terminals/resonant_coding_terminal.json";

    public GuiResonantTemplateCodingTerm(ContainerResonantTemplateCodingTerm container,
                                         InventoryPlayer playerInventory) {
        super(container, playerInventory, null, GuiStyleManager.loadStyleDoc(STYLE_PATH));
    }
}
