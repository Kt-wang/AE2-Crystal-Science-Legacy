/*
 * AE2 Crystal Science - GhostSlot
 * Ghost slot for config/filter: accepts items (not consumed), can be cleared with click.
 * Stores items in a backing IInventory, copies to AE2 ConfigInventory on change.
 */
package ae2csl.container;

import ae2.api.inventories.InternalInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nonnull;

public class GhostSlot extends Slot {

    public GhostSlot(InternalInventory configInv, int index, int x, int y) {
        super(new GhostInventory(configInv, index), index, x, y);
    }

    @Override
    public boolean isItemValid(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public void putStack(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            this.inventory.setInventorySlotContents(getSlotIndex(), ItemStack.EMPTY);
        } else {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            this.inventory.setInventorySlotContents(getSlotIndex(), copy);
        }
        this.onSlotChanged();
    }

    @Override
    @Nonnull
    public ItemStack decrStackSize(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return true;
    }

    @Override
    @Nonnull
    public ItemStack onTake(EntityPlayer player, @Nonnull ItemStack stack) {
        this.inventory.setInventorySlotContents(getSlotIndex(), ItemStack.EMPTY);
        this.onSlotChanged();
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    /**
     * Backing IInventory that syncs with AE2 ConfigInventory on changes.
     */
    private static class GhostInventory implements IInventory {
        private final InternalInventory configInv;
        private final int slot;
        private ItemStack displayStack = ItemStack.EMPTY;

        GhostInventory(InternalInventory configInv, int slot) {
            this.configInv = configInv;
            this.slot = slot;
        }

        @Override
        public int getSizeInventory() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return displayStack.isEmpty();
        }

        @Override
        public ItemStack getStackInSlot(int i) {
            return displayStack.copy();
        }

        @Override
        public ItemStack decrStackSize(int i, int c) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeStackFromSlot(int i) {
            ItemStack s = displayStack.copy();
            displayStack = ItemStack.EMPTY;
            configInv.setItemDirect(slot, ItemStack.EMPTY);
            return s;
        }

        @Override
        public void setInventorySlotContents(int i, ItemStack stack) {
            if (stack.isEmpty()) {
                displayStack = ItemStack.EMPTY;
                configInv.setItemDirect(slot, ItemStack.EMPTY);
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                displayStack = copy;
                configInv.setItemDirect(slot, stack.copy());
            }
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
        }

        @Override
        public boolean isUsableByPlayer(EntityPlayer p) {
            return true;
        }

        @Override
        public void openInventory(EntityPlayer p) {
        }

        @Override
        public void closeInventory(EntityPlayer p) {
        }

        @Override
        public boolean isItemValidForSlot(int i, ItemStack s) {
            return true;
        }

        @Override
        public int getField(int id) {
            return 0;
        }

        @Override
        public void setField(int id, int v) {
        }

        @Override
        public int getFieldCount() {
            return 0;
        }

        @Override
        public void clear() {
            displayStack = ItemStack.EMPTY;
            configInv.setItemDirect(slot, ItemStack.EMPTY);
        }

        @Override
        public String getName() {
            return "ghost";
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }

        @Override
        public ITextComponent getDisplayName() {
            return new TextComponentString("");
        }
    }
}
