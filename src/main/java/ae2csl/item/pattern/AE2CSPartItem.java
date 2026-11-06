/*
 * AE2 Crystal Science - AE2CSPartItem (1.12.2)
 * Base class for all AE2CS part items.
 * Overrides onItemUse to delegate to PartHelper.usePartItem so parts can
 * actually be placed onto cable buses (mirrors AE2's internal PartItem).
 */
package ae2csl.item.pattern;

import ae2.api.parts.IPart;
import ae2.api.parts.IPartItem;
import ae2.api.parts.PartHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class AE2CSPartItem<T extends IPart> extends Item implements IPartItem<T> {

    public AE2CSPartItem() {
        setMaxStackSize(64);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing side, float hitX, float hitY, float hitZ) {
        return PartHelper.usePartItem(player.getHeldItem(hand), player, world, pos, hand, side, hitX, hitY, hitZ);
    }
}
