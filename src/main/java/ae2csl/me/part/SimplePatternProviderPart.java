/*
 * AE2 Crystal Science - SimplePatternProviderPart (1.12.2)
 * A basic pattern provider part with 5 pattern slots.
 */
package ae2csl.me.part;

import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.api.stacks.AEItemKey;
import ae2.core.AppEng;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.items.parts.PartModels;
import ae2.parts.PartModel;
import ae2.parts.crafting.PatternProviderPart;
import ae2csl.Tags;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class SimplePatternProviderPart extends PatternProviderPart {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(Tags.MOD_ID, "part/simple_pattern_provider/base");

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_off"));
    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_on"));
    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_has_channel"));

    public SimplePatternProviderPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) return MODELS_HAS_CHANNEL;
        else if (this.isPowered()) return MODELS_ON;
        else return MODELS_OFF;
    }

    @Override
    protected PatternProviderLogic createLogic() {
        // 5 槽。只收处理样板（fork 架构：合成样板由分子装配室自持自答）
        return new PatternProviderLogic(getMainNode(), this, 5);
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(getPartItem().asItem());
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return new ItemStack(getPartItem().asItem());
    }
}
