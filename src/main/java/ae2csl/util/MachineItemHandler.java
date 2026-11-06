/*
 * AE2 Crystal Science - MachineItemHandler (1.12.2)
 * 机器自动化的物品能力视图：插入走 IInventory.isItemValidForSlot（InvWrapper 原生语义），
 * 提取按槽位谓词限制（防止样板供应器把未加工的原料又抽回去）。
 */
package ae2csl.util;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.function.IntPredicate;

public class MachineItemHandler extends InvWrapper {

    private final IntPredicate extractable;

    /**
     * @param inv         机器库存
     * @param extractable 允许被自动化提取的槽位（输出槽）
     */
    public MachineItemHandler(IInventory inv, IntPredicate extractable) {
        super(inv);
        this.extractable = extractable;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return this.extractable.test(slot) ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }
}
