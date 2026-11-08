package ae2csl.item.tools.resonating;

import ae2.hooks.IntrinsicEnchantItem;
import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.IntrinsicEnchantment;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

public class ResonatingPickaxeItem extends ItemPickaxe implements LinkableTool, IntrinsicEnchantItem {

    private final IntrinsicEnchantment intrinsicEnchantment =
        new IntrinsicEnchantment(Enchantments.FORTUNE, 3);

    public ResonatingPickaxeItem() {
        super(AE2CSToolType.RESONATING.getToolMaterial());
        setTranslationKey("resonating_pickaxe");
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
