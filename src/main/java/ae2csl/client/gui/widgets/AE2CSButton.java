/*
 * AE2 Crystal Science - AE2CSButton
 * AE2-style push button: renders the AE2 button texture (ae2:textures/guis/button.png) as a 9-slice
 * with hover/disabled variants, plus centered text. 1.12.2 port of AE2-Supergiant's AE2Button
 * (click is handled by the owning GUI via actionPerformed, matching the vanilla GuiButton contract).
 */
package ae2csl.client.gui.widgets;

import ae2.client.gui.style.Blitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

public class AE2CSButton extends GuiButton {

    private static final ResourceLocation BUTTON = new ResourceLocation("ae2", "textures/guis/button.png");
    private static final ResourceLocation BUTTON_DISABLED = new ResourceLocation("ae2", "textures/guis/button_disabled.png");
    private static final ResourceLocation BUTTON_HIGHLIGHTED = new ResourceLocation("ae2", "textures/guis/button_highlighted.png");

    public AE2CSButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        renderNineSlice(getButtonBlitter());

        int color;
        int yOffset;
        if (!this.enabled) {
            color = 0x413f54;
            yOffset = -1;
        } else if (this.hovered) {
            color = 0x517497;
            yOffset = 0;
        } else {
            color = 0xf2f2f2;
            yOffset = 1;
        }
        this.drawCenteredString(mc.fontRenderer, this.displayString, this.x + this.width / 2, this.y + (this.height - 8) / 2 + yOffset, color);
    }

    private Blitter getButtonBlitter() {
        if (!this.enabled) {
            return Blitter.texture(BUTTON_DISABLED, 200, 20);
        }
        if (this.hovered) {
            return Blitter.texture(BUTTON_HIGHLIGHTED, 200, 20);
        }
        return Blitter.texture(BUTTON, 200, 20);
    }

    private void renderNineSlice(Blitter texture) {
        int left = Math.min(3, this.width / 2);
        int right = Math.min(3, this.width - left);
        int top = Math.min(3, this.height / 2);
        int bottom = Math.min(3, this.height - top);
        int centerWidth = Math.max(0, this.width - left - right);
        int centerHeight = Math.max(0, this.height - top - bottom);

        if (left > 0 && top > 0) texture.copy().src(0, 0, 3, 3).dest(this.x, this.y, left, top).blit();
        if (centerWidth > 0 && top > 0) texture.copy().src(3, 0, 194, 3).dest(this.x + left, this.y, centerWidth, top).blit();
        if (right > 0 && top > 0) texture.copy().src(197, 0, 3, 3).dest(this.x + this.width - right, this.y, right, top).blit();
        if (left > 0 && centerHeight > 0) texture.copy().src(0, 3, 3, 14).dest(this.x, this.y + top, left, centerHeight).blit();
        if (centerWidth > 0 && centerHeight > 0) texture.copy().src(3, 3, 194, 14).dest(this.x + left, this.y + top, centerWidth, centerHeight).blit();
        if (right > 0 && centerHeight > 0) texture.copy().src(197, 3, 3, 14).dest(this.x + this.width - right, this.y + top, right, centerHeight).blit();
        if (left > 0 && bottom > 0) texture.copy().src(0, 17, 3, 3).dest(this.x, this.y + this.height - bottom, left, bottom).blit();
        if (centerWidth > 0 && bottom > 0) texture.copy().src(3, 17, 194, 3).dest(this.x + left, this.y + this.height - bottom, centerWidth, bottom).blit();
        if (right > 0 && bottom > 0) texture.copy().src(197, 17, 3, 3).dest(this.x + this.width - right, this.y + this.height - bottom, right, bottom).blit();
    }
}
