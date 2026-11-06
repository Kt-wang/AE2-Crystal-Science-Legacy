package ae2csl.item.tools.resonating;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemPickaxe;

public class ResonatingPickaxeItem extends ItemPickaxe implements LinkableTool {
    public ResonatingPickaxeItem() {
        super(AE2CSToolType.RESONATING.getToolMaterial());
        setTranslationKey("resonating_pickaxe");
    }
}
