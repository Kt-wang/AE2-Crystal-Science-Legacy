/*
 * AE2 Crystal Science - AutoTransferHelper (1.12.2)
 * 机器自动输入/输出搬运（对齐 1.21.1 源码 SideConfigComponent.onServerTick）。
 * 每 tick 逐面处理：
 *   autoExport 且该面策略允许 EXTRACT → 把输出槽物品推给邻居；
 *   autoImport 且该面策略允许 INSERT → 从邻居拉物品进输入槽。
 * 每面每 tick 最大搬运量 576（对齐源码 TRANSFER_PER_SIDE）。
 */
package ae2csl.util;

import ae2csl.machine.SideConfigData;
import ae2csl.machine.SidePolicy;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.function.IntPredicate;

public final class AutoTransferHelper {

    private static final int TRANSFER_PER_SIDE = 576;

    private AutoTransferHelper() {}

    /**
     * @param inv          机器库存
     * @param insertSlots  允许被自动输入的槽位（输入槽）
     * @param extractSlots 允许被自动输出的槽位（输出槽）
     */
    public static void tick(World world, BlockPos pos, IInventory inv, SideConfigData cfg,
                            IntPredicate insertSlots, IntPredicate extractSlots) {
        if (!cfg.isAutoExport() && !cfg.isAutoImport()) return;
        for (EnumFacing side : EnumFacing.VALUES) {
            SidePolicy policy = cfg.get(side);
            boolean doExport = cfg.isAutoExport() && policy.allowExtract();
            boolean doImport = cfg.isAutoImport() && policy.allowInsert();
            if (!doExport && !doImport) continue;

            TileEntity te = world.getTileEntity(pos.offset(side));
            if (te == null) continue;
            IItemHandler other = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite());
            if (other == null) continue;

            if (doExport) export(inv, other, extractSlots);
            if (doImport) imports(inv, other, insertSlots);
        }
    }

    /** 自 → 邻居（只推 extractSlots 允许的槽位） */
    private static void export(IInventory self, IItemHandler other, IntPredicate extractSlots) {
        int remaining = TRANSFER_PER_SIDE;
        for (int slot = 0; slot < self.getSizeInventory() && remaining > 0; slot++) {
            if (!extractSlots.test(slot)) continue;
            ItemStack inside = self.getStackInSlot(slot);
            if (inside.isEmpty()) continue;

            ItemStack toTry = inside.copy();
            toTry.setCount(Math.min(inside.getCount(), remaining));
            // 先模拟邻居能吃多少
            ItemStack overflowSim = ItemHandlerHelper.insertItem(other, toTry.copy(), true);
            int accepted = toTry.getCount() - overflowSim.getCount();
            if (accepted <= 0) continue;

            // 真正抽出 accepted，再塞进邻居
            ItemStack extracted = self.decrStackSize(slot, accepted);
            ItemStack overflow = ItemHandlerHelper.insertItem(other, extracted, false);
            if (!overflow.isEmpty()) {
                // 回滚（模拟通过后理论上不会发生，保险）
                ItemStack back = self.getStackInSlot(slot);
                if (back.isEmpty()) self.setInventorySlotContents(slot, overflow);
                else { back.grow(overflow.getCount()); self.setInventorySlotContents(slot, back); }
            }
            self.markDirty();
            remaining -= accepted;
        }
    }

    /** 邻居 → 自（只拉进 insertSlots 允许且 isItemValidForSlot 通过的槽位） */
    private static void imports(IInventory self, IItemHandler other, IntPredicate insertSlots) {
        int remaining = TRANSFER_PER_SIDE;
        for (int os = 0; os < other.getSlots() && remaining > 0; os++) {
            ItemStack inSlot = other.getStackInSlot(os);
            if (inSlot.isEmpty()) continue;

            ItemStack sim = other.extractItem(os, Math.min(inSlot.getCount(), remaining), true);
            if (sim.isEmpty()) continue;
            ItemStack overflowSim = insert(self, sim.copy(), true, insertSlots);
            int accepted = sim.getCount() - overflowSim.getCount();
            if (accepted <= 0) continue;

            ItemStack extracted = other.extractItem(os, accepted, false);
            ItemStack overflow = insert(self, extracted, false, insertSlots);
            if (!overflow.isEmpty()) {
                // 放不下的塞回邻居（保险）
                ItemHandlerHelper.insertItem(other, overflow, false);
            }
            remaining -= accepted;
        }
    }

    /** 插入 IInventory 指定槽位（先合并同类，再放空格），返回放不下的部分 */
    private static ItemStack insert(IInventory inv, ItemStack stack, boolean simulate, IntPredicate insertSlots) {
        for (int slot = 0; slot < inv.getSizeInventory() && !stack.isEmpty(); slot++) {
            if (!insertSlots.test(slot)) continue;
            ItemStack cur = inv.getStackInSlot(slot);
            if (cur.isEmpty() || !ItemHandlerHelper.canItemStacksStack(cur, stack)) continue;
            int space = Math.min(cur.getMaxStackSize(), inv.getInventoryStackLimit()) - cur.getCount();
            if (space <= 0) continue;
            int move = Math.min(space, stack.getCount());
            if (!simulate) { cur.grow(move); inv.markDirty(); }
            stack.shrink(move);
        }
        for (int slot = 0; slot < inv.getSizeInventory() && !stack.isEmpty(); slot++) {
            if (!insertSlots.test(slot)) continue;
            if (!inv.getStackInSlot(slot).isEmpty()) continue;
            if (!inv.isItemValidForSlot(slot, stack)) continue;
            int move = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inv.getInventoryStackLimit()));
            if (!simulate) {
                ItemStack placed = stack.splitStack(move);
                inv.setInventorySlotContents(slot, placed);
                inv.markDirty();
            } else {
                stack.shrink(move);
            }
        }
        return stack;
    }
}
