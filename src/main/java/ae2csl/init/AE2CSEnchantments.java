/*
 * AE2 Crystal Science - AE2CSEnchantments (1.12.2)
 * Ported from 1.21.1 source
 *
 * In 1.21.1, enchantments are data-driven via BootstrapContext.
 * In 1.12.2, enchantments are registered via direct instantiation.
 */
package ae2csl.init;

import ae2csl.Tags;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;

import java.util.ArrayList;
import java.util.List;

public final class AE2CSEnchantments {

    private static final List<Enchantment> REGISTRY = new ArrayList<>();

    /**
     * Ender Link - hidden marker enchantment for tool wireless linking.
     */
    public static final Enchantment ENDER_LINK;

    static {
        ENDER_LINK = new Enchantment(Enchantment.Rarity.COMMON, EnumEnchantmentType.ALL,
                new EntityEquipmentSlot[] { EntityEquipmentSlot.MAINHAND }) {
            @Override
            public int getMinEnchantability(int level) { return 1; }
            @Override
            public int getMaxEnchantability(int level) { return 1; }
            @Override
            public int getMaxLevel() { return 1; }
            @Override
            public boolean isTreasureEnchantment() { return true; }
            @Override
            public boolean canApplyAtEnchantingTable(net.minecraft.item.ItemStack stack) { return false; }
        };
        ENDER_LINK.setRegistryName(Tags.MOD_ID, "ender_link");
        ENDER_LINK.setName("ae2csl.ender_link");
        REGISTRY.add(ENDER_LINK);
    }

    public static List<Enchantment> getEnchantments() {
        return REGISTRY;
    }

    private AE2CSEnchantments() {}
}
