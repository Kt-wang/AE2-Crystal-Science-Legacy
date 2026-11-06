package ae2csl.item.tools.resonating;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemHoe;

public class ResonatingHoeItem extends ItemHoe implements LinkableTool {
    public ResonatingHoeItem() {
        super(AE2CSToolType.RESONATING.getToolMaterial());
        setTranslationKey("resonating_hoe");
    }
}
