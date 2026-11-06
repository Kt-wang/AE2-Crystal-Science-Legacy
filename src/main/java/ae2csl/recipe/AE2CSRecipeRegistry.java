/*
 * AE2 Crystal Science - Recipe Registry
 * Loads machine recipes from JSON at init. Works on client and server.
 */
package ae2csl.recipe;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;

import com.google.gson.*;
import net.minecraft.item.Item;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.OreDictionary;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Central recipe storage. Recipes loaded from classpath JSON at init. */
public class AE2CSRecipeRegistry {

    private static final Map<String, List<Object>> recipes = new HashMap<>();
    private static boolean loaded = false;

    public static void init() {
        if (loaded) return;
        loaded = true;

        JsonContext ctx = new JsonContext(Tags.MOD_ID);

        // Circuit etcher recipes
        loadFiles("circuit_etcher", new String[]{
            "accumulation_processor", "calculation_processor", "complex_link_processor",
            "concurrent_processor", "energy_processor", "engineering_processor",
            "logic_processor", "multidimensional_expansion_processor", "omni_link_processor",
            "origination_processor", "quantum_processor", "resonating_processor", "simple_processor"
        }, machineDir("circuit_etcher/"), ctx);

        // Crystal pulverizer recipes
        String[] pulverizerFiles = {
            "certus_quartz_crystal", "certus_quartz_dust", "charged_certus_quartz_crystal",
            "charged_certus_quartz_dust", "cobblestone", "copper_dust", "diamond_dust",
            "ember_dust_from_original_crystal", "ember_dust_from_pure_crystal",
            "emerald_dust", "ender_dust_from_pearl", "ender_dust_from_quartz",
            "entro_dust", "flour", "fluix_dust", "gold_dust", "gravel", "gunpowder",
            "irradiated_crystal_dust", "nether_quartz_dust",
            "quantum_crystal_dust_from_crystal", "quantum_crystal_dust_from_ingot",
            "quantum_infused_dust", "red_sand", "redstone",
            "redstone_crystal_dust_from_pure_redstone_crystal", "redstone_crystal_dust_from_redstone_crystal",
            "resonating_crystal_dust", "sand", "sky_dust_from_quartz",
            "sky_dust_from_sky_stone_block"
        };
        loadFiles("crystal_pulverizer", pulverizerFiles, machineDir("crystal_pulverizer/"), ctx);

        // Crystal aggregator recipes
        loadFiles("crystal_aggregator", new String[]{
            "accumulation_processor", "atomic_disassembler", "bell",
            "budding_amethyst", "calculation_processor", "certus_quartz_seed",
            "charged_certus_quartz_crystal", "chipped_budding_quartz",
            "complex_link_processor", "concurrent_processor", "crying_obsidian",
            "damaged_budding_quartz", "ember_crystal", "ember_seed",
            "enchanted_book_of_ender_link", "ender_ingot", "ender_interface",
            "ender_linker", "ender_quartz_seed", "end_stone",
            "energy_processor", "engineering_processor", "entro_budding_mostly",
            "entro_crystal_seed_from_dust", "entro_crystal_seed_from_original_seed",
            "ex_molecular_assembler", "extended_ender_interface",
            "extended_integrated_interface", "extended_integrated_interface_from_extended_pattern_and_interface",
            "extended_resonating_pattern_provider", "flawed_budding_quartz",
            "flawless_budding_quartz", "fluix_crystal", "fluix_crystal_seed",
            "fluix_pearl", "glow_ink_sac", "glowstone_dust", "gunpowder",
            "heart_of_the_sea", "heavy_core", "insulating_resin",
            "integrated_interface", "irradiated_seed", "logic_processor",
            "machine_frame_from_entro_ingot", "machine_frame_from_pure_entro_crystal",
            "meka_tool", "meteor_seed", "multidimensional_expansion_processor",
            "mysterious_cube", "nether_quartz_seed", "omni_link_processor",
            "origination_processor", "overload_card", "purified_redstone_crystal_from_redstone_crystal",
            "quantum_alloy", "quantum_processor", "quartz_oscillator_clock",
            "redstone", "redstone_crystal_seed", "resonating_crystal_dust",
            "resonating_drive", "resonating_linker", "resonating_memory_card",
            "resonating_pattern_converter", "resonating_pattern_provider",
            "resonating_processor", "resonating_seed", "simple_pattern_provider",
            "simple_processor", "singularity", "slime_ball", "wireless_hub"
        }, machineDir("crystal_aggregator/"), ctx);

        // Quartz grindstone shares pulverizer recipes
        loadFiles("quartz_grindstone", pulverizerFiles, machineDir("crystal_pulverizer/"), ctx);

        int total = recipes.values().stream().mapToInt(List::size).sum();
        AE2CrystalScienceLegacy.LOGGER.info("Loaded {} recipes across {} machine types", total, recipes.size());
    }

    private static String machineDir(String sub) {
        return "assets/" + Tags.MOD_ID + "/machine_recipes/" + sub;
    }

    private static void loadFiles(String type, String[] filenames, String dir, JsonContext ctx) {
        List<Object> list = recipes.computeIfAbsent(type, k -> new ArrayList<>());
        for (String name : filenames) {
            String path = dir + name + ".json";
            JsonObject json = loadJson(path);
            if (json == null) {
                AE2CrystalScienceLegacy.LOGGER.warn("Recipe file not found: {}", path);
                continue;
            }
            try {
                // Skip recipes whose ingredients/result items are not available
                if (!validateIngredients(json, type, name)) {
                    continue;
                }
                Object recipe = parseRecipe(type, json, ctx);
                if (recipe != null) list.add(recipe);
            } catch (Exception e) {
                AE2CrystalScienceLegacy.LOGGER.warn("Skipping recipe {}: {}", path, e.getMessage());
            }
        }
    }

    private static JsonObject loadJson(String path) {
        try (InputStream in = AE2CSRecipeRegistry.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) return null;
            JsonObject json = new JsonParser().parse(
                new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return json;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validate that all ingredient and result items are available.
     * For ore_dict ingredients: check the ore dictionary has entries.
     * For direct item ingredients: check the item exists in the registry.
     * If any are unavailable, the recipe is skipped (required mod not installed).
     */
    private static boolean validateIngredients(JsonObject json, String type, String name) {
        // Validate input fields based on recipe type
        switch (type) {
            case AE2CSRecipeTypes.CRYSTAL_PULVERIZER:
            case AE2CSRecipeTypes.QUARTZ_GRINDSTONE:
            case AE2CSRecipeTypes.ENTROPY_VARIATION_CHAMBER:
                if (!validateOneIngredient(json, "input", name)) return false;
                break;
            case AE2CSRecipeTypes.CRYSTAL_AGGREGATOR:
            case AE2CSRecipeTypes.CIRCUIT_ETCHER:
                // multi-input recipes: inputs are optional, only validate if present
                if (!validateOptionalIngredient(json, "inputA", name)) return false;
                if (!validateOptionalIngredient(json, "inputB", name)) return false;
                if (!validateOptionalIngredient(json, "inputC", name)) return false;
                break;
        }
        // Validate result item
        if (!validateResultItem(json, name)) return false;
        return true;
    }

    /** Validate a required single-input ingredient. */
    private static boolean validateOneIngredient(JsonObject json, String key, String name) {
        if (!json.has(key)) return true;
        return validateIngredientObject(json.getAsJsonObject(key), name, key);
    }

    /** Validate an optional multi-input ingredient (skip if absent or empty). */
    private static boolean validateOptionalIngredient(JsonObject json, String key, String name) {
        if (!json.has(key)) return true;
        JsonObject obj = json.getAsJsonObject(key);
        if (obj.entrySet().isEmpty()) return true;
        return validateIngredientObject(obj, name, key);
    }

    /** Check a single ingredient JSON object for ore_dict entries or direct items. */
    private static boolean validateIngredientObject(JsonObject ing, String name, String key) {
        // forge:ore_dict — check the ore dictionary has entries
        if (ing.has("type") && "forge:ore_dict".equals(ing.get("type").getAsString())) {
            String ore = ing.get("ore").getAsString();
            if (OreDictionary.getOres(ore).isEmpty()) {
                AE2CrystalScienceLegacy.LOGGER.info("Skipping recipe '{}': ore dict '{}' is empty (required mod not installed?)", name, ore);
                return false;
            }
        }
        // Direct item reference — check the item exists
        if (ing.has("item")) {
            String itemId = ing.get("item").getAsString();
            Item item = Item.getByNameOrId(itemId);
            if (item == null) {
                AE2CrystalScienceLegacy.LOGGER.info("Skipping recipe '{}': item '{}' not found (required mod not installed?)", name, itemId);
                return false;
            }
        }
        return true;
    }

    /** Validate the result item exists. */
    private static boolean validateResultItem(JsonObject json, String name) {
        if (!json.has("result")) return true;
        JsonObject result = json.getAsJsonObject("result");
        if (result.has("item")) {
            String itemId = result.get("item").getAsString();
            Item item = Item.getByNameOrId(itemId);
            if (item == null) {
                AE2CrystalScienceLegacy.LOGGER.info("Skipping recipe '{}': result item '{}' not found (required mod not installed?)", name, itemId);
                return false;
            }
        }
        return true;
    }

    private static Object parseRecipe(String type, JsonObject json, JsonContext ctx) {
        switch (type) {
            case AE2CSRecipeTypes.CIRCUIT_ETCHER:
                return AE2CSRecipeSerializers.parseCircuitEtcher(json, ctx);
            case AE2CSRecipeTypes.CRYSTAL_PULVERIZER:
            case AE2CSRecipeTypes.QUARTZ_GRINDSTONE:
            case AE2CSRecipeTypes.ENTROPY_VARIATION_CHAMBER:
                return AE2CSRecipeSerializers.parseCrystalPulverizer(json, ctx);
            case AE2CSRecipeTypes.CRYSTAL_AGGREGATOR:
                return AE2CSRecipeSerializers.parseCrystalAggregator(json, ctx);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getRecipes(String type) {
        return (List<T>) (List<?>) recipes.getOrDefault(type, Collections.emptyList());
    }

    /** Whether the built-in JSON recipes have been loaded (init phase done). */
    public static boolean isLoaded() {
        return loaded;
    }

    /** Add a recipe to a machine type at runtime (e.g. CraftTweaker). */
    public static void addRecipe(String type, Object recipe) {
        recipes.computeIfAbsent(type, k -> new ArrayList<>()).add(recipe);
    }

    /** Remove all recipes of a machine type matching the filter. Returns the number removed. */
    public static int removeRecipes(String type, java.util.function.Predicate<Object> filter) {
        List<Object> list = recipes.get(type);
        if (list == null || list.isEmpty()) return 0;
        int before = list.size();
        list.removeIf(filter);
        return before - list.size();
    }

    /** Remove every recipe of a machine type. Returns the number removed. */
    public static int removeAllRecipes(String type) {
        List<Object> list = recipes.get(type);
        if (list == null) return 0;
        int count = list.size();
        list.clear();
        return count;
    }
}
