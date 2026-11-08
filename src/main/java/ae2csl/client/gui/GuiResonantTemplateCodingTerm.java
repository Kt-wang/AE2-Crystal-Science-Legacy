/*
 * AE2 Crystal Science - GuiResonantTemplateCodingTerm (1.12.2)
 *
 * 复用 AE2 样板编码终端的界面（编码网格 / 样板槽 / 模式切换 / 自动填充），
 * 并在面板左侧自绘一个「编码为谐振样板」开关：
 * AE2 的 ME 界面不使用 vanilla buttonList 渲染，因此这里直接画 + 自己处理点击。
 */
package ae2csl.client.gui;

import ae2.client.gui.Icon;
import ae2.client.gui.me.patternencode.GuiPatternEncodingTerm;
import ae2.client.gui.style.GuiStyleManager;
import ae2csl.container.ContainerResonantTemplateCodingTerm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GuiResonantTemplateCodingTerm extends GuiPatternEncodingTerm {

    private static final String STYLE_PATH = "/screens/terminals/pattern_encoding_terminal.json";
    private static final ResourceLocation BUTTON_BG =
        new ResourceLocation("ae2", "textures/guis/icons/toolbar_button_background.png");
    private static final ResourceLocation BUTTON_BG_HOVER =
        new ResourceLocation("ae2", "textures/guis/icons/toolbar_button_background_hover.png");

    /**
     * 按钮位置：与 AE2 自己的「清空样板 / 替换开关」同一行（样式里 bottom: 159），
     * 放在它们右侧的空白处。bottom 以 GUI 底边为基准，随终端高度自适应。
     */
    private static final int BUTTON_LEFT = 100;
    private static final int BUTTON_BOTTOM = 159;
    private static final int BUTTON_SIZE = 16;

    private int buttonX() {
        return BUTTON_LEFT;
    }

    private int buttonY() {
        return this.ySize - BUTTON_BOTTOM;
    }

    public GuiResonantTemplateCodingTerm(ContainerResonantTemplateCodingTerm container,
                                         InventoryPlayer playerInventory) {
        super(container, playerInventory, null, GuiStyleManager.loadStyleDoc(STYLE_PATH));
    }

    private ContainerResonantTemplateCodingTerm menu() {
        return (ContainerResonantTemplateCodingTerm) getContainer();
    }

    private boolean isOverButton(int mouseX, int mouseY) {
        int x = guiLeft + buttonX();
        int y = guiTop + buttonY();
        return mouseX >= x && mouseX < x + BUTTON_SIZE && mouseY >= y && mouseY < y + BUTTON_SIZE;
    }

    private List<String> buildTooltip() {
        return Arrays.asList(
            I18n.format("ae2csl.gui.resonant_terminal.encode_resonating"),
            I18n.format(menu().encodeResonating
                ? "ae2csl.gui.resonant_terminal.state.on"
                : "ae2csl.gui.resonant_terminal.state.off"));
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);

        // 按钮背景（悬停/开启时用高亮版）
        boolean hovered = isOverButton(mouseX, mouseY);
        boolean on = menu().encodeResonating;
        Minecraft.getMinecraft().getTextureManager()
            .bindTexture(hovered || on ? BUTTON_BG_HOVER : BUTTON_BG);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        int bx = buttonX();
        int by = buttonY();
        drawTexturedModalRect(bx - 1, by - 2, 0, 0, 18, 20);

        // 图标（AE2 图案上传图标）
        bindTexture(new ResourceLocation("ae2", "textures/guis/icons.png"));
        Icon.PATTERN_UPLOAD.getBlitter().dest(bx, by).blit();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && isOverButton(mouseX, mouseY)) {
            menu().toggleEncodeResonating();
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (isOverButton(mouseX, mouseY)) {
            drawHoveringText(buildTooltip(), mouseX, mouseY);
        }
    }
}
