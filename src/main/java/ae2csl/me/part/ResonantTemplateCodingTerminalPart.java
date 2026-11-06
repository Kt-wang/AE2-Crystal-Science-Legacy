/*
 * AE2 Crystal Science - ResonantTemplateCodingTerminalPart (1.12.2)
 */
package ae2csl.me.part;

import ae2.api.parts.IPartCollisionHelper;
import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.core.AppEng;
import ae2.items.parts.PartModels;
import ae2.parts.AEBasePart;
import ae2.parts.PartModel;
import ae2csl.Tags;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ResonantTemplateCodingTerminalPart extends AEBasePart implements IResonantTemplateCodingTerminalHost {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(Tags.MOD_ID, "part/resonant_template_coding_terminal/base");
    @PartModels public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_off"));
    @PartModels public static final PartModel MODELS_ON = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_on"));
    @PartModels public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_has_channel"));

    private boolean pullRecipeInputsToRealGrid;
    private boolean pulledAnvilMode;
    private boolean encodeResonatingPattern;

    public ResonantTemplateCodingTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override public IPartModel getStaticModels() {
        if (isActive() && isPowered()) return MODELS_HAS_CHANNEL;
        else if (isPowered()) return MODELS_ON;
        else return MODELS_OFF;
    }
    @Override public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(5, 5, 12, 11, 11, 14);
    }

    public void saveChanges() { getHost().markForSave(); }
    public ItemStack getMainContainerIcon() { return new ItemStack(getPartItem().asItem()); }

    @Override public boolean isPullRecipeInputsToRealGrid() { return pullRecipeInputsToRealGrid; }
    @Override public void setPullRecipeInputsToRealGrid(boolean v) { this.pullRecipeInputsToRealGrid = v; }
    @Override public boolean isPulledAnvilMode() { return pulledAnvilMode; }
    @Override public void setPulledAnvilMode(boolean v) { this.pulledAnvilMode = v; }
    @Override public boolean isEncodeResonatingPattern() { return encodeResonatingPattern; }
    @Override public void setEncodeResonatingPattern(boolean v) { this.encodeResonatingPattern = v; }
}
