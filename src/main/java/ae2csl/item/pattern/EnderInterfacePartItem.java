/*
 * AE2 Crystal Science - EnderInterfacePartItem
 */
package ae2csl.item.pattern;

import ae2csl.me.part.EnderInterfacePart;

public class EnderInterfacePartItem extends AE2CSPartItem<EnderInterfacePart> {

    public EnderInterfacePartItem() {
        setMaxStackSize(64);
        setTranslationKey("ender_interface_part");
    }

    @Override
    public Class<EnderInterfacePart> getPartClass() { return EnderInterfacePart.class; }

    @Override
    public EnderInterfacePart createPart() { return new EnderInterfacePart(this); }
}
