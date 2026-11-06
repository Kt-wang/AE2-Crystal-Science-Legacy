/*
 * AE2 Crystal Science - ContainerQuartzGrindstone
 * Vanilla Container. 0..2=input(3x), 3=work, 4..6=output(3x).
 */
package ae2csl.container;

import ae2csl.tile.grindstone.TileQuartzGrindstone;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContainerQuartzGrindstone extends Container {

    private final TileQuartzGrindstone tile;

    public ContainerQuartzGrindstone(InventoryPlayer playerInv, TileQuartzGrindstone tile) {
        this.tile = tile;

        // Input slots 0-2 — horizontal at (12, 17) per screen JSON
        this.addSlotToContainer(new Slot(this.tile, 0, 12, 17));
        this.addSlotToContainer(new Slot(this.tile, 1, 30, 17));
        this.addSlotToContainer(new Slot(this.tile, 2, 48, 17));

        // Work slot 3 — centre at (80, 40)
        this.addSlotToContainer(new Slot(this.tile, 3, 80, 40));

        // Output slots 4-6 — horizontal at (112, 63)
        this.addSlotToContainer(new Slot(this.tile, 4, 112, 63));
        this.addSlotToContainer(new Slot(this.tile, 5, 130, 63));
        this.addSlotToContainer(new Slot(this.tile, 6, 148, 63));

        // Player inventory (aligned with texture slot backgrounds at y=93/151)
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                this.addSlotToContainer(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, 94 + r * 18));
        for (int c = 0; c < 9; c++)
            this.addSlotToContainer(new Slot(playerInv, c, 8 + c * 18, 152));
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
        // Machine slots 0-6 → player inventory
        if (index < 7) {
            if (!this.mergeItemStack(slot.getStack(), 7, 7 + 36, true)) return ItemStack.EMPTY;
        } else {
            // Player → input slots 0-2 first, then work slot 3
            if (!this.mergeItemStack(slot.getStack(), 0, 3, false)
                    && !this.mergeItemStack(slot.getStack(), 3, 4, false))
                return ItemStack.EMPTY;
        }
        if (slot.getStack().isEmpty()) slot.putStack(ItemStack.EMPTY);
        else slot.onSlotChanged();
        return stack;
    }
}
