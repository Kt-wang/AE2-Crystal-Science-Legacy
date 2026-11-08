/*
 * AE2 Crystal Science - GuiResonantTemplateCodingTerm (1.12.2)
 *
 * 复用 AE2 样板编码终端的界面，并加一个「编码为谐振样板」开关。
 * 做法对齐 1.21.1 源码：样式 JSON 定义按钮几何 + widgets.add(ToggleButton)，
 * ToggleButton 只画 16x16 图标（没有底板），和 AE2 自己的小开关一致。
 */
package ae2csl.client.gui;

import ae2.client.gui.Icon;
import ae2.client.gui.me.patternencode.GuiPatternEncodingTerm;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.ToggleButton;
import ae2csl.container.ContainerResonantTemplateCodingTerm;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

public class GuiResonantTemplateCodingTerm extends GuiPatternEncodingTerm {

    private static final String STYLE_PATH = "/screens/terminals/resonant_coding_terminal.json";
    private static final String WIDGET_ID = "encodeResonatingPattern";

    private final ToggleButton encodeResonatingButton;

    public GuiResonantTemplateCodingTerm(ContainerResonantTemplateCodingTerm container,
                                         InventoryPlayer playerInventory) {
        super(container, playerInventory, null, GuiStyleManager.loadStyleDoc(STYLE_PATH));

        this.encodeResonatingButton = new ToggleButton(Icon.S_PROCESSOR, Icon.S_CRAFT,
            state -> menu().toggleEncodeResonating());
        this.encodeResonatingButton.setTooltipOn(List.of(
            new TextComponentTranslation("ae2csl.gui.resonant_terminal.encode_resonating"),
            new TextComponentTranslation("ae2csl.gui.resonant_terminal.state.on")));
        this.encodeResonatingButton.setTooltipOff(List.of(
            new TextComponentTranslation("ae2csl.gui.resonant_terminal.encode_resonating"),
            new TextComponentTranslation("ae2csl.gui.resonant_terminal.state.off")));
        this.widgets.add(WIDGET_ID, this.encodeResonatingButton);
    }

    private ContainerResonantTemplateCodingTerm menu() {
        return (ContainerResonantTemplateCodingTerm) getContainer();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.encodeResonatingButton.setState(menu().encodeResonating);
    }
}
