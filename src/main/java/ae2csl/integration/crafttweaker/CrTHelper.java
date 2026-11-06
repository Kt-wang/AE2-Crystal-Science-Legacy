/*
 * AE2 Crystal Science - CraftTweaker conversion helpers
 */
package ae2csl.integration.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

final class CrTHelper {

    private CrTHelper() {}

    /** Convert a ZenScript item stack, logging an error and returning null when invalid. */
    static ItemStack toOutputStack(IItemStack stack, String machineName) {
        if (stack == null) {
            CraftTweakerAPI.logError("[AE2CSL] " + machineName + ": output stack cannot be null");
            return null;
        }
        ItemStack mc = CraftTweakerMC.getItemStack(stack);
        if (mc.isEmpty()) {
            CraftTweakerAPI.logError("[AE2CSL] " + machineName + ": output stack cannot be empty");
            return null;
        }
        return mc;
    }

    /** Convert a ZenScript ingredient; null becomes Ingredient.EMPTY. */
    static Ingredient toIngredient(IIngredient ingredient) {
        return ingredient == null ? Ingredient.EMPTY : CraftTweakerMC.getIngredient(ingredient);
    }

    static boolean isEmpty(Ingredient ingredient) {
        return ingredient == null || ingredient == Ingredient.EMPTY || ingredient.getMatchingStacks().length == 0;
    }

    static boolean validateTimings(String machineName, int energyCost, int processTime) {
        if (energyCost < 0) {
            CraftTweakerAPI.logError("[AE2CSL] " + machineName + ": energyCost must be >= 0, got " + energyCost);
            return false;
        }
        if (processTime <= 0) {
            CraftTweakerAPI.logError("[AE2CSL] " + machineName + ": processTime must be > 0, got " + processTime);
            return false;
        }
        return true;
    }
}
