/*
 * AE2 Crystal Science - ExtendedIntegratedInterfaceUpgradeItem (1.12.2)
 * 对照源码: 右键集成接口方块 → 替换为扩展版集成接口（36槽，保留TE数据）。
 */
package ae2csl.item.upgrades;

import ae2csl.init.AE2CSBlocks;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ExtendedIntegratedInterfaceUpgradeItem extends UpgradeItem {

    public ExtendedIntegratedInterfaceUpgradeItem() {
        super();
        setTranslationKey("extended_integrated_interface_upgrade");

        registerBlockReplaceInfo(() -> AE2CSBlocks.INTEGRATED_INTERFACE,
            () -> AE2CSBlocks.EX_INTEGRATED_INTERFACE_BLOCK);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format(
            "ae2csl.item.extended_integrated_interface_upgrade.tooltip"));
    }
}
