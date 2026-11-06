/*
 * AE2 Crystal Science - CraftTweaker support: Crystal Aggregator
 *
 * ZenScript examples:
 *   // 1-3 inputs, each consuming 1 item; unused slots are null
 *   mods.ae2csl.CrystalAggregator.addRecipe(<minecraft:diamond>, <minecraft:coal>, <minecraft:redstone>, null, 2000, 200);
 *
 *   // Per-input consumed amounts (arrays must have the same length, 1-3 entries)
 *   mods.ae2csl.CrystalAggregator.addRecipeWithCounts(<minecraft:emerald>,
 *       [<ore:gemQuartz>, <minecraft:redstone>], [2, 4], 2000, 200);
 *
 *   mods.ae2csl.CrystalAggregator.removeRecipe(<ae2:material:0>);
 *   mods.ae2csl.CrystalAggregator.removeAll();
 */
package ae2csl.integration.crafttweaker;

import ae2csl.recipe.AE2CSRecipeTypes;
import ae2csl.recipe.crystal_aggregator.CrystalAggregatorRecipe;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ae2csl.CrystalAggregator")
public final class ZenCrystalAggregator {

    private static final String MACHINE = "Crystal Aggregator";

    private ZenCrystalAggregator() {}

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

        CrystalAggregatorRecipe recipe = new CrystalAggregatorRecipe(
            ingredients[0], ingredients[1], ingredients[2], out, energyCost, processTime,
            counts[0], counts.length > 1 ? counts[1] : 1, counts.length > 2 ? counts[2] : 1);
        CraftTweakerAPI.apply(new CrTRecipeActions.Add(AE2CSRecipeTypes.CRYSTAL_AGGREGATOR, recipe,
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
        CraftTweakerAPI.apply(new CrTRecipeActions.Remove(AE2CSRecipeTypes.CRYSTAL_AGGREGATOR,
            r -> matcher.apply(((CrystalAggregatorRecipe) r).getResult()), MACHINE,
            "Removing " + MACHINE + " recipes with output matching " + output.toString()));
    }

    @ZenMethod
    public static void removeAll() {
        CraftTweakerAPI.apply(new CrTRecipeActions.RemoveAll(AE2CSRecipeTypes.CRYSTAL_AGGREGATOR, MACHINE));
    }
}
