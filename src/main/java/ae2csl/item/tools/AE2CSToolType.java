/*
 * AE2 Crystal Science - AE2CSToolType Enum
 * Ported from 1.21.1 source
 *
 * Defines tool material tiers for AE2CS crystal tools.
 * Replaces Ae2csToolMaterials with source-accurate tier definitions.
 */
package ae2csl.item.tools;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;

public enum AE2CSToolType {

    METEOR("meteor",
            (int) (net.minecraft.item.Item.ToolMaterial.DIAMOND.getMaxUses() * 0.9),
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getEfficiency(),
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getAttackDamage(),
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getHarvestLevel(),
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getEnchantability()
    ),
    ENDER("ender",
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getMaxUses(),
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getEfficiency(),
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getAttackDamage(),
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getHarvestLevel(),
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getEnchantability()
    ),
    RESONATING("resonating",
            (int) (net.minecraft.item.Item.ToolMaterial.DIAMOND.getMaxUses() * 1.8),
            net.minecraft.item.Item.ToolMaterial.GOLD.getEfficiency(),
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getAttackDamage(),
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getHarvestLevel(),
            net.minecraft.item.Item.ToolMaterial.DIAMOND.getEnchantability()
    );

    private final String name;
    private final Item.ToolMaterial toolMaterial;

    AE2CSToolType(String name, int maxUses, float efficiency, float attackDamage, int harvestLevel, int enchantability) {
        this.name = name;
        this.toolMaterial = EnumHelper.addToolMaterial(
                "ae2csl_" + name,
                harvestLevel,
                maxUses,
                efficiency,
                attackDamage,
                enchantability
        );
    }

    public String getName() {
        return name;
    }

    public Item.ToolMaterial getToolMaterial() {
        return toolMaterial;
    }

    /**
     * Get the repair item stack for this tool type.
     * Actual crystal item reference is wired via the init system.
     */
    public ItemStack getRepairItemStack() {
        return ItemStack.EMPTY; // Will be wired when crystal items are fully registered
    }
}
