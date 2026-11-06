/*
 * AE2 Crystal Science - SideFilteredItemHandler (1.12.2)
 * 按面策略过滤的物品能力视图（对齐 1.21.1 源码 SideConfigComponent.appEngInvForSide）：
 * 策略不允许的方向上，插入/提取直接拒绝。
 */
package ae2csl.util;

import ae2csl.machine.SidePolicy;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class SideFilteredItemHandler implements IItemHandler {

    private final IItemHandler delegate;
    private final SidePolicy policy;

    public SideFilteredItemHandler(IItemHandler delegate, SidePolicy policy) {
        this.delegate = delegate;
        this.policy = policy;
    }

    @Override public int getSlots() { return delegate.getSlots(); }
    @Override public ItemStack getStackInSlot(int slot) { return delegate.getStackInSlot(slot); }
    @Override public int getSlotLimit(int slot) { return delegate.getSlotLimit(slot); }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return policy.allowInsert() ? delegate.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return policy.allowExtract() ? delegate.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }
}
