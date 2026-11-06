/*
 * AE2 Crystal Science - MirrorPatternProviderPartItem
 */
package ae2csl.item.pattern;

import ae2csl.me.part.MirrorPatternProviderPart;

public class MirrorPatternProviderPartItem extends AE2CSPartItem<MirrorPatternProviderPart> {

    public MirrorPatternProviderPartItem() {
        setMaxStackSize(64);
        setTranslationKey("mirror_pattern_provider_part");
    }

    @Override
    public Class<MirrorPatternProviderPart> getPartClass() { return MirrorPatternProviderPart.class; }

    @Override
    public MirrorPatternProviderPart createPart() { return new MirrorPatternProviderPart(this); }
}
