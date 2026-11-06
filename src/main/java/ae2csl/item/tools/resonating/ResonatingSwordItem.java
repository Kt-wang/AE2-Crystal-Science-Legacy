package ae2csl.item.tools.resonating;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemSword;

public class ResonatingSwordItem extends ItemSword implements LinkableTool {
    public ResonatingSwordItem() {
        super(AE2CSToolType.RESONATING.getToolMaterial());
        setTranslationKey("resonating_sword");
    }
}
