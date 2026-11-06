/*
 * AE2 Crystal Science - ContainerCrystalGrowthChamber
 * 54 machine input slots in 6×9 grid (BREAK_AFTER_9COLS)
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2csl.tile.crystal.TileCrystalGrowthChamber;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public class ContainerCrystalGrowthChamber extends AEBaseContainer {

    private final TileCrystalGrowthChamber tile;

    public ContainerCrystalGrowthChamber(InventoryPlayer playerInv, TileCrystalGrowthChamber tile) {
        super(playerInv, tile);
        this.tile = tile;

        // 54 machine input slots: 6 rows × 9 columns at (8, 18), 18px spacing
        for (int row = 0; row < 6; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(this.tile, col + row * 9, 8 + col * 18, 18 + row * 18), SlotSemantics.MACHINE_INPUT);

        // Upgrade slots (54-57): vertical column on the right, speed cards only, limit 1
        for (int i = 0; i < 4; i++) {
            final int slotIndex = 54 + i;
            this.addSlot(new Slot(this.tile, slotIndex, ae2csl.util.AE2CSUpgradeLayout.slotX(176), ae2csl.util.AE2CSUpgradeLayout.SLOT_Y + i * ae2csl.util.AE2CSUpgradeLayout.SLOT_SIZE) {
                @Override public int getSlotStackLimit() { return 1; }
                @Override public boolean isItemValid(net.minecraft.item.ItemStack stack) { return tile.isItemValidForSlot(slotIndex, stack); }
            }, SlotSemantics.UPGRADE);
        }

        // Player inventory: y=142 (224-82=142)
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                this.addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, 140 + r * 18), SlotSemantics.PLAYER_INVENTORY);
        for (int c = 0; c < 9; c++)
            this.addSlot(new Slot(playerInv, c, 8 + c * 18, 198), SlotSemantics.PLAYER_HOTBAR);
    }

    public java.util.List<net.minecraft.inventory.Slot> getUpgradeSlots() {
        return this.inventorySlots.subList(54, 58);
    }

    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (net.minecraft.inventory.IContainerListener l : this.listeners) {
            l.sendWindowProperty(this, 0, tile.getField(0));
            l.sendWindowProperty(this, 1, tile.getField(1));
        }
    }

    @Override public void updateProgressBar(int id, int data) { tile.setField(id, data); }
    @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) { return true; }
}
