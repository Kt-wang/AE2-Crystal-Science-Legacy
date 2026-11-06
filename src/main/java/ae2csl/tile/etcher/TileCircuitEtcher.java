/* AE2 Crystal Science - TileCircuitEtcher */
package ae2csl.tile.etcher;

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

public class TileCircuitEtcher extends AENetworkedPoweredTile implements IInventory, EnergyComponent.IPowerProvider, ae2csl.machine.ISideConfigHost {
    private static final double MAX_POWER = 80000;
    private static final double ENERGY_PER_TICK = 200;
    private static final int SLOTS = 8; // 0..2 = input, 3 = output, 4..7 = upgrades (speed cards)

    private int recipeProgress;          // 已支付的能量（AE），完成条件 >= activeRecipeEnergyCost（对齐 1.21.1 源码）
    private int activeRecipeEnergyCost;  // 活动配方的总能耗（GUI 百分比进度基数）
    private boolean isActive;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    private final MachineComponentContainer components = new MachineComponentContainer();
    private final GenericStackInvComponent invComponent = new GenericStackInvComponent();
    private final EnergyComponent energyComponent = new EnergyComponent(this);

    public TileCircuitEtcher() { super(MAX_POWER);
        invComponent.addPort(InvPort.WORK, this);
        components.addInventory(invComponent);
        components.addService(EnergyComponent.class, energyComponent); }

    @Override protected ItemStack getMachineRepresentation() {
        return new ItemStack(AE2CSBlocks.CIRCUIT_ETCHER);
    }

    // 对齐 1.21.1 源码 CircuitEtcherBlockEntity.serverTick：进度按能量累积、部分提取、供能决定速度
        @Override public void update() {
        if (world == null || world.isRemote) return;

        // Charge internal buffer from the AE grid
        chargeFromGrid(MAX_POWER);
        checkActive(getCurrentPower() > 0);

        // 自动输入/输出（对齐 1.21.1 源码 SideConfigComponent）：输入槽 0..2，输出槽 3
        ae2csl.util.AutoTransferHelper.tick(world, pos, this, sideConfig, s -> s < 3, s -> s == 3);

        // 1) 更新/确认活动配方
        ae2csl.recipe.input.ThreeItemStackRecipeInput input = new ae2csl.recipe.input.ThreeItemStackRecipeInput(inventory.get(0), inventory.get(1), inventory.get(2));
        ae2csl.recipe.circuit_etcher.CircuitEtcherRecipe recipe = null;
        for (Object obj : ae2csl.recipe.AE2CSRecipeRegistry.getRecipes("circuit_etcher")) {
            ae2csl.recipe.circuit_etcher.CircuitEtcherRecipe r = (ae2csl.recipe.circuit_etcher.CircuitEtcherRecipe) obj;
            if (r.matches(input)) { recipe = r; break; }
        }
        if (recipe == null) { recipeProgress = 0; activeRecipeEnergyCost = 0; return; }
        activeRecipeEnergyCost = Math.max(1, recipe.getEnergyCost());

        // 2) 若未完成：推进进度 + 扣能量。提取是"部分"的：电容/供能不足时按实扣量推进，
        //    永不死锁（2 张超频卡需求 > 电容时退化为每 tick 扣空电容）
        if (recipeProgress < activeRecipeEnergyCost) {
            double needed = Math.min(getEnergyPerTick(activeRecipeEnergyCost), activeRecipeEnergyCost - recipeProgress);
            double drawn = Math.min(needed, getCurrentPower());
            if (drawn > 0) {
                setCurrentPower(getCurrentPower() - drawn);
                recipeProgress = Math.min(recipeProgress + (int) drawn, activeRecipeEnergyCost);
                markDirty();
            }
        }

        // 3) 已经完成：消耗资源并产出
        if (recipeProgress >= activeRecipeEnergyCost) {
            ItemStack result = recipe.assemble(input);
            if (result.isEmpty()) { recipeProgress = 0; return; } // 配方异常：清空状态
            ItemStack output = inventory.get(3);
            boolean canOutput = output.isEmpty()
                || (output.isItemEqual(result)
                    && output.getCount() + result.getCount() <= output.getMaxStackSize());
            if (!canOutput) { recipeProgress = activeRecipeEnergyCost; return; } // 输出放不下：钳在满进度等待
            recipe.consumeInputs(input);
            if (output.isEmpty()) inventory.set(3, result);
            else output.grow(result.getCount());
            recipeProgress = 0;
            markDirty();
        }
    }

    /**
     * 每 tick 扣能上限（对齐 1.21.1 源码 CircuitEtcherBlockEntity.getEnergyPerTick）。
     * 超频卡：上限提升至 配方总能耗/4（1 张）或 配方总能耗（2 张）——是上限而非硬性要求，
     * 供能不足时按实扣量降速；装超频卡后加速卡失效（见 getSpeedMultiplier）。
     */
    private double getEnergyPerTick(int activeEnergyCost) {
        double normal = ENERGY_PER_TICK * getSpeedMultiplier();
        int oc = countOverloadCards();
        if (oc == 0 || activeEnergyCost <= 0) return normal;
        int targetTicks = oc == 1 ? 4 : 1;
        return Math.max(normal, Math.ceil((double) activeEnergyCost / targetTicks));
    }

    private void checkActive(boolean a) { if (a != isActive) { isActive = a; notifyBlockUpdate(); } }

    
    private void notifyBlockUpdate() {
        World w = getWorld(); BlockPos p = getPos();
        if (w != null && !w.isRemote) {
            IBlockState state = w.getBlockState(p);
            IBlockState newState = state.withProperty(ae2csl.block.etcher.CircuitEtcherBlock.ACTIVE, isActive);
            w.notifyBlockUpdate(p, state, newState, 3);
        }
    }

    /** 已支付能量（AE），完成条件为 >= 活动配方总能耗 */
    public int getRecipeProgress() { return recipeProgress; }
    public boolean isActive() { return isActive; }
    public double getMaxPower() { return MAX_POWER; }

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

    /** Number of AE2 speed cards installed in the upgrade slots (4-7). */
    public int countSpeedCards() {
        int count = 0;
        for (int i = 4; i < SLOTS; i++) {
            ItemStack s = inventory.get(i);
            if (!s.isEmpty() && s.getItem() == ae2.core.definitions.AEItems.SPEED_CARD.item()) count++;
        }
        return count;
    }

    /** Number of overclock (overload) cards installed in the upgrade slots (max 2 effective). */
    public int countOverloadCards() {
        int count = 0;
        for (int i = 4; i < SLOTS; i++) {
            ItemStack s = inventory.get(i);
            if (!s.isEmpty() && s.getItem().getRegistryName() != null
                    && "overload_card".equals(s.getItem().getRegistryName().getPath())) count++;
        }
        return Math.min(2, count);
    }

    private int getSpeedMultiplier() {
        // 装陨石超频卡后加速卡不再提供额外加速（对齐源码 onUpgradesChanged）
        return countOverloadCards() > 0 ? 1 : 1 << Math.min(4, countSpeedCards());
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
    @Override public boolean isItemValidForSlot(int i, ItemStack s) {
        if (i >= 4) {
            // 超频卡最多 2 张（文档标注上限），加速卡不限
            if (ae2csl.util.UpgradeCardHelper.isOverloadCard(s)) return canAcceptOverloadCard(i);
            return isUpgradeCard(s);
        }
        return i < 3;
    }

    /** 已有 2 张超频卡（不含本槽）时拒绝继续插入 */
    private boolean canAcceptOverloadCard(int slot) {
        int installed = 0;
        for (int j = 4; j < SLOTS; j++) {
            if (j != slot && ae2csl.util.UpgradeCardHelper.isOverloadCard(inventory.get(j))) installed++;
        }
        return installed < 2;
    }

    private static boolean isUpgradeCard(ItemStack s) {
        if (s.getItem() == ae2.core.definitions.AEItems.SPEED_CARD.item()) return true;
        return s.getItem().getRegistryName() != null
            && "overload_card".equals(s.getItem().getRegistryName().getPath());
    }
    private int clientProgressPct;
    private int clientEnergyPct;
    public int getClientProgressPct() { return clientProgressPct; }
    public int getClientEnergyPct() { return clientEnergyPct; }

    // 进度按能量累积（最大 57600 AE），超过 SPacketWindowProperty 的 short 上限，故同步百分比
    @Override public int getField(int id) { switch(id) { case 0: return activeRecipeEnergyCost > 0 ? (int)(recipeProgress * 100.0 / activeRecipeEnergyCost) : 0; case 1: return MAX_POWER > 0 ? (int)(getCurrentPower() / MAX_POWER * 100) : 0; default: return 0; } }
    @Override public void setField(int id, int v) { switch(id) { case 0: clientProgressPct = Math.min(100, Math.max(0, v)); break; case 1: clientEnergyPct = Math.min(100, Math.max(0, v)); break; } }
    @Override public int getFieldCount() { return 2; }
    @Override public void clear() { inventory.clear(); }
    @Override public String getName() { return "container.circuit_etcher"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentTranslation(getName()); }
    public IInventory getInventory() { return this; }

    // ---- 侧面配置（自动输入/输出 + 按面能力过滤） ----
    private final ae2csl.machine.SideConfigData sideConfig = new ae2csl.machine.SideConfigData(this::markDirty);
    @Override public ae2csl.machine.SideConfigData getSideConfig() { return sideConfig; }
    @Override public int getMachineGuiId() { return ae2csl.client.gui.GuiHandler.GUI_CIRCUIT_ETCHER; }

    @Override public void readFromNBT(NBTTagCompound c) { super.readFromNBT(c); recipeProgress = c.getInteger("recipeProgress"); activeRecipeEnergyCost = c.getInteger("activeRecipeEnergyCost"); isActive = c.getBoolean("isActive"); sideConfig.readFromNBT(c); inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY); ItemStackHelper.loadAllItems(c, inventory); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound c) { super.writeToNBT(c); c.setInteger("recipeProgress", recipeProgress); c.setInteger("activeRecipeEnergyCost", activeRecipeEnergyCost); c.setBoolean("isActive", isActive); sideConfig.writeToNBT(c); ItemStackHelper.saveAllItems(c, inventory); return c; }
}
