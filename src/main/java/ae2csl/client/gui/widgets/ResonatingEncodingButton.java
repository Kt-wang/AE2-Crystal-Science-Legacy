/*
 * AE2 Crystal Science - ResonatingEncodingButton (1.12.2)
 *
 * 对齐 1.21.1 源码 ResonantTemplateCodingTermScreen.ResonatingPatternEncodingButton：
 * 只绘制 16x16 图标（无工具栏底板），开启/关闭用两张不同图标，
 * 悬停提示走 AE2 的 ITooltip 机制。
 */
package ae2csl.client.gui.widgets;

import ae2.client.gui.Icon;
import ae2.client.gui.style.Blitter;
import ae2.client.gui.widgets.IconButton;
import ae2csl.Tags;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import java.awt.Rectangle;
import java.util.List;

public class ResonatingEncodingButton extends IconButton {

    private static final ResourceLocation ICON_TEXTURE =
        new ResourceLocation(Tags.MOD_ID, "textures/gui/terminal_icon.png");

    /** 对齐源码：terminal_icon.png 80x64，(32,16) 普通编码、(48,16) 谐振编码 */
    private static final Blitter NORMAL_ICON = Blitter.texture(ICON_TEXTURE, 80, 64).src(32, 16, 16, 16);
    private static final Blitter RESONATING_ICON = Blitter.texture(ICON_TEXTURE, 80, 64).src(48, 16, 16, 16);

    private boolean state;
    private List<ITextComponent> tooltipOff = java.util.Collections.emptyList();
    private List<ITextComponent> tooltipOn = java.util.Collections.emptyList();

    public ResonatingEncodingButton(Runnable onPress) {
        super(onPress);
        this.width = 16;
        this.height = 16;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public void setTooltip(List<ITextComponent> off, List<ITextComponent> on) {
        this.tooltipOff = off;
        this.tooltipOn = on;
    }

    @Override
    protected Icon getIcon() {
        return null;
    }

    @Override
    public void drawButton(@Nonnull Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }
        Blitter icon = (this.state ? RESONATING_ICON : NORMAL_ICON).copy();
        if (!this.enabled) {
            icon.opacity(0.5F);
        }
        icon.dest(this.x, this.y).zOffset(20).blit();
    }

    @Override
    public @Nonnull List<ITextComponent> getTooltipMessage() {
        return this.state ? this.tooltipOn : this.tooltipOff;
    }

    @Override
    public Rectangle getTooltipArea() {
        return new Rectangle(this.x, this.y, 16, 16);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return this.visible;
    }
}
