/*
 * AE2 Crystal Science - MeteoritePatternProviderPartItem
 */
package ae2csl.item.pattern;

import ae2csl.me.part.MeteoritePatternProviderPart;

public class MeteoritePatternProviderPartItem extends AE2CSPartItem<MeteoritePatternProviderPart> {

    public MeteoritePatternProviderPartItem() {
        setMaxStackSize(64);
        setTranslationKey("meteorite_pattern_provider_part");
    }

    @Override
    public Class<MeteoritePatternProviderPart> getPartClass() { return MeteoritePatternProviderPart.class; }

    @Override
    public MeteoritePatternProviderPart createPart() { return new MeteoritePatternProviderPart(this); }
}
