/*
 * AE2 Crystal Science - Crystal Pulverizer Recipe
 * Ported from 1.21.1: common/recipe/crystal_pulverizer/CrystalPulverizerRecipe.java
 */
package ae2csl.recipe.crystal_pulverizer;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import javax.annotation.Nullable;

public class CrystalPulverizerRecipe {

    private final Ingredient input;
    private final ItemStack result;
    private final int energyCost;
    private final int processTime;

    public CrystalPulverizerRecipe(Ingredient input, ItemStack result, int energyCost, int processTime) {
        this.input = input;
        this.result = result;
        this.energyCost = energyCost;
        this.processTime = processTime;
    }

    public Ingredient getInput() { return input; }
    public ItemStack getResult() { return result.copy(); }
    public int getEnergyCost() { return energyCost; }
    public int getProcessTime() { return processTime; }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && input.apply(stack);
    }

    @Nullable
    public ItemStack assemble(ItemStack input) {
        if (!matches(input)) return null;
        return result.copy();
    }
}
