/*
 * AE2 Crystal Science - UpgradeCardHelper (1.12.2)
 * 升级卡判断小工具：陨石超频卡（overload_card）。
 */
package ae2csl.util;

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
}
