/*
 * AE2 Crystal Science - GuiMeteoritePatternProvider (1.12.2)
 * 自装配式（陨石）样板供应器 GUI，176×294 单页 63 槽（源码布局）。
 * 基于 fork GuiUpgradeable + 源码样式 JSON；不继承 fork GuiPatternProvider
 * （其分页重定位与私有槽位状态机不适配 63 槽单页）。
 */
package ae2csl.client.gui;

import ae2.api.client.AEKeyRendering;
import ae2.api.config.BlockingMode;
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
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.ServerSettingToggleButton;
import ae2.client.gui.widgets.SettingToggleButton;
import ae2.client.gui.widgets.ToggleButton;
import ae2.core.localization.GuiText;
import ae2.core.localization.InGameTooltip;
import ae2.core.network.InitNetwork;
import ae2.core.network.serverbound.ConfigValueServerPacket;
import ae2.util.EnumCycler;
import ae2csl.container.ContainerMeteoritePatternProvider;
import ae2csl.me.logic.MeteoritePatternProviderHost;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.awt.Rectangle;

public class GuiMeteoritePatternProvider extends GuiUpgradeable<ContainerMeteoritePatternProvider> {

    private static final String STYLE_PATH = "/screens/meteorite_pattern_provider_menu.json";

    private final SettingToggleButton<BlockingMode> blockingModeButton;
    private final SettingToggleButton<LockCraftingMode> lockCraftingModeButton;
    private final ToggleButton showInPatternAccessTerminalButton;
    private final LockReason lockReason;

    public GuiMeteoritePatternProvider(InventoryPlayer playerInv, MeteoritePatternProviderHost host) {
        this(new ContainerMeteoritePatternProvider(playerInv, host), playerInv);
    }

    private GuiMeteoritePatternProvider(ContainerMeteoritePatternProvider container,
                                        InventoryPlayer playerInv) {
        super(container, playerInv, GuiStyleManager.loadStyleDoc(STYLE_PATH));

        this.blockingModeButton = addToLeftToolbar(
            new ServerSettingToggleButton<>(Settings.BLOCKING_MODE, BlockingMode.NO));
        this.lockCraftingModeButton = addToLeftToolbar(
            new ServerSettingToggleButton<>(Settings.LOCK_CRAFTING_MODE, LockCraftingMode.NONE));
        this.showInPatternAccessTerminalButton = new ToggleButton(Icon.PATTERN_ACCESS_SHOW,
            Icon.PATTERN_ACCESS_HIDE,
            GuiText.PatternAccessTerminal.text(), GuiText.PatternAccessTerminalHint.text(),
            this::selectPatternProviderMode);
        addToLeftToolbar(this.showInPatternAccessTerminalButton);

        this.lockReason = new LockReason(this);
        widgets.add("lockReason", this.lockReason);

        widgets.addOpenPriorityButton();
    }

    private void selectPatternProviderMode(boolean ignored) {
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
        ContainerMeteoritePatternProvider menu = getContainer();

        this.blockingModeButton.set(menu.blockingMode);
        this.lockCraftingModeButton.set(menu.getLockCraftingMode());
        this.showInPatternAccessTerminalButton.setState(menu.getShowInAccessTerminal() == YesNo.YES);
        this.lockReason.setVisible(menu.getLockCraftingMode() != LockCraftingMode.NONE);
    }

    /**
     * 锁定状态显示（与集成接口 GUI 同款）
     */
    private static class LockReason implements ICompositeWidget {

        private boolean visible = false;
        private int x;
        private int y;
        private final GuiMeteoritePatternProvider screen;

        public LockReason(GuiMeteoritePatternProvider screen) {
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
            String text;
            int color;
            if (menu.getCraftingLockedReason() == LockCraftingMode.NONE) {
                icon = Icon.UNLOCKED;
                text = GuiText.CraftingLockIsUnlocked.getLocal();
                color = 0x7DA9D2;
            } else {
                icon = Icon.LOCKED;
                text = GuiText.CraftingLockIsLocked.getLocal();
                color = 0xC1424B;
            }

            icon.getBlitter().dest(x, y).blit();
            Minecraft.getMinecraft().fontRenderer.drawString(text, x + 15, y + 5, color);
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
}
