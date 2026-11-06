/*
 * AE2 Crystal Science - TileQuartzOscillatorClock (1.12.2)
 * 对照 1.21.1 源码 common/block/entity/QuartzOscillatorClockBlockEntity.java
 * 石英振荡时钟：按间隔+脉宽输出红石脉冲。红石卡解锁红石控制模式。
 */
package ae2csl.tile.oscillator;

import ae2.api.networking.IGridNodeListener;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.tile.grid.AENetworkedTile;
import ae2csl.block.oscillator.QuartzOscillatorClockBlock;
import ae2csl.init.AE2CSBlocks;
import ae2csl.me.logic.QuartzOscillatorClockHost;
import ae2csl.me.logic.QuartzOscillatorClockLogic;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.List;

public class TileQuartzOscillatorClock extends AENetworkedTile
    implements QuartzOscillatorClockHost, IConfigurableObject, IUpgradeableObject {

    private final QuartzOscillatorClockLogic logic = createLogic();

    protected QuartzOscillatorClockLogic createLogic() {
        return new QuartzOscillatorClockLogic(getMainNode(), this,
            Item.getItemFromBlock(AE2CSBlocks.QUARTZ_OSCILLATOR_CLOCK));
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return getLogic().getUpgrades();
    }

    @Override
    public IConfigManager getConfigManager() {
        return getLogic().getConfigManager();
    }

    @Override
    public QuartzOscillatorClockLogic getLogic() {
        return this.logic;
    }

    @Override
    public TileEntity getTileEntity() {
        return this;
    }

    @Override
    public EnumSet<EnumFacing> getTargets() {
        return EnumSet.allOf(EnumFacing.class);
    }

    @Override
    public void saveChanges() {
        markDirty();
    }

    @Override
    public void setPulseActive(boolean active) {
        if (world == null || world.isRemote) {
            return;
        }

        IBlockState state = getBlockState();
        if (!(state.getBlock() instanceof QuartzOscillatorClockBlock)) {
            return;
        }

        boolean old = state.getValue(QuartzOscillatorClockBlock.ACTIVE);
        if (old == active) {
            return;
        }

        world.setBlockState(pos, state.withProperty(QuartzOscillatorClockBlock.ACTIVE, active), 3);

        BlockPos p = getPos();
        Block block = state.getBlock();
        world.notifyNeighborsOfStateChange(p, block, false);
        for (EnumFacing dir : getTargets()) {
            world.notifyNeighborsOfStateChange(p.offset(dir), block, false);
        }

        markDirty();
    }

    @Override
    public boolean isPulseActive() {
        IBlockState state = getBlockState();
        if (!(state.getBlock() instanceof QuartzOscillatorClockBlock)) {
            return false;
        }
        return state.getValue(QuartzOscillatorClockBlock.ACTIVE);
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        // 节点状态变化无需额外处理（逻辑自带 Ticker）
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(AE2CSBlocks.QUARTZ_OSCILLATOR_CLOCK);
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
    public void addAdditionalDrops(List<ItemStack> drops) {
        super.addAdditionalDrops(drops);
        this.logic.addDrops(drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.logic.clearContent();
    }
}
