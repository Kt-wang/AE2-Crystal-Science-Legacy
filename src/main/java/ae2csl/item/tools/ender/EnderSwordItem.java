/*
 * AE2 Crystal Science - Ender Sword
 * Ported from 1.21.1 source
 */
package ae2csl.item.tools.ender;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemSword;

public class EnderSwordItem extends ItemSword implements LinkableTool {

    public EnderSwordItem() {
        super(AE2CSToolType.ENDER.getToolMaterial());
        setTranslationKey("ender_sword");
    }
}
