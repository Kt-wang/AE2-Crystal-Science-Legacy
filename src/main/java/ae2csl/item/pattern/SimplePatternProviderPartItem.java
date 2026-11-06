/*
 * AE2 Crystal Science - SimplePatternProviderPartItem
 */
package ae2csl.item.pattern;

import ae2csl.me.part.SimplePatternProviderPart;

public class SimplePatternProviderPartItem extends AE2CSPartItem<SimplePatternProviderPart> {

    public SimplePatternProviderPartItem() {
        setMaxStackSize(64);
        setTranslationKey("simple_pattern_provider_part");
    }

    @Override
    public Class<SimplePatternProviderPart> getPartClass() { return SimplePatternProviderPart.class; }

    @Override
    public SimplePatternProviderPart createPart() { return new SimplePatternProviderPart(this); }
}
