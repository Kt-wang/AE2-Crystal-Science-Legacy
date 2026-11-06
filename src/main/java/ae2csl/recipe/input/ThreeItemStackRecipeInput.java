/*
 * AE2 Crystal Science - Three Item Stack Recipe Input
 * Ported from 1.21.1: common/recipe/input/ThreeItemStackRecipeInput.java
 */
package ae2csl.recipe.input;

import net.minecraft.item.ItemStack;

/** Represents the 3 input slots of a machine like CircuitEtcher */
public class ThreeItemStackRecipeInput {
    private final ItemStack inputA, inputB, inputC;

    public ThreeItemStackRecipeInput(ItemStack a, ItemStack b, ItemStack c) {
        this.inputA = a; this.inputB = b; this.inputC = c;
    }

    public ItemStack getInputA() { return inputA; }
    public ItemStack getInputB() { return inputB; }
    public ItemStack getInputC() { return inputC; }

    public ItemStack getSlot(int i) {
        switch (i) { case 0: return inputA; case 1: return inputB; case 2: return inputC; default: return ItemStack.EMPTY; }
    }
}
