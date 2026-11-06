/*
 * AE2 Crystal Science - TileQuartzGrindstone
 * Hand-cranked grinder. Powered by AE2's wooden crank (ICrankable).
 * Recipes use CrystalPulverizerRecipe, same as Crystal Pulverizer but slower/cheaper.
 * 7 slots: 0..2=input, 3=work, 4..6=output.
 */
package ae2csl.tile.grindstone;

import ae2.api.config.AccessRestriction;
import ae2.api.implementations.blockentities.ICrankable;
import ae2.api.AECapabilities;
import ae2.api.config.Actionable;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class TileQuartzGrindstone extends AENetworkedPoweredTile implements IInventory, ICrankable, ae2csl.machine.ISideConfigHost {

    private static final double MAX_POWER = 10000;          // matches source
    private static final double BASE_ENERGY_PER_TICK = 200;  // matches source
    private static final double CRANK_POWER = 1600;          // AE per crank turn
    private static final int SLOTS = 7;  // 0..2=input, 3=work, 4..6=output

    private int recipeProgress;
    private int activeRecipeEnergyCost;
    private boolean isActive;
    private boolean needRefreshRecipe = true;
    @Nullable private ResourceLocation activeRecipeId;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public TileQuartzGrindstone() { super(MAX_POWER); }

    @Override protected ItemStack getMachineRepresentation() { return new ItemStack(AE2CSBlocks.QUARTZ_GRINDSTONE); }
    @Override public AccessRestriction getPowerFlow() { return AccessRestriction.WRITE; }

    // ---- ICrankable ----
    @Override public boolean canTurn() { return getCurrentPower() < MAX_POWER; }

    @Override public void applyTurn() {
        injectAEPower(CRANK_POWER, Actionable.MODULATE);
        markDirty();
    }

    // ---- Ticking ----
    @Override public void update() {
        if (world == null || world.isRemote) return;

        // 自动输入/输出（对齐 1.21.1 源码 SideConfigComponent）：输入/工作槽 0..3，输出槽 4..6
        ae2csl.util.AutoTransferHelper.tick(world, pos, this, sideConfig, s -> s < 4, s -> s >= 4);

        // Push items from input to work slot (matches source pushRawsIntoWork)
        if (inventory.get(3).isEmpty()) {
            for (int i = 0; i < 3; i++) {
                ItemStack s = inventory.get(i);
                if (!s.isEmpty()) {
                    inventory.set(3, s.copy());
                    inventory.set(i, ItemStack.EMPTY);
                    needRefreshRecipe = true;
                    break;
                }
            }
        }
        if (inventory.get(3).isEmpty()) {
            recipeProgress = 0; isActive = false; return;
        }

        // Find recipe for work slot contents
        CrystalPulverizerRecipe activeRecipe = null;
        for (CrystalPulverizerRecipe r : AE2CSRecipeRegistry.<CrystalPulverizerRecipe>getRecipes("quartz_grindstone")) {
            if (r != null && r.matches(inventory.get(3))) { activeRecipe = r; break; }
        }
        if (activeRecipe == null) { recipeProgress = 0; isActive = false; return; }

        activeRecipeEnergyCost = activeRecipe.getEnergyCost();
        isActive = true;

        // Advance progress with energy
        if (recipeProgress < activeRecipeEnergyCost) {
            double needed = Math.min(BASE_ENERGY_PER_TICK, activeRecipeEnergyCost - recipeProgress);
            double drawn = tryDrawPower(needed);
            if (drawn <= 0) return;
            recipeProgress = Math.min(recipeProgress + (int) drawn, activeRecipeEnergyCost);
            markDirty();
        }

        // Complete: consume input, produce output
        if (recipeProgress >= activeRecipeEnergyCost) {
            if (!activeRecipe.matches(inventory.get(3))) {
                recipeProgress = 0; activeRecipeEnergyCost = 0; return;
            }
            ItemStack result = activeRecipe.getResult();
            // Check output space
            boolean canOutput = false;
            for (int i = 4; i < 7; i++) {
                ItemStack out = inventory.get(i);
                if (out.isEmpty() || (out.isItemEqual(result) && out.getCount() + result.getCount() <= out.getMaxStackSize())) {
                    canOutput = true; break;
                }
            }
            if (!canOutput) { recipeProgress = activeRecipeEnergyCost; return; }

            inventory.get(3).shrink(1);
            if (inventory.get(3).isEmpty()) inventory.set(3, ItemStack.EMPTY);
            for (int i = 4; i < 7; i++) {
                ItemStack out = inventory.get(i);
                if (out.isEmpty()) { inventory.set(i, result.copy()); break; }
                else if (out.isItemEqual(result) && out.getCount() + result.getCount() <= out.getMaxStackSize()) {
                    out.grow(result.getCount()); break;
                }
            }
            recipeProgress = 0; activeRecipeEnergyCost = 0;
            needRefreshRecipe = true;
            markDirty();
        }

        boolean newActive = (activeRecipeEnergyCost > 0 && recipeProgress > 0) || !inventory.get(3).isEmpty();
        if (newActive != isActive) { isActive = newActive; notifyBlockUpdate(); }
    }

    /** 自动化能力：插入限输入槽 0..2，提取限输出槽 4..6（不抽工作槽 3） */
    @Override
    protected ae2csl.util.MachineItemHandler createAutomationItemHandler() {
        return new ae2csl.util.MachineItemHandler(this, slot -> slot >= 4);
    }

    private void notifyBlockUpdate() {
        World w = getWorld(); BlockPos p = getPos();
        if (w != null && !w.isRemote) { IBlockState s = w.getBlockState(p); w.notifyBlockUpdate(p, s, s, 3); }
    }

    public int getRecipeProgress() { return recipeProgress; }
    public int getMaxProgress() { return activeRecipeEnergyCost > 0 ? activeRecipeEnergyCost : 6000; }
    public boolean isActive() { return isActive; }

    // ---- IInventory ----
    @Override public int getSizeInventory() { return SLOTS; }
    @Override public boolean isEmpty() { for (ItemStack s : inventory) if (!s.isEmpty()) return false; return true; }
    @Override public ItemStack getStackInSlot(int i) { return i >= 0 && i < SLOTS ? inventory.get(i) : ItemStack.EMPTY; }
    @Override public ItemStack decrStackSize(int i, int c) { ItemStack s = ItemStackHelper.getAndSplit(inventory, i, c); if (!s.isEmpty()) markDirty(); return s; }
    @Override public ItemStack removeStackFromSlot(int i) { ItemStack s = ItemStackHelper.getAndRemove(inventory, i); markDirty(); return s; }
    @Override public void setInventorySlotContents(int i, ItemStack s) { if (i >= 0 && i < SLOTS) { inventory.set(i, s); if (s.getCount() > 64) s.setCount(64); needRefreshRecipe = (i == 3 || i < 3); markDirty(); } }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUsableByPlayer(EntityPlayer p) { return world.getTileEntity(pos) == this && p.getDistanceSq(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) <= 64; }
    @Override public void openInventory(EntityPlayer p) {}
    @Override public void closeInventory(EntityPlayer p) {}
    @Override public boolean isItemValidForSlot(int i, ItemStack s) { return i < 4; }
    @Override public int getField(int id) { switch(id) { case 0: return recipeProgress; case 1: return (int)getCurrentPower(); default: return 0; } }
    @Override public void setField(int id, int v) { switch(id) { case 0: recipeProgress = v; break; case 1: setCurrentPower(v); break; } }
    @Override public int getFieldCount() { return 2; }
    @Override public void clear() { inventory.clear(); }
    @Override public String getName() { return "container.quartz_grindstone"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentTranslation(getName()); }

    // ---- Capabilities (expose CRANKABLE for AE2 wooden crank) ----
    @Override public boolean hasCapability(Capability<?> cap, @Nullable EnumFacing facing) {
        if (cap == AECapabilities.CRANKABLE) return true;
        return super.hasCapability(cap, facing);
    }
    @SuppressWarnings("unchecked")
    @Nullable @Override public <T> T getCapability(Capability<T> cap, @Nullable EnumFacing facing) {
        if (cap == AECapabilities.CRANKABLE) return (T) this;
        return super.getCapability(cap, facing);
    }

    // ---- 侧面配置（自动输入/输出 + 按面能力过滤） ----
    private final ae2csl.machine.SideConfigData sideConfig = new ae2csl.machine.SideConfigData(this::markDirty);
    @Override public ae2csl.machine.SideConfigData getSideConfig() { return sideConfig; }
    @Override public int getMachineGuiId() { return ae2csl.client.gui.GuiHandler.GUI_QUARTZ_GRINDSTONE; }

    @Override public void readFromNBT(NBTTagCompound c) { super.readFromNBT(c); sideConfig.readFromNBT(c); recipeProgress = c.getInteger("recipeProgress"); activeRecipeEnergyCost = c.getInteger("activeRecipeEnergyCost"); isActive = c.getBoolean("isActive"); if (c.hasKey("activeRecipeId")) activeRecipeId = new ResourceLocation(c.getString("activeRecipeId")); inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY); ItemStackHelper.loadAllItems(c, inventory); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound c) { super.writeToNBT(c); sideConfig.writeToNBT(c); c.setInteger("recipeProgress", recipeProgress); c.setInteger("activeRecipeEnergyCost", activeRecipeEnergyCost); c.setBoolean("isActive", isActive); if (activeRecipeId != null) c.setString("activeRecipeId", activeRecipeId.toString()); ItemStackHelper.saveAllItems(c, inventory); return c; }
}
