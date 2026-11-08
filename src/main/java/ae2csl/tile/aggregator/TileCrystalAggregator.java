/* AE2 Crystal Science - TileCrystalAggregator */
package ae2csl.tile.aggregator;

import ae2csl.machine.MachineComponentContainer;
import ae2csl.machine.component.GenericStackInvComponent;
import ae2csl.machine.component.EnergyComponent;
import ae2csl.machine.component.InvPort;
import ae2csl.tile.base.AENetworkedPoweredTile;
import ae2csl.init.AE2CSBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class TileCrystalAggregator extends AENetworkedPoweredTile implements IInventory, EnergyComponent.IPowerProvider, ae2csl.machine.ISideConfigHost, ae2csl.machine.IUpgradeSlotsHost {
    private static final double MAX_POWER = 80000;
    private static final double ENERGY_PER_TICK = 200;
    private static final int SLOTS = 8;  // 0..2 input, 3 output, 4..7 upgrades (speed cards)

    private int recipeProgress;
    private int currentProcessTime;   // processTime of the active recipe (for GUI progress bar)
    private transient double progressAcc; // 欠压降速的小数进度累积
    private boolean isActive;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    private final MachineComponentContainer components = new MachineComponentContainer();
    private final GenericStackInvComponent invComponent = new GenericStackInvComponent();
    private final EnergyComponent energyComponent = new EnergyComponent(this);

    public TileCrystalAggregator() { super(MAX_POWER);
        invComponent.addPort(InvPort.WORK, this);
        components.addInventory(invComponent);
        components.addService(EnergyComponent.class, energyComponent); }

    @Override protected ItemStack getMachineRepresentation() {
        return new ItemStack(AE2CSBlocks.CRYSTAL_AGGREGATOR);
    }

        @Override public void update() {
        if (world == null || world.isRemote) return;

        // Find matching recipe
        java.util.List<ItemStack> inputs = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) inputs.add(inventory.get(i));

        ae2csl.recipe.crystal_aggregator.CrystalAggregatorRecipe recipe = null;
        for (Object obj : ae2csl.recipe.AE2CSRecipeRegistry.getRecipes("crystal_aggregator")) {
            ae2csl.recipe.crystal_aggregator.CrystalAggregatorRecipe r = (ae2csl.recipe.crystal_aggregator.CrystalAggregatorRecipe) obj;
            if (r.matches(inputs)) { recipe = r; break; }
        }

        boolean canCraft = false;
        if (recipe != null) {
            ItemStack result = recipe.getResult();
            ItemStack output = inventory.get(3);
            canCraft = output.isEmpty()
                || (output.isItemEqual(result)
                    && output.getCount() + result.getCount() <= output.getMaxStackSize());
            if (canCraft) currentProcessTime = recipe.getProcessTime();
        }

        // Reset progress if recipe or output becomes invalid
        if (!canCraft) { recipeProgress = 0; progressAcc = 0; }

        // Charge internal buffer from the AE grid
        chargeFromGrid(MAX_POWER);

        // 自动输入/输出（对齐 1.21.1 源码 SideConfigComponent）：输入槽 0..2，输出槽 3
        ae2csl.util.AutoTransferHelper.tick(world, pos, this, sideConfig, s -> s < 3, s -> s == 3);

        // 陨石超频卡：1 张 → 一次操作最多 4 刻；2 张 → 最多 1 刻；装卡后加速卡失效
        int oc = countOverloadCards();
        int mult;
        if (oc > 0) {
            int targetTicks = oc == 1 ? 4 : 1;
            mult = recipe != null ? Math.max(1, (recipe.getProcessTime() + targetTicks - 1) / targetTicks) : 1;
        } else {
            mult = getSpeedMultiplier();
        }
        double energyPerTick = ENERGY_PER_TICK * mult;
        // 供能不足时按实际供能比例降速（不会完全停摆）
        boolean canWork = false;
        if (canCraft) {
            double drawn = Math.min(energyPerTick, getCurrentPower());
            if (drawn > 0) {
                setCurrentPower(getCurrentPower() - drawn);
                progressAcc += mult * drawn / energyPerTick;
                int whole = (int) progressAcc;
                if (whole > 0) {
                    progressAcc -= whole;
                    recipeProgress += whole;
                }
                canWork = true;
            }
        }

        if (canWork) {
            if (recipeProgress >= recipe.getProcessTime()) {
                recipeProgress = 0;
                progressAcc = 0;
                recipe.consumeInputs(inputs);
                ItemStack result = recipe.getResult();
                ItemStack output = inventory.get(3);
                if (output.isEmpty()) inventory.set(3, result);
                else output.grow(result.getCount());
            }
            markDirty();
        }

        boolean newActive = canWork;
        if (newActive != isActive) { isActive = newActive; notifyBlockUpdate(); }
    }

    
    private void notifyBlockUpdate() {
        World w = getWorld(); BlockPos p = getPos();
        if (w != null && !w.isRemote) { IBlockState s = w.getBlockState(p); w.notifyBlockUpdate(p, s, s, 3); }
    }

    public int getRecipeProgress() { return recipeProgress; }
    public int getMaxProgress() { return currentProcessTime > 0 ? currentProcessTime : 200; }
    public boolean isActive() { return isActive; }
    public double getMaxPower() { return MAX_POWER; }

    /** Number of AE2 speed cards installed in the upgrade slots (4-7). */
    public int countSpeedCards() {
        int count = 0;
        for (int i = 4; i < SLOTS; i++) {
            ItemStack s = inventory.get(i);
            if (!s.isEmpty() && s.getItem() == ae2.core.definitions.AEItems.SPEED_CARD.item()) count++;
        }
        return count;
    }

    private int getSpeedMultiplier() {
        // 装陨石超频卡后加速卡不再提供额外加速
        return countOverloadCards() > 0 ? 1 : 1 << Math.min(4, countSpeedCards());
    }

    /** 陨石超频卡数量（升级槽 4..7，最多 2 张生效） */
    public int countOverloadCards() {
        int count = 0;
        for (int i = 4; i < SLOTS; i++) {
            if (ae2csl.util.UpgradeCardHelper.isOverloadCard(inventory.get(i))) count++;
        }
        return Math.min(2, count);
    }

    // ---- EnergyComponent.IPowerProvider ----
    @Override public double getStoredPower() { return getCurrentPower(); }
    @Override public double consumePower(double amount) {
        double available = getCurrentPower();
        double taken = Math.min(amount, available);
        setCurrentPower(available - taken);
        return taken;
    }
    @Override public double storePower(double amount) {
        double space = getMaxPower() - getCurrentPower();
        double added = Math.min(amount, space);
        setCurrentPower(getCurrentPower() + added);
        return added;
    }

    public MachineComponentContainer getMachineComponents() { return components; }


    /** 自动化能力：插入限输入槽 0..2，提取限输出槽 3 */
    @Override
    protected ae2csl.util.MachineItemHandler createAutomationItemHandler() {
        return new ae2csl.util.MachineItemHandler(this, slot -> slot == 3);
    }

    @Override public int getSizeInventory() { return SLOTS; }
    @Override public boolean isEmpty() { for (ItemStack s : inventory) if (!s.isEmpty()) return false; return true; }
    @Override public ItemStack getStackInSlot(int i) { return i >= 0 && i < SLOTS ? inventory.get(i) : ItemStack.EMPTY; }
    @Override public ItemStack decrStackSize(int i, int c) { ItemStack s = ItemStackHelper.getAndSplit(inventory, i, c); if (!s.isEmpty()) markDirty(); return s; }
    @Override public ItemStack removeStackFromSlot(int i) { ItemStack s = ItemStackHelper.getAndRemove(inventory, i); markDirty(); return s; }
    @Override public void setInventorySlotContents(int i, ItemStack s) { if (i >= 0 && i < SLOTS) { inventory.set(i, s); if (s.getCount() > 64) s.setCount(64); markDirty(); } }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUsableByPlayer(EntityPlayer p) { return world.getTileEntity(pos) == this && p.getDistanceSq(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) <= 64; }
    @Override public void openInventory(EntityPlayer p) {}
    @Override public void closeInventory(EntityPlayer p) {}

    /** 升级槽起始索引（记忆卡复制升级卡用） */
    @Override public int getUpgradeSlotStart() { return 4; }

    @Override public boolean isItemValidForSlot(int i, ItemStack s) {
        if (i >= 4) {
            return ae2csl.util.UpgradeCardHelper.canInsert(this, 4, SLOTS, i, s,
                net.minecraft.item.Item.getItemFromBlock(AE2CSBlocks.CRYSTAL_AGGREGATOR));
        }
        return i < 3;
    }

    private int clientEnergyPct;
    public int getClientEnergyPct() { return clientEnergyPct; }

    @Override public int getField(int id) { switch(id) { case 0: return recipeProgress; case 1: return MAX_POWER > 0 ? (int)(getCurrentPower() / MAX_POWER * 100) : 0; case 2: return currentProcessTime; default: return 0; } }
    @Override public void setField(int id, int v) { switch(id) { case 0: recipeProgress = v; break; case 1: clientEnergyPct = Math.min(100, Math.max(0, v)); break; case 2: currentProcessTime = v; break; } }
    @Override public int getFieldCount() { return 3; }
    @Override public void clear() { inventory.clear(); }
    @Override public String getName() { return "container.crystal_aggregator"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentTranslation(getName()); }
    public IInventory getInventory() { return this; }

    // ---- 侧面配置（自动输入/输出 + 按面能力过滤） ----
    private final ae2csl.machine.SideConfigData sideConfig = new ae2csl.machine.SideConfigData(this::markDirty);
    @Override public ae2csl.machine.SideConfigData getSideConfig() { return sideConfig; }
    @Override public int getMachineGuiId() { return ae2csl.client.gui.GuiHandler.GUI_CRYSTAL_AGGREGATOR; }

    @Override public void readFromNBT(NBTTagCompound c) { super.readFromNBT(c); recipeProgress = c.getInteger("recipeProgress"); currentProcessTime = c.getInteger("currentProcessTime"); isActive = c.getBoolean("isActive"); sideConfig.readFromNBT(c); inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY); ItemStackHelper.loadAllItems(c, inventory); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound c) { super.writeToNBT(c); c.setInteger("recipeProgress", recipeProgress); c.setInteger("currentProcessTime", currentProcessTime); c.setBoolean("isActive", isActive); sideConfig.writeToNBT(c); ItemStackHelper.saveAllItems(c, inventory); return c; }
}
