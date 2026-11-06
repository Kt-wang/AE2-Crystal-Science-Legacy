/*
 * AE2 Crystal Science - AE2CSUpgradePanel (1.12.2)
 * Replicates the ME-Interface upgrade panel look (AE2 UpgradesPanel):
 * light panel from ae2:textures/guis/extra_panels.png hanging on the RIGHT edge
 * of the GUI, plus the AE2 upgrade slot icon on empty slots
 * (like AppEngSlot with SlotBackgroundIcon.UPGRADE).
 *
 * Usage: call draw(guiLeft, guiTop, guiWidth, upgradeSlots) in
 * drawGuiContainerBackgroundLayer. Slot positions come from
 * ae2csl.util.AE2CSUpgradeLayout.
 */
package ae2csl.client.gui;

import ae2.client.gui.style.Blitter;
import ae2csl.util.AE2CSUpgradeLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public final class AE2CSUpgradePanel {

    /** ae2:textures/guis/icons/background_upgrade.png (Icon.BACKGROUND_UPGRADE). */
    private static final ResourceLocation SLOT_ICON =
        new ResourceLocation("ae2", "textures/guis/icons/background_upgrade.png");

    /**
     * Returns the empty upgrade slot under the mouse, or null.
     */
    public static Slot getHoveredUpgradeSlot(List<Slot> upgradeSlots, int guiLeft, int guiTop, int mouseX, int mouseY) {
        for (Slot slot : upgradeSlots) {
            if (slot.getHasStack()) continue;
            int x = guiLeft + slot.xPos;
            int y = guiTop + slot.yPos;
            if (mouseX >= x && mouseX < x + AE2CSUpgradeLayout.SLOT_SIZE
                && mouseY >= y && mouseY < y + AE2CSUpgradeLayout.SLOT_SIZE) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Tooltip lines shown when hovering an empty upgrade slot:
     * "Compatible Upgrades" header + supported cards (speed card only, per 1.21.1 source).
     */
    public static List<String> getCompatibleUpgradesTooltip() {
        return getCompatibleUpgradesTooltip(false);
    }

    /**
     * @param withOverload also list the Meteorite Overclock Card (etcher / entropy chamber)
     */
    public static List<String> getCompatibleUpgradesTooltip(boolean withOverload) {
        List<String> lines = new java.util.ArrayList<>();
        lines.add(ae2.core.localization.GuiText.CompatibleUpgrades.text().getFormattedText());
        lines.add(new net.minecraft.item.ItemStack(ae2.core.definitions.AEItems.SPEED_CARD.item()).getDisplayName());
        if (withOverload) {
            net.minecraft.item.Item overload = net.minecraft.item.Item.REGISTRY.getObject(
                new net.minecraft.util.ResourceLocation("ae2csl", "overload_card"));
            if (overload != null) lines.add(new net.minecraft.item.ItemStack(overload).getDisplayName());
        }
        return lines;
    }

    /**
     * Screen-space bounds of the upgrade panel (for JEI exclusion areas etc.).
     */
    public static java.awt.Rectangle getPanelBounds(int guiLeft, int guiTop, int guiWidth, int slotCount) {
        int height = AE2CSUpgradeLayout.TOP_PADDING + slotCount * AE2CSUpgradeLayout.SLOT_SIZE
            + AE2CSUpgradeLayout.BOTTOM_PADDING;
        return new java.awt.Rectangle(guiLeft + guiWidth + 1, guiTop, AE2CSUpgradeLayout.PANEL_WIDTH, height);
    }

    private AE2CSUpgradePanel() {}

    /**
     * Draw the upgrade panel background for the given slots.
     * Replicates UpgradesPanel.drawBackgroundLayer/drawSlot.
     */
    public static void draw(int guiLeft, int guiTop, int guiWidth, List<Slot> upgradeSlots) {
        int slotCount = upgradeSlots.size();
        if (slotCount <= 0) return;

        Blitter background = Blitter.texture("guis/extra_panels.png", 128, 128);

        // drawSlot: dest x = slotOriginX - LEFT_PADDING; slotOriginX = guiLeft + slotX + 1 (matches AE2 rounding)
        int slotOriginX = guiLeft + AE2CSUpgradeLayout.slotX(guiWidth) + 1 - AE2CSUpgradeLayout.LEFT_PADDING;
        int slotOriginY = guiTop + AE2CSUpgradeLayout.SLOT_Y - 1;

        for (int i = 0; i < slotCount; i++) {
            boolean borderTop = i == 0;
            boolean borderBottom = i + 1 == slotCount;

            int srcX = 0;
            int srcY = AE2CSUpgradeLayout.TOP_PADDING;
            int srcWidth = AE2CSUpgradeLayout.PANEL_WIDTH;
            int srcHeight = AE2CSUpgradeLayout.SLOT_SIZE;

            int x = slotOriginX;
            int y = slotOriginY + i * AE2CSUpgradeLayout.SLOT_SIZE;

            if (borderTop) {
                y -= AE2CSUpgradeLayout.TOP_PADDING;
                srcY = 0;
                srcHeight += AE2CSUpgradeLayout.TOP_PADDING;
            }
            if (borderBottom) {
                srcHeight += AE2CSUpgradeLayout.BOTTOM_PADDING;
            }

            background.copy().src(srcX, srcY, srcWidth, srcHeight).dest(x, y).blit();
        }

        // Upgrade slot icons on empty slots (AppEngSlot background icon behavior)
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(SLOT_ICON);
        for (Slot slot : upgradeSlots) {
            if (slot.getHasStack()) continue;
            Gui.drawModalRectWithCustomSizedTexture(
                guiLeft + slot.xPos, guiTop + slot.yPos, 0, 0, 16, 16, 16, 16);
        }
    }
}
