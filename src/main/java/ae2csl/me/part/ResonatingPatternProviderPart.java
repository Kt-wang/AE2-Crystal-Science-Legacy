/*
 * AE2 Crystal Science - ResonatingPatternProviderPart (1.12.2)
 */
package ae2csl.me.part;

import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.core.AppEng;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.items.parts.PartModels;
import ae2.parts.PartModel;
import ae2.parts.crafting.PatternProviderPart;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.me.logic.ResonatingPatternProviderHost;
import ae2csl.me.logic.ResonatingPatternProviderLogic;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ResonatingPatternProviderPart extends PatternProviderPart implements ResonatingPatternProviderHost {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(Tags.MOD_ID, "part/resonating_pattern_provider/base");

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_off"));
    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_on"));
    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_has_channel"));

    public ResonatingPatternProviderPart(IPartItem<?> partItem) {
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
        int patternSize = isExtended() ? 36 : 9;
        return new ResonatingPatternProviderLogic(getMainNode(), this, patternSize);
    }

    @Override
    public ResonatingPatternProviderLogic getResonatingLogic() {
        return (ResonatingPatternProviderLogic) getLogic();
    }

    @Override
    public boolean isExtended() {
        return false;
    }

    @Override
    public void markForLogicClientUpdate() {
        if (getTileEntity() != null && getTileEntity().getWorld() != null && !getTileEntity().getWorld().isRemote) {
            getHost().markForUpdate();
        }
    }

    /**
     * 走自己的 GUI（拉动模式按钮需要我们的容器）
     */
    @Override
    public boolean onUseWithoutItem(net.minecraft.entity.player.EntityPlayer player, net.minecraft.util.math.Vec3d pos) {
        if (!player.world.isRemote) {
            net.minecraft.util.math.BlockPos tilePos = this.getTileEntity().getPos();
            player.openGui(AE2CrystalScienceLegacy.INSTANCE,
                ae2csl.client.gui.GuiHandler.GUI_RESONATING_PATTERN_PROVIDER_PART + this.getSide().ordinal(),
                player.world, tilePos.getX(), tilePos.getY(), tilePos.getZ());
        }
        return true;
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, net.minecraft.entity.player.EntityPlayer player,
                               net.minecraft.util.EnumHand hand, net.minecraft.util.math.Vec3d pos) {
        if (super.onUseItemOn(heldItem, player, hand, pos)) {
            return true;
        }
        return this.onUseWithoutItem(player, pos);
    }

    // ---- 记忆卡默认目标（对齐源码 importSettings/exportSettings） ----

    @Override
    public void exportSettings(ae2.util.SettingsFrom mode, net.minecraft.nbt.NBTTagCompound output) {
        super.exportSettings(mode, output);
        if (mode == ae2.util.SettingsFrom.DISMANTLE_ITEM || mode == ae2.util.SettingsFrom.MEMORY_CARD) {
            getResonatingLogic().writeDefaultsToSettings(output);
        }
    }

    @Override
    public void importSettings(ae2.util.SettingsFrom mode, net.minecraft.nbt.NBTTagCompound input,
                               @javax.annotation.Nullable net.minecraft.entity.player.EntityPlayer player) {
        super.importSettings(mode, input, player);
        if (mode == ae2.util.SettingsFrom.DISMANTLE_ITEM || mode == ae2.util.SettingsFrom.MEMORY_CARD) {
            getResonatingLogic().readDefaultsFromSettings(input);
        }
    }
}
