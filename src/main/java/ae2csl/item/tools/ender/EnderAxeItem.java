/*
 * AE2 Crystal Science - Ender Axe
 * Ported from 1.21.1 source
 */
package ae2csl.item.tools.ender;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemAxe;

public class EnderAxeItem extends ItemAxe implements LinkableTool {

    public EnderAxeItem() {
        super(AE2CSToolType.ENDER.getToolMaterial(), AE2CSToolType.ENDER.getToolMaterial().getAttackDamage() + 5.0F, -3.0F);
        setTranslationKey("ender_axe");
    }
}
