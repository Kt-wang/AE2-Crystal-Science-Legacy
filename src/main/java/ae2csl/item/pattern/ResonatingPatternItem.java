/*
 * AE2 Crystal Science - ResonatingPatternItem (1.12.2)
 * 谐振样板物品。继承 fork 的 EncodedPatternItem —— 这样 AE2 的
 * PatternDetailsHelper(AEPatternDecoder) 能识别并解码它，网络才能看到谐振样板。
 *
 * 交互（对齐源码 ResonatingPatternItem）：
 *  - 右键方块面：标记 / 取消标记「当前选中输入槽」的远程目标；
 *  - 潜行 + 滚轮：在非空输入槽之间切换（跳过空槽），并提示当前槽与目标；
 *  - 未编码时可以像普通编码样板一样拆解。
 */
package ae2csl.item.pattern;

import ae2.crafting.pattern.EncodedPatternItem;
import ae2csl.api.util.GlobalPos;
import ae2csl.item.IResonatingTargetModeItem;
import ae2csl.me.crafting.EncodedResonatingPattern;
import ae2csl.me.crafting.ResonatingPatternDetails;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ResonatingPatternItem extends EncodedPatternItem<ResonatingPatternDetails>
    implements IResonatingTargetModeItem {

    private static final String TAG_SELECTED_INPUT = "resonating_selected_input";

    public ResonatingPatternItem() {
        super((what, level) -> new ResonatingPatternDetails(what), null, 1);
        setTranslationKey("resonating_pattern");
    }

    // ------------------------------------------------------------------
    // 标记目标
    // ------------------------------------------------------------------

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                           float hitX, float hitY, float hitZ, EnumHand hand) {
        if (player == null || hand != EnumHand.MAIN_HAND) {
            return EnumActionResult.PASS;
        }
        // 潜行留给滚轮切换，不在方块上做标记
        if (player.isSneaking()) {
            return EnumActionResult.PASS;
        }

        ItemStack stack = player.getHeldItem(hand);
        EncodedResonatingPattern encoded = readEncodedFromStack(stack);
        if (encoded == null) {
            return EnumActionResult.PASS;
        }

        // 只在服务端改数据，客户端返回 SUCCESS 拦截后续方块交互
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        int size = encoded.getSparseInputs().size();
        if (size <= 0) {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_pattern.no_inputs"));
            return EnumActionResult.SUCCESS;
        }

        int validCount = countNonEmptyInputs(encoded);
        if (validCount <= 0) {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_pattern.no_valid_inputs"));
            return EnumActionResult.SUCCESS;
        }

        int selected = resolveSelectedNonEmpty(stack, encoded);
        if (selected < 0) {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_pattern.no_valid_inputs"));
            return EnumActionResult.SUCCESS;
        }

        int ordinal = ordinalOfNonEmptyInput(encoded, selected);
        ITextComponent selectedInfo = buildSelectedInputInfo(encoded, selected);

        EncodedResonatingPattern.Target target = new EncodedResonatingPattern.Target(
            new GlobalPos(world.provider.getDimension(), pos), side);
        Optional<EncodedResonatingPattern.Target> current = encoded.targetOfSparseInput(selected);
        boolean sameTarget = current.isPresent()
            && current.get().getPos().equals(target.getPos())
            && current.get().getFace() == target.getFace();

        writeEncodedToStack(stack, encoded.withTarget(selected, sameTarget ? null : target));

        if (ae2csl.AE2CrystalScienceLegacy.LOGGER.isDebugEnabled()) {
            ae2csl.AE2CrystalScienceLegacy.LOGGER.debug("[resonating] mark pattern slot={} ordinal={}/{} -> {}",
                selected, ordinal, validCount,
                sameTarget ? "cleared" : target.getPos().getPos() + "/" + side.getName());
        }

        if (sameTarget) {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_pattern.unmarked",
                ordinal, validCount, selectedInfo));
        } else {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_pattern.marked",
                ordinal, validCount, selectedInfo, pos.getX(), pos.getY(), pos.getZ(), side.getName()));
        }
        return EnumActionResult.SUCCESS;
    }

    /** 有编码时不允许右键空气拆解，避免误操作（对齐源码 use） */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (readEncodedFromStack(stack) != null) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        return super.onItemRightClick(world, player, hand);
    }

    // ------------------------------------------------------------------
    // 滚轮切换选中输入槽
    // ------------------------------------------------------------------

    @Override
    public void scrollSelectedInputAndToast(EntityPlayer player, ItemStack stack, boolean next) {
        EncodedResonatingPattern encoded = readEncodedFromStack(stack);
        if (encoded == null) {
            return;
        }

        if (encoded.getSparseInputs().size() <= 0) {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_pattern.no_inputs"));
            return;
        }
        int validCount = countNonEmptyInputs(encoded);
        if (validCount <= 0) {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_pattern.no_valid_inputs"));
            return;
        }

        int current = resolveSelectedNonEmpty(stack, encoded);
        if (current < 0) {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_pattern.no_valid_inputs"));
            return;
        }

        int nextIndex = next ? findNextNonEmptyInput(encoded, current) : findPrevNonEmptyInput(encoded, current);
        if (nextIndex < 0) {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_pattern.no_valid_inputs"));
            return;
        }
        setSelectedInput(stack, nextIndex);

        int ordinal = ordinalOfNonEmptyInput(encoded, nextIndex);
        ITextComponent selectedInfo = buildSelectedInputInfo(encoded, nextIndex);
        Optional<EncodedResonatingPattern.Target> target = encoded.targetOfSparseInput(nextIndex);
        if (target.isPresent()) {
            BlockPos pos = target.get().getPos().getPos();
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_pattern.selected_input_marked",
                ordinal, validCount, selectedInfo,
                pos.getX(), pos.getY(), pos.getZ(), target.get().getFace().getName()));
        } else {
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.resonating_pattern.selected_input_unmarked",
                ordinal, validCount, selectedInfo));
        }
    }

    // ------------------------------------------------------------------
    // 选中槽 / 编码读写
    // ------------------------------------------------------------------

    /** 样板当前选中的输入索引 */
    public static int getSelectedInput(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return 0;
        }
        return stack.getTagCompound().getInteger(TAG_SELECTED_INPUT);
    }

    public static void setSelectedInput(ItemStack stack, int index) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger(TAG_SELECTED_INPUT, clampSelected(index, Integer.MAX_VALUE));
    }

    @Nullable
    private static EncodedResonatingPattern readEncodedFromStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey(ResonatingPatternDetails.TAG_ENCODED, 10)) {
            return null;
        }
        try {
            return EncodedResonatingPattern.readFromNBT(tag.getCompoundTag(ResonatingPatternDetails.TAG_ENCODED));
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeEncodedToStack(ItemStack stack, EncodedResonatingPattern encoded) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setTag(ResonatingPatternDetails.TAG_ENCODED, encoded.writeToNBT());
    }

    // ------------------------------------------------------------------
    // 选中槽辅助（对齐源码）
    // ------------------------------------------------------------------

    private static boolean isNonEmptySparseInput(EncodedResonatingPattern encoded, int sparseIndex) {
        if (sparseIndex < 0 || sparseIndex >= encoded.getSparseInputs().size()) {
            return false;
        }
        var stack = encoded.getSparseInputs().get(sparseIndex);
        return stack != null && stack.amount() > 0;
    }

    private static int countNonEmptyInputs(EncodedResonatingPattern encoded) {
        int count = 0;
        for (int i = 0; i < encoded.getSparseInputs().size(); i++) {
            if (isNonEmptySparseInput(encoded, i)) {
                count++;
            }
        }
        return count;
    }

    private static int findNextNonEmptyInput(EncodedResonatingPattern encoded, int start) {
        int size = encoded.getSparseInputs().size();
        if (size <= 0) {
            return -1;
        }
        for (int step = 1; step <= size; step++) {
            int index = (start + step) % size;
            if (isNonEmptySparseInput(encoded, index)) {
                return index;
            }
        }
        return -1;
    }

    private static int findPrevNonEmptyInput(EncodedResonatingPattern encoded, int start) {
        int size = encoded.getSparseInputs().size();
        if (size <= 0) {
            return -1;
        }
        for (int step = 1; step <= size; step++) {
            int index = (start - step) % size;
            if (index < 0) {
                index += size;
            }
            if (isNonEmptySparseInput(encoded, index)) {
                return index;
            }
        }
        return -1;
    }

    /** 第几个非空输入（1 起） */
    private static int ordinalOfNonEmptyInput(EncodedResonatingPattern encoded, int sparseIndex) {
        if (!isNonEmptySparseInput(encoded, sparseIndex)) {
            return 0;
        }
        int ordinal = 0;
        for (int i = 0; i <= sparseIndex && i < encoded.getSparseInputs().size(); i++) {
            if (isNonEmptySparseInput(encoded, i)) {
                ordinal++;
            }
        }
        return ordinal;
    }

    /** 保证选中槽指向非空输入，必要时写回物品 NBT；无有效输入返回 -1 */
    private static int resolveSelectedNonEmpty(ItemStack stack, EncodedResonatingPattern encoded) {
        int size = encoded.getSparseInputs().size();
        if (size <= 0) {
            return -1;
        }
        int selected = clampSelected(getSelectedInput(stack), size - 1);
        if (isNonEmptySparseInput(encoded, selected)) {
            return selected;
        }
        int next = findNextNonEmptyInput(encoded, selected - 1);
        if (next >= 0) {
            setSelectedInput(stack, next);
        }
        return next;
    }

    private static int clampSelected(int selected, int max) {
        if (selected < 0) {
            return 0;
        }
        return Math.min(selected, max);
    }

    private static ITextComponent buildSelectedInputInfo(EncodedResonatingPattern encoded, int sparseIndex) {
        if (sparseIndex < 0 || sparseIndex >= encoded.getSparseInputs().size()) {
            return new TextComponentTranslation("ae2csl.msg.resonating_pattern.input_invalid");
        }
        var stack = encoded.getSparseInputs().get(sparseIndex);
        if (stack == null || stack.amount() <= 0) {
            return new TextComponentTranslation("ae2csl.msg.resonating_pattern.input_empty");
        }
        return stack.what().getDisplayName();
    }

    public static void scrollSelectedInput(EntityPlayer player, ItemStack stack, boolean next) {
        if (stack.getItem() instanceof ResonatingPatternItem) {
            ((ResonatingPatternItem) stack.getItem()).scrollSelectedInputAndToast(player, stack, next);
        }
    }

    // ------------------------------------------------------------------
    // Tooltip：让玩家能看到哪些输入已标记（便于验证标记是否生效）
    // ------------------------------------------------------------------

    @SideOnly(Side.CLIENT)
    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flagIn) {
        EncodedResonatingPattern encoded = readEncodedFromStack(stack);
        tooltip.add("§7" + I18n.format("ae2csl.item.resonating_pattern.usage"));
        if (encoded == null) {
            return;
        }

        tooltip.add("§7" + I18n.format("ae2csl.item.resonating_pattern.selected",
            getSelectedInput(stack) + 1));

        int marked = 0;
        int total = 0;
        for (int i = 0; i < encoded.getSparseInputs().size(); i++) {
            var input = encoded.getSparseInputs().get(i);
            if (input == null || input.amount() <= 0) {
                continue;
            }
            total++;

            Optional<EncodedResonatingPattern.Target> target = encoded.targetOfSparseInput(i);
            if (target.isEmpty()) {
                continue;
            }
            marked++;
            if (marked <= 9) {
                BlockPos pos = target.get().getPos().getPos();
                tooltip.add("§a" + I18n.format("ae2csl.item.resonating_pattern.target",
                    i + 1, input.what().getDisplayName().getFormattedText(),
                    pos.getX(), pos.getY(), pos.getZ(), target.get().getFace().getName()));
            }
        }

        if (marked == 0) {
            tooltip.add("§7" + I18n.format("ae2csl.item.resonating_pattern.no_targets"));
        } else if (marked > 9) {
            tooltip.add("§7" + I18n.format("ae2csl.item.resonating_pattern.more", marked, total));
        }
    }
}
