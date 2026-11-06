package ae2csl.item.tools.meteor;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemAxe;

public class MeteorAxeItem extends ItemAxe implements LinkableTool {
    public MeteorAxeItem() {
        super(AE2CSToolType.METEOR.getToolMaterial(), AE2CSToolType.METEOR.getToolMaterial().getAttackDamage() + 6.0F, -3.0F);
        setTranslationKey("meteor_axe");
    }
}
