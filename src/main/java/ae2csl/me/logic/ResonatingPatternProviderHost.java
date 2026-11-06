/*
 * AE2 Crystal Science - ResonatingPatternProviderHost Interface
 * Ported from 1.21.1 source
 */
package ae2csl.me.logic;

import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.upgrades.UpgradeInventories;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2csl.me.crafting.EncodedResonatingPattern;

import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Optional;

public interface ResonatingPatternProviderHost extends PatternProviderLogicHost, IUpgradeableObject {

    ResonatingPatternProviderLogic getResonatingLogic();

    @Override
    default IUpgradeInventory getUpgrades() {
        if (getLogic() instanceof ResonatingPatternProviderLogic logic) {
            return logic.getUpgrades();
        } else {
            return UpgradeInventories.empty();
        }
    }

    boolean isExtended();

    void markForLogicClientUpdate();

    default void readDefaultsFromItem(ItemStack stack) {
        getResonatingLogic().readDefaultsFromItem(stack);
    }

    default void writeDefaultsToStack(ItemStack stack) {
        getResonatingLogic().writeDefaultsToStack(stack);
    }

    default List<Optional<EncodedResonatingPattern.Target>> getDefaultInputTargets() {
        return getResonatingLogic().getDefaultInputTargets();
    }

    default int getDefaultSelectedInput() {
        return getResonatingLogic().getDefaultSelectedInput();
    }

    default void setDefaultInputTarget(int input, Optional<EncodedResonatingPattern.Target> target) {
        getResonatingLogic().setDefaultInputTarget(input, target);
    }

    default void setDefaultSelectedInput(int input) {
        getResonatingLogic().setDefaultSelectedInput(input);
    }
}
