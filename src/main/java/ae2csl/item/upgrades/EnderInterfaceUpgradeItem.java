/*
 * AE2 Crystal Science - EnderInterfaceUpgradeItem (1.12.2)
 * 对照源码: 右键 AE2 接口方块 → 替换为末影接口（保留TE数据）。
 */
package ae2csl.item.upgrades;

import ae2.core.definitions.AEBlocks;
import ae2csl.init.AE2CSBlocks;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class EnderInterfaceUpgradeItem extends UpgradeItem {

    public EnderInterfaceUpgradeItem() {
        super();
        setTranslationKey("ender_interface_upgrade");

        registerBlockReplaceInfo(() -> AEBlocks.INTERFACE.block(), () -> AE2CSBlocks.ENDER_INTERFACE_BLOCK);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format(
            "ae2csl.item.ender_interface_upgrade.tooltip"));
    }
}
