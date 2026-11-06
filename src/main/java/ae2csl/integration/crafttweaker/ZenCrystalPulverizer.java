/*
 * AE2 Crystal Science - CraftTweaker support: Crystal Pulverizer
 *
 * ZenScript example:
 *   mods.ae2csl.CrystalPulverizer.addRecipe(<minecraft:gravel>, <minecraft:cobblestone>, 800, 100);
 *   mods.ae2csl.CrystalPulverizer.removeRecipe(<ore:dustCertusQuartz>);
 *   mods.ae2csl.CrystalPulverizer.removeAll();
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
@ZenClass("mods.ae2csl.CrystalPulverizer")
public final class ZenCrystalPulverizer {

    private static final String MACHINE = "Crystal Pulverizer";

    private ZenCrystalPulverizer() {}

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
        CraftTweakerAPI.apply(new CrTRecipeActions.Add(AE2CSRecipeTypes.CRYSTAL_PULVERIZER, recipe,
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
        CraftTweakerAPI.apply(new CrTRecipeActions.Remove(AE2CSRecipeTypes.CRYSTAL_PULVERIZER,
            r -> matcher.apply(((CrystalPulverizerRecipe) r).getResult()), MACHINE,
            "Removing " + MACHINE + " recipes with output matching " + output.toString()));
    }

    @ZenMethod
    public static void removeAll() {
        CraftTweakerAPI.apply(new CrTRecipeActions.RemoveAll(AE2CSRecipeTypes.CRYSTAL_PULVERIZER, MACHINE));
    }
}
