/*
 * AE2 Crystal Science - TileResonatingDrive (1.12.2)
 * 谐振ME驱动器：大容量存储元件驱动器。
 * 逻辑对照 AE2-Supergiant 的 TileDrive，差异：
 *  - CELL_COUNT 由配置决定（默认 630 = 10 页 × 63 槽），GUI 侧分页显示；
 *  - 不做逐格 LED / 模型内嵌元件渲染，因此方块实体流同步只走父类（朝向等），
 *    元件状态不随 Tile 同步（GUI 槽位由容器槽同步负责）。
 */
package ae2csl.tile.storage;

import ae2.api.inventories.InternalInventory;
import ae2.api.implementations.blockentities.IChestOrDrive;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.stacks.AEItemKey;
import ae2.api.storage.IStorageMounts;
import ae2.api.storage.IStorageProvider;
import ae2.api.storage.MEStorage;
import ae2.api.storage.StorageCells;
import ae2.api.storage.StorageHelper;
import ae2.api.storage.cells.CellState;
import ae2.api.storage.cells.StorageCell;
import ae2.api.storage.cells.StorageCellStatistics;
import ae2.helpers.IPriorityHost;
import ae2.container.ISubGui;
import ae2.me.helpers.MachineSource;
import ae2.me.storage.DriveWatcher;
import ae2.tile.grid.AENetworkedInvTile;
import ae2.util.inv.AppEngCellInventory;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.filter.IAEItemFilter;
import ae2csl.AE2CSConfig;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.init.AE2CSBlocks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import io.netty.buffer.ByteBuf;

import org.jetbrains.annotations.Nullable;

public class TileResonatingDrive extends AENetworkedInvTile
    implements IPriorityHost, IStorageProvider, IChestOrDrive {

    /** 每页槽位数（GUI 一侧 9×7 网格） */
    public static final int SLOTS_PER_PAGE = 63;

    /**
     * 元件槽总数：读取配置并向下取整到一页（63）的倍数。
     * 不在静态初始化时缓存，避免配置尚未同步时固化默认值。
     */
    public static int getCellSlotCount() {
        int configured = Math.max(SLOTS_PER_PAGE, AE2CSConfig.resonatingDriveCellCount);
        return configured - configured % SLOTS_PER_PAGE;
    }

    private final int cellCount;
    private final AppEngCellInventory inv;
    private final DriveWatcher[] invBySlot;
    private boolean isCached;
    private int priority;
    private boolean wasOnline;
    /** 上次打开 GUI 时停留的页码（全局，不区分玩家），持久化到 NBT */
    private int lastPage;
    /** 是否自动从 ME 网络存储中拉取存储元件到空槽位 */
    private boolean autoPullCells;

    /** 舱位 LED 数量（模型固定 10 舱位） */
    private static final int LED_COUNT = 10;
    /** LED 填充档位：0=灭，1..15=填充渐变（量化避免频繁同步） */
    private static final int LED_MAX_LEVEL = 15;
    /** 客户端：10 盏 LED 的档位（0=灭） */
    private final int[] ledLevels = new int[LED_COUNT];
    /** 客户端：各舱位代表元件（页/组内第一个非空槽的物品） */
    private final Item[] clientCellItems = new Item[LED_COUNT];
    /** 服务端：各舱位代表元件缓存 */
    private final Item[] repItems = new Item[LED_COUNT];
    private boolean clientPowered;
    /** 服务端：上次同步的 LED 打包值（变了才 markForUpdate） */
    private long lastPackedLeds = -1;

    public TileResonatingDrive() {
        this.cellCount = getCellSlotCount();
        this.inv = new AppEngCellInventory(this, cellCount);
        this.invBySlot = new DriveWatcher[cellCount];
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).addService(IStorageProvider.class, this)
            .addService(IGridTickable.class, new AutoPullTicker());
        inv.setFilter(new CellValidInventoryFilter());
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(AE2CSBlocks.RESONATING_DRIVE_BLOCK);
    }

    public int getCellCount() {
        // IChestOrDrive 契约：舱位数（模型固定 10 舱位）；元件槽总数见 getTotalCellSlots()
        return LED_COUNT;
    }

    /** 元件槽总数（容器分页用） */
    public int getTotalCellSlots() {
        return cellCount;
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.isCached = false;
        this.priority = data.getInteger("priority");
        this.lastPage = data.getInteger("lastPage");
        this.autoPullCells = data.getBoolean("autoPullCells");
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        this.inv.persist();
        super.saveAdditional(data);
        data.setInteger("priority", this.priority);
        data.setInteger("lastPage", this.lastPage);
        data.setBoolean("autoPullCells", this.autoPullCells);
    }

    public int getLastPage() {
        return this.lastPage;
    }

    public void setLastPage(int lastPage) {
        if (this.lastPage != lastPage) {
            this.lastPage = lastPage;
            saveChanges();
        }
    }

    public boolean isAutoPullCells() {
        return this.autoPullCells;
    }

    /**
     * 客户端读取 LED 档位（TESR 用）。0=灭，1..15=填充渐变。
     */
    public int getLedLevel(int led) {
        return led >= 0 && led < LED_COUNT ? this.ledLevels[led] : 0;
    }

    public boolean isPowered() {
        return isClientSide() ? clientPowered : getMainNode().isOnline();
    }

    // ---- IChestOrDrive：舱位视图（DriveModelData 渲染舱内元件用） ----

    @Nullable
    @Override
    public Item getCellItem(int slot) {
        if (slot < 0 || slot >= LED_COUNT) {
            return null;
        }
        return isClientSide() ? clientCellItems[slot] : repItems[slot];
    }

    @Override
    public CellState getCellStatus(int slot) {
        if (slot < 0 || slot >= LED_COUNT) {
            return CellState.ABSENT;
        }
        if (isClientSide()) {
            return ledLevels[slot] > 0 ? CellState.NOT_EMPTY : CellState.ABSENT;
        }
        return repItems[slot] != null ? CellState.NOT_EMPTY : CellState.ABSENT;
    }

    @Override
    public boolean isCellBlinking(int slot) {
        return false;
    }

    /** 对齐 TileDrive：真实元件槽的原始 StorageCell */
    @Nullable
    @Override
    public StorageCell getOriginalCellInventory(int slot) {
        if (slot < 0 || slot >= cellCount) {
            return null;
        }
        return invBySlot[slot] != null ? invBySlot[slot].getCell() : null;
    }

    // ---- LED 状态同步：10 灯 × 4bit 档位 + 1bit 供电，压进一个 long ----

    @Override
    protected void writeToStream(ByteBuf data) {
        super.writeToStream(data);
        data.writeLong(this.lastPackedLeds >= 0 ? this.lastPackedLeds : computePackedLeds());
        for (int i = 0; i < LED_COUNT; i++) {
            data.writeInt(this.repItems[i] == null ? 0 : Item.getIdFromItem(this.repItems[i]));
        }
    }

    @Override
    protected boolean readFromStream(ByteBuf data) {
        boolean changed = super.readFromStream(data);
        long packed = data.readLong();
        boolean powered = (packed & (1L << 40)) != 0;
        if (powered != this.clientPowered) {
            this.clientPowered = powered;
            changed = true;
        }
        for (int i = 0; i < LED_COUNT; i++) {
            int level = (int) ((packed >> (i * 4)) & 0xF);
            if (this.ledLevels[i] != level) {
                this.ledLevels[i] = level;
                changed = true;
            }
        }
        for (int i = 0; i < LED_COUNT; i++) {
            Item item = Item.getItemById(data.readInt());
            if (this.clientCellItems[i] != item) {
                this.clientCellItems[i] = item;
                changed = true;
            }
        }
        return changed;
    }

    /**
     * 重算 10 盏 LED 的填充档位（服务端），变化时才 markForUpdate。
     * 映射规则：页数 ≤10 时一灯一页、多余灯灭；页数 >10 时按 ⌊i×N/10⌋ 均匀分组，一灯管多页。
     * 页/组填充率 = Σ已用字节 ÷ Σ总容量；无统计接口的第三方元件按 CellState 折算。
     */
    private void updateLedLevels() {
        long packed = computePackedLeds();
        boolean itemsChanged = updateRepItems();
        if (packed != this.lastPackedLeds || itemsChanged) {
            this.lastPackedLeds = packed;
            markForUpdate();
        }
    }

    /** 重算各舱位的代表元件（页/组内第一个非空槽），返回是否有变化 */
    private boolean updateRepItems() {
        boolean changed = false;
        int pages = cellCount / SLOTS_PER_PAGE;
        for (int led = 0; led < LED_COUNT; led++) {
            Item rep = null;
            int[] range = bayRange(led, pages);
            if (range != null) {
                for (int slot = range[0] * SLOTS_PER_PAGE; slot < range[1] * SLOTS_PER_PAGE; slot++) {
                    ItemStack stack = inv.getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        rep = stack.getItem();
                        break;
                    }
                }
            }
            if (this.repItems[led] != rep) {
                this.repItems[led] = rep;
                changed = true;
            }
        }
        return changed;
    }

    /** 舱位 LED 覆盖的页范围 [from, to)，无对应页返回 null */
    @Nullable
    private static int[] bayRange(int led, int pages) {
        if (pages <= LED_COUNT) {
            return led < pages ? new int[]{led, led + 1} : null;
        }
        int from = (int) ((long) led * pages / LED_COUNT);
        int to = (int) ((long) (led + 1) * pages / LED_COUNT);
        return from < to ? new int[]{from, to} : null;
    }

    private long computePackedLeds() {
        long packed = 0;
        int pages = cellCount / SLOTS_PER_PAGE;
        for (int led = 0; led < LED_COUNT; led++) {
            int level = computeLedLevel(led, pages);
            packed |= (long) level << (led * 4);
        }
        if (getMainNode().isOnline()) {
            packed |= 1L << 40;
        }
        return packed;
    }

    private int computeLedLevel(int led, int pages) {
        int[] range = bayRange(led, pages);
        if (range == null) {
            return 0;
        }
        int from = range[0], to = range[1];

        long used = 0, total = 0;
        boolean anyCell = false;
        for (int page = from; page < to; page++) {
            for (int slot = page * SLOTS_PER_PAGE; slot < (page + 1) * SLOTS_PER_PAGE; slot++) {
                DriveWatcher watcher = invBySlot[slot];
                if (watcher == null) {
                    continue;
                }
                anyCell = true;
                StorageCell cell = watcher.getCell();
                if (cell instanceof StorageCellStatistics stats) {
                    used += stats.getUsedBytes();
                    total += stats.getTotalBytes();
                } else {
                    // 无统计接口：按状态折算成 0/25%/75%/100%
                    switch (cell.getStatus()) {
                        case EMPTY -> total += 4;
                        case NOT_EMPTY -> { used += 1; total += 4; }
                        case TYPES_FULL -> { used += 3; total += 4; }
                        case FULL -> { used += 4; total += 4; }
                        default -> { }
                    }
                }
            }
        }
        if (!anyCell || total <= 0) {
            return 0;
        }
        double ratio = (double) used / total;
        return 1 + (int) Math.round(ratio * (LED_MAX_LEVEL - 1));
    }

    public void setAutoPullCells(boolean autoPullCells) {
        if (this.autoPullCells != autoPullCells) {
            this.autoPullCells = autoPullCells;
            saveChanges();
        }
    }

    /**
     * 自动拉取：找第一个空槽，从网络存储中提取一个存储元件放进去。
     * 每 tick 最多拉一个，由 AutoPullTicker 驱动（20 tick 一次）。
     * 优先级一致：同一网络上若存在优先级更高、同样开着自动拉取且还有空槽的谐振驱动器，
     * 本驱动器本轮让路，由高优先级的先拉。
     */
    private void tryPullCellFromNetwork() {
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return;
        }

        if (shouldYieldToHigherPriorityDrive(grid)) {
            return;
        }

        int target = findEmptyCellSlot();
        if (target < 0) {
            return;
        }

        MEStorage networkInv = grid.getStorageService().getInventory();
        var energy = grid.getEnergyService();
        var src = new MachineSource(this);
        for (var entry : networkInv.getAvailableStacks()) {
            if (!(entry.getKey() instanceof AEItemKey itemKey)) {
                continue;
            }
            if (!StorageCells.isCellHandled(itemKey.toStack())) {
                continue;
            }
            long extracted = StorageHelper.poweredExtraction(energy, networkInv, itemKey, 1, src);
            if (extracted > 0) {
                inv.insertItem(target, itemKey.toStack(1), false);
                return;
            }
        }
    }

    /** 第一个空元件槽，无空槽返回 -1 */
    private int findEmptyCellSlot() {
        for (int i = 0; i < cellCount; i++) {
            if (inv.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasEmptyCellSlot() {
        return findEmptyCellSlot() >= 0;
    }

    /**
     * 同一网格上存在「优先级更高 + 开着自动拉取 + 在线 + 还有空槽」的谐振驱动器时，本轮让路。
     * 优先级相同则互不让路（各自每 20 tick 拉一个）。
     */
    private boolean shouldYieldToHigherPriorityDrive(ae2.api.networking.IGrid grid) {
        for (TileResonatingDrive other : grid.getMachines(TileResonatingDrive.class)) {
            if (other == this || other.isInvalid()) {
                continue;
            }
            if (other.isAutoPullCells()
                && other.getPriority() > this.getPriority()
                && other.getMainNode().isOnline()
                && other.hasEmptyCellSlot()) {
                return true;
            }
        }
        return false;
    }

    private class AutoPullTicker implements IGridTickable {
        @Override
        public TickingRequest getTickingRequest(IGridNode node) {
            return new TickingRequest(20, 20, false);
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
            updateLedLevels();
            if (autoPullCells && getMainNode().isOnline()) {
                tryPullCellFromNetwork();
            }
            return TickRateModulation.SAME;
        }
    }

    @Override
    public AppEngCellInventory getInternalInventory() {
        return this.inv;
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        if (this.isCached) {
            this.isCached = false;
            updateState();
        }

        updateLedLevels();
        IStorageProvider.requestUpdate(getMainNode());
        markForUpdate();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        boolean currentOnline = getMainNode().isOnline();
        if (this.wasOnline != currentOnline) {
            this.wasOnline = currentOnline;
            IStorageProvider.requestUpdate(getMainNode());
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        updateState();
        updateLedLevels();
    }

    @Override
    public void mountInventories(IStorageMounts storageMounts) {
        if (getMainNode().isOnline()) {
            updateState();
            for (var inventory : invBySlot) {
                if (inventory != null) {
                    storageMounts.mount(inventory, priority);
                }
            }
        }
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return getItemFromTile();
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_RESONATING_DRIVE,
            world, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int newValue) {
        this.priority = newValue;
        this.isCached = false;
        updateState();
        IStorageProvider.requestUpdate(getMainNode());
        saveChanges();
    }

    @Nullable
    public MEStorage getCellInventory(int slot) {
        if (slot < 0 || slot >= cellCount) {
            return null;
        }
        return invBySlot[slot];
    }

    public void invalidateCellInventory(int slot) {
        if (slot < 0 || slot >= cellCount) {
            throw new IllegalArgumentException("Invalid drive cell slot: " + slot);
        }

        this.isCached = false;
        updateState();
        IStorageProvider.requestUpdate(getMainNode());
        saveChanges();
        markForUpdate();
    }

    private void updateState() {
        if (!isCached) {
            double power = 2.0;
            for (int slot = 0; slot < cellCount; slot++) {
                power += updateStateForSlot(slot);
            }
            getMainNode().setIdlePowerUsage(power);
            isCached = true;
        }
    }

    private double updateStateForSlot(int slot) {
        inv.setHandler(slot, null);
        invBySlot[slot] = null;
        ItemStack stack = inv.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            StorageCell cell = StorageCells.getCellInventory(stack, this::onCellContentChanged);
            if (cell != null) {
                inv.setHandler(slot, cell);
                invBySlot[slot] = new DriveWatcher(cell, stack, this::saveChanges);
                return cell.getIdleDrain();
            }
        }
        return 0;
    }

    private void onCellContentChanged() {
        saveChanges();
    }

    private static class CellValidInventoryFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return !stack.isEmpty() && StorageCells.isCellHandled(stack);
        }

        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return IAEItemFilter.super.allowExtract(inv, slot, amount);
        }
    }
}
