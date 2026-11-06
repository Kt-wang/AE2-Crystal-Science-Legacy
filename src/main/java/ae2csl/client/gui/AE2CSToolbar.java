/*
 * AE2 Crystal Science - AE2CSToolbar (1.12.2)
 * 左侧工具栏贴条（复刻 AE2 fork VerticalButtonBar 的贴法）：
 * ae2:textures/guis/sprites/vertical_buttons_bg.png（21x26，顶部边框 2、底部边框 4），
 * 从面板左缘外延伸并紧贴面板；按钮从 guiTop+2 起、间距 22 竖排。
 *
 * 用法：drawGuiContainerBackgroundLayer 里调 draw(guiLeft, guiTop, 按钮数)；
 * 按钮坐标用 iconButtonX/buttonX + buttonY。
 */
package ae2csl.client.gui;

import ae2.client.gui.style.Blitter;
import net.minecraft.util.ResourceLocation;

public final class AE2CSToolbar {

    private static final ResourceLocation BG =
        new ResourceLocation("ae2", "textures/guis/sprites/vertical_buttons_bg.png");
    private static final int SRC_W = 21, SRC_H = 26;
    private static final int TOP_BORDER = 2, BOTTOM_BORDER = 4;

    /** 按钮竖排间距（对齐 fork：按钮高 16 + VERTICAL_SPACING 6） */
    public static final int SPACING = 22;

    private AE2CSToolbar() {}

    /**
     * 对齐 fork 的实际布局（样式表 verticalToolbar: left=3, top=1 + VerticalButtonBar MARGIN=2）：
     * 16 宽按钮 x = guiLeft+3-2-16 = guiLeft-15，首个 y = guiTop+1+2 = guiTop+3；
     * 贴条 x = guiLeft+3-16-4-2 = guiLeft-19、宽 21 → 右缘超出面板边 2px，形成"焊在面板上"的效果。
     */

    /** 18x20 逻辑按钮（如 AutoModeToggleButton）的 x（视觉范围 guiLeft-16..guiLeft+2） */
    public static int buttonX(int guiLeft) { return guiLeft - 16; }

    /** AE2CSIconButton（逻辑 16x16、背景在 x-1 画 18x20）的 x */
    public static int iconButtonX(int guiLeft) { return guiLeft - 15; }

    /** 第 index 个按钮的 y（首个 guiTop+3，间距 22） */
    public static int buttonY(int guiTop, int index) { return guiTop + 3 + index * SPACING; }

    /** 画 n 个按钮高的工具栏背景条（右缘超出面板边 2px，与面板焊接） */
    public static void draw(int guiLeft, int guiTop, int buttonCount) {
        if (buttonCount <= 0) return;
        int x = guiLeft - 19;      // 右缘 = guiLeft+2，叠进面板 2px
        int y = guiTop;
        int w = 21;
        int h = SPACING * buttonCount + 6; // 对齐 fork：2 + 22n + 4
        Blitter bg = Blitter.texture(BG, SRC_W, SRC_H);
        // 顶部边框
        bg.copy().src(0, 0, SRC_W, TOP_BORDER).dest(x, y, w, TOP_BORDER).blit();
        // 中部拉伸
        bg.copy().src(0, TOP_BORDER, SRC_W, SRC_H - TOP_BORDER - BOTTOM_BORDER)
          .dest(x, y + TOP_BORDER, w, h - TOP_BORDER - BOTTOM_BORDER).blit();
        // 底部边框
        bg.copy().src(0, SRC_H - BOTTOM_BORDER, SRC_W, BOTTOM_BORDER)
          .dest(x, y + h - BOTTOM_BORDER, w, BOTTOM_BORDER).blit();
    }
}
