/*
 * AE2 Crystal Science - GuiIntegratedInterface (1.12.2)
 * 对照 1.21.1 源码 client/gui/IntegratedInterfaceGUI.java
 * 左侧工具栏: 模糊/阻挡/锁定合成/样板访问终端/翻页 + 优先级 + 每槽设置数量齿轮 + 锁定状态显示
 */
package ae2csl.client.gui;

import ae2.api.client.AEKeyRendering;
import ae2.api.config.FuzzyMode;
import ae2.api.config.LockCraftingMode;
import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.api.stacks.AmountFormat;
import ae2.api.stacks.GenericStack;
import ae2.client.Point;
import ae2.client.gui.ICompositeWidget;
import ae2.client.gui.Icon;
import ae2.client.gui.Tooltip;
import ae2.client.gui.implementations.GuiUpgradeable;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.IconButton;
import ae2.client.gui.widgets.ServerSettingToggleButton;
import ae2.client.gui.widgets.SettingToggleButton;
import ae2.client.gui.widgets.ToggleButton;
import ae2.container.SlotSemantics;
import ae2.core.definitions.AEItems;
import ae2.core.localization.ButtonToolTips;
import ae2.core.localization.GuiText;
import ae2.core.localization.InGameTooltip;
import ae2.core.network.InitNetwork;
import ae2.core.network.serverbound.ConfigValueServerPacket;
import ae2.util.EnumCycler;
import ae2csl.container.ContainerIntegratedInterface;
import ae2csl.me.logic.IntegratedInterfaceHost;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class GuiIntegratedInterface extends GuiUpgradeable<ContainerIntegratedInterface> {

    private static final String STYLE_PATH = "/screens/integrated_interface_menu.json";
    private static final String EXTENDED_STYLE_PATH = "/screens/extended_integrated_interface_menu.json";

    private final SettingToggleButton<FuzzyMode> fuzzyMode;
    private final SettingToggleButton<ae2.api.config.BlockingMode> blockingModeButton;
    private final SettingToggleButton<LockCraftingMode> lockCraftingModeButton;
    private final ToggleButton showInPatternAccessTerminalButton;
    private final LockReason lockReason;

    private final List<SetAmountButton> amountButtons = new ArrayList<>();

    private final PageButton nextPageButton;
    private final PageButton prevPageButton;

    public GuiIntegratedInterface(InventoryPlayer playerInv, IntegratedInterfaceHost host) {
        this(new ContainerIntegratedInterface(playerInv, host), playerInv,
            GuiStyleManager.loadStyleDoc(host.isExtended() ? EXTENDED_STYLE_PATH : STYLE_PATH));
    }

    private GuiIntegratedInterface(ContainerIntegratedInterface container, InventoryPlayer playerInv,
                                   GuiStyle style) {
        super(container, playerInv, style);
        ContainerIntegratedInterface menu = getContainer();

        this.fuzzyMode = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        addToLeftToolbar(this.fuzzyMode);
        this.blockingModeButton = new ServerSettingToggleButton<>(Settings.BLOCKING_MODE,
            ae2.api.config.BlockingMode.NO);
        this.addToLeftToolbar(this.blockingModeButton);
        this.lockCraftingModeButton = new ServerSettingToggleButton<>(Settings.LOCK_CRAFTING_MODE,
            LockCraftingMode.NONE);
        this.addToLeftToolbar(this.lockCraftingModeButton);
        this.showInPatternAccessTerminalButton = new ToggleButton(Icon.PATTERN_ACCESS_SHOW,
            Icon.PATTERN_ACCESS_HIDE,
            GuiText.PatternAccessTerminal.text(), GuiText.PatternAccessTerminalHint.text(),
            this::selectNextPatternProviderMode);
        this.addToLeftToolbar(this.showInPatternAccessTerminalButton);

        this.nextPageButton = new PageButton(Icon.ARROW_RIGHT, () -> menu.onTogglePageButton(1));
        this.nextPageButton.setMessage(GuiText.PatternProviderPageNext.text());
        this.addToLeftToolbar(this.nextPageButton);

        this.prevPageButton = new PageButton(Icon.ARROW_LEFT, () -> menu.onTogglePageButton(-1));
        this.prevPageButton.setMessage(GuiText.PatternProviderPagePrevious.text());
        this.addToLeftToolbar(this.prevPageButton);

        this.lockReason = new LockReason(this);
        widgets.add("lockReason", this.lockReason);

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

    private void selectNextPatternProviderMode(boolean ignored) {
        InitNetwork.sendToServer(new ConfigValueServerPacket(
            getContainer().windowId,
            Settings.PATTERN_ACCESS_TERMINAL,
            EnumCycler.rotateEnum(
                getContainer().getShowInAccessTerminal(),
                isHandlingRightClick(),
                Settings.PATTERN_ACCESS_TERMINAL.getValues())));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        ContainerIntegratedInterface menu = getContainer();

        this.fuzzyMode.set(menu.getFuzzyMode());
        this.fuzzyMode.setVisibility(menu.hasUpgrade(AEItems.FUZZY_CARD.item()));
        this.lockReason.setVisible(menu.getLockCraftingMode() != LockCraftingMode.NONE);
        this.blockingModeButton.set(menu.blockingMode);
        this.lockCraftingModeButton.set(menu.getLockCraftingMode());
        this.showInPatternAccessTerminalButton.setState(menu.getShowInAccessTerminal() == YesNo.YES);

        this.prevPageButton.setVisibility(menu.getPageCount() > 1 && menu.getCurrentPage() > 0);
        this.nextPageButton.setVisibility(
            menu.getPageCount() > 1 && menu.getCurrentPage() + 1 < menu.getPageCount());

        var configSlots = menu.getSlots(SlotSemantics.CONFIG);
        for (int i = 0; i < amountButtons.size(); i++) {
            var button = amountButtons.get(i);
            button.setVisibility(!configSlots.get(i).getStack().isEmpty());
        }
    }

    /**
     * 锁定状态显示（对齐源码 PatternProviderLockReason 内部类）
     */
    private static class LockReason implements ICompositeWidget {

        private boolean visible = false;
        private int x;
        private int y;

        private final GuiIntegratedInterface screen;

        public LockReason(GuiIntegratedInterface screen) {
            this.screen = screen;
        }

        @Override
        public void setPosition(Point position) {
            x = position.x();
            y = position.y();
        }

        @Override
        public void setSize(int width, int height) {
        }

        @Override
        public Rectangle getBounds() {
            return new Rectangle(x, y, 126, 16);
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        @Override
        public void drawForegroundLayer(Rectangle bounds, Point mouse) {
            var menu = screen.getContainer();

            Icon icon;
            String lockStatusText;
            int color;
            if (menu.getCraftingLockedReason() == LockCraftingMode.NONE) {
                icon = Icon.UNLOCKED;
                lockStatusText = GuiText.CraftingLockIsUnlocked.getLocal();
                color = 0x7DA9D2;
            } else {
                icon = Icon.LOCKED;
                lockStatusText = GuiText.CraftingLockIsLocked.getLocal();
                color = 0xC1424B;
            }

            icon.getBlitter().dest(x, y).blit();
            Minecraft.getMinecraft().fontRenderer.drawString(lockStatusText, x + 15, y + 5, color);
        }

        @Nullable
        @Override
        public Tooltip getTooltip(int mouseX, int mouseY) {
            var menu = screen.getContainer();
            LockCraftingMode reason = menu.getCraftingLockedReason();
            return switch (reason) {
                case LOCK_UNTIL_PULSE -> new Tooltip(InGameTooltip.CraftingLockedUntilPulse.text());
                case LOCK_WHILE_HIGH -> new Tooltip(InGameTooltip.CraftingLockedByRedstoneSignal.text());
                case LOCK_WHILE_LOW -> new Tooltip(InGameTooltip.CraftingLockedByLackOfRedstoneSignal.text());
                case LOCK_UNTIL_RESULT -> {
                    GenericStack unlockStack = menu.getUnlockStack();
                    ITextComponent stackName = unlockStack != null
                        ? AEKeyRendering.getDisplayName(unlockStack.what())
                        : GuiText.Error.text();
                    ITextComponent stackAmount = unlockStack != null
                        ? new TextComponentString(
                        unlockStack.what().formatAmount(unlockStack.amount(), AmountFormat.FULL))
                        : GuiText.Error.text();
                    yield new Tooltip(InGameTooltip.CraftingLockedUntilResult.text(stackName, stackAmount));
                }
                case NONE -> null;
            };
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

    private static class PageButton extends IconButton {

        private final Icon icon;

        PageButton(Icon icon, Runnable onPress) {
            super(onPress);
            this.icon = icon;
        }

        @Override
        protected Icon getIcon() {
            return this.icon;
        }
    }
}
