/*
 * AE2 Crystal Science - ContainerQuartzOscillatorClock (1.12.2)
 * 对照 1.21.1 源码 common/menu/QuartzOscillatorClockMenu.java
 */
package ae2csl.container;

import ae2.api.util.IConfigManager;
import ae2.container.guisync.GuiSync;
import ae2.container.implementations.UpgradeableContainer;
import ae2csl.api.settings.AE2CSSettings;
import ae2csl.api.settings.SoundMode;
import ae2csl.me.logic.QuartzOscillatorClockHost;

import net.minecraft.entity.player.InventoryPlayer;

public class ContainerQuartzOscillatorClock extends UpgradeableContainer<QuartzOscillatorClockHost> {

    private static final String ACTION_CHANGE_PULSE_HOLD = "change_pulse_count_hold";
    private static final String ACTION_CHANGE_PULSE_WIDTH = "change_pulse_width_ticks";

    @GuiSync(10)
    public int pulseCountHold = 10;

    @GuiSync(11)
    public int pulseWidthTicks = 1;

    @GuiSync(12)
    public SoundMode soundMode = SoundMode.UNMUTE;

    public ContainerQuartzOscillatorClock(InventoryPlayer ip, QuartzOscillatorClockHost host) {
        super(ip, host);

        registerClientAction(ACTION_CHANGE_PULSE_HOLD, Integer.class, this::onChangePulseCountHold);
        registerClientAction(ACTION_CHANGE_PULSE_WIDTH, Integer.class, this::onChangePulseWidthTicks);
    }

    public void sendChangePulseCountHold(int newValue) {
        sendClientAction(ACTION_CHANGE_PULSE_HOLD, newValue);
    }

    public void sendChangePulseWidthTicks(int newValue) {
        sendClientAction(ACTION_CHANGE_PULSE_WIDTH, newValue);
    }

    private void onChangePulseCountHold(int newValue) {
        getHost().getLogic().setCurrentHold(newValue);
    }

    private void onChangePulseWidthTicks(int newValue) {
        getHost().getLogic().setPulseWidthTicks(newValue);
    }

    @Override
    protected int getPlayerInventoryTop() {
        return 198;
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            pulseCountHold = getHost().getLogic().getCurrentHold();
            pulseWidthTicks = getHost().getLogic().getPulseWidthTicks();
        }
        super.broadcastChanges();
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        if (cm.hasSetting(AE2CSSettings.REDSTONE_CONTROLLED_NO_PULSE)) {
            this.setRedStoneMode(cm.getSetting(AE2CSSettings.REDSTONE_CONTROLLED_NO_PULSE));
        }
        if (cm.hasSetting(AE2CSSettings.SOUND_MODE)) {
            this.soundMode = cm.getSetting(AE2CSSettings.SOUND_MODE);
        }
    }
}
