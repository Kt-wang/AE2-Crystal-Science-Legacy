/*
 * AE2 Crystal Science - GuiEnderInterface (1.12.2)
 * 对照 1.21.1 源码 client/gui/EnderInterfaceGUI.java
 * 在接口 GUI 基础上增加黑白名单/范围显示开关与吸收范围调节按钮。
 */
package ae2csl.client.gui;

import ae2.api.config.FuzzyMode;
import ae2.api.config.Settings;
import ae2.client.gui.Icon;
import ae2.client.gui.implementations.GuiUpgradeable;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.IconButton;
import ae2.client.gui.widgets.ServerSettingToggleButton;
import ae2.client.gui.widgets.SettingToggleButton;
import ae2.container.SlotSemantics;
import ae2.core.definitions.AEItems;
import ae2.core.localization.ButtonToolTips;
import ae2csl.api.settings.AE2CSSettingAppearances;
import ae2csl.api.settings.AE2CSSettings;
import ae2csl.api.settings.BlackListMode;
import ae2csl.api.settings.ShowRangeMode;
import ae2csl.client.gui.icon.AE2CSIcon;
import ae2csl.client.gui.widgets.AE2CSSettingToggleButton;
import ae2csl.client.gui.widgets.BlitterIconButton;
import ae2csl.container.ContainerEnderInterface;
import ae2csl.me.logic.EnderInterfaceHost;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.List;

public class GuiEnderInterface extends GuiUpgradeable<ContainerEnderInterface> {

    private static final String STYLE_PATH = "/screens/ender_interface_menu.json";
    private static final String EXTENDED_STYLE_PATH = "/screens/extended_ender_interface_menu.json";

    private final SettingToggleButton<FuzzyMode> fuzzyMode;
    private final AE2CSSettingToggleButton<BlackListMode> blackListModeButton;
    private final AE2CSSettingToggleButton<ShowRangeMode> showRangeButton;
    private final BlitterIconButton addRangeButton;
    private final BlitterIconButton reduceRangeButton;

    private final List<SetAmountButton> amountButtons = new ArrayList<>();

    public GuiEnderInterface(InventoryPlayer playerInv, EnderInterfaceHost host) {
        this(new ContainerEnderInterface(playerInv, host), playerInv,
            GuiStyleManager.loadStyleDoc(host.isExtended() ? EXTENDED_STYLE_PATH : STYLE_PATH));
    }

    private GuiEnderInterface(ContainerEnderInterface container, InventoryPlayer playerInv, GuiStyle style) {
        super(container, playerInv, style);
        ContainerEnderInterface menu = getContainer();

        this.fuzzyMode = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        addToLeftToolbar(this.fuzzyMode);

        this.blackListModeButton = new AE2CSSettingToggleButton<>(
            AE2CSSettings.BLACK_LIST_MODE, BlackListMode.WHITELIST,
            value -> value == BlackListMode.BLACKLIST
                ? AE2CSIcon.BLACK_LIST_MODE.getBlitter()
                : AE2CSIcon.WHITE_LIST_MODE.getBlitter(),
            value -> tooltipLines(AE2CSSettings.BLACK_LIST_MODE, value));
        addToLeftToolbar(this.blackListModeButton);

        this.showRangeButton = new AE2CSSettingToggleButton<>(
            AE2CSSettings.SHOW_RANGE_MODE, ShowRangeMode.HIDE_RANGE,
            value -> value == ShowRangeMode.SHOW_RANGE
                ? Icon.OVERLAY_ON.getBlitter()
                : Icon.OVERLAY_OFF.getBlitter(),
            value -> tooltipLines(AE2CSSettings.SHOW_RANGE_MODE, value));
        addToLeftToolbar(this.showRangeButton);

        this.addRangeButton = new BlitterIconButton(() -> menu.sendChangeAbsorbRange(1),
            () -> AE2CSIcon.ADDITION_SIGN.getBlitter());
        widgets.add("add_range_button", addRangeButton);

        this.reduceRangeButton = new BlitterIconButton(() -> menu.sendChangeAbsorbRange(-1),
            () -> AE2CSIcon.SUBTRACTION_SIGN.getBlitter());
        widgets.add("reduce_range_button", reduceRangeButton);

        widgets.addOpenPriorityButton();

        List<Slot> configSlots = menu.getSlots(SlotSemantics.CONFIG);
        for (int i = 0; i < configSlots.size(); i++) {
            int slotIndex = i;
            SetAmountButton button = new SetAmountButton(() -> menu.openSetAmountMenu(slotIndex));
            button.setDisableBackground(true);
            button.setMessage(ButtonToolTips.InterfaceSetStockAmount.text());
            widgets.add("amtButton" + (1 + i), button);
            amountButtons.add(button);
        }
    }

    private static <T extends Enum<T>> List<ITextComponent> tooltipLines(ae2.api.config.Setting<T> setting,
                                                                         T value) {
        var appearance = AE2CSSettingAppearances.getOrNull(setting, value);
        return appearance != null ? appearance.getTooltipLines() : List.of();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        ContainerEnderInterface menu = getContainer();

        this.fuzzyMode.set(menu.getFuzzyMode());
        this.fuzzyMode.setVisibility(menu.hasUpgrade(AEItems.FUZZY_CARD.item()));
        this.blackListModeButton.set(menu.blackListMode);
        this.showRangeButton.set(menu.showRange);

        setTextContent("absorb_range", new TextComponentString(String.valueOf(menu.absorbRange)));

        var configSlots = menu.getSlots(SlotSemantics.CONFIG);
        for (int i = 0; i < amountButtons.size(); i++) {
            var button = amountButtons.get(i);
            button.setVisibility(!configSlots.get(i).getStack().isEmpty());
        }
    }

    private static class SetAmountButton extends IconButton {

        public SetAmountButton(Runnable onPress) {
            super(onPress);
        }

        @Override
        protected Icon getIcon() {
            return this.hovered ? Icon.COG : Icon.COG_DISABLED;
        }
    }
}
