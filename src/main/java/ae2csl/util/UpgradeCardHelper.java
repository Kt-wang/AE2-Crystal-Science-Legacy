/*
 * AE2 Crystal Science - UpgradeCardHelper (1.12.2)
 *
 * 机器升级槽的统一判定。
 *
 * 对齐 1.21.1 源码（CrystalAggregatorBlockEntity 等）：机器是否接受某张卡、
 * 以及能装几张，完全由 AE2 的 Upgrades 注册表决定（源码里升级库存是
 * UpgradeInventories.forMachine(...)，其过滤器就是 getMaxInstallable）。
 * 因此这里只做两件事：查注册表 + 统计已装数量。
 */
package ae2csl.util;

import ae2.api.upgrades.Upgrades;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class UpgradeCardHelper {

    private UpgradeCardHelper() {
    }

    /** 是否陨石超频卡（对齐 TileCircuitEtcher 的注册名判断方式） */
    public static boolean isOverloadCard(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation name = stack.getItem().getRegistryName();
        return name != null && "overload_card".equals(name.getPath());
    }

    /**
     * 该卡对这台机器允许安装几张。
     *
     * @param card        待插入的卡
     * @param machineItem 机器对应的物品（方块物品）
     * @return 0 表示这台机器不接受该卡
     */
    public static int getMaxInstallable(ItemStack card, Item machineItem) {
        if (card.isEmpty()) {
            return 0;
        }
        if (machineItem == null) {
            return 0;
        }
        // 对齐 1.21.1 源码：是否接受、能装几张，完全由 Upgrades 注册表决定。
        // 其它 mod 的升级卡若要支持某台机器，由它自己（或脚本）调用
        // Upgrades.add(card, 机器物品, 上限) 登记即可，本模组不需要硬编码。
        return Upgrades.getMaxInstallable(card.getItem(), machineItem);
    }

    /** 统计升级槽中该卡的数量（excludeSlot < 0 表示不排除） */
    public static int countInstalled(IInventory inv, int upgradeStart, int upgradeEnd,
                                     Item card, int excludeSlot) {
        int count = 0;
        for (int i = upgradeStart; i < upgradeEnd; i++) {
            if (i == excludeSlot) {
                continue;
            }
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == card) {
                count++;
            }
        }
        return count;
    }

    /**
     * 升级槽放行判断：卡被这台机器接受，且未超过上限。
     *
     * @param slot 正在放入的升级槽索引
     */
    public static boolean canInsert(IInventory inv, int upgradeStart, int upgradeEnd, int slot,
                                    ItemStack card, Item machineItem) {
        if (card.isEmpty()) {
            return true;
        }
        int max = getMaxInstallable(card, machineItem);
        if (max <= 0) {
            return false;
        }
        return countInstalled(inv, upgradeStart, upgradeEnd, card.getItem(), slot) < max;
    }
}
