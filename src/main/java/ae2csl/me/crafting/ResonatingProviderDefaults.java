/*
 * AE2 Crystal Science - ResonatingProviderDefaults (1.12.2)
 * Ported from 1.21.1 source (Codec → NBT)
 *
 * Stores default input/output targets for ResonatingPatternProvider on ItemStack NBT.
 */
package ae2csl.me.crafting;

import ae2csl.init.AE2CSDataComponents;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ResonatingProviderDefaults {

    public static final int DEFAULT_INPUT_SLOTS = 81;

    private ResonatingProviderDefaults() {}

    public static List<Optional<EncodedResonatingPattern.Target>> readTargets(ItemStack stack) {
        NBTTagCompound data = AE2CSDataComponents.getSideConfig(stack); // reuse NBT storage
        if (data == null || !data.hasKey("defaults")) return emptyTargets();
        return readTargetsFromNBT(data.getCompoundTag("defaults"));
    }

    public static void writeTargets(ItemStack stack, List<Optional<EncodedResonatingPattern.Target>> targets) {
        // Use the defaults NBT key on the item
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound defaultsTag = writeTargetsToNBT(targets);
        root.setTag("defaults", defaultsTag);
        // Store via a simple NBT key approach
        ensureTag(stack);
        NBTTagCompound tag = stack.getTagCompound();
        tag.setTag(AE2CSDataComponents.KEY_RESONATING_PROVIDER_DEFAULTS, root);
    }

    public static int getSelectedInput(ItemStack stack) {
        NBTTagCompound data = AE2CSDataComponents.getSideConfig(stack);
        if (data == null || !data.hasKey("defaults")) return 0;
        return data.getCompoundTag("defaults").getInteger("selected");
    }

    public static void setSelectedInput(ItemStack stack, int selected) {
        List<Optional<EncodedResonatingPattern.Target>> targets = readTargets(stack);
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound defaultsTag = writeTargetsToNBT(targets);
        defaultsTag.setInteger("selected", clampSelected(selected));
        root.setTag("defaults", defaultsTag);
        ensureTag(stack);
        NBTTagCompound tag = stack.getTagCompound();
        tag.setTag(AE2CSDataComponents.KEY_RESONATING_PROVIDER_DEFAULTS, root);
    }

    public static int clampSelected(int selected) {
        if (selected < 0) return 0;
        if (selected >= DEFAULT_INPUT_SLOTS) return DEFAULT_INPUT_SLOTS - 1;
        return selected;
    }

    public static boolean hasAnyTarget(List<Optional<EncodedResonatingPattern.Target>> targets) {
        return targets.stream().anyMatch(Optional::isPresent);
    }

    public static List<Optional<EncodedResonatingPattern.Target>> emptyTargets() {
        List<Optional<EncodedResonatingPattern.Target>> out = new ArrayList<>(DEFAULT_INPUT_SLOTS);
        for (int i = 0; i < DEFAULT_INPUT_SLOTS; i++) {
            out.add(Optional.empty());
        }
        return out;
    }

    // ---- NBT helpers ----
    private static NBTTagCompound writeTargetsToNBT(List<Optional<EncodedResonatingPattern.Target>> targets) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("count", targets.size());
        for (int i = 0; i < targets.size(); i++) {
            Optional<EncodedResonatingPattern.Target> opt = targets.get(i);
            if (opt.isPresent()) {
                tag.setTag("t_" + i, opt.get().write());
            }
        }
        return tag;
    }

    private static List<Optional<EncodedResonatingPattern.Target>> readTargetsFromNBT(NBTTagCompound tag) {
        int count = tag.getInteger("count");
        List<Optional<EncodedResonatingPattern.Target>> targets = new ArrayList<>(Math.max(count, DEFAULT_INPUT_SLOTS));
        for (int i = 0; i < count; i++) {
            String key = "t_" + i;
            if (tag.hasKey(key)) {
                targets.add(Optional.of(EncodedResonatingPattern.Target.read(tag.getCompoundTag(key))));
            } else {
                targets.add(Optional.empty());
            }
        }
        while (targets.size() < DEFAULT_INPUT_SLOTS) {
            targets.add(Optional.empty());
        }
        return targets;
    }

    private static void ensureTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
    }
}
