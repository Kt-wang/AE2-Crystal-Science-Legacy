/*
 * AE2 Crystal Science - ContainerResonatingDriveSettings (1.12.2)
 * 谐振ME驱动器设置子界面容器：无槽位，仅同步「自动拉取存储元件」开关。
 * 复用 AEBaseContainer 的 @GuiSync + registerClientAction，不需要自定义网络包。
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import ae2.container.guisync.GuiSync;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.tile.storage.TileResonatingDrive;

import net.minecraft.entity.player.InventoryPlayer;

public class ContainerResonatingDriveSettings extends AEBaseContainer {

    private static final String ACTION_TOGGLE_AUTO_PULL = "toggleAutoPull";
    private static final String ACTION_BACK = "back";

    @GuiSync(92)
    public boolean autoPull;

    public ContainerResonatingDriveSettings(InventoryPlayer ip, TileResonatingDrive drive) {
        super(ip, drive);
        registerClientAction(ACTION_TOGGLE_AUTO_PULL, this::toggleAutoPullServer);
        registerClientAction(ACTION_BACK, this::backToDrive);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide() && getTileEntity() instanceof TileResonatingDrive drive) {
            this.autoPull = drive.isAutoPullCells();
        }
        super.broadcastChanges();
    }

    /** 客户端：切换自动拉取 */
    public void toggleAutoPull() {
        if (isClientSide()) {
            sendClientAction(ACTION_TOGGLE_AUTO_PULL);
        }
    }

    /** 客户端：返回驱动器主界面 */
    public void goBack() {
        if (isClientSide()) {
            sendClientAction(ACTION_BACK);
        }
    }

    private void toggleAutoPullServer() {
        if (getTileEntity() instanceof TileResonatingDrive drive) {
            drive.setAutoPullCells(!drive.isAutoPullCells());
        }
    }

    private void backToDrive() {
        if (getTileEntity() != null) {
            var pos = getTileEntity().getPos();
            getPlayer().openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_RESONATING_DRIVE,
                getTileEntity().getWorld(), pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
