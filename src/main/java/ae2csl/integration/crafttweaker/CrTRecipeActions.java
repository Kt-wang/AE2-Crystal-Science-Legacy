/*
 * AE2 Crystal Science - CraftTweaker recipe actions
 * Deferred IAction implementations that mutate AE2CSRecipeRegistry.
 */
package ae2csl.integration.crafttweaker;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.recipe.AE2CSRecipeRegistry;
import crafttweaker.IAction;

import java.util.function.Predicate;

public final class CrTRecipeActions {

    private CrTRecipeActions() {}

    public static final class Add implements IAction {
        private final String type;
        private final Object recipe;
        private final String description;

        public Add(String type, Object recipe, String description) {
            this.type = type;
            this.recipe = recipe;
            this.description = description;
        }

        @Override
        public void apply() {
            CrTBridge.enqueue(() -> AE2CSRecipeRegistry.addRecipe(type, recipe));
        }

        @Override
        public String describe() {
            return description;
        }
    }

    public static final class Remove implements IAction {
        private final String type;
        private final Predicate<Object> filter;
        private final String description;
        private final String machineName;

        public Remove(String type, Predicate<Object> filter, String machineName, String description) {
            this.type = type;
            this.filter = filter;
            this.machineName = machineName;
            this.description = description;
        }

        @Override
        public void apply() {
            CrTBridge.enqueue(() -> {
                int removed = AE2CSRecipeRegistry.removeRecipes(type, filter);
                AE2CrystalScienceLegacy.LOGGER.info("CraftTweaker: removed {} {} recipe(s)", removed, machineName);
            });
        }

        @Override
        public String describe() {
            return description;
        }
    }

    public static final class RemoveAll implements IAction {
        private final String type;
        private final String machineName;

        public RemoveAll(String type, String machineName) {
            this.type = type;
            this.machineName = machineName;
        }

        @Override
        public void apply() {
            CrTBridge.enqueue(() -> {
                int removed = AE2CSRecipeRegistry.removeAllRecipes(type);
                AE2CrystalScienceLegacy.LOGGER.info("CraftTweaker: removed all {} {} recipe(s)", removed, machineName);
            });
        }

        @Override
        public String describe() {
            return "Removing all " + machineName + " recipes";
        }
    }
}
