/*
 * AE2 Crystal Science - ContainerEntropyVariationChamber
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2csl.tile.entropy.TileEntropyVariationChamber;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public class ContainerEntropyVariationChamber extends AEBaseContainer {

    private final TileEntropyVariationChamber tile;

    public ContainerEntropyVariationChamber(InventoryPlayer playerInv, TileEntropyVariationChamber tile) {
        super(playerInv, tile);
        this.tile = tile;

        // Machine slots per AE2 style JSON
        this.addSlot(new Slot(this.tile, 0, 46, 41), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new Slot(this.tile, 1, 98, 32), SlotSemantics.MACHINE_OUTPUT);

        // Upgrade slots (2-5): ME-Interface-style panel on the right edge, speed cards only
        for (int i = 0; i < 4; i++) {
            final int slotIndex = 2 + i;
            this.addSlot(new Slot(this.tile, slotIndex, ae2csl.util.AE2CSUpgradeLayout.slotX(176),
                    ae2csl.util.AE2CSUpgradeLayout.SLOT_Y + i * ae2csl.util.AE2CSUpgradeLayout.SLOT_SIZE) {
                @Override public int getSlotStackLimit() { return 1; }
                @Override public boolean isItemValid(net.minecraft.item.ItemStack stack) { return tile.isItemValidForSlot(slotIndex, stack); }
            }, SlotSemantics.UPGRADE);
        }

        // Player inventory
        // Player inventory: AE2 style bottom=84 -> y=94; hotbar bottom=26 -> y=152
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                this.addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, 94 + r * 18), SlotSemantics.PLAYER_INVENTORY);
        for (int c = 0; c < 9; c++)
            this.addSlot(new Slot(playerInv, c, 8 + c * 18, 152), SlotSemantics.PLAYER_HOTBAR);
    }


    public java.util.List<Slot> getUpgradeSlots() {
        return this.inventorySlots.subList(2, 6);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (net.minecraft.inventory.IContainerListener l : this.listeners) {
            l.sendWindowProperty(this, 0, tile.getField(0));
            l.sendWindowProperty(this, 1, tile.getField(1));
            l.sendWindowProperty(this, 2, tile.getField(2));
            l.sendWindowProperty(this, 3, tile.getField(3));
            l.sendWindowProperty(this, 4, tile.getField(4));
            l.sendWindowProperty(this, 5, tile.getField(5));
        }
    }

    @Override
    public void updateProgressBar(int id, int data) {
        tile.setField(id, data);
    }

    @Override
    public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) { return true; }
}
