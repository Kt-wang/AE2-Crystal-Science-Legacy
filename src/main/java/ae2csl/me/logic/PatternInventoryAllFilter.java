/*
 * AE2 Crystal Science - PatternInventoryAllFilter (1.12.2)
 * 接收所有已编码样板（合成+处理）的样板栏过滤器。
 *
 * 背景：fork 的 PatternProviderLogic.PatternInventoryFilter（private）会拒绝
 * IAssemblerPattern（合成样板），而 1.21.1 源码（AE2 官方过滤器）两者都收——
 * 简易/陨石样板供应器都需要放合成样板。用 setFilter 覆盖 fork 的私有过滤器。
 * 行为对齐 fork 原过滤器（解码校验 + 防重复），仅去掉 IAssemblerPattern 排除。
 */
package ae2csl.me.logic;

import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.inventories.InternalInventory;
import ae2.api.stacks.AEItemKey;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2.util.inv.filter.IAEItemFilter;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class PatternInventoryAllFilter implements IAEItemFilter {

    private final PatternProviderLogicHost host;

    public PatternInventoryAllFilter(PatternProviderLogicHost host) {
        this.host = host;
    }

    @Override
    public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
        World level = host.getTileEntity().getWorld();
        if (stack.isEmpty() || level == null) {
            return false;
        }

        IPatternDetails details = PatternDetailsHelper.decodePattern(stack, level);
        return details != null && !hasSamePatternInOtherSlot(inv, slot, stack);
    }

    private boolean hasSamePatternInOtherSlot(InternalInventory inv, int slot, ItemStack stack) {
        AEItemKey pattern = AEItemKey.of(stack);
        if (pattern == null) {
            return false;
        }

        for (int i = 0; i < inv.size(); i++) {
            if (i == slot) {
                continue;
            }
            if (pattern.equals(AEItemKey.of(inv.getStackInSlot(i)))) {
                return true;
            }
        }
        return false;
    }
}
