/*
 * AE2 Crystal Science - GuiQuartzOscillatorClock (1.12.2)
 * 对照 1.21.1 源码 client/gui/QuartzOscillatorClockGUI.java
 * 两个数值输入（脉冲间隔/脉冲宽度）+ 音效开关 + 红石模式（需红石卡）。
 */
package ae2csl.client.gui;

import ae2.api.config.RedstoneMode;
import ae2.client.gui.NumberEntryType;
import ae2.client.gui.implementations.GuiUpgradeable;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.NumberEntryWidget;
import ae2.core.definitions.AEItems;
import ae2csl.api.settings.AE2CSSettingAppearances;
import ae2csl.api.settings.AE2CSSettings;
import ae2csl.api.settings.SoundMode;
import ae2csl.client.gui.icon.AE2CSIcon;
import ae2csl.client.gui.widgets.AE2CSSettingToggleButton;
import ae2csl.container.ContainerQuartzOscillatorClock;
import ae2csl.me.logic.QuartzOscillatorClockHost;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public class GuiQuartzOscillatorClock extends GuiUpgradeable<ContainerQuartzOscillatorClock> {

    private static final String STYLE_PATH = "/screens/quartz_oscillator_clock_menu.json";

    private final AE2CSSettingToggleButton<SoundMode> soundModeButton;
    private final AE2CSSettingToggleButton<RedstoneMode> redstoneModeButton;
    private final NumberEntryWidget levelCountHold;
    private final NumberEntryWidget levelPulseWidth;

    public GuiQuartzOscillatorClock(InventoryPlayer playerInv, QuartzOscillatorClockHost host) {
        super(new ContainerQuartzOscillatorClock(playerInv, host), playerInv,
            GuiStyleManager.loadStyleDoc(STYLE_PATH));
        ContainerQuartzOscillatorClock menu = getContainer();

        this.soundModeButton = new AE2CSSettingToggleButton<>(
            AE2CSSettings.SOUND_MODE, SoundMode.UNMUTE,
            value -> value == SoundMode.UNMUTE
                ? AE2CSIcon.SOUND_UNMUTE.getBlitter()
                : AE2CSIcon.SOUND_MUTE.getBlitter(),
            value -> tooltipLines(AE2CSSettings.SOUND_MODE, value));
        this.addToLeftToolbar(this.soundModeButton);

        this.redstoneModeButton = new AE2CSSettingToggleButton<>(
            AE2CSSettings.REDSTONE_CONTROLLED_NO_PULSE, RedstoneMode.HIGH_SIGNAL,
            GuiQuartzOscillatorClock::redstoneIcon,
            value -> tooltipLines(AE2CSSettings.REDSTONE_CONTROLLED_NO_PULSE, value));
        this.addToLeftToolbar(this.redstoneModeButton);

        this.levelCountHold = widgets.addNumberEntryWidget("level_count_hold", NumberEntryType.UNITLESS);
        this.levelCountHold.setTextFieldStyle(this.style.getWidget("level_count_hold_input"));
        this.levelCountHold.setOnChange(this::onCountDownChange);
        this.levelCountHold.setOnConfirm(this::onClose);

        this.levelPulseWidth = widgets.addNumberEntryWidget("level_pulse_width", NumberEntryType.UNITLESS);
        this.levelPulseWidth.setTextFieldStyle(this.style.getWidget("level_pulse_width_input"));
        this.levelPulseWidth.setOnChange(this::onPulseWidthChange);
        this.levelPulseWidth.setOnConfirm(this::onClose);
    }

    private static ae2.client.gui.style.Blitter redstoneIcon(RedstoneMode value) {
        var icon = switch (value) {
            case IGNORE -> ae2.client.gui.Icon.REDSTONE_IGNORE;
            case HIGH_SIGNAL -> ae2.client.gui.Icon.REDSTONE_HIGH;
            case LOW_SIGNAL -> ae2.client.gui.Icon.REDSTONE_LOW;
            default -> ae2.client.gui.Icon.REDSTONE_IGNORE;
        };
        return icon.getBlitter();
    }

    private static <T extends Enum<T>> List<ITextComponent> tooltipLines(ae2.api.config.Setting<T> setting,
                                                                         T value) {
        var appearance = AE2CSSettingAppearances.getOrNull(setting, value);
        return appearance != null ? appearance.getTooltipLines() : List.of();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        ContainerQuartzOscillatorClock menu = getContainer();

        this.soundModeButton.set(menu.soundMode);

        boolean hasRedstoneCard = menu.hasUpgrade(AEItems.REDSTONE_CARD.item());
        this.redstoneModeButton.setVisibility(hasRedstoneCard);
        this.redstoneModeButton.set(menu.getRedStoneMode());

        this.levelCountHold.setLongValue(menu.pulseCountHold);
        this.levelPulseWidth.setLongValue(menu.pulseWidthTicks);
    }

    private void onCountDownChange() {
        this.levelCountHold.getIntValue().ifPresent(value -> {
            if (value != getContainer().pulseCountHold) {
                getContainer().sendChangePulseCountHold(value);
            }
        });
    }

    private void onPulseWidthChange() {
        this.levelPulseWidth.getIntValue().ifPresent(value -> {
            if (value != getContainer().pulseWidthTicks) {
                getContainer().sendChangePulseWidthTicks(value);
            }
        });
    }

    private void onClose() {
        if (this.mc.player != null) {
            this.mc.player.closeScreen();
        }
    }
}
