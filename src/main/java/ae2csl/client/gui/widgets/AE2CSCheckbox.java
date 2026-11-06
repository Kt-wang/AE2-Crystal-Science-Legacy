/*
 * AE2 Crystal Science - AE2CSCheckbox
 * AE2-style checkbox/radio: renders the checkbox icon (ae2:textures/guis/checkbox.png) plus a label.
 * 1.12.2 port of AE2-Supergiant's AECheckbox (dropped the GuiStyle dependency; colors are hardcoded
 * from ae2 common/palette.json). Supports both square checkbox and round radio modes; the selected
 * state is toggled by the owning GUI via actionPerformed.
 */
package ae2csl.client.gui.widgets;

import ae2.client.gui.style.Blitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

public class AE2CSCheckbox extends GuiButton {

    private static final ResourceLocation CHECKBOX_TEX = new ResourceLocation("ae2", "textures/guis/checkbox.png");
    private static final int TEX_W = 64, TEX_H = 64;
    private static final int RADIO_SIZE = 14;

    private static final Blitter UNCHECKED             = Blitter.texture(CHECKBOX_TEX, TEX_W, TEX_H).src(0, 28, 22, 12);
    private static final Blitter UNCHECKED_FOCUS       = Blitter.texture(CHECKBOX_TEX, TEX_W, TEX_H).src(22, 28, 22, 12);
    private static final Blitter CHECKED               = Blitter.texture(CHECKBOX_TEX, TEX_W, TEX_H).src(0, 40, 22, 12);
    private static final Blitter CHECKED_FOCUS         = Blitter.texture(CHECKBOX_TEX, TEX_W, TEX_H).src(22, 40, 22, 12);
    private static final Blitter RADIO_UNCHECKED       = Blitter.texture(CHECKBOX_TEX, TEX_W, TEX_H).src(2 * RADIO_SIZE, 0, RADIO_SIZE, RADIO_SIZE);
    private static final Blitter RADIO_UNCHECKED_FOCUS = Blitter.texture(CHECKBOX_TEX, TEX_W, TEX_H).src(3 * RADIO_SIZE, 0, RADIO_SIZE, RADIO_SIZE);
    private static final Blitter RADIO_CHECKED         = Blitter.texture(CHECKBOX_TEX, TEX_W, TEX_H).src(2 * RADIO_SIZE, RADIO_SIZE, RADIO_SIZE, RADIO_SIZE);
    private static final Blitter RADIO_CHECKED_FOCUS   = Blitter.texture(CHECKBOX_TEX, TEX_W, TEX_H).src(3 * RADIO_SIZE, RADIO_SIZE, RADIO_SIZE, RADIO_SIZE);

    private boolean selected;
    private boolean radio;

    public AE2CSCheckbox(int id, int x, int y, int width, int height, String label) {
        super(id, x, y, width, height, label);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isRadio() {
        return radio;
    }

    public void setRadio(boolean radio) {
        this.radio = radio;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

        Blitter icon = getIcon();
        icon.dest(this.x, this.y).opacity(this.enabled ? 1.0f : 0.5f).blit();

        // ae2 common/palette.json: DEFAULT_TEXT_COLOR / MUTED_TEXT_COLOR
        int textColor = this.enabled ? 0x413f54 : 0x878fa5;
        int textX = this.x + (this.radio ? 16 : 26);
        mc.fontRenderer.drawString(this.displayString, textX, this.y + 4, textColor);
    }

    private Blitter getIcon() {
        if (this.radio) {
            if (this.hovered) {
                return this.selected ? RADIO_CHECKED_FOCUS.copy() : RADIO_UNCHECKED_FOCUS.copy();
            }
            return this.selected ? RADIO_CHECKED.copy() : RADIO_UNCHECKED.copy();
        }
        if (this.hovered) {
            return this.selected ? CHECKED_FOCUS.copy() : UNCHECKED_FOCUS.copy();
        }
        return this.selected ? CHECKED.copy() : UNCHECKED.copy();
    }
}
