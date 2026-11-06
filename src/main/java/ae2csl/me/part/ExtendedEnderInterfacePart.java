/*
 * AE2 Crystal Science - ExtendedEnderInterfacePart (1.12.2)
 * 扩展版末影接口部件（吸收配置 36 槽 + extended 模型）。
 */
package ae2csl.me.part;

import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.core.AppEng;
import ae2.items.parts.PartModels;
import ae2.parts.PartModel;
import ae2csl.Tags;

import net.minecraft.util.ResourceLocation;

public class ExtendedEnderInterfacePart extends EnderInterfacePart {

    public static final ResourceLocation MODEL_EXTENDED = new ResourceLocation(Tags.MOD_ID,
        "part/ender_interface/extended");
    @PartModels
    public static final PartModel EXTENDED_MODELS_OFF = new PartModel(MODEL_EXTENDED,
        AppEng.makeId("part/interface_off"));
    @PartModels
    public static final PartModel EXTENDED_MODELS_ON = new PartModel(MODEL_EXTENDED,
        AppEng.makeId("part/interface_on"));
    @PartModels
    public static final PartModel EXTENDED_MODELS_HAS_CHANNEL = new PartModel(MODEL_EXTENDED,
        AppEng.makeId("part/interface_has_channel"));

    public ExtendedEnderInterfacePart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public boolean isExtended() {
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return EXTENDED_MODELS_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return EXTENDED_MODELS_ON;
        } else {
            return EXTENDED_MODELS_OFF;
        }
    }
}
