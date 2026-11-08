/*
 * AE2 Crystal Science - ResonatingLinkerItem (1.12.2)
 * 对照 1.21.1 common/item/ResonatingLinkerItem。
 *
 *  - 右键谐振样板供应器 → 绑定（记录维度+坐标+部件面）；
 *  - 绑定后右键任意方块面 → 就地编辑「该供应器当前默认输入槽」的目标；
 *  - 潜行 + 滚轮 → 切换该供应器的默认输入槽并提示；
 *  - 潜行 + 右键 → 清除链接器记住的供应器绑定；
 *  - 未绑定时滚轮只提示先绑定。
 */
package ae2csl.item.linker;

import ae2csl.api.util.GlobalPos;
import ae2csl.item.IResonatingTargetModeItem;
import ae2csl.item.PatternProviderBindingHelper;
import ae2csl.me.crafting.EncodedResonatingPattern;
import ae2csl.me.crafting.ResonatingProviderDefaults;
import ae2csl.me.logic.ResonatingPatternProviderHost;
import ae2csl.me.logic.ResonatingPatternProviderReference;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ResonatingLinkerItem extends Item implements IResonatingTargetModeItem {

    public ResonatingLinkerItem() {
        setTranslationKey("resonating_linker");
        setMaxStackSize(1);
    }

    // ------------------------------------------------------------------
    // 绑定 / 就地编辑默认目标
    // ------------------------------------------------------------------

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                           float hitX, float hitY, float hitZ, EnumHand hand) {
        if (player == null || hand != EnumHand.MAIN_HAND) {
            return EnumActionResult.PASS;
        }

        ItemStack stack = player.getHeldItem(hand);
        Vec3d clickLocation = new Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ);

        // 0) 潜行 + 右键 → 清除链接器当前绑定
        if (player.isSneaking()) {
            if (!world.isRemote) {
                boolean hadBinding = readReference(stack) != null;
                clearReference(stack);
                player.sendMessage(new TextComponentTranslation(hadBinding
                    ? "ae2csl.msg.resonating_linker.unbound"
                    : "ae2csl.msg.resonating_linker.no_provider"));
            }
            return EnumActionResult.SUCCESS;
        }

        // 1) 点击谐振供应器 → 绑定
        ResonatingPatternProviderHost clicked =
            PatternProviderBindingHelper.resolveClickedResonatingProvider(world, pos, side, clickLocation);
        if (clicked != null) {
            if (world.isRemote) {
                return EnumActionResult.SUCCESS;
            }
            EnumFacing partSide = PatternProviderBindingHelper.resolvePartSide(world, pos, side, clickLocation);
            writeReference(stack, new ResonatingPatternProviderReference(
                new GlobalPos(world.provider.getDimension(), pos), partSide));
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_linker.bound_provider"));
            return EnumActionResult.SUCCESS;
        }

        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        // 2) 已绑定 → 编辑该供应器的默认目标
        ResonatingPatternProviderReference reference = readReference(stack);
        if (reference == null) {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_linker.no_provider"));
            return EnumActionResult.SUCCESS;
        }

        ResonatingPatternProviderHost host = reference.resolve(world);
        if (host == null) {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_linker.provider_unavailable"));
            return EnumActionResult.SUCCESS;
        }

        int selected = host.getDefaultSelectedInput();
        EncodedResonatingPattern.Target target = new EncodedResonatingPattern.Target(
            new GlobalPos(world.provider.getDimension(), pos), side);
        Optional<EncodedResonatingPattern.Target> current = host.getDefaultInputTargets().get(selected);

        boolean sameTarget = current.isPresent()
            && current.get().getPos().equals(target.getPos())
            && current.get().getFace() == target.getFace();

        if (sameTarget) {
            host.setDefaultInputTarget(selected, Optional.empty());
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_provider.unmarked",
                selected + 1, ResonatingProviderDefaults.DEFAULT_INPUT_SLOTS));
        } else {
            host.setDefaultInputTarget(selected, Optional.of(target));
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_provider.marked",
                selected + 1, ResonatingProviderDefaults.DEFAULT_INPUT_SLOTS,
                pos.getX(), pos.getY(), pos.getZ(), side.getName()));
        }
        host.markForLogicClientUpdate();
        return EnumActionResult.SUCCESS;
    }

    // ------------------------------------------------------------------
    // 滚轮切换默认输入槽
    // ------------------------------------------------------------------

    @Override
    public void scrollSelectedInputAndToast(EntityPlayer player, ItemStack stack, boolean next) {
        ResonatingPatternProviderReference reference = readReference(stack);
        if (reference != null && player != null && !player.world.isRemote) {
            ResonatingPatternProviderHost host = reference.resolve(player.world);
            if (host == null) {
                player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_linker.provider_unavailable"));
                return;
            }

            int selected = Math.floorMod(host.getDefaultSelectedInput() + (next ? 1 : -1),
                ResonatingProviderDefaults.DEFAULT_INPUT_SLOTS);
            host.setDefaultSelectedInput(selected);
            host.markForLogicClientUpdate();

            Optional<EncodedResonatingPattern.Target> target = host.getDefaultInputTargets().get(selected);
            if (target.isPresent()) {
                BlockPos pos = target.get().getPos().getPos();
                player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_provider.selected_marked",
                    selected + 1, ResonatingProviderDefaults.DEFAULT_INPUT_SLOTS,
                    pos.getX(), pos.getY(), pos.getZ(), target.get().getFace().getName()));
            } else {
                player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_provider.selected_unmarked",
                    selected + 1, ResonatingProviderDefaults.DEFAULT_INPUT_SLOTS));
            }
            return;
        }

        // 未绑定：提示先绑定（对齐源码；不再回退到物品自身的默认目标）
        if (player != null) {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_linker.no_provider"));
        }
    }

    // ------------------------------------------------------------------
    // 绑定引用存取
    // ------------------------------------------------------------------

    private static void writeReference(ItemStack stack, ResonatingPatternProviderReference reference) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setTag(ResonatingPatternProviderReference.TAG_REFERENCE, reference.write());
    }

    private static void clearReference(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag(ResonatingPatternProviderReference.TAG_REFERENCE);
        }
    }

    @Nullable
    private static ResonatingPatternProviderReference readReference(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey(ResonatingPatternProviderReference.TAG_REFERENCE, 10)) {
            return null;
        }
        return ResonatingPatternProviderReference.read(
            tag.getCompoundTag(ResonatingPatternProviderReference.TAG_REFERENCE));
    }

    // ------------------------------------------------------------------
    // 兼容旧行为（方块物品的 item-side 默认目标 / 清空配方）
    // ------------------------------------------------------------------

    public static boolean hasStoredTargets(ItemStack stack) {
        if (readReference(stack) != null) {
            return true;
        }
        return !ResonatingProviderDefaults.readTargets(stack).stream().allMatch(t -> !t.isPresent());
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format("ae2csl.item.resonating_linker.tooltip"));

        ResonatingPatternProviderReference reference = readReference(stack);
        if (reference == null) {
            tooltip.add("§7" + net.minecraft.client.resources.I18n.format(
                "ae2csl.item.resonating_linker.provider.unbound"));
            return;
        }

        BlockPos pos = reference.pos().getPos();
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format(
            "ae2csl.item.resonating_linker.provider",
            pos.getX(), pos.getY(), pos.getZ()));
    }
}
