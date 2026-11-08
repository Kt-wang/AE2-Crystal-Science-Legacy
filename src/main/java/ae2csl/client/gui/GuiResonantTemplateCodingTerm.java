/*
 * AE2 Crystal Science - GuiResonantTemplateCodingTerm (1.12.2)
 *
 * 复用 AE2 样板编码终端的界面（编码网格 / 样板槽 / 模式切换 / 自动填充），
 * 并在左侧工具栏加一个「编码为谐振样板」开关按钮。
 */
package ae2csl.client.gui;

import ae2.client.gui.Icon;
import ae2.client.gui.me.patternencode.GuiPatternEncodingTerm;
import ae2.client.gui.style.GuiStyleManager;
import ae2csl.client.gui.icon.AdaptedAE2Icon;
import ae2csl.client.gui.widgets.AE2CSIconButton;
import ae2csl.container.ContainerResonantTemplateCodingTerm;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.io.IOException;
import java.util.Arrays;

public class GuiResonantTemplateCodingTerm extends GuiPatternEncodingTerm {

    private static final String STYLE_PATH = "/screens/terminals/pattern_encoding_terminal.json";
    private static final int BTN_ENCODE_RESONATING = 100;

    private AE2CSIconButton encodeResonatingButton;

    public GuiResonantTemplateCodingTerm(ContainerResonantTemplateCodingTerm container,
                                         InventoryPlayer playerInventory) {
        super(container, playerInventory, null, GuiStyleManager.loadStyleDoc(STYLE_PATH));
    }

    private ContainerResonantTemplateCodingTerm menu() {
        return (ContainerResonantTemplateCodingTerm) getContainer();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.encodeResonatingButton = new AE2CSIconButton(BTN_ENCODE_RESONATING,
            AE2CSToolbar.iconButtonX(guiLeft), AE2CSToolbar.buttonY(guiTop, 0),
            new AdaptedAE2Icon(Icon.PATTERN_UPLOAD), buildTooltip());
        this.buttonList.add(this.encodeResonatingButton);
    }

    private java.util.List<String> buildTooltip() {
        boolean on = menu().encodeResonating;
        return Arrays.asList(
            I18n.format("ae2csl.gui.resonant_terminal.encode_resonating"),
            I18n.format(on ? "ae2csl.gui.resonant_terminal.state.on"
                           : "ae2csl.gui.resonant_terminal.state.off"));
    }

    @Override
    protected void actionPerformed(net.minecraft.client.gui.GuiButton button) throws IOException {
        if (button.id == BTN_ENCODE_RESONATING) {
            menu().toggleEncodeResonating();
            if (this.encodeResonatingButton != null) {
                this.encodeResonatingButton.setTooltip(buildTooltip());
                this.encodeResonatingButton.setArmed(menu().encodeResonating);
            }
            return;
        }
        super.actionPerformed(button);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.encodeResonatingButton != null) {
            this.encodeResonatingButton.setTooltip(buildTooltip());
            this.encodeResonatingButton.setArmed(menu().encodeResonating);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        // 图标按钮工具提示
        for (net.minecraft.client.gui.GuiButton btn : this.buttonList) {
            if (btn instanceof AE2CSIconButton && btn.visible && btn.isMouseOver()) {
                java.util.List<String> tooltip = ((AE2CSIconButton) btn).getTooltip();
                if (tooltip != null && !tooltip.isEmpty()) {
                    drawHoveringText(tooltip, mouseX, mouseY);
                    break;
                }
            }
        }
    }
}
