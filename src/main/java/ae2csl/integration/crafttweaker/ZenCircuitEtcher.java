/*
 * AE2 Crystal Science - CraftTweaker support: Circuit Etcher
 *
 * ZenScript examples:
 *   // 1-3 inputs, each consuming 1 item; unused slots are null
 *   mods.ae2csl.CircuitEtcher.addRecipe(<minecraft:redstone>, <minecraft:gold_ingot>, null, null, 1600, 200);
 *
 *   // Per-input consumed amounts (arrays must have the same length, 1-3 entries)
 *   mods.ae2csl.CircuitEtcher.addRecipeWithCounts(<minecraft:comparator>,
 *       [<ore:gemQuartz>, <minecraft:redstone>, <ore:stone>], [1, 2, 1], 1600, 200);
 *
 *   mods.ae2csl.CircuitEtcher.removeRecipe(<appliedenergistics2:material:24>);
 *   mods.ae2csl.CircuitEtcher.removeAll();
 */
package ae2csl.integration.crafttweaker;

import ae2csl.recipe.AE2CSRecipeTypes;
import ae2csl.recipe.circuit_etcher.CircuitEtcherRecipe;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ae2csl.CircuitEtcher")
public final class ZenCircuitEtcher {

    private static final String MACHINE = "Circuit Etcher";

    private ZenCircuitEtcher() {}

    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient inputA, IIngredient inputB, IIngredient inputC,
                                 int energyCost, int processTime) {
        addRecipeWithCounts(output, new IIngredient[]{inputA, inputB, inputC}, new int[]{1, 1, 1}, energyCost, processTime);
    }

    @ZenMethod
    public static void addRecipeWithCounts(IItemStack output, IIngredient[] inputs, int[] counts,
                                           int energyCost, int processTime) {
        ItemStack out = CrTHelper.toOutputStack(output, MACHINE);
        if (out == null) return;
        if (!CrTHelper.validateTimings(MACHINE, energyCost, processTime)) return;

        Ingredient[] ingredients = ZenThreeInputSupport.validate(MACHINE, inputs, counts);
        if (ingredients == null) return;

        CircuitEtcherRecipe recipe = new CircuitEtcherRecipe(
            ingredients[0], ingredients[1], ingredients[2], out, energyCost, processTime,
            counts[0], counts.length > 1 ? counts[1] : 1, counts.length > 2 ? counts[2] : 1);
        CraftTweakerAPI.apply(new CrTRecipeActions.Add(AE2CSRecipeTypes.CIRCUIT_ETCHER, recipe,
            "Adding " + MACHINE + " recipe for " + out.getDisplayName()));
    }

    /** Remove every recipe whose result matches the given ingredient. */
    @ZenMethod
    public static void removeRecipe(IIngredient output) {
        if (output == null) {
            CraftTweakerAPI.logError("[AE2CSL] " + MACHINE + ": output matcher cannot be null");
            return;
        }
        Ingredient matcher = CrTHelper.toIngredient(output);
        CraftTweakerAPI.apply(new CrTRecipeActions.Remove(AE2CSRecipeTypes.CIRCUIT_ETCHER,
            r -> matcher.apply(((CircuitEtcherRecipe) r).getResult()), MACHINE,
            "Removing " + MACHINE + " recipes with output matching " + output.toString()));
    }

    @ZenMethod
    public static void removeAll() {
        CraftTweakerAPI.apply(new CrTRecipeActions.RemoveAll(AE2CSRecipeTypes.CIRCUIT_ETCHER, MACHINE));
    }
}
