/*
 * AE2 Crystal Science - TileIntegratedInterface (1.12.2)
 * 对照 1.21.1 源码 common/block/entity/IntegratedInterfaceBlockEntity.java
 * 同时包含 ME 样板供应器与 ME 接口的能力。即旧版 ME 接口。
 * 接口形态见 {@link ae2csl.me.part.IntegratedInterfacePart}
 */
package ae2csl.tile.integrated;

import ae2.api.AECapabilities;
import ae2.api.inventories.ISegmentedInventory;
import ae2.api.inventories.InternalInventory;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.IManagedGridNode;
import ae2.api.orientation.BlockOrientation;
import ae2.api.stacks.AEItemKey;
import ae2.api.util.AECableType;
import ae2.block.crafting.PushDirection;
import ae2.tile.grid.AENetworkedInvTile;
import ae2csl.block.integrated.IntegratedInterfaceBlock;
import ae2csl.init.AE2CSBlocks;
import ae2csl.me.logic.IntegratedInterfaceHost;
import ae2csl.me.logic.IntegratedInterfaceLogic;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class TileIntegratedInterface extends AENetworkedInvTile implements IntegratedInterfaceHost {

    private static final IGridNodeListener<TileIntegratedInterface> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(TileIntegratedInterface nodeOwner, IGridNode node) {
            nodeOwner.markDirty();
        }

        @Override
        public void onStateChanged(TileIntegratedInterface nodeOwner, IGridNode node, State state) {
            nodeOwner.onMainNodeStateChanged(state);
        }
    };

    private final IntegratedInterfaceLogic logic = createLogic();
    private int priority;
    private int tickCounter;

    public TileIntegratedInterface() {
        // 懒初始化（logic 字段初始化时 getMainNode() 已由父类构造器创建）
    }

    @Override
    protected IManagedGridNode createMainNode() {
        return GridHelper.createManagedNode(this, NODE_LISTENER);
    }

    /**
     * 是否为扩展版（36槽）。普通版恒 false；EX 变体由子类覆盖（阶段D）。
     * 注意：不能在构造期读 getBlockType()（world 尚未赋值会 NPE）。
     */
    @Override
    public boolean isExtended() {
        return false;
    }

    protected IntegratedInterfaceLogic createLogic() {
        int size = isExtended() ? 36 : 9;
        return new IntegratedInterfaceLogic(this.getMainNode(), this, size, size);
    }

    @Override
    public IntegratedInterfaceLogic getLogic() {
        return logic;
    }

    /**
     * 获取接口对应的目标面
     */
    @Override
    public EnumSet<EnumFacing> getTargets() {
        EnumFacing pushDirection = getPushDirection().getDirection();
        if (pushDirection == null) {
            return EnumSet.allOf(EnumFacing.class);
        } else {
            return EnumSet.of(pushDirection);
        }
    }

    /**
     * 可以用线缆进行连接的面，即除了目标面之外的面
     */
    @Override
    public EnumSet<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        EnumFacing pushDirection = getPushDirection().getDirection();
        if (pushDirection == null) {
            return EnumSet.allOf(EnumFacing.class);
        }
        return EnumSet.complementOf(EnumSet.of(pushDirection));
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        // 推送面不接线缆
        EnumFacing pushDirection = getPushDirection().getDirection();
        if (pushDirection == dir) {
            return AECableType.NONE;
        }
        return AECableType.SMART;
    }

    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        this.logic.onMainNodeStateChanged();
    }

    /**
     * 由方块的 neighborChanged 调用（红石锁）
     */
    public void onNeighborChanged() {
        this.logic.updateRedstoneState();
    }

    /**
     * 推送面被扳手修改后调用
     */
    public void onPushDirectionChanged() {
        this.onGridConnectableSidesChanged();
    }

    @Override
    public void update() {
        super.update();
        if (world != null && !world.isRemote && tickCounter++ % 20 == 0) {
            this.logic.doPullWork();
        }
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
        this.onGridConnectableSidesChanged();
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        this.logic.writeToNBT(data);
        data.setInteger("priority", this.priority);
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.logic.readFromNBT(data);
        this.priority = data.getInteger("priority");
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

    // ---- IntegratedInterfaceHost ----

    @Override
    public TileEntity getTileEntity() {
        return this;
    }

    @Override
    public void saveChanges() {
        markDirty();
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(getMainMenuIcon());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(AE2CSBlocks.INTEGRATED_INTERFACE);
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(AE2CSBlocks.INTEGRATED_INTERFACE);
    }

    // ---- IPriorityHost ----

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int newValue) {
        this.priority = newValue;
        markDirty();
    }

    // ---- 记忆卡（对照源码 exportSettings/importSettings） ----

    private static final String MEMORY_CARD_PUSH_DIRECTION = "pushDirection";
    private static final String MEMORY_CARD_PRIORITY = "priority";

    @Override
    public void exportSettings(ae2.util.SettingsFrom mode, NBTTagCompound output) {
        super.exportSettings(mode, output);
        if (mode == ae2.util.SettingsFrom.MEMORY_CARD) {
            this.logic.exportSettings(output);
            output.setInteger(MEMORY_CARD_PUSH_DIRECTION, getPushDirection().ordinal());
            output.setInteger(MEMORY_CARD_PRIORITY, this.priority);
        }
    }

    @Override
    public void importSettings(ae2.util.SettingsFrom mode, NBTTagCompound input,
                               @Nullable net.minecraft.entity.player.EntityPlayer player) {
        super.importSettings(mode, input, player);
        if (mode == ae2.util.SettingsFrom.MEMORY_CARD) {
            this.logic.importSettings(input, player);

            if (input.hasKey(MEMORY_CARD_PRIORITY, net.minecraftforge.common.util.Constants.NBT.TAG_INT)) {
                setPriority(input.getInteger(MEMORY_CARD_PRIORITY));
            }

            // 恢复目标面
            if (input.hasKey(MEMORY_CARD_PUSH_DIRECTION, net.minecraftforge.common.util.Constants.NBT.TAG_INT)) {
                int ordinal = input.getInteger(MEMORY_CARD_PUSH_DIRECTION);
                PushDirection[] values = PushDirection.values();
                if (ordinal >= 0 && ordinal < values.length) {
                    IBlockState state = getBlockState();
                    if (state != null && state.getBlock() instanceof IntegratedInterfaceBlock) {
                        this.world.setBlockState(this.pos,
                            state.withProperty(IntegratedInterfaceBlock.PUSH_DIRECTION, values[ordinal]), 3);
                        this.onPushDirectionChanged();
                    }
                }
            }
        }
    }

    // ---- Sub-inventory access ----

    @Nullable
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (ISegmentedInventory.UPGRADES.equals(id)) {
            return this.logic.getUpgrades();
        }
        return null;
    }

    private PushDirection getPushDirection() {
        IBlockState state = getBlockState();
        if (state == null || !(state.getBlock() instanceof IntegratedInterfaceBlock)) {
            return PushDirection.ALL;
        }
        return state.getValue(IntegratedInterfaceBlock.PUSH_DIRECTION);
    }

    // ---- Capabilities ----

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.ME_STORAGE) {
            return AECapabilities.ME_STORAGE.cast(
                this.logic.getExposedMEStorage(facing == null ? EnumFacing.NORTH : facing));
        }
        if (capability == AECapabilities.GENERIC_INTERNAL_INV) {
            return AECapabilities.GENERIC_INTERNAL_INV.cast(this.logic.getStorageInv());
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.ME_STORAGE) {
            return true;
        }
        if (capability == AECapabilities.GENERIC_INTERNAL_INV) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }
}
