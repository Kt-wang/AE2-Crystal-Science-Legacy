/*
 * AE2 Crystal Science - LinkableTool Interface
 * Ported from 1.21.1 source
 *
 * Marker interface for tools that can be wirelessly linked to an AE2 network.
 */
package ae2csl.item.tools;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

public interface LinkableTool {

    String NBT_LINK_TARGET = "ae2csl_link_target";

    /**
     * Get the linked position from an item stack's NBT.
     * Returns null if not linked.
     */
    @Nullable
    static int[] getLinkedPosition(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(NBT_LINK_TARGET)) return null;
        int[] pos = tag.getIntArray(NBT_LINK_TARGET);
        return pos != null && pos.length >= 4 ? pos : null;
    }

    /**
     * Set the linked position on an item stack's NBT.
     * Format: [dimension, x, y, z]
     */
    static void setLinkedPosition(ItemStack stack, int dimension, int x, int y, int z) {
        if (stack.isEmpty()) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setIntArray(NBT_LINK_TARGET, new int[] { dimension, x, y, z });
    }

    static void unlink(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag(NBT_LINK_TARGET);
        }
    }
}
