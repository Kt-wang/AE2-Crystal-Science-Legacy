/*
 * AE2 Crystal Science - QuartzOscillatorClockPart (1.12.2)
 */
package ae2csl.me.part;

import ae2.api.parts.IPartCollisionHelper;
import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.core.AppEng;
import ae2.items.parts.PartModels;
import ae2.parts.AEBasePart;
import ae2.parts.PartModel;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.client.gui.GuiHandler;
import ae2csl.me.logic.QuartzOscillatorClockHost;
import ae2csl.me.logic.QuartzOscillatorClockLogic;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

public class QuartzOscillatorClockPart extends AEBasePart implements QuartzOscillatorClockHost, IUpgradeableObject, IConfigurableObject {

    public static final ResourceLocation M_OFF = new ResourceLocation(Tags.MOD_ID, "part/quartz_oscillator_clock/base_off");
    public static final ResourceLocation M_ON = new ResourceLocation(Tags.MOD_ID, "part/quartz_oscillator_clock/base_on");
    @PartModels public static final PartModel MODELS_OFF = new PartModel(M_OFF, AppEng.makeId("part/interface_off"));
    @PartModels public static final PartModel MODELS_ON = new PartModel(M_ON, AppEng.makeId("part/interface_on"));
    @PartModels public static final PartModel MODELS_HAS_CHANNEL = new PartModel(M_OFF, AppEng.makeId("part/interface_has_channel"));

    private final QuartzOscillatorClockLogic logic;
    private boolean pulseActive = false;

    public QuartzOscillatorClockPart(IPartItem<?> partItem) {
        super(partItem);
        this.logic = new QuartzOscillatorClockLogic(getMainNode(), this, getPartItem().asItem());
    }

    @Override public IPartModel getStaticModels() {
        if (pulseActive) return MODELS_ON;
        else if (isActive() && isPowered()) return MODELS_HAS_CHANNEL;
        else return MODELS_OFF;
    }

    @Override public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(5, 5, 12, 11, 11, 14);
    }

    @Override public boolean onUseWithoutItem(EntityPlayer player, Vec3d pos) {
        if (!player.world.isRemote) {
            BlockPos tilePos = this.getTileEntity().getPos();
            player.openGui(AE2CrystalScienceLegacy.INSTANCE,
                GuiHandler.GUI_QUARTZ_OSCILLATOR_CLOCK_PART + this.getSide().ordinal(),
                player.world, tilePos.getX(), tilePos.getY(), tilePos.getZ());
        }
        return true;
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, EntityPlayer player, net.minecraft.util.EnumHand hand,
                               Vec3d pos) {
        if (super.onUseItemOn(heldItem, player, hand, pos)) {
            return true;
        }
        return this.onUseWithoutItem(player, pos);
    }

    // QuartzOscillatorClockHost
    @Override public QuartzOscillatorClockLogic getLogic() { return logic; }
    @Override public void saveChanges() { getHost().markForSave(); }
    @Override public EnumSet<EnumFacing> getTargets() {
        EnumFacing side = getSide();
        return side != null ? EnumSet.of(side) : EnumSet.noneOf(EnumFacing.class);
    }
    @Override public void setPulseActive(boolean active) {
        if (getTileEntity() == null) { this.pulseActive = active; return; }
        World level = getTileEntity().getWorld();
        if (level == null || level.isRemote) { this.pulseActive = active; return; }
        if (this.pulseActive == active) return;
        this.pulseActive = active;
        if (getHost() != null) getHost().markForUpdate();
        BlockPos pos = getTileEntity().getPos();
        level.notifyNeighborsOfStateChange(pos, getTileEntity().getBlockType(), true);
    }
    @Override public boolean isPulseActive() { return pulseActive; }

    // IUpgradeableObject
    @Override public IUpgradeInventory getUpgrades() { return logic.getUpgrades(); }
    // IConfigurableObject
    @Override public IConfigManager getConfigManager() { return logic.getConfigManager(); }

    public ItemStack getMainMenuIcon() { return new ItemStack(getPartItem().asItem()); }
    @Override public boolean canConnectRedstone() { return true; }
    @Override public int isProvidingWeakPower() { return pulseActive ? 15 : 0; }
    @Override public int isProvidingStrongPower() { return pulseActive ? 15 : 0; }

    @Override public void writeToStream(PacketBuffer data) {
        super.writeToStream(data);
        data.writeBoolean(pulseActive);
    }
    @Override public boolean readFromStream(PacketBuffer data) {
        boolean redraw = super.readFromStream(data);
        boolean np = data.readBoolean();
        if (pulseActive != np) { pulseActive = np; redraw = true; }
        return redraw;
    }
    @Override public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.logic.readFromNBT(data);
        this.pulseActive = false;
    }
    @Override public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.logic.writeToNBT(data);
    }
    @Override public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        logic.addDrops(drops);
    }
    @Override public void clearContent() { super.clearContent(); logic.clearContent(); }
}
