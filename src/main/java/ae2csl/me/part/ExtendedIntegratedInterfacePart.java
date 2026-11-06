/*
 * AE2 Crystal Science - ExtendedIntegratedInterfacePart (1.12.2)
 * 扩展版集成接口部件（36槽 + extended 模型）。
 * 对照源码 IntegratedInterfacePart 的 EXTENDED_MODELS_*。
 */
package ae2csl.me.part;

import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.core.AppEng;
import ae2.items.parts.PartModels;
import ae2.parts.PartModel;
import ae2csl.Tags;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ExtendedIntegratedInterfacePart extends IntegratedInterfacePart {

    public static final ResourceLocation MODEL_EXTENDED = new ResourceLocation(Tags.MOD_ID,
        "part/integrated_interface/extended");
    @PartModels
    public static final PartModel EXTENDED_MODELS_OFF = new PartModel(MODEL_EXTENDED,
        AppEng.makeId("part/interface_off"));
    @PartModels
    public static final PartModel EXTENDED_MODELS_ON = new PartModel(MODEL_EXTENDED,
        AppEng.makeId("part/interface_on"));
    @PartModels
    public static final PartModel EXTENDED_MODELS_HAS_CHANNEL = new PartModel(MODEL_EXTENDED,
        AppEng.makeId("part/interface_has_channel"));

    public ExtendedIntegratedInterfacePart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public boolean isExtended() {
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        if (isActive() && isPowered()) {
            return EXTENDED_MODELS_HAS_CHANNEL;
        } else if (isPowered()) {
            return EXTENDED_MODELS_ON;
        } else {
            return EXTENDED_MODELS_OFF;
        }
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return this.getPartItem().asItemStack();
    }
}
