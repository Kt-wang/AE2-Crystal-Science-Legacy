/*
 * AE2 Crystal Science - Ender Shovel
 * Ported from 1.21.1 source
 */
package ae2csl.item.tools.ender;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemSpade;

public class EnderShovelItem extends ItemSpade implements LinkableTool {

    public EnderShovelItem() {
        super(AE2CSToolType.ENDER.getToolMaterial());
        setTranslationKey("ender_shovel");
    }
}
