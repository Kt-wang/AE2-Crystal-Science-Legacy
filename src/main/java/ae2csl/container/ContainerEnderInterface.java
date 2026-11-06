/*
 * AE2 Crystal Science - ContainerEnderInterface (1.12.2)
 * 对照 1.21.1 源码 common/menu/EnderInterfaceMenu.java
 * 在集成接口容器基础上增加吸收配置槽(PROCESSING_INPUTS)与范围/黑白名单同步。
 */
package ae2csl.container;

import ae2.api.inventories.InternalInventory;
import ae2.api.util.IConfigManager;
import ae2.container.SlotSemantics;
import ae2.container.guisync.GuiSync;
import ae2.container.slot.FakeSlot;
import ae2csl.api.settings.AE2CSSettings;
import ae2csl.api.settings.BlackListMode;
import ae2csl.api.settings.ShowRangeMode;
import ae2csl.me.logic.EnderInterfaceHost;

import net.minecraft.entity.player.InventoryPlayer;

public class ContainerEnderInterface extends ContainerIntegratedInterface {

    private static final String ACTION_CHANGE_ABSORB_RANGE = "change_absorb_range";

    @GuiSync(20)
    public BlackListMode blackListMode = BlackListMode.WHITELIST;
    @GuiSync(21)
    public int absorbRange = 3;
    @GuiSync(22)
    public ShowRangeMode showRange = ShowRangeMode.HIDE_RANGE;

    public ContainerEnderInterface(InventoryPlayer ip, EnderInterfaceHost host) {
        super(ip, host);

        InternalInventory absorbConfigWrap = getEnderInterfaceHost().getEnderInterfaceLogic()
            .getAbsorbConfigInventory().createGuiWrapper();
        for (int i = 0; i < absorbConfigWrap.size(); i++) {
            addSlot(new FakeSlot(absorbConfigWrap, i, 0, 0), SlotSemantics.PROCESSING_INPUTS);
        }

        registerClientAction(ACTION_CHANGE_ABSORB_RANGE, Integer.class, this::onChangeAbsorbRange);
    }

    @Override
    protected int getPlayerInventoryTop() {
        return this.extended ? 215 : 179;
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        super.loadSettingsFromHost(cm);
        if (cm.hasSetting(AE2CSSettings.SHOW_RANGE_MODE)) {
            this.showRange = cm.getSetting(AE2CSSettings.SHOW_RANGE_MODE);
        }
        if (cm.hasSetting(AE2CSSettings.BLACK_LIST_MODE)) {
            this.blackListMode = cm.getSetting(AE2CSSettings.BLACK_LIST_MODE);
        }
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            this.absorbRange = getEnderInterfaceHost().getEnderInterfaceLogic().getRange();
        }
        super.broadcastChanges();
    }

    public void sendChangeAbsorbRange(int delta) {
        sendClientAction(ACTION_CHANGE_ABSORB_RANGE, delta);
    }

    private void onChangeAbsorbRange(int delta) {
        var logic = getEnderInterfaceHost().getEnderInterfaceLogic();
        logic.setRange(logic.getRange() + delta);
    }

    private EnderInterfaceHost getEnderInterfaceHost() {
        return (EnderInterfaceHost) getHost();
    }
}
