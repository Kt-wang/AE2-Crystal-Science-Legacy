/*
 * AE2 Crystal Science - ExtendedEnderInterfaceUpgradeItem (1.12.2)
 * 对照源码: 右键末影接口方块 → 替换为扩展版（吸收配置36槽，保留TE数据）。
 */
package ae2csl.item.upgrades;

import ae2csl.init.AE2CSBlocks;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ExtendedEnderInterfaceUpgradeItem extends UpgradeItem {

    public ExtendedEnderInterfaceUpgradeItem() {
        super();
        setTranslationKey("extended_ender_interface_upgrade");

        registerBlockReplaceInfo(() -> AE2CSBlocks.ENDER_INTERFACE_BLOCK,
            () -> AE2CSBlocks.EX_ENDER_INTERFACE_BLOCK);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format(
            "ae2csl.item.extended_ender_interface_upgrade.tooltip"));
    }
}
