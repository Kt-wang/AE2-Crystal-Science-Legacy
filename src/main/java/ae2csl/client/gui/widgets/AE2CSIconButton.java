/*
 * AE2 Crystal Science - AE2CSIconButton
 * AE2-style icon button: renders a 18x20 toolbar background plus a 16x16 icon, and carries a
 * hover tooltip. 1.12.2 port of the 1.21.1 AE2CSIconButton (itself based on AE2-Supergiant's
 * IconButton). The background uses AE2's toolbar button background texture; the icon is any
 * IButtonIcon.
 */
package ae2csl.client.gui.widgets;

import ae2.client.gui.style.Blitter;
import ae2csl.client.gui.icon.IButtonIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public class AE2CSIconButton extends GuiButton {

    private static final ResourceLocation TOOLBAR_BG =
            new ResourceLocation("ae2", "textures/guis/icons/toolbar_button_background.png");
    private static final ResourceLocation TOOLBAR_BG_HOVER =
            new ResourceLocation("ae2", "textures/guis/icons/toolbar_button_background_hover.png");

    private IButtonIcon icon;
    private List<String> tooltip;
    private boolean armed;

    public AE2CSIconButton(int id, int x, int y, IButtonIcon icon, List<String> tooltip) {
        super(id, x, y, 16, 16, "");
        this.icon = icon;
        this.tooltip = tooltip;
    }

    public void setIcon(IButtonIcon icon) {
        this.icon = icon;
    }

    public void setTooltip(List<String> tooltip) {
        this.tooltip = tooltip;
    }

    /** Forces the hover ("lit") visual even when not hovered, e.g. for a pending confirm. */
    public void setArmed(boolean armed) {
        this.armed = armed;
    }

    public List<String> getTooltip() {
        return tooltip;
    }

    public void setVisibility(boolean vis) {
        this.visible = vis;
        this.enabled = vis;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        boolean highlighted = this.hovered || this.armed;
        int yOffset = highlighted ? 1 : 0;

        // 18x20 toolbar button background (AE2)
        Blitter.texture(highlighted ? TOOLBAR_BG_HOVER : TOOLBAR_BG)
                .dest(this.x - 1, this.y + yOffset, 18, 20)
                .zOffset(2)
                .blit();

        // 16x16 icon
        if (this.icon != null) {
            this.icon.getBlitter()
                    .dest(this.x, this.y + 1 + yOffset)
                    .zOffset(3)
                    .blit();
        }
    }
}
