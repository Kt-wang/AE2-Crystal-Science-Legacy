package ae2csl.item.tools.resonating;

import ae2csl.item.tools.AE2CSToolType;
import ae2csl.item.tools.LinkableTool;
import net.minecraft.item.ItemAxe;

public class ResonatingAxeItem extends ItemAxe implements LinkableTool {
    public ResonatingAxeItem() {
        super(AE2CSToolType.RESONATING.getToolMaterial(), AE2CSToolType.RESONATING.getToolMaterial().getAttackDamage() + 6.0F, -3.0F);
        setTranslationKey("resonating_axe");
    }
}
