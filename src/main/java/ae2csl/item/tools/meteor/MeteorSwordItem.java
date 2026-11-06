package ae2csl.item.tools.meteor;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemSword;

public class MeteorSwordItem extends ItemSword implements LinkableTool {
    public MeteorSwordItem() {
        super(AE2CSToolType.METEOR.getToolMaterial());
        setTranslationKey("meteor_sword");
    }
}
