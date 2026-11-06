/*
 * AE2 Crystal Science - ResonatingPatternProviderUpgradeItem (1.12.2)
 * 对照源码: 右键 AE2 样板供应器方块 → 替换为谐振样板供应器（保留TE数据）。
 * 部件替换走合成配方（resonating_pattern_provider swap 配方）。
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

public class ResonatingPatternProviderUpgradeItem extends UpgradeItem {

    public ResonatingPatternProviderUpgradeItem() {
        super();
        setTranslationKey("resonating_pattern_provider_upgrade");

        registerBlockReplaceInfo(() -> AEBlocks.PATTERN_PROVIDER.block(),
            () -> AE2CSBlocks.RESONATING_PATTERN_PROVIDER);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format(
            "ae2csl.item.resonating_pattern_provider_upgrade.tooltip"));
    }
}
