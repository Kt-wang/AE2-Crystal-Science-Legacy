/*
 * AE2 Crystal Science - AE2 Networked TileEntity Base
 * Provides AE2 grid connectivity for machines.
 */
package ae2csl.tile.base;

import ae2.api.networking.*;
import ae2.api.util.AECableType;
import ae2.api.AECapabilities;
import ae2.me.ManagedGridNode;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public abstract class AENetworkedTileBase extends TileEntity implements ITickable, IInWorldGridNodeHost, IGridNodeListener<AENetworkedTileBase> {

    private ManagedGridNode mainNode;

    // ---- Subclass hooks ----
    protected abstract ItemStack getMachineRepresentation();
    protected double getIdlePowerUsage() { return 1.0; }
    protected void onGridReady() {}
    protected GridFlags[] getMainNodeFlags() { return new GridFlags[]{GridFlags.REQUIRE_CHANNEL}; }

    // ---- Managed Grid Node ----
    public ManagedGridNode getMainNode() {
        if (mainNode == null && world != null) {
            mainNode = (ManagedGridNode) GridHelper.createManagedNode(this, this);
            mainNode.setInWorldNode(true);
            mainNode.setIdlePowerUsage(getIdlePowerUsage());
            mainNode.setFlags(getMainNodeFlags());
            ItemStack rep = getMachineRepresentation();
            if (!rep.isEmpty()) mainNode.setVisualRepresentation(rep);
        }
        return mainNode;
    }

    // ---- IGridNodeListener ----
    @Override public void onSaveChanges(AENetworkedTileBase nodeOwner, IGridNode node) { markDirty(); }
    @Override public void onStateChanged(AENetworkedTileBase owner, IGridNode node, State state) {
        if (state == State.GRID_BOOT) onGridReady();
    }

    // ---- IInWorldGridNodeHost ----
    @Override public IGridNode getGridNode(EnumFacing dir) { return mainNode != null ? mainNode.getNode() : null; }
    @Override public AECableType getCableConnectionType(EnumFacing dir) { return AECableType.COVERED; }

    // ---- Lifecycle ----
    @Override public void onLoad() { super.onLoad(); getMainNode().create(world, pos); }
    @Override public void onChunkUnload() { super.onChunkUnload(); destroyMainNode(); }
    @Override public void invalidate() { super.invalidate(); destroyMainNode(); }

    /**
     * Destroy and drop the main node so a later {@link #onLoad()} can recreate a fresh node.
     * AE2's {@code ManagedGridNode} can only be {@code create()}d once; {@code destroy()} nulls
     * the inner node but leaves the init data cleared, so reusing the same instance would throw
     * "The node has already been initialized" on the next load. Nulling the reference forces
     * {@link #getMainNode()} to build a brand-new node.
     */
    private void destroyMainNode() {
        if (mainNode != null) {
            mainNode.destroy();
            mainNode = null;
            onMainNodeDestroyed();
        }
    }

    /** Hook for subclasses to reset one-shot registration flags when the node is torn down. */
    protected void onMainNodeDestroyed() {}

    @Override public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (mainNode != null && nbt.hasKey("ae_node"))
            mainNode.loadFromNBT(nbt.getCompoundTag("ae_node"));
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (mainNode != null) {
            NBTTagCompound tag = new NBTTagCompound();
            mainNode.saveToNBT(tag);
            nbt.setTag("ae_node", tag);
        }
        return nbt;
    }

    // ---- Capabilities ----
    @Override public boolean hasCapability(Capability<?> cap, @Nullable EnumFacing facing) {
        if (cap == AECapabilities.IN_WORLD_GRID_NODE_HOST) return true;
        if (cap == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (getAutomationItemHandler() == null) return false;
            // 侧配：策略为 NONE 的面不暴露物品能力（对齐 1.21.1 源码 SideConfigComponent）
            if (facing != null && this instanceof ae2csl.machine.ISideConfigHost) {
                ae2csl.machine.SidePolicy p = ((ae2csl.machine.ISideConfigHost) this).getSideConfig().get(facing);
                return p.allowInsert() || p.allowExtract();
            }
            return true;
        }
        return super.hasCapability(cap, facing);
    }
    @SuppressWarnings("unchecked")
    @Nullable @Override public <T> T getCapability(Capability<T> cap, @Nullable EnumFacing facing) {
        if (cap == AECapabilities.IN_WORLD_GRID_NODE_HOST) return (T) this;
        if (cap == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            net.minecraftforge.items.IItemHandler handler = getAutomationItemHandler();
            if (handler == null) return super.getCapability(cap, facing);
            // 侧配：按面策略过滤插入/提取（每次新建以实时反映策略变化）
            if (facing != null && this instanceof ae2csl.machine.ISideConfigHost) {
                ae2csl.machine.SidePolicy p = ((ae2csl.machine.ISideConfigHost) this).getSideConfig().get(facing);
                if (!p.allowInsert() && !p.allowExtract()) return null;
                return (T) new ae2csl.util.SideFilteredItemHandler(handler, p);
            }
            return (T) handler;
        }
        return super.getCapability(cap, facing);
    }

    // ---- 自动化物品能力（样板供应器/管道等通过 ITEM_HANDLER_CAPABILITY 推拉物品）----

    private net.minecraftforge.items.IItemHandler automationItemHandler;
    private boolean automationItemHandlerCreated;

    /**
     * 子类覆写：创建机器的物品能力视图（插入受 isItemValidForSlot 约束，提取限输出槽）。
     * 返回 null 表示不对外暴露物品能力。
     */
    @Nullable
    protected ae2csl.util.MachineItemHandler createAutomationItemHandler() {
        return null;
    }

    @Nullable
    private net.minecraftforge.items.IItemHandler getAutomationItemHandler() {
        if (!automationItemHandlerCreated) {
            automationItemHandlerCreated = true;
            automationItemHandler = createAutomationItemHandler();
        }
        return automationItemHandler;
    }
}
