/*
 * AE2 Crystal Science - SidePolicyButton (1.12.2)
 * 侧配界面的方向策略按钮（对齐 1.21.1 源码 AECSSideConfigToggleButton）：
 * 18x20 彩色背景（全部=紫 / 输入=红 / 输出=蓝 / 禁用=深灰，悬停变亮），
 * 中央渲染该方向邻居方块的物品图标（源码 getItemOverlay），点击循环策略。
 */
package ae2csl.client.gui.widgets;

import ae2csl.client.gui.icon.AE2CSIcon;
import ae2csl.client.gui.icon.IButtonIcon;
import ae2csl.machine.SidePolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class SidePolicyButton extends GuiButton {

    private final EnumFacing side;
    private SidePolicy policy = SidePolicy.ALL;
    private ItemStack overlay = ItemStack.EMPTY;

    public SidePolicyButton(int id, int x, int y, EnumFacing side) {
        super(id, x, y, 18, 20, "");
        this.side = side;
    }

    public EnumFacing getSide() { return side; }
    public SidePolicy getPolicy() { return policy; }
    public void setPolicy(SidePolicy policy) { this.policy = policy; }

    /** 邻居方块物品图标（源码 getItemOverlay），空 stack 表示不渲染 */
    public void setOverlay(ItemStack stack) { this.overlay = stack == null ? ItemStack.EMPTY : stack; }

    private static IButtonIcon backgroundFor(SidePolicy p, boolean hovered) {
        switch (p) {
            case INSERT: return hovered ? AE2CSIcon.BUTTON_RED_DARK_HOVER : AE2CSIcon.BUTTON_RED_DARK;
            case EXTRACT: return hovered ? AE2CSIcon.BUTTON_BLUE_DARK_HOVER : AE2CSIcon.BUTTON_BLUE_DARK;
            case NONE: return hovered ? AE2CSIcon.BUTTON_ORIGINAL_DARK_HOVER : AE2CSIcon.BUTTON_ORIGINAL_DARK;
            default: return hovered ? AE2CSIcon.BUTTON_PURPLE_DARK_HOVER : AE2CSIcon.BUTTON_PURPLE_DARK;
        }
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        // 彩色背景（18x20）
        backgroundFor(this.policy, this.hovered).getBlitter().dest(this.x, this.y).blit();

        // 邻居方块物品图标（16x16 居中）
        if (!this.overlay.isEmpty()) {
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableRescaleNormal();
            mc.getRenderItem().renderItemIntoGUI(this.overlay, this.x + 1, this.y + 2);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
