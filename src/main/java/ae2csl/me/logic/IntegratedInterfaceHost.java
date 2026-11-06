/*
 * AE2 Crystal Science - IntegratedInterfaceHost (1.12.2)
 * Ported from 1.21.1 common/me/logic/IntegratedInterfaceHost.java
 * 持有者可以是方块实体(TileIntegratedInterface)，也可以是线缆部件(IntegratedInterfacePart)。
 */
package ae2csl.me.logic;

import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.inventories.InternalInventory;
import ae2.api.networking.IGrid;
import ae2.api.stacks.AEItemKey;
import ae2.api.storage.ISubGuiHost;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.container.ISubGui;
import ae2.helpers.IPriorityHost;
import ae2.helpers.patternprovider.PatternContainer;
import ae2.util.ConfigInventory;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.EnumSet;

public interface IntegratedInterfaceHost extends IConfigurableObject, IPriorityHost, IUpgradeableObject,
    PatternContainer, ISubGuiHost {

    IntegratedInterfaceLogic getLogic();

    boolean isExtended();

    /**
     * @return 此持有者在游戏中的方块实体
     */
    TileEntity getTileEntity();

    /**
     * 样板供应的目标面
     */
    EnumSet<EnumFacing> getTargets();

    /**
     * 获取配置槽
     */
    default ConfigInventory getConfigInv() {
        return getLogic().getConfigInv();
    }

    /**
     * 获取存储槽
     */
    default ConfigInventory getStorageInv() {
        return getLogic().getStorageInv();
    }

    /**
     * 标脏
     */
    void saveChanges();

    // IConfigurableObject

    @Override
    default IConfigManager getConfigManager() {
        return getLogic().getConfigManager();
    }

    // IUpgradeableObject

    @Override
    default IUpgradeInventory getUpgrades() {
        return getLogic().getUpgrades();
    }

    // PatternContainer

    /**
     * 获取当前连接的网络
     */
    @Override
    default @Nullable IGrid getGrid() {
        return getLogic().getGrid();
    }

    /**
     * 在样板管理器中显示的icon
     */
    AEItemKey getTerminalIcon();

    /**
     * 主菜单图标（1.12.2 用 ItemStack）
     */
    ItemStack getMainMenuIcon();

    // ISubGuiHost

    @Override
    default ItemStack getMainContainerIcon() {
        return getMainMenuIcon();
    }

    /**
     * 子界面返回主界面（方块形态重开主GUI；部件形态在阶段C处理）
     */
    @Override
    default void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        TileEntity tile = getTileEntity();
        player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_INTEGRATED_INTERFACE, tile.getWorld(),
            tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
    }

    /**
     * 是否显示在样板管理器
     */
    @Override
    default boolean isVisibleInTerminal() {
        return getConfigManager().getSetting(Settings.PATTERN_ACCESS_TERMINAL) == YesNo.YES;
    }

    /**
     * 获取样板槽
     */
    @Override
    default InternalInventory getTerminalPatternInventory() {
        return getLogic().getPatternInventory();
    }

    /**
     * 获取样板在终端中的显示顺序
     */
    @Override
    default long getTerminalSortOrder() {
        BlockPos pos = getTileEntity().getPos();
        return (long) pos.getZ() << 24 ^ (long) pos.getX() << 8 ^ pos.getY();
    }

    /**
     * 获取样板在终端中分类的显示组
     */
    @Override
    default PatternContainerGroup getTerminalGroup() {
        return getLogic().getTerminalGroup();
    }

    /**
     * 样板终端用：是否包含指定已编码样板
     */
    @Override
    default boolean containsPattern(AEItemKey pattern) {
        return getLogic().containsPattern(pattern);
    }
}
