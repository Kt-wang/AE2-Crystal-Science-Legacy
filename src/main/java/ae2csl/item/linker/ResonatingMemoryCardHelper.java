/*
 * AE2 Crystal Science - ResonatingMemoryCardHelper (1.12.2)
 * 对照 1.21.1 common/item/ResonatingMemoryCardHelper。
 *
 * 提供：查找玩家身上「有数据」的谐振记忆卡；把当前槽配置应用到方块/部件。
 */
package ae2csl.item.linker;

import ae2.api.implementations.items.MemoryCardMessages;
import ae2.api.parts.IPart;
import ae2.parts.AEBasePart;
import ae2.tile.AEBaseTile;
import ae2.util.SettingsFrom;

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
