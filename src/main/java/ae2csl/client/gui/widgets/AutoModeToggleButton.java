/*
 * AE2 Crystal Science - AutoModeToggleButton (1.12.2)
 * 侧配界面的自动输入/输出开关按钮（对齐 1.21.1 源码 AECSAutoModeToggleButton）：
 * 18x20，启用=亮色背景（BUTTON_ORIGINAL_LIGHT）、禁用=暗色背景（BUTTON_ORIGINAL_DARK），
 * 悬停用对应 _HOVER 变体，中央渲染 RECEIVER_STATE / SENDER_STATE 图标。
 */
package ae2csl.client.gui.widgets;

import ae2csl.client.gui.icon.AE2CSIcon;
import ae2csl.client.gui.icon.IButtonIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import java.util.List;

public class AutoModeToggleButton extends GuiButton {

    private final IButtonIcon icon;
    private boolean enabledState;
    private List<String> tooltip;

    public AutoModeToggleButton(int id, int x, int y, IButtonIcon icon) {
        super(id, x, y, 18, 20, "");
        this.icon = icon;
    }

    public boolean isEnabledState() { return enabledState; }
    public void setEnabledState(boolean enabled) { this.enabledState = enabled; }
    public List<String> getTooltip() { return tooltip; }
    public void setTooltip(List<String> tooltip) { this.tooltip = tooltip; }

    private IButtonIcon background() {
        if (this.enabledState) {
            return this.hovered ? AE2CSIcon.BUTTON_ORIGINAL_LIGHT_HOVER : AE2CSIcon.BUTTON_ORIGINAL_LIGHT;
        }
        return this.hovered ? AE2CSIcon.BUTTON_ORIGINAL_DARK_HOVER : AE2CSIcon.BUTTON_ORIGINAL_DARK;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        background().getBlitter().dest(this.x, this.y).blit();
        if (this.icon != null) {
            this.icon.getBlitter().dest(this.x + 1, this.y + 2).blit();
        }
    }
}
