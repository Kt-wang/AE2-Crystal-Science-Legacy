/*
 * AE2 Crystal Science - ExtendedResonatingPatternProviderUpgradeItem (1.12.2)
 * 对照源码: 右键谐振样板供应器方块 → 替换为扩展谐振样板供应器（保留TE数据）。
 * 部件替换走合成配方（extended_resonating_pattern_provider swap 配方）。
 */
package ae2csl.item.upgrades;

import ae2csl.init.AE2CSBlocks;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ExtendedResonatingPatternProviderUpgradeItem extends UpgradeItem {

    public ExtendedResonatingPatternProviderUpgradeItem() {
        super();
        setTranslationKey("extended_resonating_pattern_provider_upgrade");

        registerBlockReplaceInfo(() -> AE2CSBlocks.RESONATING_PATTERN_PROVIDER,
            () -> AE2CSBlocks.EX_RESONATING_PATTERN_PROVIDER_BLOCK);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format(
            "ae2csl.item.extended_resonating_pattern_provider_upgrade.tooltip"));
    }
}
