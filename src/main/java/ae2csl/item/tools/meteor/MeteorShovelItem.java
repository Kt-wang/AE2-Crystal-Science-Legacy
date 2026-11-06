package ae2csl.item.tools.meteor;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemSpade;

public class MeteorShovelItem extends ItemSpade implements LinkableTool {
    public MeteorShovelItem() {
        super(AE2CSToolType.METEOR.getToolMaterial());
        setTranslationKey("meteor_shovel");
    }
}
