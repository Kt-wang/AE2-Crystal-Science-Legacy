/*
 * AE2 Crystal Science - ExtendedResonatingPatternProviderPartItem
 */
package ae2csl.item.pattern;

import ae2csl.me.part.ExtendedResonatingPatternProviderPart;

public class ExtendedResonatingPatternProviderPartItem extends AE2CSPartItem<ExtendedResonatingPatternProviderPart> {

    public ExtendedResonatingPatternProviderPartItem() {
        setMaxStackSize(64);
        setTranslationKey("extended_resonating_pattern_provider_part");
    }

    @Override
    public Class<ExtendedResonatingPatternProviderPart> getPartClass() {
        return ExtendedResonatingPatternProviderPart.class;
    }

    @Override
    public ExtendedResonatingPatternProviderPart createPart() {
        return new ExtendedResonatingPatternProviderPart(this);
    }
}
