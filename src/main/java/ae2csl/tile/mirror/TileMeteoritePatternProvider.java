/*
 * AE2 Crystal Science - TileMeteoritePatternProvider
 * Extends AE2 PatternProviderLogicHost for pattern pushing to adjacent machines.
 */
package ae2csl.tile.mirror;

import ae2.api.AECapabilities;
import ae2.api.networking.IGridNodeListener;
import ae2.api.orientation.BlockOrientation;
import ae2.api.stacks.AEItemKey;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.tile.grid.AENetworkedTile;
import ae2csl.init.AE2CSBlocks;
import ae2csl.me.logic.MeteoritePatternProviderHost;
import ae2csl.me.logic.MeteoritePatternProviderLogic;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class TileMeteoritePatternProvider extends AENetworkedTile
    implements MeteoritePatternProviderHost, IConfigurableObject, ITickable {

    private final MeteoritePatternProviderLogic logic =
        new MeteoritePatternProviderLogic(this.getMainNode(), this, 63);

    @Override
    public MeteoritePatternProviderLogic getLogic() {
        return this.logic;
    }

    /**
     * 每 tick 驱动快速合成产物输出（对齐源码 Ticker 的 workCraftedContents 部分）
     */
    @Override
    public void update() {
        super.update();
        if (world != null && !world.isRemote) {
            this.logic.tickServer();
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
        return new ItemStack(AE2CSBlocks.METEORITE_PATTERN_PROVIDER_BLOCK);
    }

    @Override
    public TileEntity getTileEntity() {
        return this;
    }

    /**
     * 样板推送目标面（PUSH_DIRECTION）
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
        if (state == null || !(state.getBlock() instanceof ae2csl.block.mirror.MeteoritePatternProviderBlock)) {
            return ae2.block.crafting.PushDirection.ALL;
        }
        return state.getValue(ae2csl.block.mirror.MeteoritePatternProviderBlock.PUSH_DIRECTION);
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return new ItemStack(AE2CSBlocks.METEORITE_PATTERN_PROVIDER_BLOCK);
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.logic.getConfigManager();
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(new ItemStack(AE2CSBlocks.METEORITE_PATTERN_PROVIDER_BLOCK));
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops) {
        super.addAdditionalDrops(drops);
        this.logic.addDrops(drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.logic.clearContent();
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
