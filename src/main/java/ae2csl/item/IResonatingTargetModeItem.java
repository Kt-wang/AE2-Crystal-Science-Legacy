/*
 * AE2 Crystal Science - IResonatingTargetModeItem Interface
 * Ported from 1.21.1 source
 *
 * Items that support scrolling through resonant target modes.
 */
package ae2csl.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface IResonatingTargetModeItem {

    void scrollSelectedInputAndToast(EntityPlayer player, ItemStack stack, boolean next);
}
