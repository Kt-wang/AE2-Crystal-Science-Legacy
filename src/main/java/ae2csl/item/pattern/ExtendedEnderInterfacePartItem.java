/*
 * AE2 Crystal Science - ExtendedEnderInterfacePartItem
 */
package ae2csl.item.pattern;

import ae2csl.me.part.ExtendedEnderInterfacePart;

public class ExtendedEnderInterfacePartItem extends AE2CSPartItem<ExtendedEnderInterfacePart> {

    public ExtendedEnderInterfacePartItem() {
        setMaxStackSize(64);
        setTranslationKey("extended_ender_interface_part");
    }

    @Override
    public Class<ExtendedEnderInterfacePart> getPartClass() {
        return ExtendedEnderInterfacePart.class;
    }

    @Override
    public ExtendedEnderInterfacePart createPart() {
        return new ExtendedEnderInterfacePart(this);
    }
}
