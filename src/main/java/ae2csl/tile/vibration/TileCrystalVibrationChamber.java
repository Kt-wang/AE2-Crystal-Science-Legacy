/*
 * AE2 Crystal Science - Crystal Vibration Chamber
 * Burns PureCrystalItems and vanilla furnace fuels to generate AE power.
 * Power flow: READ (grid extracts FROM this generator).
 */
package ae2csl.tile.vibration;

import ae2.api.config.AccessRestriction;
import ae2.api.config.Actionable;
import ae2csl.init.AE2CSBlocks;
import ae2csl.item.crystal.PureCrystalItem;
import ae2csl.tile.base.AENetworkedPoweredTile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class TileCrystalVibrationChamber extends AENetworkedPoweredTile implements IInventory, ae2csl.machine.ISideConfigHost {

    private static final double FALLBACK_ENERGY_PER_TICK = 100;
    private static final int INVENTORY_SIZE = 4; // 0 = fuel, 1..3 = upgrades (speed cards)

    private int remainingBurnTime;
    private int maxBurnTime;
    private double energyPerTick;
    private boolean isActive;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    public TileCrystalVibrationChamber() { super(1000000); }

    @Override protected ItemStack getMachineRepresentation() { return new ItemStack(AE2CSBlocks.CRYSTAL_VIBRATION_CHAMBER); }
    @Override protected double getIdlePowerUsage() { return 0; }
    @Override public AccessRestriction getPowerFlow() { return AccessRestriction.READ; }

    /**
     * Check if an ItemStack can be used as fuel.
     * PureCrystalItems use their own stats; vanilla furnace fuels use furnace burn time + fallback AE/t.
     */
    public static boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof PureCrystalItem) {
            PureCrystalItem c = (PureCrystalItem) stack.getItem();
            return c.getBurnTime(stack) > 0 && c.getEnergyPerTick(stack) > 0;
        }
        return TileEntityFurnace.getItemBurnTime(stack) > 0;
    }

    public static int getFuelBurnTime(ItemStack stack) {
        if (stack.getItem() instanceof PureCrystalItem)
            return ((PureCrystalItem) stack.getItem()).getBurnTime(stack);
        return TileEntityFurnace.getItemBurnTime(stack);
    }

    public static double getFuelEnergyPerTick(ItemStack stack) {
        if (stack.getItem() instanceof PureCrystalItem)
            return ((PureCrystalItem) stack.getItem()).getEnergyPerTick(stack);
        return FALLBACK_ENERGY_PER_TICK;
    }

    // ---- Ticking ----
    @Override public void update() {
        if (world == null || world.isRemote) return;

        // 自动输入/输出（对齐 1.21.1 源码 SideConfigComponent）：燃料槽 0；发电机无物品产出，无输出槽
        ae2csl.util.AutoTransferHelper.tick(world, pos, this, sideConfig, s -> s == 0, s -> false);

        if (remainingBurnTime <= 0) {
            ItemStack fuel = inventory.get(0);
            if (isFuel(fuel)) {
                remainingBurnTime = getFuelBurnTime(fuel);
                maxBurnTime = remainingBurnTime;
                energyPerTick = getFuelEnergyPerTick(fuel);
                fuel.shrink(1);
                if (fuel.isEmpty()) inventory.set(0, ItemStack.EMPTY);
                markDirty();
            }
        }

        if (remainingBurnTime > 0) {
            // 陨石超频卡：燃烧最多 4 刻（1 张）/ 1 刻（2 张）内完成，产能按燃烧速率等比放大，
            // 总能量守恒（对齐 1.21.1 源码 CrystalVibrationChamberBlockEntity）；装卡后加速卡失效
            int oc = countOverloadCards();
            int speedCards = oc > 0 ? 0 : countSpeedCards();
            int normalCost = 1 + speedCards;
            int burnCost = normalCost;
            if (oc > 0 && maxBurnTime > 0) {
                int targetTicks = oc == 1 ? 4 : 1;
                burnCost = Math.max(normalCost, (maxBurnTime + targetTicks - 1) / targetTicks);
            }
            burnCost = Math.min(remainingBurnTime, burnCost);
            remainingBurnTime -= burnCost;
            if (getCurrentPower() < getMaxPower()) {
                injectAEPower(energyPerTick * (1 + 0.5 * speedCards) * burnCost / normalCost, Actionable.MODULATE);
            }
            tryPushPower(getCurrentPower());
            if (!isActive) { isActive = true; notifyBlockUpdate(); }
            markDirty();
        } else {
            if (isActive) { isActive = false; energyPerTick = 0; notifyBlockUpdate(); }
        }
    }

    private void notifyBlockUpdate() {
        World w = getWorld(); BlockPos p = getPos();
        if (w != null && !w.isRemote) { IBlockState s = w.getBlockState(p); w.notifyBlockUpdate(p, s, s, 3); }
    }

    public int getRemainingBurnTime() { return remainingBurnTime; }
    public int getMaxBurnTime() { return maxBurnTime; }
    public double getEnergyPerTick() { return energyPerTick; }
    public boolean isActive() { return isActive; }
    public int getMaxProgress() { return 100; }

    /** Number of AE2 speed cards installed in the upgrade slots (1-3). */
    public int countSpeedCards() {
        int count = 0;
        for (int i = 1; i < INVENTORY_SIZE; i++) {
            ItemStack s = inventory.get(i);
            if (!s.isEmpty() && s.getItem() == ae2.core.definitions.AEItems.SPEED_CARD.item()) count++;
        }
        return Math.min(3, count);
    }

    /** 陨石超频卡数量（升级槽 1..3，最多 2 张生效） */
    public int countOverloadCards() {
        int count = 0;
        for (int i = 1; i < INVENTORY_SIZE; i++) {
            if (ae2csl.util.UpgradeCardHelper.isOverloadCard(inventory.get(i))) count++;
        }
        return Math.min(2, count);
    }

    /** 自动化能力：插入限燃料槽（isItemValidForSlot 约束）；发电机无产出，禁止提取 */
    @Override
    protected ae2csl.util.MachineItemHandler createAutomationItemHandler() {
        return new ae2csl.util.MachineItemHandler(this, slot -> false);
    }

    // ---- IInventory ----
    @Override public int getSizeInventory() { return INVENTORY_SIZE; }
    @Override public boolean isEmpty() { for (ItemStack s : inventory) if (!s.isEmpty()) return false; return true; }
    @Override public ItemStack getStackInSlot(int i) { return i >= 0 && i < INVENTORY_SIZE ? inventory.get(i) : ItemStack.EMPTY; }
    @Override public ItemStack decrStackSize(int i, int c) { if (i < 0 || i >= INVENTORY_SIZE) return ItemStack.EMPTY; ItemStack s = ItemStackHelper.getAndSplit(inventory, i, c); if (!s.isEmpty()) markDirty(); return s; }
    @Override public ItemStack removeStackFromSlot(int i) { if (i < 0 || i >= INVENTORY_SIZE) return ItemStack.EMPTY; ItemStack s = ItemStackHelper.getAndRemove(inventory, i); markDirty(); return s; }
    @Override public void setInventorySlotContents(int i, ItemStack s) { if (i >= 0 && i < INVENTORY_SIZE) { inventory.set(i, s); if (s.getCount() > 64) s.setCount(64); markDirty(); } }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUsableByPlayer(EntityPlayer p) { return world.getTileEntity(pos) == this && p.getDistanceSq(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) <= 64; }
    @Override public void openInventory(EntityPlayer p) {}
    @Override public void closeInventory(EntityPlayer p) {}

    @Override public boolean isItemValidForSlot(int i, ItemStack s) {
        if (i >= 1) {
            return ae2csl.util.UpgradeCardHelper.canInsert(this, 1, INVENTORY_SIZE, i, s,
                net.minecraft.item.Item.getItemFromBlock(AE2CSBlocks.CRYSTAL_VIBRATION_CHAMBER));
        }
        return isFuel(s);
    }

    private int clientEnergyPct;
    public int getClientEnergyPct() { return clientEnergyPct; }

    @Override public int getField(int id) { switch(id) { case 0: return remainingBurnTime; case 1: return getMaxPower() > 0 ? (int)(getCurrentPower() / getMaxPower() * 100) : 0; case 2: return maxBurnTime; default: return 0; } }
    @Override public void setField(int id, int v) { switch(id) { case 0: remainingBurnTime = v; break; case 1: clientEnergyPct = Math.min(100, Math.max(0, v)); break; case 2: maxBurnTime = v; break; } }
    @Override public int getFieldCount() { return 3; }
    @Override public void clear() { inventory.clear(); }
    @Override public String getName() { return "container.crystal_vibration_chamber"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentTranslation(getName()); }

    // ---- 侧面配置（自动输入/输出 + 按面能力过滤） ----
    private final ae2csl.machine.SideConfigData sideConfig = new ae2csl.machine.SideConfigData(this::markDirty);
    @Override public ae2csl.machine.SideConfigData getSideConfig() { return sideConfig; }
    @Override public int getMachineGuiId() { return ae2csl.client.gui.GuiHandler.GUI_CRYSTAL_VIBRATION_CHAMBER; }

    // ---- NBT ----
    @Override public void readFromNBT(NBTTagCompound c) {
        super.readFromNBT(c);
        sideConfig.readFromNBT(c);
        remainingBurnTime = c.getInteger("remainingBurnTime");
        maxBurnTime = c.getInteger("maxBurnTime");
        energyPerTick = c.getDouble("energyPerTick");
        isActive = c.getBoolean("isActive");
        inventory = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(c, inventory);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound c) {
        super.writeToNBT(c);
        sideConfig.writeToNBT(c);
        c.setInteger("remainingBurnTime", remainingBurnTime);
        c.setInteger("maxBurnTime", maxBurnTime);
        c.setDouble("energyPerTick", energyPerTick);
        c.setBoolean("isActive", isActive);
        ItemStackHelper.saveAllItems(c, inventory);
        return c;
    }
}
