/*
 * AE2 Crystal Science - ContainerCrystalVibrationChamber
 * Vanilla Container. 0=fuel slot.
 */
package ae2csl.container;

import ae2csl.tile.vibration.TileCrystalVibrationChamber;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerCrystalVibrationChamber extends Container {

    private final TileCrystalVibrationChamber tile;

    public ContainerCrystalVibrationChamber(InventoryPlayer playerInv, TileCrystalVibrationChamber tile) {
        this.tile = tile;

        // Fuel slot
        this.addSlotToContainer(new Slot(this.tile, 0, 80, 37));

        // Upgrade slots (1-3): ME-Interface-style panel on the right edge, speed cards only
        for (int i = 0; i < 3; i++) {
            final int slotIndex = 1 + i;
            this.addSlotToContainer(new Slot(this.tile, slotIndex, ae2csl.util.AE2CSUpgradeLayout.slotX(176),
                    ae2csl.util.AE2CSUpgradeLayout.SLOT_Y + i * ae2csl.util.AE2CSUpgradeLayout.SLOT_SIZE) {
                @Override public int getSlotStackLimit() { return 1; }
                @Override public boolean isItemValid(net.minecraft.item.ItemStack stack) { return tile.isItemValidForSlot(slotIndex, stack); }
            });
        }

        // Player inventory (texture slot borders at y=100/118/136 => items at y=84; hotbar y=141)
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                this.addSlotToContainer(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++)
            this.addSlotToContainer(new Slot(playerInv, c, 8 + c * 18, 142));
    }

    public java.util.List<Slot> getUpgradeSlots() {
        return this.inventorySlots.subList(1, 4);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (IContainerListener l : this.listeners) {
            l.sendWindowProperty(this, 0, tile.getField(0));
            l.sendWindowProperty(this, 1, tile.getField(1));
            l.sendWindowProperty(this, 2, tile.getField(2));
        }
    }

    @Override public void updateProgressBar(int id, int data) { tile.setField(id, data); }
    @Override public boolean canInteractWith(@Nonnull EntityPlayer player) { return tile.isUsableByPlayer(player); }

    @Nonnull @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack().copy();
        if (index < 4) {
            // fuel slot or upgrade slots -> player inventory (slots 4..39)
            if (!this.mergeItemStack(slot.getStack(), 4, 4 + 36, true)) return ItemStack.EMPTY;
        } else {
            // player inventory -> fuel slot + upgrade slots (validity enforced by isItemValid)
            if (!this.mergeItemStack(slot.getStack(), 0, 4, false)) return ItemStack.EMPTY;
        }
        if (slot.getStack().isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        return stack;
    }
}
