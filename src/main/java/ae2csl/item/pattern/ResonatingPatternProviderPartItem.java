package ae2csl.item.pattern;

import ae2csl.me.part.ResonatingPatternProviderPart;

public class ResonatingPatternProviderPartItem extends AE2CSPartItem<ResonatingPatternProviderPart> {

    public ResonatingPatternProviderPartItem() {
        setMaxStackSize(64);
        setTranslationKey("resonating_pattern_provider_part");
    }

    @Override
    public Class<ResonatingPatternProviderPart> getPartClass() { return ResonatingPatternProviderPart.class; }

    @Override
    public ResonatingPatternProviderPart createPart() { return new ResonatingPatternProviderPart(this); }
}
