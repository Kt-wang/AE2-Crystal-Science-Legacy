/*
 * AE2 Crystal Science - AE2CSUpgradeLayout (1.12.2)
 * GUI-relative layout constants for the ME-Interface-style upgrade panel
 * (extra_panels.png panel hanging on the RIGHT edge of the GUI,
 * matching AE2's UpgradesPanel geometry with style "upgrades": right=1, top=0).
 * Must stay side-safe: no client-only imports.
 */
package ae2csl.util;

public final class AE2CSUpgradeLayout {

    // Same constants as AE2's UpgradesPanel
    public static final int SLOT_SIZE = 18;
    public static final int LEFT_PADDING = 5;
    public static final int RIGHT_PADDING = 6;
    public static final int TOP_PADDING = 5;
    public static final int BOTTOM_PADDING = 7;

    /** Panel width = LEFT_PADDING + SLOT_SIZE + RIGHT_PADDING = 29. */
    public static final int PANEL_WIDTH = LEFT_PADDING + SLOT_SIZE + RIGHT_PADDING; // 29

    /** Panel origin offset relative to the GUI right edge (style right=1 + UpgradesPanel OFFSET_X 2). */
    private static final int PANEL_X = 1;
    /** UpgradesPanel SLOT_X within the panel. */
    private static final int PANEL_SLOT_X = 4;

    /** GUI-relative Y for the first upgrade slot (SLOT_Y = TOP_PADDING + 1). */
    public static final int SLOT_Y = TOP_PADDING + 1; // 6

    /** GUI-relative X for upgrade slots, given the GUI width (xSize). */
    public static int slotX(int guiWidth) {
        return guiWidth + PANEL_X + PANEL_SLOT_X; // guiWidth + 5
    }

    private AE2CSUpgradeLayout() {}
}
