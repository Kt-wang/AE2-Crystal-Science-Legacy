/*
 * AE2 Crystal Science - ContainerIntegratedInterface (1.12.2)
 * 对照 1.21.1 源码 common/menu/IntegratedInterfaceMenu.java
 * 配置(GhostSlot) + 存储(AppEngSlot) + 样板(RestrictedInputSlot)，分页 + 升级槽。
 */
package ae2csl.container;

import ae2.api.config.BlockingMode;
import ae2.api.config.LockCraftingMode;
import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.api.inventories.InternalInventory;
import ae2.api.stacks.GenericStack;
import ae2.api.util.IConfigManager;
import ae2.container.SlotSemantics;
import ae2.container.guisync.GuiSync;
import ae2.container.implementations.UpgradeableContainer;
import ae2.container.slot.AppEngSlot;
import ae2.container.slot.FakeSlot;
import ae2.container.slot.RestrictedInputSlot;
import ae2csl.me.logic.IntegratedInterfaceHost;
import ae2csl.me.logic.IntegratedInterfaceLogic;

import net.minecraft.entity.player.InventoryPlayer;

import it.unimi.dsi.fastutil.shorts.ShortSet;

public class ContainerIntegratedInterface extends UpgradeableContainer<IntegratedInterfaceHost> {

    public static final String ACTION_OPEN_SET_AMOUNT = "setAmount";
    public static final String ACTION_SET_PAGE = "setPage";

    /** 每页槽位数（源码普通版9槽1页，扩展版36槽4页） */
    public static final int SLOTS_PER_PAGE = 9;

    @GuiSync(10)
    public BlockingMode blockingMode = BlockingMode.NO;
    @GuiSync(11)
    public YesNo showInAccessTerminal = YesNo.YES;
    @GuiSync(12)
    public LockCraftingMode lockCraftingMode = LockCraftingMode.NONE;
    @GuiSync(13)
    public LockCraftingMode craftingLockedReason = LockCraftingMode.NONE;
    @GuiSync(14)
    public GenericStack unlockStack = null;
    @GuiSync(15)
    public int currentPage;
    @GuiSync(16)
    public int pageCount;

    private final PageInventory configPageInventory;
    private final PageInventory storagePageInventory;

    private final IntegratedInterfaceLogic logic;
    public final boolean extended;

    public ContainerIntegratedInterface(InventoryPlayer ip, IntegratedInterfaceHost host) {
        super(ip, host);

        this.logic = host.getLogic();
        this.extended = host.isExtended();
        this.pageCount = Math.max(1, (logic.getStorageSize() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);

        registerClientAction(ACTION_OPEN_SET_AMOUNT, Integer.class, this::openSetAmountMenu);
        registerClientAction(ACTION_SET_PAGE, Integer.class, this::setPage);

        // 样板槽（全部添加，翻页时启用/禁用）
        InternalInventory patternInv = host.getTerminalPatternInventory();
        for (int i = 0; i < patternInv.size(); ++i) {
            this.addSlot(
                new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.PROVIDER_PATTERN, patternInv, i),
                SlotSemantics.ENCODED_PATTERN);
        }

        // 配置槽（分页）
        this.configPageInventory = new PageInventory(host.getConfigInv().createGuiWrapper(), SLOTS_PER_PAGE);
        for (int i = 0; i < SLOTS_PER_PAGE; ++i) {
            this.addSlot(new FakeSlot(this.configPageInventory, i, 0, 0), SlotSemantics.CONFIG);
        }

        // 存储槽（分页）
        this.storagePageInventory = new PageInventory(host.getStorageInv().createGuiWrapper(), SLOTS_PER_PAGE);
        for (int i = 0; i < SLOTS_PER_PAGE; ++i) {
            this.addSlot(new AppEngSlot(this.storagePageInventory, i), SlotSemantics.STORAGE);
        }

        if (isClientSide()) {
            togglePage(this.currentPage);
        }
    }

    private static int clampPage(int page, int pageCount) {
        if (pageCount <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(page, pageCount - 1));
    }

    @Override
    protected int getPlayerInventoryTop() {
        return 149;
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager configManager) {
        if (configManager.hasSetting(Settings.FUZZY_MODE)) {
            this.setFuzzyMode(configManager.getSetting(Settings.FUZZY_MODE));
        }
        if (configManager.hasSetting(Settings.BLOCKING_MODE)) {
            this.blockingMode = configManager.getSetting(Settings.BLOCKING_MODE);
        }
        if (configManager.hasSetting(Settings.PATTERN_ACCESS_TERMINAL)) {
            this.showInAccessTerminal = configManager.getSetting(Settings.PATTERN_ACCESS_TERMINAL);
        }
        if (configManager.hasSetting(Settings.LOCK_CRAFTING_MODE)) {
            this.lockCraftingMode = configManager.getSetting(Settings.LOCK_CRAFTING_MODE);
        }
        this.craftingLockedReason = logic.getCraftingLockedReason();
        this.unlockStack = logic.getUnlockStack();
    }

    @Override
    public void onClientDataSync(ShortSet updatedFields) {
        super.onClientDataSync(updatedFields);
        this.configPageInventory.setPage(this.currentPage);
        this.storagePageInventory.setPage(this.currentPage);
        togglePage(this.currentPage);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            this.pageCount = Math.max(1,
                (getHost().getLogic().getStorageSize() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);
            this.currentPage = clampPage(this.currentPage, this.pageCount);
        }
        this.configPageInventory.setPage(this.currentPage);
        this.storagePageInventory.setPage(this.currentPage);
        super.broadcastChanges();
    }

    /**
     * 翻页（客户端直接执行，然后通知服务端）
     */
    public void onTogglePageButton(int delta) {
        int newPage = this.currentPage + delta;
        if (newPage >= pageCount) {
            newPage = 0;
        } else if (newPage < 0) {
            newPage = pageCount - 1;
        }
        setPage(newPage);
    }

    public void setPage(int page) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_PAGE, page);
            this.currentPage = clampPage(page, this.pageCount);
            this.configPageInventory.setPage(this.currentPage);
            this.storagePageInventory.setPage(this.currentPage);
            togglePage(this.currentPage);
            return;
        }
        this.currentPage = clampPage(page, this.pageCount);
        this.configPageInventory.setPage(this.currentPage);
        this.storagePageInventory.setPage(this.currentPage);
        this.detectAndSendChanges();
    }

    /**
     * 根据翻页数启用和禁用样板槽
     */
    private void togglePage(int pageIndex) {
        int activeStart = pageIndex * SLOTS_PER_PAGE;
        int activeEnd = activeStart + SLOTS_PER_PAGE;

        var patternSlots = getSlots(SlotSemantics.ENCODED_PATTERN);
        for (int i = 0; i < patternSlots.size(); ++i) {
            if (patternSlots.get(i) instanceof AppEngSlot appEngSlot) {
                appEngSlot.setSlotEnabled(i >= activeStart && i < activeEnd);
                // 对齐源码：所有样板槽固定在第一行，翻页只切换启用状态
                appEngSlot.yPos = 115;
            }
        }
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    public int getPageCount() {
        return this.pageCount;
    }

    public YesNo getShowInAccessTerminal() {
        return showInAccessTerminal;
    }

    public LockCraftingMode getLockCraftingMode() {
        return lockCraftingMode;
    }

    public LockCraftingMode getCraftingLockedReason() {
        return craftingLockedReason;
    }

    public GenericStack getUnlockStack() {
        return unlockStack;
    }

    public IntegratedInterfaceLogic getLogic() {
        return logic;
    }

    /**
     * 打开用来设置数量的子菜单
     *
     * @param configSlot 当前页内的槽位序号
     */
    public void openSetAmountMenu(int configSlot) {
        if (isClientSide()) {
            sendClientAction(ACTION_OPEN_SET_AMOUNT, configSlot);
            return;
        }

        int actualSlot = this.currentPage * SLOTS_PER_PAGE + configSlot;
        if (configSlot < 0 || configSlot >= SLOTS_PER_PAGE
            || actualSlot < 0 || actualSlot >= getHost().getConfigInv().size()) {
            return;
        }
        var stack = getHost().getConfigInv().getStack(actualSlot);
        if (stack != null) {
            ContainerSetStockAmount.open(getPlayer(), getHost(), actualSlot, stack.what(),
                (int) stack.amount());
        }
    }

    @Override
    public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
        return !getHost().getTileEntity().isInvalid();
    }
}
