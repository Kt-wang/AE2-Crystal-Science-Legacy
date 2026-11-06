/*
 * AE2 Crystal Science - shared validation for 3-input machine Zen classes
 */
package ae2csl.integration.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.api.item.IIngredient;
import net.minecraft.item.crafting.Ingredient;

final class ZenThreeInputSupport {

    private ZenThreeInputSupport() {}

    /**
     * Validate the inputs/counts arrays and pad them to length 3.
     * Returns a length-3 Ingredient array, or null after logging an error.
     * The returned counts are guaranteed readable at indices 0..len-1 by the caller.
     */
    static Ingredient[] validate(String machineName, IIngredient[] inputs, int[] counts) {
        if (inputs == null || inputs.length == 0 || inputs.length > 3) {
            CraftTweakerAPI.logError("[AE2CSL] " + machineName + ": expected 1-3 input ingredients");
            return null;
        }
        if (counts == null || counts.length != inputs.length) {
            CraftTweakerAPI.logError("[AE2CSL] " + machineName + ": counts array must have the same length as inputs");
            return null;
        }
        for (int count : counts) {
            if (count < 1) {
                CraftTweakerAPI.logError("[AE2CSL] " + machineName + ": input counts must be >= 1");
                return null;
            }
        }

        Ingredient[] ingredients = new Ingredient[]{Ingredient.EMPTY, Ingredient.EMPTY, Ingredient.EMPTY};
        int nonEmpty = 0;
        for (int i = 0; i < inputs.length; i++) {
            Ingredient ing = CrTHelper.toIngredient(inputs[i]);
            if (!CrTHelper.isEmpty(ing)) nonEmpty++;
            ingredients[i] = ing;
        }
        if (nonEmpty == 0) {
            CraftTweakerAPI.logError("[AE2CSL] " + machineName + ": at least one non-empty input ingredient is required");
            return null;
        }
        return ingredients;
    }
}
