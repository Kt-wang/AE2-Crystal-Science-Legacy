/*
 * AE2 Crystal Science - ExtendedIntegratedInterfacePartItem
 */
package ae2csl.item.pattern;

import ae2csl.me.part.ExtendedIntegratedInterfacePart;

public class ExtendedIntegratedInterfacePartItem extends AE2CSPartItem<ExtendedIntegratedInterfacePart> {

    public ExtendedIntegratedInterfacePartItem() {
        setMaxStackSize(64);
        setTranslationKey("extended_integrated_interface_part");
    }

    @Override
    public Class<ExtendedIntegratedInterfacePart> getPartClass() {
        return ExtendedIntegratedInterfacePart.class;
    }

    @Override
    public ExtendedIntegratedInterfacePart createPart() {
        return new ExtendedIntegratedInterfacePart(this);
    }
}
