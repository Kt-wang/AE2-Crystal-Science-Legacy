/*
 * AE2 Crystal Science - Ender Pickaxe
 * Ported from 1.21.1 source
 */
package ae2csl.item.tools.ender;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemPickaxe;

public class EnderPickaxeItem extends ItemPickaxe implements LinkableTool {

    public EnderPickaxeItem() {
        super(AE2CSToolType.ENDER.getToolMaterial());
        setTranslationKey("ender_pickaxe");
    }
}
