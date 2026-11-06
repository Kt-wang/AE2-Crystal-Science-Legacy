/*
 * AE2 Crystal Science - CraftTweaker support: Quartz Grindstone
 *
 * The grindstone shares its built-in recipe set with the Crystal Pulverizer, but the
 * two lists are independent: changes here do not affect the pulverizer and vice versa.
 *
 * ZenScript example:
 *   mods.ae2csl.QuartzGrindstone.addRecipe(<minecraft:sand>, <minecraft:gravel>, 4, 20);
 *   mods.ae2csl.QuartzGrindstone.removeRecipe(<ore:dustQuartz>);
 *   mods.ae2csl.QuartzGrindstone.removeAll();
 */
package ae2csl.integration.crafttweaker;

import ae2csl.recipe.AE2CSRecipeTypes;
import ae2csl.recipe.crystal_pulverizer.CrystalPulverizerRecipe;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.ae2csl.QuartzGrindstone")
public final class ZenQuartzGrindstone {

    private static final String MACHINE = "Quartz Grindstone";

    private ZenQuartzGrindstone() {}

    @ZenMethod
    public static void addRecipe(IItemStack output, IIngredient input, int energyCost, int processTime) {
        ItemStack out = CrTHelper.toOutputStack(output, MACHINE);
        if (out == null) return;
        if (input == null) {
            CraftTweakerAPI.logError("[AE2CSL] " + MACHINE + ": input ingredient cannot be null");
            return;
        }
        if (!CrTHelper.validateTimings(MACHINE, energyCost, processTime)) return;

        Ingredient in = CrTHelper.toIngredient(input);
        CrystalPulverizerRecipe recipe = new CrystalPulverizerRecipe(in, out, energyCost, processTime);
        CraftTweakerAPI.apply(new CrTRecipeActions.Add(AE2CSRecipeTypes.QUARTZ_GRINDSTONE, recipe,
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
        CraftTweakerAPI.apply(new CrTRecipeActions.Remove(AE2CSRecipeTypes.QUARTZ_GRINDSTONE,
            r -> matcher.apply(((CrystalPulverizerRecipe) r).getResult()), MACHINE,
            "Removing " + MACHINE + " recipes with output matching " + output.toString()));
    }

    @ZenMethod
    public static void removeAll() {
        CraftTweakerAPI.apply(new CrTRecipeActions.RemoveAll(AE2CSRecipeTypes.QUARTZ_GRINDSTONE, MACHINE));
    }
}
