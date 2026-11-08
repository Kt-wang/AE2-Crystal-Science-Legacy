/*
 * AE2 Crystal Science - ResonatingMemoryCardHelper (1.12.2)
 * 对照 1.21.1 common/item/ResonatingMemoryCardHelper。
 *
 * 提供：查找玩家身上「有数据」的谐振记忆卡；把当前槽配置应用到方块/部件。
 */
package ae2csl.item.linker;

import ae2.api.implementations.items.MemoryCardMessages;
import ae2.api.parts.IPart;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.util.IConfigurableObject;
import ae2.api.util.IConfigManager;
import ae2.parts.AEBasePart;
import ae2.tile.AEBaseTile;
import ae2.util.SettingsFrom;

import ae2csl.machine.ISideConfigHost;
import ae2csl.machine.IUpgradeSlotsHost;
import ae2csl.machine.SideConfigData;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class ResonatingMemoryCardHelper {

    private ResonatingMemoryCardHelper() {}

    @Nullable
    public static ItemStack findActiveCard(EntityPlayer player) {
        if (player == null) {
            return null;
        }
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof ResonatingMemoryCardItem card && card.hasSelectedData(stack)) {
                return stack;
            }
        }
        return null;
    }

    /** 该方块实体是否是本模组「可复制设置」的机器 */
    public static boolean isAecsConfigurable(TileEntity tile) {
        return tile instanceof ISideConfigHost
            || tile instanceof IConfigurableObject
            || tile instanceof IUpgradeableObject
            || tile instanceof IUpgradeSlotsHost;
    }

    /**
     * 导出本模组机器的设置：侧配置 + 配置管理器（红石等）+ 升级卡。
     * 对齐 AE2 记忆卡的 exportSettings 语义。
     */
    public static NBTTagCompound exportAecsSettings(TileEntity tile) {
        NBTTagCompound out = new NBTTagCompound();
        if (tile instanceof ISideConfigHost sideHost && sideHost.getSideConfig() != null) {
            NBTTagCompound side = new NBTTagCompound();
            sideHost.getSideConfig().writeToNBT(side);
            out.setTag("side_config", side);
        }
        if (tile instanceof IConfigurableObject configurable && configurable.getConfigManager() != null) {
            NBTTagCompound cfg = new NBTTagCompound();
            for (java.util.Map.Entry<String, String> e : configurable.getConfigManager().exportSettings().entrySet()) {
                cfg.setString(e.getKey(), e.getValue());
            }
            out.setTag("config_manager", cfg);
        }
        NBTTagCompound upgrades = exportUpgrades(tile);
        if (upgrades != null) {
            out.setTag("upgrades", upgrades);
        }
        return out;
    }

    /** 导入本模组机器的设置 */
    public static void importAecsSettings(TileEntity tile, NBTTagCompound data, EntityPlayer player) {
        if (data == null) {
            return;
        }
        if (tile instanceof ISideConfigHost sideHost && sideHost.getSideConfig() != null
            && data.hasKey("side_config")) {
            sideHost.getSideConfig().readFromNBT(data.getCompoundTag("side_config"));
        }
        if (tile instanceof IConfigurableObject configurable && configurable.getConfigManager() != null
            && data.hasKey("config_manager")) {
            IConfigManager manager = configurable.getConfigManager();
            NBTTagCompound cfg = data.getCompoundTag("config_manager");
            java.util.Map<String, String> values = new java.util.HashMap<>();
            for (String key : cfg.getKeySet()) {
                values.put(key, cfg.getString(key));
            }
            manager.importSettings(values);
        }
        if (data.hasKey("upgrades")) {
            importUpgrades(tile, data.getCompoundTag("upgrades"));
        }
        tile.markDirty();
        if (tile.getWorld() != null) {
            tile.getWorld().notifyBlockUpdate(tile.getPos(),
                tile.getWorld().getBlockState(tile.getPos()),
                tile.getWorld().getBlockState(tile.getPos()), 3);
        }
    }

    @Nullable
    private static NBTTagCompound exportUpgrades(TileEntity tile) {
        if (tile instanceof IUpgradeableObject upgradeable) {
            IUpgradeInventory inv = upgradeable.getUpgrades();
            if (inv == null) {
                return null;
            }
            NBTTagCompound tag = new NBTTagCompound();
            int i = 0;
            for (net.minecraft.item.ItemStack stack : inv) {
                if (!stack.isEmpty()) {
                    NBTTagCompound entry = new NBTTagCompound();
                    stack.writeToNBT(entry);
                    tag.setTag("s" + i, entry);
                }
                i++;
            }
            tag.setInteger("count", i);
            return tag;
        }
        if (tile instanceof IUpgradeSlotsHost slotsHost && tile instanceof net.minecraft.inventory.IInventory inv) {
            int start = slotsHost.getUpgradeSlotStart();
            if (start < 0) {
                return null;
            }
            NBTTagCompound tag = new NBTTagCompound();
            int count = 0;
            for (int i = start; i < inv.getSizeInventory(); i++) {
                net.minecraft.item.ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    NBTTagCompound entry = new NBTTagCompound();
                    stack.writeToNBT(entry);
                    tag.setTag("s" + count, entry);
                }
                count++;
            }
            tag.setInteger("count", count);
            return tag;
        }
        return null;
    }

    private static void importUpgrades(TileEntity tile, NBTTagCompound tag) {
        int count = tag.getInteger("count");
        if (tile instanceof IUpgradeableObject upgradeable) {
            IUpgradeInventory inv = upgradeable.getUpgrades();
            if (inv == null) {
                return;
            }
            for (int i = 0; i < Math.min(count, inv.size()); i++) {
                inv.setItemDirect(i, readEntry(tag, i));
            }
            return;
        }
        if (tile instanceof IUpgradeSlotsHost slotsHost && tile instanceof net.minecraft.inventory.IInventory inv) {
            int start = slotsHost.getUpgradeSlotStart();
            if (start < 0) {
                return;
            }
            for (int i = 0; i < count && start + i < inv.getSizeInventory(); i++) {
                inv.setInventorySlotContents(start + i, readEntry(tag, i));
            }
        }
    }

    private static net.minecraft.item.ItemStack readEntry(NBTTagCompound tag, int index) {
        String key = "s" + index;
        if (!tag.hasKey(key, 10)) {
            return net.minecraft.item.ItemStack.EMPTY;
        }
        return new net.minecraft.item.ItemStack(tag.getCompoundTag(key));
    }

    /**
     * 本模组机器的记忆卡交互：潜行=保存到当前槽，否则=应用当前槽。
     * 返回 true 表示已消费该次点击。
     */
    public static boolean tryUseOnAecsTile(World world, BlockPos pos, ItemStack cardStack, EntityPlayer player) {
        if (cardStack.isEmpty() || player == null || !(cardStack.getItem() instanceof ResonatingMemoryCardItem card)) {
            return false;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null || !isAecsConfigurable(tile)) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }

        if (player.isSneaking()) {
            NBTTagCompound data = exportAecsSettings(tile);
            String name = world.getBlockState(pos).getBlock().getLocalizedName();
            card.storeSelected(cardStack, name, data);
            card.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
        } else {
            NBTTagCompound data = card.getData(cardStack);
            if (data == null || data.isEmpty()) {
                card.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
            } else {
                importAecsSettings(tile, data, player);
                card.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
            }
        }
        return true;
    }

    /**
     * 把快捷栏中「有数据」的谐振记忆卡当前槽应用到方块（放置时自动恢复配置）。
     */
    public static void tryApplyToBlockEntity(EntityPlayer player, AEBaseTile tile) {
        ItemStack card = findActiveCard(player);
        if (card == null) {
            return;
        }

        ResonatingMemoryCardItem cardItem = (ResonatingMemoryCardItem) card.getItem();
        NBTTagCompound data = cardItem.getData(card);
        if (data == null || data.isEmpty()) {
            return;
        }

        tile.importSettings(SettingsFrom.MEMORY_CARD, data, player);
        player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_memory_card.auto_apply",
            cardItem.getSelectedSlot(card) + 1,
            ResonatingMemoryCardItem.SLOT_COUNT,
            cardItem.getSelectedSlotName(card)));
    }

    /**
     * 右键方块时的谐振记忆卡交互（保存 / 应用）。返回 true 表示已消费该次点击。
     * 供 AEBaseTileBlock mixin 与「非 AEBaseTileBlock 的自建方块」共用。
     */
    public static boolean tryUseOnBlock(World world, BlockPos pos, ItemStack cardStack, EntityPlayer player) {
        if (cardStack.isEmpty() || player == null || !(cardStack.getItem() instanceof ResonatingMemoryCardItem card)) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof AEBaseTile baseTile)) {
            return false;
        }

        if (player.isSneaking()) {
            NBTTagCompound data = baseTile.exportSettings(SettingsFrom.MEMORY_CARD);
            card.storeSelected(cardStack, world.getBlockState(pos).getBlock().getLocalizedName(), data);
            card.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
        } else {
            NBTTagCompound data = card.getData(cardStack);
            if (data == null || data.isEmpty()) {
                card.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
            } else {
                baseTile.importSettings(SettingsFrom.MEMORY_CARD, data, player);
                card.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
            }
        }
        return true;
    }

    /** 右键部件时的谐振记忆卡交互。 */
    public static boolean tryUseOnPart(AEBasePart part, ItemStack cardStack, EntityPlayer player) {
        if (cardStack.isEmpty() || player == null || !(cardStack.getItem() instanceof ResonatingMemoryCardItem card)) {
            return false;
        }

        World world = part.getTileEntity() == null ? null : part.getTileEntity().getWorld();
        if (world == null || world.isRemote) {
            return true;
        }

        if (player.isSneaking()) {
            NBTTagCompound data = part.exportSettings(SettingsFrom.MEMORY_CARD);
            String name = new ItemStack((Item) part.getPartItem()).getDisplayName();
            card.storeSelected(cardStack, name, data);
            card.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
        } else {
            NBTTagCompound data = card.getData(cardStack);
            if (data == null || data.isEmpty()) {
                card.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
            } else {
                part.importSettings(SettingsFrom.MEMORY_CARD, data, player);
                card.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
            }
        }
        return true;
    }

    public static void tryApplyToPart(EntityPlayer player, IPart part) {
        ItemStack card = findActiveCard(player);
        if (card == null) {
            return;
        }

        ResonatingMemoryCardItem cardItem = (ResonatingMemoryCardItem) card.getItem();
        NBTTagCompound data = cardItem.getData(card);
        if (data == null || data.isEmpty()) {
            return;
        }

        part.importSettings(SettingsFrom.MEMORY_CARD, data, player);
        player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_memory_card.auto_apply",
            cardItem.getSelectedSlot(card) + 1,
            ResonatingMemoryCardItem.SLOT_COUNT,
            cardItem.getSelectedSlotName(card)));
    }
}
