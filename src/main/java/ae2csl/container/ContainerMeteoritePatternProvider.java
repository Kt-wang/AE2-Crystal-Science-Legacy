/*
 * AE2 Crystal Science - ContainerMeteoritePatternProvider (1.12.2)
 * 自装配式（陨石）样板供应器容器。
 * 不继承 fork 的 ContainerPatternProvider——其 36槽/页分页会禁用 36 号以后的槽位
 * （私有 updatePatternSlotState 在同步前执行，无法拦截），而源码是 63 槽 7×9 单页。
 * 63 样板槽 + 9 返回仓 + 升级槽（4 速度卡）。
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
import ae2csl.me.logic.MeteoritePatternProviderHost;

import net.minecraft.entity.player.InventoryPlayer;

public class ContainerMeteoritePatternProvider extends UpgradeableContainer<MeteoritePatternProviderHost> {

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

    public ContainerMeteoritePatternProvider(InventoryPlayer ip, MeteoritePatternProviderHost host) {
        super(ip, host);

        // 63 样板槽（过滤器在逻辑层 PatternInventoryAllFilter：全收）
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
