/*
 * AE2 Crystal Science - ResonatingPatternDetails (1.12.2)
 * Ported from 1.21.1 source (simplified)
 */
package ae2csl.me.crafting;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2csl.api.util.PatternHelper;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class ResonatingPatternDetails implements IPatternDetails {

    private final AEItemKey definition;
    private final List<GenericStack> sparseInputs;
    private final List<GenericStack> sparseOutputs;
    private final List<Optional<EncodedResonatingPattern.Target>> inputTargets;
    private final IInput[] inputs;
    private final List<GenericStack> condensedOutputs;

    public ResonatingPatternDetails(AEItemKey definition, EncodedResonatingPattern encoded) {
        this.definition = definition;
        this.sparseInputs = encoded.getSparseInputs();
        this.sparseOutputs = encoded.getSparseOutputs();
        this.inputTargets = encoded.getInputTargets();

        List<GenericStack> condensedInputs = PatternHelper.condenseStacks(sparseInputs);
        this.inputs = new IInput[condensedInputs.size()];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = new ResonatingInput(condensedInputs.get(i));
        }

        this.condensedOutputs = PatternHelper.condenseStacks(sparseOutputs);
    }

    /** 样板物品 NBT 中编码数据的键 */
    public static final String TAG_ENCODED = "encoded_resonating_pattern";

    /**
     * 从物品 AEKey 直接解码（EncodedPatternItem 的解码器入口）
     */
    public ResonatingPatternDetails(AEItemKey definition) {
        this(definition, readEncoded(definition));
    }

    private static EncodedResonatingPattern readEncoded(AEItemKey definition) {
        net.minecraft.nbt.NBTTagCompound tag = definition.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_ENCODED, 10)) {
            throw new IllegalArgumentException("Given item does not encode a resonating pattern: " + definition);
        }
        return EncodedResonatingPattern.readFromNBT(tag.getCompoundTag(TAG_ENCODED));
    }

    /**
     * 把样板信息编码到 ItemStack 上（对齐源码 encode(stack, inputs, outputs)）
     */
    public static void encode(ItemStack stack, List<GenericStack> sparseInputs, List<GenericStack> sparseOutputs) {
        if (sparseInputs.stream().noneMatch(Objects::nonNull)) {
            throw new IllegalArgumentException("At least one input must be non-null.");
        }
        Objects.requireNonNull(sparseOutputs.get(0), "The first (primary) output must be non-null.");

        var targets = new ArrayList<Optional<EncodedResonatingPattern.Target>>(sparseInputs.size());
        for (int i = 0; i < sparseInputs.size(); i++) {
            targets.add(Optional.empty());
        }

        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        }
        stack.getTagCompound().setTag(TAG_ENCODED,
            new EncodedResonatingPattern(sparseInputs, sparseOutputs, targets).writeToNBT());
    }

    /**
     * 从处理样板获得对应的谐振样板（对齐源码 encode(patternItem)）
     * 1.12.2 适配：需要 world 解码处理样板
     */
    public static ItemStack encode(ItemStack patternItem, net.minecraft.world.World level) {
        if (patternItem.isEmpty() || level == null) {
            return ItemStack.EMPTY;
        }

        if (!(patternItem.getItem().getRegistryName() != null
            && patternItem.getItem().getRegistryName().toString().equals("ae2:processing_pattern"))) {
            return ItemStack.EMPTY;
        }

        IPatternDetails decoded = ae2.api.crafting.PatternDetailsHelper.decodePattern(patternItem, level);
        if (!(decoded instanceof ae2.crafting.pattern.AEProcessingPattern processing)) {
            return ItemStack.EMPTY;
        }

        ItemStack resonatingItem = new ItemStack(
            net.minecraft.item.Item.REGISTRY.getObject(
                new net.minecraft.util.ResourceLocation("ae2csl", "resonating_pattern")));
        if (resonatingItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<GenericStack> sparseInputs = new ArrayList<>(processing.getSparseInputs());
        List<GenericStack> sparseOutputs = new ArrayList<>(processing.getSparseOutputs());
        encode(resonatingItem, sparseInputs, sparseOutputs);
        return resonatingItem;
    }

    public static int clampSelected(int selected, int sparseSize) {
        if (sparseSize <= 0) return 0;
        if (selected < 0) return 0;
        if (selected >= sparseSize) return sparseSize - 1;
        return selected;
    }

    public List<GenericStack> getSparseInputs() { return sparseInputs; }
    public List<GenericStack> getSparseOutputs() { return sparseOutputs; }
    public List<Optional<EncodedResonatingPattern.Target>> getInputTargets() { return inputTargets; }

    public Optional<EncodedResonatingPattern.Target> getTargetForSparseInputIndex(int sparseIndex) {
        if (sparseIndex < 0 || sparseIndex >= inputTargets.size()) return Optional.empty();
        return inputTargets.get(sparseIndex);
    }

    @Override
    public AEItemKey getDefinition() { return definition; }

    @Override
    public IInput[] getInputs() { return inputs; }

    @Override
    public List<GenericStack> getOutputs() { return condensedOutputs; }

    // ---- IInput implementation ----
    private static class ResonatingInput implements IInput {
        private final GenericStack[] template;
        private final long multiplier;

        ResonatingInput(GenericStack stack) {
            this.template = new GenericStack[] { new GenericStack(stack.what(), 1) };
            this.multiplier = stack.amount();
        }

        @Override
        public GenericStack[] possibleInputs() { return template; }

        @Override
        public long getMultiplier() { return multiplier; }

        @Override
        public boolean isValid(AEKey input, World level) {
            return input.matches(template[0]);
        }

        @Nullable
        @Override
        public AEKey getRemainingKey(AEKey template) { return null; }
    }
}
