/*
 * AE2 Crystal Science - TileCrystalGrowthChamber
 * Updated: 54 slots (6×9 grid) matching source, seed growth logic
 */
package ae2csl.tile.crystal;

import ae2.api.config.AccessRestriction;
import ae2csl.tile.base.AENetworkedPoweredTile;
import ae2csl.init.AE2CSBlocks;
import ae2csl.item.crystal.CrystalSeedItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class TileCrystalGrowthChamber extends AENetworkedPoweredTile implements IInventory, ae2csl.machine.ISideConfigHost, ae2csl.machine.IUpgradeSlotsHost {

    private static final double MAX_POWER = 40000;
    private static final double ENERGY_PER_TICK = 8;
    private static final int SLOTS = 58; // 0..53 = seeds (6x9), 54..57 = upgrades

    private int recipeProgress;
    private boolean isActive;
    /** 四周等效的「晶体催生加速器」数量（对齐源码 growthNum） */
    private int growthAccelerators;
    private int workTickCountDown; // 操作间隔计数（默认 10 刻一轮，超频卡缩短）
    private NonNullList<ItemStack> inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public TileCrystalGrowthChamber() { super(MAX_POWER); }

    @Override public AccessRestriction getPowerFlow() { return AccessRestriction.WRITE; }

    @Override protected ItemStack getMachineRepresentation() {
        return new ItemStack(AE2CSBlocks.CRYSTAL_GROWTH_CHAMBER);
    }

    // ---- Ticking ----
    @Override public void update() {
        if (world == null || world.isRemote) return;
        // Charge internal buffer from the AE grid, then consume from it
        chargeFromGrid(MAX_POWER);

        // 自动输入/输出（对齐 1.21.1 源码 SideConfigComponent）：种子槽 0..53；已长成的晶体（非种子）才可被推出
        ae2csl.util.AutoTransferHelper.tick(world, pos, this, sideConfig,
            s -> s < 54,
            s -> s < 54 && !(inventory.get(s).getItem() instanceof CrystalSeedItem));

        // 相邻设备装了晶体生长卡就等同于一台晶体催生加速器（对齐源码 updateGrowthNum）
        if (world.rand.nextFloat() < 0.1F) {
            updateGrowthAccelerators();
        }

        // 陨石超频卡：一轮操作的间隔 10 刻 → 1 张 4 刻 / 2 张 1 刻；装卡后加速卡失效
        int oc = countOverloadCards();
        int speedCards = oc > 0 ? 0 : getSpeedCardCount();
        int interval = oc == 1 ? 4 : oc >= 2 ? 1 : 10;

        if (--workTickCountDown > 0) return;

        // 每轮工作量按 10 刻基础批量化（100×(1+加速卡)），吞吐与原逐刻模型一致；
        // 超频卡靠缩短间隔提速（1 张 2.5×、2 张 10×），能耗同比例放大，需充足供能
        double energyCost = ENERGY_PER_TICK * (1 + speedCards) * interval;
        if (getCurrentPower() < energyCost) {
            workTickCountDown = 1; // 供能不足，下一刻重试
            return;
        }
        // 与源码 growthTick = base + speedCards + growthNum 对齐（再乘批量化系数）
        int growthAmount = 10 * (1 + speedCards + growthAccelerators) * 10;

        boolean didGrow = false;
        for (int i = 0; i < 54; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof CrystalSeedItem)) continue;
            CrystalSeedItem seed = (CrystalSeedItem) stack.getItem();
            int ticks = seed.getGrowTicks(stack) + growthAmount;
            if (ticks >= seed.getOvergrowTick(stack)) {
                inventory.set(i, new ItemStack(seed.getGrowTo(), stack.getCount()));
            } else {
                seed.setGrowTicks(stack, ticks);
            }
            didGrow = true;
        }
        if (didGrow) {
            setCurrentPower(getCurrentPower() - energyCost);
            markDirty();
        }

        if (didGrow != isActive) { isActive = didGrow; notifyBlockUpdate(); }

        workTickCountDown = interval;
    }

    /** 陨石超频卡数量（升级槽 54..57，最多 2 张生效） */
    public int countOverloadCards() {
        int count = 0;
        for (int i = 54; i < SLOTS; i++) {
            if (ae2csl.util.UpgradeCardHelper.isOverloadCard(inventory.get(i))) count++;
        }
        return Math.min(2, count);
    }

    /** Number of AE2 speed cards installed in the upgrade slots (54-57). */
    public int getSpeedCardCount() {
        int n = 0;
        for (int i = 54; i < SLOTS; i++) {
            ItemStack s = inventory.get(i);
            if (!s.isEmpty() && s.getItem() == ae2.core.definitions.AEItems.SPEED_CARD.item()) n++;
        }
        return n;
    }

    private void notifyBlockUpdate() {
        World w = getWorld(); BlockPos p = getPos();
        if (w != null && !w.isRemote) { IBlockState s = w.getBlockState(p); w.notifyBlockUpdate(p, s, s, 3); }
    }

    public boolean isActive() { return isActive; }
    public double getMaxPower() { return MAX_POWER; }
    public int getRecipeProgress() { return recipeProgress; }
    public int getMaxProgress() { return 100; }

    // ---- IInventory ----
    /** 自动化能力：插入限种子/晶体（isItemValidForSlot 约束），提取限 0..53（种子原地长成晶体） */
    @Override
    protected ae2csl.util.MachineItemHandler createAutomationItemHandler() {
        return new ae2csl.util.MachineItemHandler(this, slot -> slot < 54);
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
    @Override public int getUpgradeSlotStart() { return 54; }

    @Override public boolean isItemValidForSlot(int i, ItemStack s) {
        if (i >= 54) {
            return ae2csl.util.UpgradeCardHelper.canInsert(this, 54, SLOTS, i, s,
                net.minecraft.item.Item.getItemFromBlock(AE2CSBlocks.CRYSTAL_GROWTH_CHAMBER));
        }
        return s.getItem() instanceof CrystalSeedItem || s.getItem() instanceof ae2csl.item.crystal.PureCrystalItem;
    }

    /**
     * 统计四周的催生加速器数量：AE2 晶体催生加速器，以及任何装了晶体生长卡的相邻设备/部件。
     * 对齐 1.21.1 源码 CrystalGrowthChamberBlockEntity#updateGrowthNum。
     */
    private void updateGrowthAccelerators() {
        if (world == null || world.isRemote) {
            return;
        }
        Item growthCard = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
            ae2csl.Tags.MOD_ID, "crystal_growth_card"));
        if (growthCard == null) {
            return;
        }

        int count = 0;
        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos neighbour = pos.offset(side);
            TileEntity te = world.getTileEntity(neighbour);
            if (te == null) {
                continue;
            }
            if (te instanceof ae2.tile.misc.TileGrowthAccelerator) {
                count++;
                continue;
            }
            // 相邻方块（机器 / 接口 / 样板供应器）
            if (te instanceof ae2.api.upgrades.IUpgradeableObject upgradeable
                && upgradeable.isUpgradedWith(growthCard)) {
                count++;
                continue;
            }
            // 相邻线缆上的部件
            if (te instanceof ae2.tile.networking.TileCableBus cableBus) {
                ae2.api.parts.IPart part = cableBus.getPart(side.getOpposite());
                if (part instanceof ae2.api.upgrades.IUpgradeableObject upgradeablePart
                    && upgradeablePart.isUpgradedWith(growthCard)) {
                    count++;
                }
            }
        }
        this.growthAccelerators = count;
    }

    @Override public int getField(int id) { switch(id) { case 0: return recipeProgress; case 1: return (int)getCurrentPower(); default: return 0; } }
    @Override public void setField(int id, int v) { switch(id) { case 0: recipeProgress = v; break; case 1: setCurrentPower(v); break; } }
    @Override public int getFieldCount() { return 2; }
    @Override public void clear() { inventory.clear(); }
    @Override public String getName() { return "container.crystal_growth_chamber"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentTranslation(getName()); }

    // ---- 侧面配置（自动输入/输出 + 按面能力过滤） ----
    private final ae2csl.machine.SideConfigData sideConfig = new ae2csl.machine.SideConfigData(this::markDirty);
    @Override public ae2csl.machine.SideConfigData getSideConfig() { return sideConfig; }
    @Override public int getMachineGuiId() { return ae2csl.client.gui.GuiHandler.GUI_CRYSTAL_GROWTH_CHAMBER; }

    @Override public void readFromNBT(NBTTagCompound c) { super.readFromNBT(c); recipeProgress = c.getInteger("recipeProgress"); isActive = c.getBoolean("isActive"); workTickCountDown = c.getInteger("workTickCountDown"); sideConfig.readFromNBT(c); inventory = NonNullList.withSize(SLOTS, ItemStack.EMPTY); ItemStackHelper.loadAllItems(c, inventory); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound c) { super.writeToNBT(c); c.setInteger("recipeProgress", recipeProgress); c.setBoolean("isActive", isActive); c.setInteger("workTickCountDown", workTickCountDown); sideConfig.writeToNBT(c); ItemStackHelper.saveAllItems(c, inventory); return c; }
}
