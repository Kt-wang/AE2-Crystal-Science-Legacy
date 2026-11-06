/*
 * AE2 Crystal Science - ContainerResonatingPatternProvider (1.12.2)
 * 对照 1.21.1 源码 common/menu/ResonatingPatternProviderMenu.java
 * 谐振样板槽 + 返回仓 + 升级槽 + 拉动模式同步。
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
import ae2.container.slot.RestrictedInputSlot;
import ae2csl.api.settings.AE2CSSettings;
import ae2csl.api.settings.PullMode;
import ae2csl.me.logic.ResonatingPatternProviderHost;

import net.minecraft.entity.player.InventoryPlayer;

public class ContainerResonatingPatternProvider extends UpgradeableContainer<ResonatingPatternProviderHost> {

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
    public PullMode pullMode = PullMode.PULL_OFF;

    public final boolean extended;

    public ContainerResonatingPatternProvider(InventoryPlayer ip, ResonatingPatternProviderHost host) {
        super(ip, host);
        this.extended = host.isExtended();

        // 样板槽
        InternalInventory patternInv = host.getLogic().getPatternInv();
        for (int i = 0; i < patternInv.size(); i++) {
            this.addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.PROVIDER_PATTERN,
                patternInv, i), SlotSemantics.ENCODED_PATTERN);
        }

        // 返回仓
        InternalInventory returnInv = host.getLogic().getReturnInv().createGuiWrapper();
        for (int i = 0; i < returnInv.size(); i++) {
            this.addSlot(new AppEngSlot(returnInv, i), SlotSemantics.STORAGE);
        }
    }

    @Override
    protected int getPlayerInventoryTop() {
        return this.extended ? 148 : 133;
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        if (cm.hasSetting(Settings.BLOCKING_MODE)) {
            this.blockingMode = cm.getSetting(Settings.BLOCKING_MODE);
        }
        if (cm.hasSetting(Settings.PATTERN_ACCESS_TERMINAL)) {
            this.showInAccessTerminal = cm.getSetting(Settings.PATTERN_ACCESS_TERMINAL);
        }
        if (cm.hasSetting(Settings.LOCK_CRAFTING_MODE)) {
            this.lockCraftingMode = cm.getSetting(Settings.LOCK_CRAFTING_MODE);
        }
        if (cm.hasSetting(AE2CSSettings.PULL_MODE)) {
            this.pullMode = cm.getSetting(AE2CSSettings.PULL_MODE);
        }
        this.craftingLockedReason = getHost().getLogic().getCraftingLockedReason();
        this.unlockStack = getHost().getLogic().getUnlockStack();
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
}
