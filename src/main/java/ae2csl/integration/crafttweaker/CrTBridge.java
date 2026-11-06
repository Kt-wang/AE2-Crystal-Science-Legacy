/*
 * AE2 Crystal Science - CraftTweaker Bridge
 *
 * CraftTweaker 1.12.2 executes scripts during preInit, while this mod loads its
 * JSON machine recipes during init. To make removals actually effective, CrT
 * actions are queued here and applied right after the JSON recipes are loaded.
 *
 * This class must NOT reference any CraftTweaker classes — it is touched by the
 * main mod class even when CraftTweaker is not installed.
 */
package ae2csl.integration.crafttweaker;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.recipe.AE2CSRecipeRegistry;

import java.util.ArrayList;
import java.util.List;

public final class CrTBridge {

    private static final List<Runnable> pending = new ArrayList<>();

    private CrTBridge() {}

    /** Run immediately if recipes are already loaded, otherwise queue until JSON loading finishes. */
    public static void enqueue(Runnable action) {
        if (AE2CSRecipeRegistry.isLoaded()) {
            action.run();
        } else {
            pending.add(action);
        }
    }

    /** Apply all queued CraftTweaker actions. Called after AE2CSRecipeRegistry.init(). */
    public static void applyPending() {
        if (pending.isEmpty()) return;
        AE2CrystalScienceLegacy.LOGGER.info("Applying {} CraftTweaker recipe action(s)", pending.size());
        for (Runnable r : pending) {
            try {
                r.run();
            } catch (Exception e) {
                AE2CrystalScienceLegacy.LOGGER.warn("CraftTweaker recipe action failed: {}", e.getMessage());
            }
        }
        pending.clear();
    }
}
