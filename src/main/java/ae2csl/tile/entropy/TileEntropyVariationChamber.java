/* AE2 Crystal Science - TileEntropyVariationChamber */
package ae2csl.tile.entropy;

import ae2.recipes.AERecipeTypes;
import ae2.recipes.entropy.EntropyMode;
import ae2.recipes.entropy.EntropyRecipe;
import ae2csl.machine.MachineComponentContainer;
import ae2csl.machine.component.GenericStackInvComponent;
import ae2csl.machine.component.EnergyComponent;
import ae2csl.machine.component.InvPort;
import ae2csl.tile.base.AENetworkedPoweredTile;
import ae2csl.init.AE2CSBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.List;

public class TileEntropyVariationChamber extends AENetworkedPoweredTile implements IInventory, EnergyComponent.IPowerProvider, ae2csl.machine.ISideConfigHost, ae2csl.machine.IUpgradeSlotsHost {
    private static final double MAX_POWER = 120000;
    private static final double ENERGY_PER_TICK = 200;
    private static final int RECIPE_ENERGY_COST = 1600;  // 200 AE/tick × 8 tick，对齐源码 activeRecipeEnergyCost
    private static final int TANK_CAPACITY = 8000;
    private static final int SLOTS = 6;          // 0=input item, 1=output item, 2..5=upgrades (speed cards)

    private int recipeProgress;          // 已支付的能量（AE），完成条件 >= RECIPE_ENERGY_COST（对齐 1.21.1 源码）
    private boolean isActive;
    private EntropyMode mode = EntropyMode.HEAT;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    // Input tank: holds water/lava; output tank: receives water.
    private final FluidTank inputTank = new FluidTank(TANK_CAPACITY) {
        @Override public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && (fluid.getFluid() == FluidRegistry.WATER || fluid.getFluid() == FluidRegistry.LAVA);
        }
    };
    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY);

    // Client-side render sync (updated each server tick, pushed via getField).
    private int syncEnergyPct;         // 0-100
    private int syncInputFluidAmount;  // 0-8000
    private int syncInputFluidType;    // 0 empty, 1 water, 2 lava
    private int syncOutputFluidAmount; // 0-8000

    private final MachineComponentContainer components = new MachineComponentContainer();
    private final GenericStackInvComponent invComponent = new GenericStackInvComponent();
    private final EnergyComponent energyComponent = new EnergyComponent(this);

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[]{
                inputTank.getTankProperties()[0], outputTank.getTankProperties()[0]
            };
        }
        @Override public int fill(FluidStack resource, boolean doFill) { return inputTank.fill(resource, doFill); }
        @Override public FluidStack drain(FluidStack resource, boolean doDrain) { return outputTank.drain(resource, doDrain); }
        @Override public FluidStack drain(int maxDrain, boolean doDrain) { return outputTank.drain(maxDrain, doDrain); }
    };

    public TileEntropyVariationChamber() { super(MAX_POWER);
        invComponent.addPort(InvPort.WORK, this);
        components.addInventory(invComponent);
        components.addService(EnergyComponent.class, energyComponent); }

    @Override protected ItemStack getMachineRepresentation() {
        return new ItemStack(AE2CSBlocks.ENTROPY_VARIATION_CHAMBER);
    }

    // 对齐 1.21.1 源码 EntropyVariationReactionChamberBlockEntity.serverTick：进度按能量累积、部分提取
    @Override public void update() {
        if (world == null || world.isRemote) return;
        // Charge internal buffer from the AE grid, then consume from it
        chargeFromGrid(MAX_POWER);
        checkActive(getCurrentPower() > 0);

        // 自动输入/输出（对齐 1.21.1 源码 SideConfigComponent）：输入槽 0，输出槽 1（不含流体）
        ae2csl.util.AutoTransferHelper.tick(world, pos, this, sideConfig, s -> s == 0, s -> s == 1);

        // Refresh fluid/energy render sync
        syncEnergyPct = (int)(getCurrentPower() / getMaxPower() * 100);
        FluidStack inFluid = inputTank.getFluid();
        syncInputFluidAmount = inFluid == null ? 0 : inFluid.amount;
        syncInputFluidType = fluidType(inFluid);
        FluidStack outFluid2 = outputTank.getFluid();
        syncOutputFluidAmount = outFluid2 == null ? 0 : outFluid2.amount;

        // Resolve input block state: block item first, then input tank fluid
        ItemStack inputItem = inventory.get(0);
        IBlockState blockState = getBlockStateFromItem(inputItem);
        boolean fromItem = blockState != null;
        if (blockState == null) {
            blockState = getBlockStateFromFluid(inputTank.getFluid());
        }

        EntropyRecipe recipe = blockState == null ? null : findRecipe(mode, blockState, inputTank.getFluid());
        ItemStack outItem = recipe == null ? ItemStack.EMPTY : getOutputItem(recipe, blockState);
        FluidStack outFluid = recipe == null ? null : getOutputFluid(recipe, blockState, inputTank.getFluid());

        boolean hasOutput = !outItem.isEmpty() || outFluid != null;
        if (blockState == null || recipe == null || !hasOutput) { recipeProgress = 0; return; }

        // 2) 若未完成：推进进度 + 扣能量。提取是"部分"的：电容/供能不足时按实扣量推进，
        //    永不死锁（2 张超频卡需求 > 电容时退化为每 tick 扣空电容）；断电保留进度
        if (recipeProgress < RECIPE_ENERGY_COST) {
            double needed = Math.min(getEnergyPerTick(), RECIPE_ENERGY_COST - recipeProgress);
            double drawn = Math.min(needed, getCurrentPower());
            if (drawn > 0) {
                setCurrentPower(getCurrentPower() - drawn);
                recipeProgress = Math.min(recipeProgress + (int) drawn, RECIPE_ENERGY_COST);
                markDirty();
            }
        }

        // 3) 已经完成：校验输出空间，消耗资源并产出
        if (recipeProgress >= RECIPE_ENERGY_COST) {
            ItemStack outputSlot = inventory.get(1);
            if (!outItem.isEmpty()) {
                boolean canOutput = outputSlot.isEmpty()
                    || (outputSlot.isItemEqual(outItem) && outputSlot.getCount() + outItem.getCount() <= outputSlot.getMaxStackSize());
                if (!canOutput) { recipeProgress = RECIPE_ENERGY_COST; return; } // 输出放不下：钳在满进度等待
            }
            if (outFluid != null && outputTank.fill(outFluid, false) != outFluid.amount) {
                recipeProgress = RECIPE_ENERGY_COST; return;
            }
            // Consume input
            if (fromItem) {
                inputItem.shrink(1);
                if (inputItem.isEmpty()) inventory.set(0, ItemStack.EMPTY);
            } else {
                inputTank.drain(Fluid.BUCKET_VOLUME, true);
            }
            // Produce output
            if (!outItem.isEmpty()) {
                if (outputSlot.isEmpty()) inventory.set(1, outItem.copy());
                else outputSlot.grow(outItem.getCount());
            }
            if (outFluid != null) {
                outputTank.fill(outFluid, true);
            }
            recipeProgress = 0;
            markDirty();
        }
    }

    /**
     * 每 tick 扣能上限（对齐 1.21.1 源码 EntropyVariationReactionChamberBlockEntity.getEnergyPerTick）。
     * 超频卡：上限提升至 总能耗/4（1 张）或 总能耗（2 张）——是上限而非硬性要求，
     * 供能不足时按实扣量降速；装超频卡后加速卡失效（见 getSpeedMultiplier）。
     */
    private double getEnergyPerTick() {
        double normal = ENERGY_PER_TICK * getSpeedMultiplier();
        int oc = countOverloadCards();
        if (oc == 0) return normal;
        int targetTicks = oc == 1 ? 4 : 1;
        return Math.max(normal, Math.ceil((double) RECIPE_ENERGY_COST / targetTicks));
    }

    private void checkActive(boolean a) { if (a != isActive) { isActive = a; notifyBlockUpdate(); } }

    // ---- Mode ----
    public void toggleMode() {
        mode = mode == EntropyMode.HEAT ? EntropyMode.COOL : EntropyMode.HEAT;
        recipeProgress = 0;
        markDirty();
    }
    public EntropyMode getMode() { return mode; }

    // ---- Input/output resolution ----

    // Convert a block item into its IBlockState (from metadata), null if not a block item.
    @Nullable private static IBlockState getBlockStateFromItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (item instanceof ItemBlock) {
            Block block = ((ItemBlock) item).getBlock();
            return block.getStateFromMeta(stack.getItemDamage());
        }
        return null;
    }

    // Map an input tank fluid to the block state the entropy recipes expect (source water/lava).
    @Nullable private static IBlockState getBlockStateFromFluid(@Nullable FluidStack fs) {
        if (fs == null) return null;
        Fluid fluid = fs.getFluid();
        if (fluid == FluidRegistry.WATER) return Blocks.WATER.getDefaultState();
        if (fluid == FluidRegistry.LAVA) return Blocks.LAVA.getDefaultState();
        return null;
    }

    // Find the entropy recipe matching the configured mode and block/fluid input.
    @Nullable private static EntropyRecipe findRecipe(EntropyMode mode, IBlockState blockState, @Nullable FluidStack fluidStack) {
        for (EntropyRecipe recipe : AERecipeTypes.ENTROPY.getRecipes()) {
            if (recipe.matches(mode, blockState, fluidStack)) {
                return recipe;
            }
        }
        return null;
    }

    // Block (non-fluid) output as an item, or the first drop.
    private static ItemStack getOutputItem(EntropyRecipe recipe, IBlockState blockState) {
        IBlockState outputState = recipe.getOutputBlockState(blockState);
        if (outputState != null && outputState.getBlock() != Blocks.AIR) {
            Block block = outputState.getBlock();
            if (FluidRegistry.lookupFluidForBlock(block) == null) {
                Item item = Item.getItemFromBlock(block);
                if (item != null) {
                    return new ItemStack(item, 1, block.getMetaFromState(outputState));
                }
            }
        }
        List<ItemStack> drops = recipe.getDrops();
        if (drops != null && !drops.isEmpty()) {
            return drops.get(0).copy();
        }
        return ItemStack.EMPTY;
    }

    // Fluid output (explicit fluid output, or a block output that is a fluid block).
    // Note: 1.12.2 FluidRegistry has only still WATER/LAVA fluids; flowing blocks map to the same fluid.
    @Nullable private static FluidStack getOutputFluid(EntropyRecipe recipe, IBlockState blockState, @Nullable FluidStack inputFluid) {
        FluidStack fs = recipe.getOutputFluidStack(inputFluid);
        if (fs != null) return fs;
        IBlockState outputState = recipe.getOutputBlockState(blockState);
        if (outputState != null && outputState.getBlock() != Blocks.AIR) {
            Fluid fluid = FluidRegistry.lookupFluidForBlock(outputState.getBlock());
            if (fluid != null) return new FluidStack(fluid, Fluid.BUCKET_VOLUME);
        }
        return null;
    }

    private static int fluidType(@Nullable FluidStack fs) {
        if (fs == null) return 0;
        if (fs.getFluid() == FluidRegistry.WATER) return 1;
        if (fs.getFluid() == FluidRegistry.LAVA) return 2;
        return 0;
    }

    // ---- Capabilities (fluids) ----
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }
    @SuppressWarnings("unchecked")
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) fluidHandler;
        }
        return super.getCapability(capability, facing);
    }

    private void notifyBlockUpdate() {
        World w = getWorld(); BlockPos p = getPos();
        if (w != null && !w.isRemote) { IBlockState s = w.getBlockState(p); w.notifyBlockUpdate(p, s, s, 3); }
    }

    /** 已支付能量（AE），完成条件为 >= RECIPE_ENERGY_COST */
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

    public MachineComponentContainer getMachineComponents() { return components; }


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
    @Override public int getUpgradeSlotStart() { return 2; }

    @Override public boolean isItemValidForSlot(int i, ItemStack s) {
        if (i >= 2) {
            return ae2csl.util.UpgradeCardHelper.canInsert(this, 2, SLOTS, i, s,
                net.minecraft.item.Item.getItemFromBlock(AE2CSBlocks.ENTROPY_VARIATION_CHAMBER));
        }
        return i == 0 && s.getItem() instanceof ItemBlock;
    }



    /** 自动化能力：插入限输入槽 0，提取限输出槽 1 */
    @Override
    protected ae2csl.util.MachineItemHandler createAutomationItemHandler() {
        return new ae2csl.util.MachineItemHandler(this, slot -> slot == 1);
    }

    /** Number of AE2 speed cards installed in the upgrade slots (2-5). */
    public int countSpeedCards() {
        int count = 0;
        for (int i = 2; i < SLOTS; i++) {
            ItemStack s = inventory.get(i);
            if (!s.isEmpty() && s.getItem() == ae2.core.definitions.AEItems.SPEED_CARD.item()) count++;
        }
        return count;
    }

    /** Number of overclock (overload) cards installed in the upgrade slots (max 2 effective). */
    public int countOverloadCards() {
        int count = 0;
        for (int i = 2; i < SLOTS; i++) {
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

    private int clientProgressPct;
    public int getClientProgressPct() { return clientProgressPct; }

    @Override public int getField(int id) {
        switch (id) {
            case 0: return (int) (recipeProgress * 100.0 / RECIPE_ENERGY_COST); // 同步百分比（GUI 进度条）
            case 1: return syncEnergyPct;
            case 2: return mode.ordinal();
            case 3: return syncInputFluidAmount;
            case 4: return syncInputFluidType;
            case 5: return syncOutputFluidAmount;
            default: return 0;
        }
    }
    @Override public void setField(int id, int v) {
        switch (id) {
            case 0: clientProgressPct = Math.min(100, Math.max(0, v)); break;
            case 1: syncEnergyPct = v; break;
            case 2: mode = EntropyMode.values()[v % 2]; break;
            case 3: syncInputFluidAmount = v; break;
            case 4: syncInputFluidType = v; break;
            case 5: syncOutputFluidAmount = v; break;
        }
    }
    @Override public int getFieldCount() { return 6; }
    @Override public void clear() { inventory.clear(); }
    @Override public String getName() { return "container.entropy_variation_chamber"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentTranslation(getName()); }
    public IInventory getInventory() { return this; }

    // ---- 侧面配置（自动输入/输出 + 按面能力过滤） ----
    private final ae2csl.machine.SideConfigData sideConfig = new ae2csl.machine.SideConfigData(this::markDirty);
    @Override public ae2csl.machine.SideConfigData getSideConfig() { return sideConfig; }
    @Override public int getMachineGuiId() { return ae2csl.client.gui.GuiHandler.GUI_ENTROPY_VARIATION_CHAMBER; }

    @Override public void readFromNBT(NBTTagCompound c) {
        super.readFromNBT(c);
        sideConfig.readFromNBT(c);
        recipeProgress = c.getInteger("recipeProgress");
        isActive = c.getBoolean("isActive");
        mode = EntropyMode.values()[c.getInteger("mode") % 2];
        inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(c, inventory);
        if (c.hasKey("inputTank")) inputTank.readFromNBT(c.getCompoundTag("inputTank"));
        if (c.hasKey("outputTank")) outputTank.readFromNBT(c.getCompoundTag("outputTank"));
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound c) {
        super.writeToNBT(c);
        sideConfig.writeToNBT(c);
        c.setInteger("recipeProgress", recipeProgress);
        c.setBoolean("isActive", isActive);
        c.setInteger("mode", mode.ordinal());
        ItemStackHelper.saveAllItems(c, inventory);
        c.setTag("inputTank", inputTank.writeToNBT(new NBTTagCompound()));
        c.setTag("outputTank", outputTank.writeToNBT(new NBTTagCompound()));
        return c;
    }
}
