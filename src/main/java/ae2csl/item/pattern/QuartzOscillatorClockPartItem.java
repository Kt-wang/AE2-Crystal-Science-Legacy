/*
 * AE2 Crystal Science - QuartzOscillatorClockPartItem
 */
package ae2csl.item.pattern;

import ae2csl.me.part.QuartzOscillatorClockPart;

public class QuartzOscillatorClockPartItem extends AE2CSPartItem<QuartzOscillatorClockPart> {

    public QuartzOscillatorClockPartItem() {
        setMaxStackSize(64);
        setTranslationKey("quartz_oscillator_clock_part");
    }

    @Override
    public Class<QuartzOscillatorClockPart> getPartClass() { return QuartzOscillatorClockPart.class; }

    @Override
    public QuartzOscillatorClockPart createPart() { return new QuartzOscillatorClockPart(this); }
}
