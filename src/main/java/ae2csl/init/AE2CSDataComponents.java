/*
 * AE2 Crystal Science - AE2CSDataComponents (1.12.2 NBT-based)
 * Ported from 1.21.1 source
 *
 * In 1.21.1, item data is stored via DataComponentType + Codec + StreamCodec.
 * In 1.12.2, item data is stored in NBTTagCompound. This class provides
 * NBT key constants and read/write helpers for all item data components.
 */
package ae2csl.init;

import ae2csl.api.linker.broadcast.MemoryCardBandInfo;
import ae2csl.me.logic.MirroredPatternProviderTarget;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

public final class AE2CSDataComponents {

    // ---- NBT Tag Keys ----
    public static final String KEY_GROW_PROCESS = "ae2csl_grow_process";
    public static final String KEY_MEMORY_CARD_BAND_INFO = "ae2csl_band_info";
    public static final String KEY_BROADCASTER_RECEIVER_EXPECT_CHANNELS = "ae2csl_receiver_expect_channels";
    public static final String KEY_ENDER_EMITTER_POS = "ae2csl_ender_emitter_pos";
    public static final String KEY_ENCODED_RESONATING_PATTERN = "ae2csl_encoded_resonating_pattern";
    public static final String KEY_RESONATING_PATTERN_SELECTED_INPUT = "ae2csl_resonating_selected_input";
    public static final String KEY_SIDE_CONFIG = "ae2csl_side_config";
    public static final String KEY_RESONATING_CONVERTER_INV = "ae2csl_resonating_converter_inv";
    public static final String KEY_RESONATING_PROVIDER_DEFAULTS = "ae2csl_resonating_provider_defaults";
    public static final String KEY_WIRELESS_RESONANT_TERMINAL_DATA = "ae2csl_wireless_terminal_data";
    public static final String KEY_MIRROR_PATTERN_PROVIDER_TARGET = "ae2csl_mirror_target";
    public static final String KEY_RESONATING_MEMORY_CARD_STORAGE = "ae2csl_memory_card_storage";

    private AE2CSDataComponents() {}

    // ---- GROW_PROCESS (int) ----
    public static int getGrowProcess(ItemStack stack) {
        return getInt(stack, KEY_GROW_PROCESS, 0);
    }
    public static void setGrowProcess(ItemStack stack, int value) {
        setInt(stack, KEY_GROW_PROCESS, value);
    }

    // ---- MEMORY_CARD_BAND_INFO ----
    @Nullable
    public static MemoryCardBandInfo getMemoryCardBandInfo(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(KEY_MEMORY_CARD_BAND_INFO)) return null;
        return MemoryCardBandInfo.readFromNBT(tag.getCompoundTag(KEY_MEMORY_CARD_BAND_INFO));
    }
    public static void setMemoryCardBandInfo(ItemStack stack, @Nullable MemoryCardBandInfo info) {
        ensureTag(stack);
        NBTTagCompound tag = stack.getTagCompound();
        if (info != null) tag.setTag(KEY_MEMORY_CARD_BAND_INFO, info.writeToNBT());
        else tag.removeTag(KEY_MEMORY_CARD_BAND_INFO);
    }

    // ---- BROADCASTER_RECEIVER_EXPECT_CHANNELS (int) ----
    public static int getReceiverExpectChannels(ItemStack stack) {
        return getInt(stack, KEY_BROADCASTER_RECEIVER_EXPECT_CHANNELS, 0);
    }
    public static void setReceiverExpectChannels(ItemStack stack, int value) {
        setInt(stack, KEY_BROADCASTER_RECEIVER_EXPECT_CHANNELS, value);
    }

    // ---- ENDER_EMITTER_POS (int[]: [dim, x, y, z]) ----
    @Nullable
    public static int[] getEnderEmitterPos(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(KEY_ENDER_EMITTER_POS)) return null;
        return tag.getIntArray(KEY_ENDER_EMITTER_POS);
    }
    public static void setEnderEmitterPos(ItemStack stack, int dimension, int x, int y, int z) {
        ensureTag(stack);
        NBTTagCompound tag = stack.getTagCompound();
        tag.setIntArray(KEY_ENDER_EMITTER_POS, new int[] { dimension, x, y, z });
    }
    public static void clearEnderEmitterPos(ItemStack stack) {
        removeKey(stack, KEY_ENDER_EMITTER_POS);
    }

    // ---- RESONATING_PATTERN_SELECTED_INPUT (int) ----
    public static int getResonatingSelectedInput(ItemStack stack) {
        return getInt(stack, KEY_RESONATING_PATTERN_SELECTED_INPUT, 0);
    }
    public static void setResonatingSelectedInput(ItemStack stack, int value) {
        setInt(stack, KEY_RESONATING_PATTERN_SELECTED_INPUT, value);
    }

    // ---- MIRROR_PATTERN_PROVIDER_TARGET ----
    @Nullable
    public static MirroredPatternProviderTarget getMirrorProviderTarget(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return null;
        return MirroredPatternProviderTarget.read(stack.getTagCompound());
    }
    public static void setMirrorProviderTarget(ItemStack stack, @Nullable MirroredPatternProviderTarget target) {
        if (target != null) {
            ensureTag(stack);
            MirroredPatternProviderTarget.write(target, stack.getTagCompound());
        } else {
            removeKey(stack, KEY_MIRROR_PATTERN_PROVIDER_TARGET);
        }
    }

    // ---- SIDE_CONFIG_FOR_MEMORY_CARD (NBTTagCompound) ----
    @Nullable
    public static NBTTagCompound getSideConfig(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(KEY_SIDE_CONFIG) ? tag.getCompoundTag(KEY_SIDE_CONFIG) : null;
    }
    public static void setSideConfig(ItemStack stack, @Nullable NBTTagCompound sideConfig) {
        if (sideConfig != null) {
            ensureTag(stack);
            NBTTagCompound tag = stack.getTagCompound();
            tag.setTag(KEY_SIDE_CONFIG, sideConfig);
        } else {
            removeKey(stack, KEY_SIDE_CONFIG);
        }
    }

    // ---- WIRELESS_RESONANT_TERMINAL_DATA (NBTTagCompound) ----
    @Nullable
    public static NBTTagCompound getWirelessTerminalData(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(KEY_WIRELESS_RESONANT_TERMINAL_DATA)
                ? tag.getCompoundTag(KEY_WIRELESS_RESONANT_TERMINAL_DATA) : null;
    }
    public static void setWirelessTerminalData(ItemStack stack, @Nullable NBTTagCompound data) {
        if (data != null) {
            ensureTag(stack);
            NBTTagCompound tag = stack.getTagCompound();
            tag.setTag(KEY_WIRELESS_RESONANT_TERMINAL_DATA, data);
        } else {
            removeKey(stack, KEY_WIRELESS_RESONANT_TERMINAL_DATA);
        }
    }

    // ---- Helper methods ----
    private static int getInt(ItemStack stack, String key, int defaultVal) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return defaultVal;
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(key) ? tag.getInteger(key) : defaultVal;
    }

    private static void setInt(ItemStack stack, String key, int value) {
        ensureTag(stack);
        NBTTagCompound tag = stack.getTagCompound();
        tag.setInteger(key, value);
    }

    private static void ensureTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
    }

    private static void removeKey(ItemStack stack, String key) {
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null) tag.removeTag(key);
        }
    }
}
