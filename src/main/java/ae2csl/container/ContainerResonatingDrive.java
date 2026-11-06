/*
 * AE2 Crystal Science - ContainerResonatingDrive (1.12.2)
 * 谐振ME驱动器容器：630 格元件库存通过 PageInventory 按 63 槽/页暴露。
 * 翻页模式对照 ContainerIntegratedInterface：@GuiSync 同步页码 +
 * registerClientAction/sendClientAction 客户端动作，无需自定义 IMessage 包。
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2.container.guisync.GuiSync;
import ae2.container.slot.RestrictedInputSlot;
import ae2csl.tile.storage.TileResonatingDrive;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketSetSlot;

import it.unimi.dsi.fastutil.shorts.ShortSet;

public class ContainerResonatingDrive extends AEBaseContainer {

    public static final int SLOTS_PER_PAGE = TileResonatingDrive.SLOTS_PER_PAGE;
    private static final String ACTION_SET_PAGE = "setPage";
    private static final String ACTION_OPEN_SETTINGS = "openSettings";

    @GuiSync(90)
    public int currentPage;

    private final PageInventory cellPageInventory;
    private final int pageCount;

    public ContainerResonatingDrive(InventoryPlayer ip, TileResonatingDrive drive) {
        super(ip, drive);

        this.pageCount = Math.max(1, (drive.getTotalCellSlots() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE);

        registerClientAction(ACTION_SET_PAGE, Integer.class, page -> setPage(page, drive));
        registerClientAction(ACTION_OPEN_SETTINGS, this::openSettingsServer);

        // 服务端：恢复上次停留的页码（@GuiSync 会随开启同步给客户端）
        if (isServerSide()) {
            this.currentPage = clampPage(drive.getLastPage());
        }

        this.cellPageInventory = new PageInventory(drive.getInternalInventory(), SLOTS_PER_PAGE);
        for (int i = 0; i < SLOTS_PER_PAGE; i++) {
            this.addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.STORAGE_CELLS,
                this.cellPageInventory, i) {
                // 对齐原驱动器视觉效果：放入元件后仍绘制槽位底框（默认仅空槽绘制）
                @Override
                public boolean renderIconWithItem() {
                    return true;
                }
            }, SlotSemantics.STORAGE_CELL);
        }

        // 与 screens/resonating_drive_menu.json 中的 PLAYER_INVENTORY 位置一致
        this.addPlayerInventorySlots(8, 167);

        if (isClientSide()) {
            this.cellPageInventory.setPage(this.currentPage);
        }
    }

    private int clampPage(int page) {
        return Math.max(0, Math.min(page, this.pageCount - 1));
    }

    @Override
    public void onClientDataSync(ShortSet updatedFields) {
        super.onClientDataSync(updatedFields);
        this.cellPageInventory.setPage(this.currentPage);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            this.currentPage = clampPage(this.currentPage);
        }
        this.cellPageInventory.setPage(this.currentPage);
        super.broadcastChanges();
    }

    /**
     * 翻页（客户端立即执行，然后通知服务端；对齐集成接口的做法）
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
            this.currentPage = clampPage(page);
            this.cellPageInventory.setPage(this.currentPage);
            return;
        }
        this.currentPage = clampPage(page);
        this.cellPageInventory.setPage(this.currentPage);
        this.detectAndSendChanges();
    }

    private void setPage(int page, TileResonatingDrive drive) {
        setPage(page);
        // 记住停留页，下次打开直接回到这一页
        drive.setLastPage(this.currentPage);
        forceResyncVisibleSlots();
    }

    /**
     * 翻页后强制全量重发当前页槽位。
     * 原因：vanilla detectAndSendChanges 是差量同步，翻页后内容未变的槽位不重发；
     * 但客户端槽位内容写在客户端 Tile 库存的「页翻译后位置」，新页位置从未被写过，
     * 若相邻页有完全相同的物品（如同款新元件），不重发就会显示为空（点击才刷新）。
     */
    private void forceResyncVisibleSlots() {
        if (getPlayer() instanceof EntityPlayerMP player) {
            for (int i = 0; i < SLOTS_PER_PAGE; i++) {
                player.connection.sendPacket(new SPacketSetSlot(this.windowId, i,
                    this.cellPageInventory.getStackInSlot(i)));
            }
        }
    }

    /** 客户端：打开设置子界面 */
    public void openSettings() {
        if (isClientSide()) {
            sendClientAction(ACTION_OPEN_SETTINGS);
        }
    }

    private void openSettingsServer() {
        if (getTileEntity() != null) {
            var pos = getTileEntity().getPos();
            getPlayer().openGui(ae2csl.AE2CrystalScienceLegacy.INSTANCE,
                ae2csl.client.gui.GuiHandler.GUI_RESONATING_DRIVE_SETTINGS,
                getTileEntity().getWorld(), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    public int getPageCount() {
        return this.pageCount;
    }
}
