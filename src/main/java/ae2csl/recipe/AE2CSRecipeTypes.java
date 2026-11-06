/*
 * AE2 Crystal Science - Recipe Types
 * Ported from 1.21.1: common/init/AE2CSRecipeTypes.java
 */
package ae2csl.recipe;

import ae2csl.recipe.circuit_etcher.CircuitEtcherRecipe;
import ae2csl.recipe.crystal_aggregator.CrystalAggregatorRecipe;
import ae2csl.recipe.crystal_pulverizer.CrystalPulverizerRecipe;

import java.util.HashMap;
import java.util.Map;

/** Registry of machine recipe types (not Forge registries). */
public class AE2CSRecipeTypes {

    public static final String CIRCUIT_ETCHER = "circuit_etcher";
    public static final String CRYSTAL_AGGREGATOR = "crystal_aggregator";
    public static final String CRYSTAL_PULVERIZER = "crystal_pulverizer";
    public static final String QUARTZ_GRINDSTONE = "quartz_grindstone";
    public static final String ENTROPY_VARIATION_CHAMBER = "entropy_variation_chamber";

    private static final Map<String, Class<?>> registry = new HashMap<>();

    static {
        registry.put(CIRCUIT_ETCHER, CircuitEtcherRecipe.class);
        registry.put(CRYSTAL_AGGREGATOR, CrystalAggregatorRecipe.class);
        registry.put(CRYSTAL_PULVERIZER, CrystalPulverizerRecipe.class);
    }

    public static Class<?> getRecipeClass(String type) {
        return registry.get(type);
    }
}
