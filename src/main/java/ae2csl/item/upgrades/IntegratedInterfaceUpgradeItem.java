/*
 * AE2 Crystal Science - IntegratedInterfaceUpgradeItem (1.12.2)
 * 对照源码: 右键 AE2 接口/样板供应器方块 → 替换为集成接口（保留TE数据）。
 * 部件替换走合成配方（integrated_interface_from_integrated_interface_part_swap 等）。
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

public class IntegratedInterfaceUpgradeItem extends UpgradeItem {

    public IntegratedInterfaceUpgradeItem() {
        super();
        setTranslationKey("integrated_interface_upgrade");

        registerBlockReplaceInfo(() -> AEBlocks.INTERFACE.block(), () -> AE2CSBlocks.INTEGRATED_INTERFACE);
        registerBlockReplaceInfo(() -> AEBlocks.PATTERN_PROVIDER.block(), () -> AE2CSBlocks.INTEGRATED_INTERFACE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format(
            "ae2csl.item.integrated_interface_upgrade.tooltip"));
    }
}
