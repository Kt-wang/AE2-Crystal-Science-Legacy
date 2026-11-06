/*
 * AE2 Crystal Science - Crystal Aggregator Recipe
 * Ported from 1.21.1: common/recipe/crystal_aggregator/CrystalAggregatorRecipe.java
 *
 * Fixed 3-input (A/B/C) format with per-ingredient counts — matches source structure.
 */
package ae2csl.recipe.crystal_aggregator;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CrystalAggregatorRecipe {

    private final Ingredient inputA, inputB, inputC;
    private final ItemStack result;
    private final int energyCost;
    private final int processTime;
    private final List<Ingredient> effective;
    private final List<Integer> effectiveCounts;

    public CrystalAggregatorRecipe(Ingredient inputA, Ingredient inputB, Ingredient inputC,
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
    public CrystalAggregatorRecipe(Ingredient inputA, Ingredient inputB, Ingredient inputC,
                                    ItemStack result, int energyCost, int processTime) {
        this(inputA, inputB, inputC, result, energyCost, processTime, 1, 1, 1);
    }

    private void addIfRequired(Ingredient ing, int count) {
        if (ing != null && ing != Ingredient.EMPTY && ing.getMatchingStacks().length > 0) {
            effective.add(ing);
            effectiveCounts.add(count);
        }
    }

    public List<Ingredient> getInputs() { return effective; }
    public List<Integer> getEffectiveCounts() { return effectiveCounts; }
    public ItemStack getResult() { return result.copy(); }
    public int getEnergyCost() { return energyCost; }
    public int getProcessTime() { return processTime; }

    public boolean matches(List<ItemStack> available) {
        return findMatch(available) != null;
    }

    /**
     * Returns slot mapping: map[t] = which machine slot effective.get(t) should take from.
     * Returns null if no match.
     */
    @Nullable
    public int[] findMatch(List<ItemStack> available) {
        int size = effective.size();
        if (size == 0 || available.size() < 3) return null;

        ItemStack a = available.get(0), b = available.get(1), c = available.get(2);

        if (size == 1) {
            Ingredient A = effective.get(0);
            int cntA = effectiveCounts.get(0);
            if (A.apply(a) && a.getCount() >= cntA) return new int[] { 0 };
            if (A.apply(b) && b.getCount() >= cntA) return new int[] { 1 };
            if (A.apply(c) && c.getCount() >= cntA) return new int[] { 2 };
            return null;
        }
        if (size == 2) {
            Ingredient A = effective.get(0), B = effective.get(1);
            int cntA = effectiveCounts.get(0), cntB = effectiveCounts.get(1);
            for (int i = 0; i < 3; i++) {
                ItemStack si = available.get(i);
                if (!A.apply(si) || si.getCount() < cntA) continue;
                for (int j = 0; j < 3; j++) {
                    if (j == i) continue;
                    ItemStack sj = available.get(j);
                    if (B.apply(sj) && sj.getCount() >= cntB) return new int[] { i, j };
                }
            }
            return null;
        }
        // size == 3
        Ingredient A = effective.get(0), B = effective.get(1), C = effective.get(2);
        int cntA = effectiveCounts.get(0), cntB = effectiveCounts.get(1), cntC = effectiveCounts.get(2);
        for (int i = 0; i < 3; i++) {
            ItemStack si = available.get(i);
            if (!A.apply(si) || si.getCount() < cntA) continue;
            for (int j = 0; j < 3; j++) {
                if (j == i) continue;
                ItemStack sj = available.get(j);
                if (!B.apply(sj) || sj.getCount() < cntB) continue;
                int k = 3 - i - j;
                ItemStack sk = available.get(k);
                if (C.apply(sk) && sk.getCount() >= cntC) return new int[] { i, j, k };
            }
        }
        return null;
    }

    public void consumeInputs(List<ItemStack> items) {
        int[] mapping = findMatch(items);
        if (mapping != null) {
            for (int i = 0; i < effective.size(); i++) {
                ItemStack stack = items.get(mapping[i]);
                if (!stack.isEmpty()) stack.shrink(effectiveCounts.get(i));
            }
        }
    }

    public ItemStack assemble(List<ItemStack> inputs) {
        return result.copy();
    }
}
