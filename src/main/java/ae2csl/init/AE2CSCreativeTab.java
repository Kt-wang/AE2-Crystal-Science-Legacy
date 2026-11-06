/*
 * AE2 Crystal Science - Creative Tab
 * Ported from 1.21.1: common/init/AECSCreativeModeTabs.java
 */
package ae2csl.init;

import ae2csl.Tags;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class AE2CSCreativeTab {
    public static final CreativeTabs INSTANCE = new CreativeTabs(Tags.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            // 创造物品栏图标：ME末影广播装置
            return new ItemStack(net.minecraft.item.Item.getItemFromBlock(AE2CSBlocks.ENDER_BROADCASTER));
        }
    };
}
