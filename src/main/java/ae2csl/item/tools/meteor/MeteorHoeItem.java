package ae2csl.item.tools.meteor;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemHoe;

public class MeteorHoeItem extends ItemHoe implements LinkableTool {
    public MeteorHoeItem() {
        super(AE2CSToolType.METEOR.getToolMaterial());
        setTranslationKey("meteor_hoe");
    }
}
