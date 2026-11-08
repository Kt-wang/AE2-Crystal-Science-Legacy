package ae2csl.item.tools.resonating;

import ae2.hooks.IntrinsicEnchantItem;
import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.IntrinsicEnchantment;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

public class ResonatingHoeItem extends ItemHoe implements LinkableTool, IntrinsicEnchantItem {

    private final IntrinsicEnchantment intrinsicEnchantment =
        new IntrinsicEnchantment(Enchantments.EFFICIENCY, 5);

    public ResonatingHoeItem() {
        super(AE2CSToolType.RESONATING.getToolMaterial());
        setTranslationKey("resonating_hoe");
    }

    @Override
    public Integer getIntrinsicEnchantLevel(ItemStack stack, Enchantment enchantment) {
        return intrinsicEnchantment.getLevel(enchantment);
    }

    @Override
    public void addInformation(@Nonnull ItemStack stack, World world, @Nonnull List<String> tooltip,
                               @Nonnull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        intrinsicEnchantment.appendHoverText(tooltip);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
