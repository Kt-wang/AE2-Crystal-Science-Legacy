/*
 * AE2 Crystal Science - IntegratedInterfacePartItem
 */
package ae2csl.item.pattern;

import ae2csl.me.part.IntegratedInterfacePart;

public class IntegratedInterfacePartItem extends AE2CSPartItem<IntegratedInterfacePart> {

    public IntegratedInterfacePartItem() {
        setMaxStackSize(64);
        setTranslationKey("integrated_interface_part");
    }

    @Override
    public Class<IntegratedInterfacePart> getPartClass() { return IntegratedInterfacePart.class; }

    @Override
    public IntegratedInterfacePart createPart() { return new IntegratedInterfacePart(this); }
}
