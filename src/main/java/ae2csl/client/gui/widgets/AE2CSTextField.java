/*
 * AE2 Crystal Science - AE2CSTextField
 * AE2-style text field: replaces the vanilla black box background with AE2's text_field.png
 * 9-slice (normal / focused states). 1.12.2 port of AE2-Supergiant's AETextField rendering
 * (without the GuiStyle/palette plumbing; colors are hardcoded from ae2's common/palette.json).
 */
package ae2csl.client.gui.widgets;

import ae2.client.gui.style.Blitter;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ResourceLocation;

public class AE2CSTextField extends GuiTextField {

    private static final ResourceLocation TEXT_FIELD = new ResourceLocation("ae2", "textures/guis/text_field.png");
    private static final int TEXTURE_W = 128, TEXTURE_H = 128;
    private static final int PADDING = 2;

    public AE2CSTextField(int id, FontRenderer fontRenderer, int x, int y, int width, int height) {
        // Inset the actual text field by PADDING so the visual bounds match the requested box,
        // mirroring AE2's AETextField (which pads the field and draws the background over the full span).
        super(id, fontRenderer, x + PADDING, y + PADDING, width - 2 * PADDING, height - 2 * PADDING);
        this.setEnableBackgroundDrawing(false);
        // ae2 common/palette.json TEXTFIELD_TEXT
        this.setTextColor(0xf2f2f2);
        this.setDisabledTextColour(0xf2f2f2);
    }

    @Override
    public void drawTextBox() {
        if (!this.getVisible()) {
            return;
        }

        // text_field.png rows: 0=normal, 12=disabled, 24=focused
        int yOffset = this.isFocused() ? 24 : 0;

        int bgX = this.x - PADDING;
        int bgY = this.y - PADDING;
        int bgWidth = this.width + 2 * PADDING;

        Blitter blitter = Blitter.texture(TEXT_FIELD, TEXTURE_W, TEXTURE_H);
        // left edge (1px)
        blitter.copy().src(0, yOffset, 1, 12).dest(bgX, bgY).blit();
        // middle (tiled, up to 126px per strip)
        int remaining = bgWidth - 2;
        int cx = bgX + 1;
        while (remaining > 0) {
            int w = Math.min(126, remaining);
            blitter.copy().src(1, yOffset, w, 12).dest(cx, bgY).blit();
            cx += w;
            remaining -= w;
        }
        // right edge (1px)
        blitter.copy().src(127, yOffset, 1, 12).dest(bgX + bgWidth - 1, bgY).blit();

        // Text + caret (background drawing is disabled, so this draws only the text)
        super.drawTextBox();
    }
}
