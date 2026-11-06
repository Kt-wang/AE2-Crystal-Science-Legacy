/*
 * AE2 Crystal Science - ResonantTemplateCodingTerminalPartItem
 */
package ae2csl.item.pattern;

import ae2csl.me.part.ResonantTemplateCodingTerminalPart;

public class ResonantTemplateCodingTerminalPartItem extends AE2CSPartItem<ResonantTemplateCodingTerminalPart> {

    public ResonantTemplateCodingTerminalPartItem() {
        setMaxStackSize(64);
        setTranslationKey("resonant_template_coding_terminal_part");
    }

    @Override
    public Class<ResonantTemplateCodingTerminalPart> getPartClass() { return ResonantTemplateCodingTerminalPart.class; }

    @Override
    public ResonantTemplateCodingTerminalPart createPart() { return new ResonantTemplateCodingTerminalPart(this); }
}
