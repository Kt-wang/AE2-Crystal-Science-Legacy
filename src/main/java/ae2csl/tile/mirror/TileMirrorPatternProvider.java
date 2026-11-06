/*
 * AE2 Crystal Science - TileMirrorPatternProvider
 * Extends AE2 PatternProviderLogicHost for mirroring other pattern providers.
 */
package ae2csl.tile.mirror;

import ae2.api.AECapabilities;
import ae2.api.networking.IGridNodeListener;
import ae2.api.orientation.BlockOrientation;
import ae2.api.stacks.AEItemKey;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.tile.grid.AENetworkedTile;
import ae2csl.init.AE2CSBlocks;
import ae2csl.me.logic.MirrorPatternProviderHost;
import ae2csl.me.logic.MirrorPatternProviderLogic;
import ae2csl.me.logic.MirroredPatternProviderTarget;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class TileMirrorPatternProvider extends AENetworkedTile
    implements MirrorPatternProviderHost, IConfigurableObject {

    private final MirrorPatternProviderLogic logic =
        new MirrorPatternProviderLogic(this.getMainNode(), this);

    @Override
    public MirrorPatternProviderLogic getMirroringLogic() {
        return this.logic;
    }

    @Override
    public void markForLogicClientUpdate() {
        if (world != null && !world.isRemote) {
            this.markForClientUpdate();
        }
    }

    /** 破坏时掉落内容物（样板/升级卡），由方块 breakBlock 调用 */
    @Override
    public void addAdditionalDrops(java.util.List<ItemStack> drops) {
        super.addAdditionalDrops(drops);
        this.logic.addDrops(drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.logic.clearContent();
    }

    // ---- 镜像目标客户端同步（对照源码 writeToStream/readFromStream） ----

    @Override
    protected void writeToStream(io.netty.buffer.ByteBuf data) {
        super.writeToStream(data);
        var target = getMirroringLogic().getMirrorTarget();
        data.writeBoolean(target != null);
        if (target != null) {
            target.write(new net.minecraft.network.PacketBuffer(data));
        }
    }

    @Override
    protected boolean readFromStream(io.netty.buffer.ByteBuf data) {
        boolean redraw = super.readFromStream(data);
        var buf = new net.minecraft.network.PacketBuffer(data);
        boolean targetChanged = getMirroringLogic().readMirrorTargetFromStream(
            buf.readBoolean() ? MirroredPatternProviderTarget.read(buf) : null);
        return redraw || targetChanged;
    }

    // ---- 记忆卡/拆解设置（对照源码 importSettings/exportSettings） ----

    @Override
    public void exportSettings(ae2.util.SettingsFrom mode, NBTTagCompound output) {
        super.exportSettings(mode, output);
        if (mode == ae2.util.SettingsFrom.DISMANTLE_ITEM || mode == ae2.util.SettingsFrom.MEMORY_CARD) {
            getMirroringLogic().writeMirrorSettings(output);
        }
    }

    @Override
    public void importSettings(ae2.util.SettingsFrom mode, NBTTagCompound input,
                               @Nullable net.minecraft.entity.player.EntityPlayer player) {
        super.importSettings(mode, input, player);
        if (mode == ae2.util.SettingsFrom.DISMANTLE_ITEM || mode == ae2.util.SettingsFrom.MEMORY_CARD) {
            getMirroringLogic().readMirrorSettings(input);
            markDirty();
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        this.logic.updatePatterns();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        this.logic.onMainNodeStateChanged();
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        this.logic.writeToNBT(data);
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.logic.readFromNBT(data);
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(AE2CSBlocks.MIRROR_PATTERN_PROVIDER);
    }

    @Override
    public PatternProviderLogic getLogic() {
        return this.logic;
    }

    @Override
    public TileEntity getTileEntity() {
        return this;
    }

    /**
     * 样板推送目标面（PUSH_DIRECTION，对齐源码 PatternProviderBlock 行为）
     */
    @Override
    public EnumSet<EnumFacing> getTargets() {
        EnumFacing pushDirection = getPushDirection().getDirection();
        return pushDirection == null ? EnumSet.allOf(EnumFacing.class) : EnumSet.of(pushDirection);
    }

    @Override
    public EnumSet<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        EnumFacing pushDirection = getPushDirection().getDirection();
        return pushDirection == null ? EnumSet.allOf(EnumFacing.class)
            : EnumSet.complementOf(EnumSet.of(pushDirection));
    }

    @Override
    public ae2.api.util.AECableType getCableConnectionType(EnumFacing dir) {
        EnumFacing pushDirection = getPushDirection().getDirection();
        if (pushDirection == dir) {
            return ae2.api.util.AECableType.NONE;
        }
        return ae2.api.util.AECableType.SMART;
    }

    public void onPushDirectionChanged() {
        this.onGridConnectableSidesChanged();
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
        this.onGridConnectableSidesChanged();
    }

    private ae2.block.crafting.PushDirection getPushDirection() {
        IBlockState state = getBlockState();
        if (state == null || !(state.getBlock() instanceof ae2csl.block.mirror.MirrorPatternProviderBlock)) {
            return ae2.block.crafting.PushDirection.ALL;
        }
        return state.getValue(ae2csl.block.mirror.MirrorPatternProviderBlock.PUSH_DIRECTION);
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return new ItemStack(AE2CSBlocks.MIRROR_PATTERN_PROVIDER);
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.logic.getConfigManager();
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(new ItemStack(AE2CSBlocks.MIRROR_PATTERN_PROVIDER));
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.GENERIC_INTERNAL_INV) return true;
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.GENERIC_INTERNAL_INV)
            return AECapabilities.GENERIC_INTERNAL_INV.cast(this.logic.getReturnInv());
        return super.getCapability(capability, facing);
    }
}
