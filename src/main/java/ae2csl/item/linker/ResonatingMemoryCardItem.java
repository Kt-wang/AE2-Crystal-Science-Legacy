/*
 * AE2 Crystal Science - ResonatingMemoryCardItem (1.12.2)
 * 对照 1.21.1：extends AE2 的记忆卡（1.12.2 为 ae2.items.tools.MemoryCardItem），
 * 从而获得「右键机器保存 / 右键机器应用 / 潜行绕过方块」的标准记忆卡语义；
 * 额外提供 9 个槽位：
 *  - 潜行 + 滚轮：切换槽位（复用 ScrollResonatingPatternSelectPacket）；
 *  - 潜行右键机器：保存到当前槽；右键机器：应用当前槽
 *    （AE2 机器走 useMemoryCard mixin，本模组机器走 ResonatingMemoryCardHelper.tryUseOnAecsTile）；
 *  - 潜行右键空气/非机器方块：清空当前槽；
 *  - 放置谐振样板供应器时自动应用当前槽。
 */
package ae2csl.item.linker;

import ae2.api.implementations.items.MemoryCardMessages;
import ae2.api.parts.IPartHost;
import ae2.items.tools.MemoryCardItem;
import ae2.tile.AEBaseTile;
import ae2csl.item.IResonatingTargetModeItem;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ResonatingMemoryCardItem extends MemoryCardItem implements IResonatingTargetModeItem {

    public static final int SLOT_COUNT = ResonatingMemoryCardStorage.SLOT_COUNT;

    private static final String TAG_STORAGE = "ae2csl_memory_storage";

    public ResonatingMemoryCardItem() {
        setTranslationKey("resonating_memory_card");
    }

    // ------------------------------------------------------------------
    // 9 槽存储
    // ------------------------------------------------------------------

    public ResonatingMemoryCardStorage getStorage(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return ResonatingMemoryCardStorage.EMPTY;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_STORAGE)) {
            return ResonatingMemoryCardStorage.EMPTY;
        }
        return ResonatingMemoryCardStorage.readFromNBT(tag.getCompoundTag(TAG_STORAGE));
    }

    public void setStorage(ItemStack stack, ResonatingMemoryCardStorage storage) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setTag(TAG_STORAGE, storage.writeToNBT());
    }

    /** 当前槽位的设置数据（无数据时返回空 NBT） */
    public NBTTagCompound getData(ItemStack stack) {
        return getStorage(stack).getSelected().getData();
    }

    public boolean hasSelectedData(ItemStack stack) {
        return !getStorage(stack).getSelected().isEmpty();
    }

    public int getSelectedSlot(ItemStack stack) {
        return getStorage(stack).getSelectedSlot();
    }

    public String getSelectedSlotName(ItemStack stack) {
        return getStorage(stack).getSelected().getSettingsName();
    }

    public int getStoredSlotCount(ItemStack stack) {
        int count = 0;
        for (ResonatingMemoryCardStorage.Slot slot : getStorage(stack).getSlots()) {
            if (!slot.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public void storeSelected(ItemStack stack, String settingsName, NBTTagCompound data) {
        setStorage(stack, getStorage(stack).withSelectedSlotData(settingsName, data));
    }

    public void clearSelectedSlot(ItemStack stack) {
        setStorage(stack, getStorage(stack).clearSelectedSlot());
    }

    /** 供快捷栏查找使用（保留旧 API） */
    public static int getSelectedSlotStatic(ItemStack stack) {
        if (stack.getItem() instanceof ResonatingMemoryCardItem card) {
            return card.getSelectedSlot(stack);
        }
        return 0;
    }

    public static String getSelectedSlotNameStatic(ItemStack stack) {
        if (stack.getItem() instanceof ResonatingMemoryCardItem card) {
            return card.getSelectedSlotName(stack);
        }
        return "";
    }

    // ------------------------------------------------------------------
    // 交互：滚轮换槽 / 潜行清槽
    // ------------------------------------------------------------------

    @Override
    public void scrollSelectedInputAndToast(EntityPlayer player, ItemStack stack, boolean next) {
        int selected = Math.floorMod(getSelectedSlot(stack) + (next ? 1 : -1), SLOT_COUNT);
        setStorage(stack, getStorage(stack).withSelectedSlot(selected));
        player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_memory_card.slot",
            selected + 1, SLOT_COUNT, getSelectedSlotName(stack)));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (player.isSneaking()) {
            ItemStack stack = player.getHeldItem(hand);
            if (!world.isRemote) {
                clearSelectedSlot(stack);
                notifyUser(player, MemoryCardMessages.SETTINGS_CLEARED);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                           float hitX, float hitY, float hitZ, EnumHand hand) {
        if (player == null || hand != EnumHand.MAIN_HAND || !player.isSneaking()) {
            return EnumActionResult.PASS;
        }
        // AE 机器/线缆总线交给 block / part 的 useMemoryCard（mixin 已接入 9 槽）
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof AEBaseTile || tile instanceof IPartHost) {
            return EnumActionResult.PASS;
        }

        // 本模组自己的机器：存/读侧配置 + 红石设置 + 升级卡
        ItemStack stack = player.getHeldItem(hand);
        if (ResonatingMemoryCardHelper.tryUseOnAecsTile(world, pos, stack, player)) {
            return EnumActionResult.SUCCESS;
        }

        if (!world.isRemote) {
            clearSelectedSlot(stack);
            notifyUser(player, MemoryCardMessages.SETTINGS_CLEARED);
        }
        return EnumActionResult.SUCCESS;
    }

    // ------------------------------------------------------------------
    // Tooltip
    // ------------------------------------------------------------------

    @SideOnly(Side.CLIENT)
    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines,
                                         ITooltipFlag advancedTooltips) {
        int selected = getSelectedSlot(stack);
        String name = getSelectedSlotName(stack);
        lines.add(I18n.format("ae2csl.item.resonating_memory_card.slot", selected + 1, SLOT_COUNT));
        lines.add(I18n.format("ae2csl.item.resonating_memory_card.target",
            name.isEmpty() ? I18n.format("ae2csl.item.resonating_memory_card.empty") : name));
        lines.add(I18n.format("ae2csl.item.resonating_memory_card.stored", getStoredSlotCount(stack), SLOT_COUNT));
        lines.add("§7" + I18n.format("ae2csl.item.resonating_memory_card.usage"));
    }
}
