/*
 * AE2 Crystal Science - IntrinsicEnchantment
 * Ported from 1.21.1 source (copy from AE2)
 *
 * 内置附魔：不写入物品 NBT，仅在 EnchantmentHelper 查询时返回等级，
 * 并在 tooltip 里显示「自带 xxx」。
 */
package ae2csl.item.tools;

import ae2.core.localization.GuiText;
import net.minecraft.enchantment.Enchantment;

import java.util.List;

public final class IntrinsicEnchantment {

    private final Enchantment enchantment;
    private final int level;

    public IntrinsicEnchantment(Enchantment enchantment, int level) {
        this.enchantment = enchantment;
        this.level = level;
    }

    public void appendHoverText(List<String> tooltip) {
        tooltip.add(GuiText.IntrinsicEnchant.getLocal(enchantment.getTranslatedName(level)));
    }

    public int getLevel(Enchantment other) {
        return other == enchantment ? level : 0;
    }
}
