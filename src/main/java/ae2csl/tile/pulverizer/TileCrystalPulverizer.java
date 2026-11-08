/*
 * AE2 Crystal Science - TileCrystalPulverizer
 * Vanilla IInventory with inline upgrade slots (5-8).
 * Speed cards in upgrade slots give 2^n multiplier (up to 16x with 4 cards).
 */
package ae2csl.tile.pulverizer;

import ae2.api.config.AccessRestriction;
import ae2csl.init.AE2CSBlocks;
import ae2csl.recipe.AE2CSRecipeRegistry;
import ae2csl.recipe.crystal_pulverizer.CrystalPulverizerRecipe;
import ae2csl.tile.base.AENetworkedPoweredTile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class TileCrystalPulverizer extends AENetworkedPoweredTile implements IInventory, ae2csl.machine.ISideConfigHost, ae2csl.machine.IUpgradeSlotsHost {

    private static final double MAX_POWER = 80000;
    private static final double BASE_ENERGY_PER_TICK = 200;
    private static final int SLOTS = 9;  // 0=input, 1..4=output, 5..8=upgrades

    private int recipeProgress;
    private boolean justCompleted;
    @Nullable private ResourceLocation activeRecipeId;
    private boolean isActive;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int clientProgressPct;
    private int clientEnergyPct;
    @Nullable private transient CrystalPulverizerRecipe activeRecipe;

    public TileCrystalPulverizer() { super(MAX_POWER); }

    @Override protected ItemStack getMachineRepresentation() { return new ItemStack(AE2CSBlocks.CRYSTAL_PULVERIZER); }

    @Override public AccessRestriction getPowerFlow() { return AccessRestriction.WRITE; }

    private int countSpeedCards() {
        int count = 0;
        for (int i = 5; i < SLOTS; i++) {
            ItemStack s = inventory.get(i);
            if (!s.isEmpty()) {
                ResourceLocation n = s.getItem().getRegistryName();
                if (n != null && "speed_card".equals(n.getPath())) count++;
            }
        }
        return count;
    }

    private int getSpeedMultiplier() {
        // 装陨石超频卡后加速卡不再提供额外加速
        return countOverloadCards() > 0 ? 1 : 1 << Math.min(4, countSpeedCards());
    }

    /** 陨石超频卡数量（升级槽 5..8，最多 2 张生效） */
    public int countOverloadCards() {
        int count = 0;
        for (int i = 5; i < SLOTS; i++) {
            if (ae2csl.util.UpgradeCardHelper.isOverloadCard(inventory.get(i))) count++;
        }
        return Math.min(2, count);
    }

    /**
     * 每 tick 能耗上限。超频卡：一次操作最多 4 刻（1 张）/ 1 刻（2 张）完成，
     * 能耗按目标刻数等比例提升，需要充足供能（对齐 1.21.1 源码 CrystalPulverizerBlockEntity）。
     */
    private double getEnergyPerTick(int activeEnergyCost) {
        double normal = BASE_ENERGY_PER_TICK * getSpeedMultiplier();
        int oc = countOverloadCards();
        if (oc == 0 || activeEnergyCost <= 0) {
            return normal;
        }
        int targetTicks = oc == 1 ? 4 : 1;
        return Math.max(normal, Math.ceil((double) activeEnergyCost / targetTicks));
    }

    @Override public void update() {
        if (world == null || world.isRemote) return;
        // Charge internal buffer from the AE grid, then consume from it
        chargeFromGrid(MAX_POWER);
        checkActive(getCurrentPower() > 0);

        // 自动输入/输出（对齐 1.21.1 源码 SideConfigComponent）：输入槽 0，输出槽 1..4
        ae2csl.util.AutoTransferHelper.tick(world, pos, this, sideConfig, s -> s == 0, s -> s >= 1 && s <= 4);

        if (justCompleted) { justCompleted = false; activeRecipe = null; activeRecipeId = null; recipeProgress = 0; }
        if (activeRecipe == null && activeRecipeId != null) {
            activeRecipe = findRecipeById(activeRecipeId);
            if (activeRecipe == null) { activeRecipeId = null; recipeProgress = 0; }
        }
        if (activeRecipe == null) {
            ItemStack input = inventory.get(0);
            for (CrystalPulverizerRecipe r : AE2CSRecipeRegistry.<CrystalPulverizerRecipe>getRecipes("crystal_pulverizer")) {
                if (r != null && r.matches(input)) { activeRecipe = r; activeRecipeId = getRecipeId(r); recipeProgress = 0; break; }
            }
        }
        if (activeRecipe == null) { recipeProgress = 0; return; }

        int energyCost = activeRecipe.getEnergyCost();
        if (recipeProgress < energyCost) {
            double needed = Math.min(getEnergyPerTick(energyCost), energyCost - recipeProgress);
            double available = getCurrentPower();
            double drawn = Math.min(needed, available);
            if (drawn <= 0) return;
            setCurrentPower(available - drawn);
            recipeProgress = Math.min(recipeProgress + (int) drawn, energyCost);
            markDirty();
        }
        if (recipeProgress >= energyCost) {
            ItemStack result = activeRecipe.getResult();
            ItemStack input = inventory.get(0);
            if (!activeRecipe.matches(input)) { activeRecipe = null; activeRecipeId = null; recipeProgress = 0; return; }
            boolean canOutput = false;
            for (int i = 1; i <= 4; i++) {
                ItemStack out = inventory.get(i);
                if (out.isEmpty() || (out.getCount() < out.getMaxStackSize() && out.isItemEqual(result))) { canOutput = true; break; }
            }
            if (!canOutput) { recipeProgress = energyCost; return; }
            input.shrink(1); if (input.isEmpty()) inventory.set(0, ItemStack.EMPTY);
            for (int i = 1; i <= 4; i++) {
                ItemStack out = inventory.get(i);
                if (out.isEmpty()) { inventory.set(i, result.copy()); break; }
                else if (out.isItemEqual(result) && out.getCount() < out.getMaxStackSize()) { out.grow(result.getCount()); break; }
            }
            justCompleted = true; markDirty();
        }
    }

    private void checkActive(boolean a) { if (a != isActive) { isActive = a; notifyBlockUpdate(); } }
    private void notifyBlockUpdate() { World w = getWorld(); BlockPos p = getPos(); if (w != null && !w.isRemote) { IBlockState s = w.getBlockState(p); w.notifyBlockUpdate(p, s, s, 3); } }

    @Nullable private CrystalPulverizerRecipe findRecipeById(@Nullable ResourceLocation id) {
        if (id == null) return null;
        for (CrystalPulverizerRecipe r : AE2CSRecipeRegistry.<CrystalPulverizerRecipe>getRecipes("crystal_pulverizer"))
            if (r != null && id.equals(getRecipeId(r))) return r;
        return null;
    }
    private static ResourceLocation getRecipeId(CrystalPulverizerRecipe r) { return r.getResult().getItem().getRegistryName(); }

    public int getClientProgressPct() { return clientProgressPct; }
    public int getClientEnergyPct() { return clientEnergyPct; }

    /** 自动化能力：插入限输入槽 0（isItemValidForSlot 约束），提取限输出槽 1..4 */
    @Override
    protected ae2csl.util.MachineItemHandler createAutomationItemHandler() {
        return new ae2csl.util.MachineItemHandler(this, slot -> slot >= 1 && slot <= 4);
    }

    @Override public int getSizeInventory() { return SLOTS; }
    @Override public boolean isEmpty() { for (ItemStack s : inventory) if (!s.isEmpty()) return false; return true; }
    @Override public ItemStack getStackInSlot(int i) { return i >= 0 && i < SLOTS ? inventory.get(i) : ItemStack.EMPTY; }
    @Override public ItemStack decrStackSize(int i, int c) { ItemStack s = ItemStackHelper.getAndSplit(inventory, i, c); if (!s.isEmpty()) markDirty(); return s; }
    @Override public ItemStack removeStackFromSlot(int i) { ItemStack s = ItemStackHelper.getAndRemove(inventory, i); markDirty(); return s; }
    @Override public void setInventorySlotContents(int i, ItemStack s) { if (i >= 0 && i < SLOTS) { inventory.set(i, s); if (s.getCount() > s.getMaxStackSize()) s.setCount(s.getMaxStackSize()); markDirty(); } }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUsableByPlayer(EntityPlayer p) { return world.getTileEntity(pos) == this && p.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64; }
    @Override public void openInventory(EntityPlayer p) {}
    @Override public void closeInventory(EntityPlayer p) {}

    /** 升级槽起始索引（记忆卡复制升级卡用） */
    @Override public int getUpgradeSlotStart() { return 5; }

    @Override public boolean isItemValidForSlot(int i, ItemStack s) {
        if (i >= 5) {
            return ae2csl.util.UpgradeCardHelper.canInsert(this, 5, SLOTS, i, s,
                net.minecraft.item.Item.getItemFromBlock(AE2CSBlocks.CRYSTAL_PULVERIZER));
        }
        return i == 0;
    }

    @Override public int getField(int id) {
        switch (id) {
            case 0: if (justCompleted) return 100; if (activeRecipe != null && activeRecipe.getEnergyCost() > 0) return (int)((double)recipeProgress / activeRecipe.getEnergyCost() * 100); return 0;
            case 1: return MAX_POWER > 0 ? (int)(getCurrentPower() / MAX_POWER * 100) : 0;
            default: return 0;
        }
    }
    @Override public void setField(int id, int v) { if (id == 0) clientProgressPct = Math.min(100, Math.max(0, v)); else if (id == 1) clientEnergyPct = Math.min(100, Math.max(0, v)); }
    @Override public int getFieldCount() { return 2; }
    @Override public void clear() { inventory.clear(); }
    @Override public String getName() { return "container.crystal_pulverizer"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentTranslation(getName()); }

    // ---- 侧面配置（自动输入/输出 + 按面能力过滤） ----
    private final ae2csl.machine.SideConfigData sideConfig = new ae2csl.machine.SideConfigData(this::markDirty);
    @Override public ae2csl.machine.SideConfigData getSideConfig() { return sideConfig; }
    @Override public int getMachineGuiId() { return ae2csl.client.gui.GuiHandler.GUI_CRYSTAL_PULVERIZER; }

    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        sideConfig.readFromNBT(tag);
        recipeProgress = tag.getInteger("recipe_progress"); justCompleted = tag.getBoolean("just_completed"); isActive = tag.getBoolean("is_active");
        if (tag.hasKey("active_recipe")) activeRecipeId = new ResourceLocation(tag.getString("active_recipe"));
        inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY); ItemStackHelper.loadAllItems(tag, inventory);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        sideConfig.writeToNBT(tag);
        tag.setInteger("recipe_progress", recipeProgress); tag.setBoolean("just_completed", justCompleted); tag.setBoolean("is_active", isActive);
        if (activeRecipeId != null) tag.setString("active_recipe", activeRecipeId.toString());
        ItemStackHelper.saveAllItems(tag, inventory);
        return tag;
    }
}
