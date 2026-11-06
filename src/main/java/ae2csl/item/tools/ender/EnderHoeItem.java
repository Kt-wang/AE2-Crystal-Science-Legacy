/*
 * AE2 Crystal Science - Ender Hoe
 * Ported from 1.21.1 source
 */
package ae2csl.item.tools.ender;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemHoe;

public class EnderHoeItem extends ItemHoe implements LinkableTool {

    public EnderHoeItem() {
        super(AE2CSToolType.ENDER.getToolMaterial());
        setTranslationKey("ender_hoe");
    }
}
