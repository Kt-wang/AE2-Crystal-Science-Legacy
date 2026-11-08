/*
 * AE2 Crystal Science - GuiResonatingPattern (1.12.2)
 *
 * [P] 查看谐振样板：沿用 AE2 处理样板展示界面的背景与布局。
 */
package ae2csl.client.gui.pattern;

import ae2.client.gui.pattern.GuiPattern;
import ae2.core.AppEng;
import ae2csl.container.pattern.ContainerResonatingPattern;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiResonatingPattern extends GuiPattern<ContainerResonatingPattern> {

    private static final ResourceLocation BACKGROUND =
        AppEng.makeId("textures/guis/processing_pattern_recipe.png");

    public GuiResonatingPattern(ContainerResonatingPattern container, InventoryPlayer playerInventory) {
        super(container, playerInventory);
        this.ySize = 251;
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }
}
