package ae2csl.item.tools.resonating;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemSpade;

public class ResonatingShovelItem extends ItemSpade implements LinkableTool {
    public ResonatingShovelItem() {
        super(AE2CSToolType.RESONATING.getToolMaterial());
        setTranslationKey("resonating_shovel");
    }
}
