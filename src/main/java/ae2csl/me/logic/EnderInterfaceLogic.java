/*
 * AE2 Crystal Science - EnderInterfaceLogic (1.12.2)
 * 对照 1.21.1 源码 common/me/logic/EnderInterfaceLogic.java
 * 在常规接口上添加掉落物吸收。
 *
 * 1.12.2 适配：源码 extends AE2 的 InterfaceLogic；本 port extends 我们的
 * IntegratedInterfaceLogic（patternSize=0，无样板能力），吸收逻辑通过
 * doExtraWork/hasExtraWork 钩子并入同一个 Ticker（与源码 Ticker 行为一致）。
 */
package ae2csl.me.logic;

import ae2.api.config.Actionable;
import ae2.api.config.Settings;
import ae2.api.networking.IManagedGridNode;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.storage.MEStorage;
import ae2.api.util.IConfigManager;
import ae2.core.definitions.AEItems;
import ae2.util.ConfigInventory;
import ae2csl.api.settings.AE2CSSettings;
import ae2csl.api.settings.BlackListMode;
import ae2csl.api.settings.ShowRangeMode;
import ae2csl.init.AE2CSBlocks;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnderInterfaceLogic extends IntegratedInterfaceLogic {

    private static final int MAX_ABSORB_RANGE = 9;

    private final EnderInterfaceHost enderHost;

    private final ConfigInventory absorbConfigInventory;
    private boolean blackListMode = false; // 黑名单还是白名单？
    private int range = 3; // 吸收半径，切比雪夫距离
    private boolean renderRangeInClient;

    private Set<AEKey> absorbFilterdKeys = new HashSet<>();

    public EnderInterfaceLogic(IManagedGridNode gridNode, EnderInterfaceHost host) {
        this(gridNode, host, 9, 18);
    }

    public EnderInterfaceLogic(IManagedGridNode gridNode, EnderInterfaceHost host, int slots,
                               int absorbConfigSlots) {
        super(gridNode, host, slots, 0);
        this.enderHost = host;

        this.absorbConfigInventory = ConfigInventory.configTypes(absorbConfigSlots)
            .changeListener(this::onAbsorbConfigChange)
            .build();
        this.absorbConfigInventory.useRegisteredCapacities();
    }

    @Override
    protected IConfigManager createConfigManager() {
        // 对齐源码：末影接口只有 FUZZY + SHOW_RANGE + BLACK_LIST（无阻挡/锁定合成/样板终端设置）
        return IConfigManager.builder(this::onConfigChanged)
            .registerSetting(Settings.FUZZY_MODE, ae2.api.config.FuzzyMode.IGNORE_ALL)
            .registerSetting(AE2CSSettings.SHOW_RANGE_MODE, ShowRangeMode.HIDE_RANGE)
            .registerSetting(AE2CSSettings.BLACK_LIST_MODE, BlackListMode.WHITELIST)
            .build();
    }

    @Override
    protected Item getMachineItem() {
        // 注意：构造期间 enderHost 尚未赋值，用父类的 host 字段（父类构造器已先行赋值）
        return Item.getItemFromBlock(
            this.host != null && this.host.isExtended()
                ? AE2CSBlocks.EX_ENDER_INTERFACE_BLOCK
                : AE2CSBlocks.ENDER_INTERFACE_BLOCK);
    }

    public ConfigInventory getAbsorbConfigInventory() {
        return absorbConfigInventory;
    }

    @Override
    protected void onConfigChanged() {
        super.onConfigChanged();
        boolean newRenderRangeInClient = getConfigManager()
            .getSetting(AE2CSSettings.SHOW_RANGE_MODE) == ShowRangeMode.SHOW_RANGE;
        boolean newBlackListMode = getConfigManager()
            .getSetting(AE2CSSettings.BLACK_LIST_MODE) == BlackListMode.BLACKLIST;
        if (this.renderRangeInClient != newRenderRangeInClient) {
            this.renderRangeInClient = newRenderRangeInClient;
            this.enderHost.markForLogicClientUpdate();
        }
        if (this.blackListMode != newBlackListMode) {
            this.blackListMode = newBlackListMode;
            mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
            this.enderHost.saveChanges();
        }
    }

    public boolean isBlackListMode() {
        return blackListMode;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        int newValue = Math.max(1, Math.min(range, MAX_ABSORB_RANGE));
        if (newValue != this.range) {
            this.range = newValue;
            mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
            this.enderHost.saveChanges();
            this.enderHost.markForLogicClientUpdate();
        }
    }

    public boolean isRenderRangeInClient() {
        return renderRangeInClient;
    }

    public void setRenderRangeInClient(boolean renderRangeInClient) {
        this.renderRangeInClient = renderRangeInClient;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        absorbConfigInventory.writeToChildTag(tag, "absorb_config");
        tag.setInteger("absorbRange", range);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        absorbConfigInventory.readFromChildTag(tag, "absorb_config");
        if (tag.hasKey("absorbRange")) {
            range = tag.getInteger("absorbRange");
        }
        this.onConfigChanged();
        onAbsorbConfigChange();
    }

    private void onAbsorbConfigChange() {
        this.enderHost.saveChanges();
        mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
        this.absorbFilterdKeys = absorbConfigInventory.keySet();
    }

    private boolean hasAbsorbWork() {
        if (blackListMode) {
            return true;
        } else {
            return !absorbConfigInventory.isEmpty();
        }
    }

    @Override
    protected boolean hasExtraWork() {
        return hasAbsorbWork();
    }

    @Override
    protected boolean doExtraWork() {
        return doAbsorbWork();
    }

    private boolean doAbsorbWork() {
        if (!(this.host.getTileEntity().getWorld() instanceof WorldServer level)) {
            return false;
        }
        MEStorage storage = this.getExposedMEStorage(null);
        if (storage == null) {
            return false;
        }

        BlockPos centerPos = this.host.getTileEntity().getPos();
        AxisAlignedBB absorbAABB = new AxisAlignedBB(
            centerPos.getX() - range,
            centerPos.getY() - range,
            centerPos.getZ() - range,
            centerPos.getX() + range,
            centerPos.getY() + range,
            centerPos.getZ() + range);
        List<EntityItem> itemEntities = level.getEntitiesWithinAABB(EntityItem.class, absorbAABB);
        boolean hasAbsorbed = false;
        for (EntityItem itemEntity : itemEntities) {
            AEItemKey itemKey = AEItemKey.of(itemEntity.getItem());
            if (itemKey == null) {
                continue;
            }
            int count = itemEntity.getItem().getCount();
            if (!validAbsorbThing(itemKey)) {
                continue;
            }

            if (storage.insert(itemKey, count, Actionable.SIMULATE, actionSource) == count) {
                storage.insert(itemKey, count, Actionable.MODULATE, actionSource);
                itemEntity.setDead();
                hasAbsorbed = true;
            }
        }
        return hasAbsorbed;
    }

    private boolean validAbsorbThing(AEKey key) {
        boolean match;
        if (getUpgrades().isInstalled(AEItems.FUZZY_CARD.item())) {
            match = isInAbsorbFilter(key);
        } else {
            match = absorbFilterdKeys.contains(key);
        }

        return blackListMode != match;
    }

    private boolean absorbKeyMatches(AEKey configured, AEKey candidate) {
        if (getUpgrades().isInstalled(AEItems.FUZZY_CARD.item()) && configured.supportsFuzzyRangeSearch()) {
            var fuzzyMode = getConfigManager().getSetting(Settings.FUZZY_MODE);
            return configured.fuzzyEquals(candidate, fuzzyMode);
        }
        return configured.equals(candidate);
    }

    /**
     * 模糊卡专用匹配
     */
    private boolean isInAbsorbFilter(AEKey key) {
        for (var configured : absorbFilterdKeys) {
            if (absorbKeyMatches(configured, key)) {
                return true;
            }
        }
        return false;
    }
}
