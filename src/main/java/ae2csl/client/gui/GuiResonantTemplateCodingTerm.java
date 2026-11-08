/*
 * AE2 Crystal Science - GuiResonantTemplateCodingTerm (1.12.2)
 *
 * 复用 AE2 样板编码终端的界面，并加「编码为谐振样板」开关。
 * 对齐 1.21.1 源码：样式 JSON 定义几何 + widgets.add(自绘图标按钮)，
 * 按钮只画 terminal_icon.png 里的 16x16 图标（普通/谐振两种）。
 */
package ae2csl.client.gui;

import ae2.client.gui.me.patternencode.GuiPatternEncodingTerm;
import ae2.client.gui.style.GuiStyleManager;
import ae2csl.client.gui.widgets.ResonatingEncodingButton;
import ae2csl.container.ContainerResonantTemplateCodingTerm;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

public class GuiResonantTemplateCodingTerm extends GuiPatternEncodingTerm {

    private static final String STYLE_PATH = "/screens/terminals/resonant_coding_terminal.json";
    private static final String WIDGET_ID = "encodeResonatingPattern";

    private final ResonatingEncodingButton encodeResonatingButton;

    public GuiResonantTemplateCodingTerm(ContainerResonantTemplateCodingTerm container,
                                         InventoryPlayer playerInventory) {
        super(container, playerInventory, null, GuiStyleManager.loadStyleDoc(STYLE_PATH));

        this.encodeResonatingButton = new ResonatingEncodingButton(
            () -> menu().toggleEncodeResonating());
        this.encodeResonatingButton.setTooltip(
            List.of(new TextComponentTranslation("ae2csl.gui.resonant_terminal.encode_resonating"),
                    new TextComponentTranslation("ae2csl.gui.resonant_terminal.state.off")),
            List.of(new TextComponentTranslation("ae2csl.gui.resonant_terminal.encode_resonating"),
                    new TextComponentTranslation("ae2csl.gui.resonant_terminal.state.on")));
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
