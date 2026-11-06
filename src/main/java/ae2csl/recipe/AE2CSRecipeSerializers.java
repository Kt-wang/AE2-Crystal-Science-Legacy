/*
 * AE2 Crystal Science - Recipe Serializers (JSON → Recipe)
 * Ported from 1.21.1: common/init/AE2CSRecipeSerializers.java
 */
package ae2csl.recipe;

import ae2csl.recipe.circuit_etcher.CircuitEtcherRecipe;
import ae2csl.recipe.crystal_aggregator.CrystalAggregatorRecipe;
import ae2csl.recipe.crystal_pulverizer.CrystalPulverizerRecipe;

import com.google.gson.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.*;

/** JSON deserializers for each recipe type. */
public class AE2CSRecipeSerializers {

    /** Parse a CircuitEtcher recipe from JSON */
    public static CircuitEtcherRecipe parseCircuitEtcher(JsonObject json, JsonContext ctx) {
        Ingredient a = parseOptionalIngredientWithCount(json, "inputA", ctx);
        int countA = getInputCount(json, "inputA");
        Ingredient b = parseOptionalIngredientWithCount(json, "inputB", ctx);
        int countB = getInputCount(json, "inputB");
        Ingredient c = parseOptionalIngredientWithCount(json, "inputC", ctx);
        int countC = getInputCount(json, "inputC");
        ItemStack result = deserializeItemStack(JsonUtils.getJsonObject(json, "result"), ctx);
        int energy = JsonUtils.getInt(json, "energyCost", 200);
        int time = JsonUtils.getInt(json, "processTime", 200);
        return new CircuitEtcherRecipe(a, b, c, result, energy, time, countA, countB, countC);
    }

    private static int getInputCount(JsonObject json, String key) {
        if (!json.has(key)) return 1;
        return JsonUtils.getInt(json.getAsJsonObject(key), "count", 1);
    }

    /** Parses an ingredient and strips the 'count' field so CraftingHelper doesn't see it */
    private static Ingredient parseOptionalIngredientWithCount(JsonObject json, String key, JsonContext ctx) {
        if (!json.has(key)) return Ingredient.EMPTY;
        JsonObject obj = json.getAsJsonObject(key);
        if (obj.entrySet().isEmpty()) return Ingredient.EMPTY;
        // Build a clean JSON object without 'count' — deepCopy/remove can corrupt Gson internals
        JsonObject clean = new JsonObject();
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            if (!"count".equals(e.getKey())) clean.add(e.getKey(), e.getValue());
        }
        if (clean.entrySet().isEmpty()) return Ingredient.EMPTY;
        return CraftingHelper.getIngredient(clean, ctx);
    }

    /** Parse a CrystalPulverizer recipe from JSON */
    public static CrystalPulverizerRecipe parseCrystalPulverizer(JsonObject json, JsonContext ctx) {
        Ingredient input = CraftingHelper.getIngredient(JsonUtils.getJsonObject(json, "input"), ctx);
        ItemStack result = deserializeItemStack(JsonUtils.getJsonObject(json, "result"), ctx);
        int energy = JsonUtils.getInt(json, "energyCost", 80);
        int time = JsonUtils.getInt(json, "processTime", 160);
        return new CrystalPulverizerRecipe(input, result, energy, time);
    }

    /** Parse a CrystalAggregator recipe from JSON (3-input A/B/C + count format) */
    public static CrystalAggregatorRecipe parseCrystalAggregator(JsonObject json, JsonContext ctx) {
        Ingredient a = parseOptionalIngredientWithCount(json, "inputA", ctx);
        int countA = getInputCount(json, "inputA");
        Ingredient b = parseOptionalIngredientWithCount(json, "inputB", ctx);
        int countB = getInputCount(json, "inputB");
        Ingredient c = parseOptionalIngredientWithCount(json, "inputC", ctx);
        int countC = getInputCount(json, "inputC");
        ItemStack result = deserializeItemStack(JsonUtils.getJsonObject(json, "result"), ctx);
        int energy = JsonUtils.getInt(json, "energyCost", 120);
        int time = JsonUtils.getInt(json, "processTime", 300);
        return new CrystalAggregatorRecipe(a, b, c, result, energy, time, countA, countB, countC);
    }

    private static ItemStack deserializeItemStack(JsonObject json, JsonContext ctx) {
        return CraftingHelper.getItemStack(json, ctx);
    }
}
