/*
 * AE2 Crystal Science - MeteoritePatternProviderHost Interface
 * Ported from 1.21.1 source
 */
package ae2csl.me.logic;

import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.upgrades.UpgradeInventories;
import ae2.helpers.patternprovider.PatternProviderLogicHost;

public interface MeteoritePatternProviderHost extends PatternProviderLogicHost, IUpgradeableObject {

    @Override
    default IUpgradeInventory getUpgrades() {
        if (getLogic() instanceof MeteoritePatternProviderLogic)
            return ((MeteoritePatternProviderLogic) getLogic()).getUpgrades();
        else
            return UpgradeInventories.empty();
    }
}
