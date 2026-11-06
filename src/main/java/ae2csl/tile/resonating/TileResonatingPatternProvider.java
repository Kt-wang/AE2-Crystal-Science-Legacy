/*
 * AE2 Crystal Science - TileResonatingPatternProvider (1.12.2)
 * 对照 1.21.1 源码 common/block/entity/ResonatingPatternProviderBlockEntity.java
 * 谐振样板供应器：谐振样板支持按输入远程目标推送 + 拉动模式 + 1 升级槽。
 */
package ae2csl.tile.resonating;

import ae2.api.AECapabilities;
import ae2.api.networking.IGridNodeListener;
import ae2.api.orientation.BlockOrientation;
import ae2.api.stacks.AEItemKey;
import ae2.api.util.AECableType;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.block.crafting.PushDirection;
import ae2.tile.grid.AENetworkedTile;
import ae2csl.block.resonating.ResonatingPatternProviderBlock;
import ae2csl.init.AE2CSBlocks;
import ae2csl.me.logic.ResonatingPatternProviderHost;
import ae2csl.me.logic.ResonatingPatternProviderLogic;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class TileResonatingPatternProvider extends AENetworkedTile
    implements ResonatingPatternProviderHost, IConfigurableObject {

    private final ResonatingPatternProviderLogic logic = createLogic();

    protected ResonatingPatternProviderLogic createLogic() {
        int patternSize = isExtended() ? 36 : 9;
        return new ResonatingPatternProviderLogic(this.getMainNode(), this, patternSize);
    }

    @Override
    public boolean isExtended() {
        return false; // EX 变体由子类覆盖
    }

    @Override
    public void markForLogicClientUpdate() {
        if (world != null && !world.isRemote) {
            this.markForClientUpdate();
        }
    }

    /**
     * 每 tick 驱动谐振待发冲刷 + 拉动（源码用网格 Ticker，fork 私有成员限制改走 tile tick）
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
        return new ItemStack(AE2CSBlocks.RESONATING_PATTERN_PROVIDER);
    }

    @Override
    public ResonatingPatternProviderLogic getLogic() {
        return this.logic;
    }

    @Override
    public ResonatingPatternProviderLogic getResonatingLogic() {
        return this.logic;
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
    public AECableType getCableConnectionType(EnumFacing dir) {
        EnumFacing pushDirection = getPushDirection().getDirection();
        if (pushDirection == dir) {
            return AECableType.NONE;
        }
        return AECableType.SMART;
    }

    public void onPushDirectionChanged() {
        this.onGridConnectableSidesChanged();
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
        this.onGridConnectableSidesChanged();
    }

    private PushDirection getPushDirection() {
        IBlockState state = getBlockState();
        if (state == null || !(state.getBlock() instanceof ResonatingPatternProviderBlock)) {
            return PushDirection.ALL;
        }
        return state.getValue(ResonatingPatternProviderBlock.PUSH_DIRECTION);
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return new ItemStack(AE2CSBlocks.RESONATING_PATTERN_PROVIDER);
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.logic.getConfigManager();
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

    // ---- 记忆卡默认目标（对齐源码 importSettings/exportSettings） ----

    @Override
    public void exportSettings(ae2.util.SettingsFrom mode, NBTTagCompound output) {
        super.exportSettings(mode, output);
        if (mode == ae2.util.SettingsFrom.DISMANTLE_ITEM || mode == ae2.util.SettingsFrom.MEMORY_CARD) {
            this.logic.writeDefaultsToSettings(output);
        }
    }

    @Override
    public void importSettings(ae2.util.SettingsFrom mode, NBTTagCompound input,
                               @Nullable net.minecraft.entity.player.EntityPlayer player) {
        super.importSettings(mode, input, player);
        if (mode == ae2.util.SettingsFrom.DISMANTLE_ITEM || mode == ae2.util.SettingsFrom.MEMORY_CARD) {
            this.logic.readDefaultsFromSettings(input);
        }
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(new ItemStack(AE2CSBlocks.RESONATING_PATTERN_PROVIDER));
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
