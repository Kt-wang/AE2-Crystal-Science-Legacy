package ae2csl.item.tools.meteor;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemPickaxe;

public class MeteorPickaxeItem extends ItemPickaxe implements LinkableTool {
    public MeteorPickaxeItem() {
        super(AE2CSToolType.METEOR.getToolMaterial());
        setTranslationKey("meteor_pickaxe");
    }
}
