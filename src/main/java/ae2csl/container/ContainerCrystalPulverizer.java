/*
 * AE2 Crystal Science - ContainerCrystalPulverizer
 * Vanilla Container. 0=input, 1..4=output(2x2), 5..8=upgrades(vertical).
 * Upgrade slots limit to 1 item each.
 */
package ae2csl.container;

import ae2csl.tile.pulverizer.TileCrystalPulverizer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerCrystalPulverizer extends Container {

    private final TileCrystalPulverizer tile;

    public ContainerCrystalPulverizer(InventoryPlayer playerInv, TileCrystalPulverizer tile) {
        this.tile = tile;

        this.addSlotToContainer(new Slot(this.tile, 0, 46, 41));        this.addSlotToContainer(new Slot(this.tile, 1, 98, 32));
        this.addSlotToContainer(new Slot(this.tile, 2, 116, 32));
        this.addSlotToContainer(new Slot(this.tile, 3, 98, 50));
        this.addSlotToContainer(new Slot(this.tile, 4, 116, 50));

        // Upgrade slots — ME-Interface-style panel hanging on the right edge of the GUI
        for (int i = 5; i <= 8; i++) {
            this.addSlotToContainer(new Slot(this.tile, i, ae2csl.util.AE2CSUpgradeLayout.slotX(176),
                    ae2csl.util.AE2CSUpgradeLayout.SLOT_Y + (i - 5) * ae2csl.util.AE2CSUpgradeLayout.SLOT_SIZE) {
                @Override public int getSlotStackLimit() { return 1; }
            });
        }

        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                this.addSlotToContainer(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, 94 + r * 18));
        for (int c = 0; c < 9; c++)
            this.addSlotToContainer(new Slot(playerInv, c, 8 + c * 18, 152));
    }

    public java.util.List<Slot> getUpgradeSlots() {
        return this.inventorySlots.subList(5, 9);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (IContainerListener l : this.listeners) {
            l.sendWindowProperty(this, 0, tile.getField(0));
            l.sendWindowProperty(this, 1, tile.getField(1));
        }
    }

    @Override public void updateProgressBar(int id, int data) { tile.setField(id, data); }
    @Override public boolean canInteractWith(@Nonnull EntityPlayer player) { return tile.isUsableByPlayer(player); }

    @Nonnull @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack().copy();
        if (index < 9) {
            if (!this.mergeItemStack(slot.getStack(), 9, 9 + 36, true)) return ItemStack.EMPTY;
        } else {
            if (!this.mergeItemStack(slot.getStack(), 5, 9, false)
                    && !this.mergeItemStack(slot.getStack(), 0, 1, false))
                return ItemStack.EMPTY;
        }
        if (slot.getStack().isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        return stack;
    }
}
