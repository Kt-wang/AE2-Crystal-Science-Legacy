/*
 * AE2 Crystal Science - ResonatingPatternConverterItem (1.12.2)
 * 谐振样板转换器：手持右键打开 GUI，左侧放处理样板 → 右侧出谐振样板。
 * 对照源码 ResonatingPatternConverterItem + ResonatingPatternConverterMenuHost。
 */
package ae2csl.item.linker;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import java.util.List;

public class ResonatingPatternConverterItem extends Item {

    public ResonatingPatternConverterItem() {
        setMaxStackSize(1);
        setTranslationKey("resonating_pattern_converter");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (hand == EnumHand.MAIN_HAND) {
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_RESONATING_PATTERN_CONVERTER,
                world, 0, 0, 0);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format(
            "ae2csl.item.resonating_pattern_converter.tooltip"));
    }
}
