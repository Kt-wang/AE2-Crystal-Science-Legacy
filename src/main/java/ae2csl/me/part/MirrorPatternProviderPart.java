/*
 * AE2 Crystal Science - MirrorPatternProviderPart (1.12.2)
 * Pattern provider that mirrors another provider's patterns.
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
import ae2.util.SettingsFrom;
import ae2csl.Tags;
import ae2csl.me.logic.MirrorPatternProviderHost;
import ae2csl.me.logic.MirrorPatternProviderLogic;
import ae2csl.me.logic.MirroredPatternProviderTarget;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class MirrorPatternProviderPart extends PatternProviderPart implements MirrorPatternProviderHost {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(Tags.MOD_ID, "part/mirror_pattern_provider/base");

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_off"));
    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_on"));
    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_has_channel"));

    public MirrorPatternProviderPart(IPartItem<?> partItem) {
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
        return new MirrorPatternProviderLogic(getMainNode(), this);
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(getPartItem().asItem());
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return new ItemStack(getPartItem().asItem());
    }

    @Override
    public MirrorPatternProviderLogic getMirroringLogic() {
        return (MirrorPatternProviderLogic) getLogic();
    }

    @Override
    public void markForLogicClientUpdate() {
        if (getTileEntity() != null && getTileEntity().getWorld() != null && !getTileEntity().getWorld().isRemote) {
            getHost().markForUpdate();
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, NBTTagCompound input, @Nullable EntityPlayer player) {
        super.importSettings(mode, input, player);
        if (mode == SettingsFrom.DISMANTLE_ITEM || mode == SettingsFrom.MEMORY_CARD) {
            getMirroringLogic().readFromNBT(input);
            saveChanges();
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, NBTTagCompound output) {
        super.exportSettings(mode, output);
        if (mode == SettingsFrom.DISMANTLE_ITEM || mode == SettingsFrom.MEMORY_CARD) {
            getMirroringLogic().writeToNBT(output);
        }
    }

    @Override
    public void writeToStream(PacketBuffer data) {
        super.writeToStream(data);
        MirroredPatternProviderTarget target = getMirroringLogic().getMirrorTarget();
        data.writeBoolean(target != null);
        if (target != null) {
            target.write(data);
        }
    }

    @Override
    public boolean readFromStream(PacketBuffer data) {
        boolean redraw = super.readFromStream(data);
        boolean hasTarget = data.readBoolean();
        MirroredPatternProviderTarget target = hasTarget ? MirroredPatternProviderTarget.read(data) : null;
        boolean targetChanged = getMirroringLogic().readMirrorTargetFromStream(target);
        return redraw || targetChanged;
    }

    /**
     * 对齐源码：镜像供应器部件没有自己的 GUI，右键重定向到被镜像目标的 GUI
     */
    @Override
    public boolean onUseWithoutItem(EntityPlayer player, net.minecraft.util.math.Vec3d pos) {
        if (!player.world.isRemote) {
            ae2csl.me.logic.MirrorPatternProviderGui.openTargetGui(player, getMirroringLogic());
        }
        return true;
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, EntityPlayer player, net.minecraft.util.EnumHand hand,
                               net.minecraft.util.math.Vec3d pos) {
        if (super.onUseItemOn(heldItem, player, hand, pos)) {
            return true;
        }
        return this.onUseWithoutItem(player, pos);
    }
}
