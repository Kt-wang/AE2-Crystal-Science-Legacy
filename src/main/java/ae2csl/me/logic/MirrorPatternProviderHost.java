/*
 * AE2 Crystal Science - MirrorPatternProviderHost Interface
 * Ported from 1.21.1 source
 */
package ae2csl.me.logic;

import ae2.helpers.patternprovider.PatternProviderLogicHost;

public interface MirrorPatternProviderHost extends PatternProviderLogicHost {

    MirrorPatternProviderLogic getMirroringLogic();

    void markForLogicClientUpdate();

    @Override
    default boolean isVisibleInTerminal() {
        return false;
    }
}
