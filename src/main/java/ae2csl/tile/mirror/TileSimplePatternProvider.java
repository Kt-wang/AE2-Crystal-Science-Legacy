/*
 * AE2 Crystal Science - TileSimplePatternProvider (1.12.2)
 * 对照 1.21.1 源码 common/block/entity/SimplePatternProviderBlockEntity.java
 * 一个只有 5 个槽位的样板供应器（fork PatternProviderLogic 直接驱动）。
 */
package ae2csl.tile.mirror;

import ae2.api.AECapabilities;
import ae2.api.networking.IGridNodeListener;
import ae2.api.orientation.BlockOrientation;
import ae2.api.stacks.AEItemKey;
import ae2.api.util.AECableType;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.block.crafting.PushDirection;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2.tile.grid.AENetworkedTile;
import ae2csl.block.mirror.SimplePatternProviderBlock;
import ae2csl.init.AE2CSBlocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class TileSimplePatternProvider extends AENetworkedTile
    implements PatternProviderLogicHost, IConfigurableObject {

    private final PatternProviderLogic logic =
        new PatternProviderLogic(this.getMainNode(), this, 5);

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
        return new ItemStack(AE2CSBlocks.SIMPLE_PATTERN_PROVIDER_BLOCK);
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
     * 样板推送的目标面（PUSH_DIRECTION 属性）
     */
    @Override
    public EnumSet<EnumFacing> getTargets() {
        EnumFacing pushDirection = getPushDirection().getDirection();
        return pushDirection == null ? EnumSet.allOf(EnumFacing.class) : EnumSet.of(pushDirection);
    }

    /**
     * 可接线缆的面（除推送面之外）
     */
    @Override
    public EnumSet<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        EnumFacing pushDirection = getPushDirection().getDirection();
        return pushDirection == null ? EnumSet.allOf(EnumFacing.class)
            : EnumSet.complementOf(EnumSet.of(pushDirection));
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        EnumFacing pushDirection = getPushDirection().getDirection();
        if (pushDirection == dir) {
            return AECableType.NONE;
        }
        return AECableType.SMART;
    }

    /**
     * 推送面被扳手修改后调用
     */
    public void onPushDirectionChanged() {
        this.onGridConnectableSidesChanged();
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
        this.onGridConnectableSidesChanged();
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return new ItemStack(AE2CSBlocks.SIMPLE_PATTERN_PROVIDER_BLOCK);
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.logic.getConfigManager();
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(new ItemStack(AE2CSBlocks.SIMPLE_PATTERN_PROVIDER_BLOCK));
    }

    private PushDirection getPushDirection() {
        IBlockState state = getBlockState();
        if (state == null || !(state.getBlock() instanceof SimplePatternProviderBlock)) {
            return PushDirection.ALL;
        }
        return state.getValue(SimplePatternProviderBlock.PUSH_DIRECTION);
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
        if (capability == AECapabilities.GENERIC_INTERNAL_INV) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.GENERIC_INTERNAL_INV) {
            return AECapabilities.GENERIC_INTERNAL_INV.cast(this.logic.getReturnInv());
        }
        return super.getCapability(capability, facing);
    }
}
