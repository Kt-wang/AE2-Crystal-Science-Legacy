/*
 * AE2 Crystal Science - Circuit Etcher Recipe
 * Ported from 1.21.1: common/recipe/circuit_etcher/CircuitEtcherRecipe.java
 */
package ae2csl.recipe.circuit_etcher;

import ae2csl.recipe.input.ThreeItemStackRecipeInput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CircuitEtcherRecipe {

    private final Ingredient inputA, inputB, inputC;
    private final ItemStack result;
    private final int energyCost;
    private final int processTime;
    private final List<Ingredient> effective;
    private final List<Integer> effectiveCounts;

    public CircuitEtcherRecipe(Ingredient inputA, Ingredient inputB, Ingredient inputC,
                                ItemStack result, int energyCost, int processTime,
                                int countA, int countB, int countC) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.inputC = inputC;
        this.result = result;
        this.energyCost = energyCost;
        this.processTime = processTime;

        this.effective = new ArrayList<>(3);
        this.effectiveCounts = new ArrayList<>(3);
        addIfRequired(inputA, countA);
        addIfRequired(inputB, countB);
        addIfRequired(inputC, countC);
    }

    /** Backward-compatible constructor (count defaults to 1) */
    public CircuitEtcherRecipe(Ingredient inputA, Ingredient inputB, Ingredient inputC,
                                ItemStack result, int energyCost, int processTime) {
        this(inputA, inputB, inputC, result, energyCost, processTime, 1, 1, 1);
    }

    private void addIfRequired(Ingredient ing, int count) {
        if (ing != null && ing != Ingredient.EMPTY && ing.getMatchingStacks().length > 0) {
            effective.add(ing);
            effectiveCounts.add(count);
        }
    }

    public List<Ingredient> getEffective() { return effective; }
    public List<Integer> getEffectiveCounts() { return effectiveCounts; }
    public ItemStack getResult() { return result.copy(); }
    public int getEnergyCost() { return energyCost; }
    public int getProcessTime() { return processTime; }

    public boolean matches(ThreeItemStackRecipeInput in) {
        return findMatch(in) != null;
    }

    /**
     * Returns slot mapping: map[t] = which machine slot effective.get(t) should take from.
     * Returns null if no match.
     */
    @Nullable
    public int[] findMatch(ThreeItemStackRecipeInput in) {
        ItemStack[] stacks = { in.getInputA(), in.getInputB(), in.getInputC() };
        int size = effective.size();
        if (size == 0) return null;

        if (size == 1) {
            Ingredient A = effective.get(0);
            int cntA = effectiveCounts.get(0);
            for (int i = 0; i < 3; i++) {
                if (A.apply(stacks[i]) && stacks[i].getCount() >= cntA) return new int[] { i };
            }
            return null;
        }
        if (size == 2) {
            Ingredient A = effective.get(0), B = effective.get(1);
            int cntA = effectiveCounts.get(0), cntB = effectiveCounts.get(1);
            for (int i = 0; i < 3; i++) {
                if (!A.apply(stacks[i]) || stacks[i].getCount() < cntA) continue;
                for (int j = 0; j < 3; j++) {
                    if (j == i) continue;
                    if (B.apply(stacks[j]) && stacks[j].getCount() >= cntB) return new int[] { i, j };
                }
            }
            return null;
        }
        // size == 3
        Ingredient A = effective.get(0), B = effective.get(1), C = effective.get(2);
        int cntA = effectiveCounts.get(0), cntB = effectiveCounts.get(1), cntC = effectiveCounts.get(2);
        for (int i = 0; i < 3; i++) {
            if (!A.apply(stacks[i]) || stacks[i].getCount() < cntA) continue;
            for (int j = 0; j < 3; j++) {
                if (j == i) continue;
                if (!B.apply(stacks[j]) || stacks[j].getCount() < cntB) continue;
                int k = 3 - i - j;
                if (C.apply(stacks[k]) && stacks[k].getCount() >= cntC) return new int[] { i, j, k };
            }
        }
        return null;
    }

    public void consumeInputs(ThreeItemStackRecipeInput in) {
        int[] mapping = findMatch(in);
        if (mapping != null) {
            for (int i = 0; i < effective.size(); i++) {
                ItemStack stack = in.getSlot(mapping[i]);
                if (!stack.isEmpty()) stack.shrink(effectiveCounts.get(i));
            }
        }
    }

    public ItemStack assemble(ThreeItemStackRecipeInput in) {
        return result.copy();
    }

    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(effective);
        return list;
    }
}
