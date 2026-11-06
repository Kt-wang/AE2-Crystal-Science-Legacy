/*
 * AE2 Crystal Science - SimplePatternProviderMirrorHelper
 * Ported from 1.21.1 source
 *
 * Helper for binding/clearing mirror pattern provider targets on ItemStacks.
 */
package ae2csl.item;

import ae2.api.parts.IPartHost;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2csl.api.util.GlobalPos;
import ae2csl.me.logic.MirrorPatternProviderHost;
import ae2csl.me.logic.MirroredPatternProviderTarget;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class SimplePatternProviderMirrorHelper {

    private SimplePatternProviderMirrorHelper() {}

    /**
     * Try to bind a mirror target from a clicked block.
     */
    public static boolean tryBind(ItemStack stack, EntityPlayer player, World world, BlockPos pos, int dimension) {
        if (player.isSneaking()) return false;

        TileEntity te = world.getTileEntity(pos);
        if (te == null) return false;

        PatternProviderLogicHost targetHost = PatternProviderBindingHelper.resolvePatternProvider(world, pos, net.minecraft.util.math.Vec3d.ZERO);
        if (targetHost == null) return false;

        if (targetHost instanceof MirrorPatternProviderHost) {
            if (!world.isRemote) {
                player.sendMessage(new TextComponentTranslation("ae2csl.msg.mirror_pattern_provider.invalid_target")
                        .setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.RED)));
            }
            return true;
        }

        EnumFacing partSide = null;
        if (te instanceof IPartHost) {
            IPartHost partHost = (IPartHost) te;
            for (EnumFacing side : EnumFacing.VALUES) {
                Object part = partHost.getPart(side);
                if (part instanceof PatternProviderLogicHost) {
                    partSide = side;
                    break;
                }
            }
        }

        MirroredPatternProviderTarget target = new MirroredPatternProviderTarget(
                new GlobalPos(dimension, pos), partSide);

        if (!world.isRemote) {
            writeTarget(stack, target);
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.mirror_pattern_provider.bound",
                    pos.getX(), pos.getY(), pos.getZ()));
        }
        return true;
    }

    /**
     * Try to clear the mirror target from a stack.
     */
    public static boolean tryClear(ItemStack stack, EntityPlayer player) {
        if (!player.isSneaking()) return false;

        if (!player.world.isRemote) {
            clearTarget(stack);
            player.sendMessage(new TextComponentTranslation("ae2csl.msg.mirror_pattern_provider.cleared"));
        }
        return true;
    }

    @Nullable
    public static MirroredPatternProviderTarget getTarget(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return null;
        return MirroredPatternProviderTarget.read(stack.getTagCompound());
    }

    public static void writeTarget(ItemStack stack, @Nullable MirroredPatternProviderTarget target) {
        if (stack.isEmpty()) return;
        if (!stack.hasTagCompound()) stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        MirroredPatternProviderTarget.write(target, stack.getTagCompound());
    }

    public static void clearTarget(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return;
        MirroredPatternProviderTarget.write(null, stack.getTagCompound());
    }
}
