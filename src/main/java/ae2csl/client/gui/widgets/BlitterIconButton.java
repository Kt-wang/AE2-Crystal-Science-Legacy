/*
 * AE2 Crystal Science - BlitterIconButton (1.12.2)
 * fork IconButton 的变体：图标来自任意 Blitter（如 ae2cs 图标图集），
 * 而非 fork Icon 图集（fork Icon.register 不支持 UV 子区域）。
 * 背景仍用 AE2 工具栏按钮背景。
 */
package ae2csl.client.gui.widgets;

import ae2.client.gui.Icon;
import ae2.client.gui.style.Blitter;
import ae2.client.gui.widgets.IconButton;

import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class BlitterIconButton extends IconButton {

    private final Supplier<Blitter> iconSupplier;

    public BlitterIconButton(Runnable onPress, Supplier<Blitter> iconSupplier) {
        super(onPress);
        this.iconSupplier = iconSupplier;
    }

    @Nullable
    @Override
    protected Icon getIcon() {
        return null; // 绘制已在本类 drawButton 中处理
    }

    @Nullable
    protected Blitter getBlitterIcon() {
        return iconSupplier.get();
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width
            && mouseY < this.y + this.height;

        int yOffset = this.hovered ? 1 : 0;

        if (!isDisableBackground()) {
            Icon bgIcon = this.hovered ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER
                : this.isFocused() ? Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
            bgIcon.getBlitter()
                .dest(this.x - 1, this.y + yOffset, 18, 20)
                .zOffset(2)
                .blit();
        }

        Blitter icon = getBlitterIcon();
        if (icon != null) {
            icon.dest(this.x, this.y + 1 + yOffset).zOffset(3).blit();
        }
    }
}
