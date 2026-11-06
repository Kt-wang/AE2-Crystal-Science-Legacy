/*
 * AE2 Crystal Science - MirrorLinkerItem (1.12.2)
 * Links Mirror Pattern Providers to a target provider.
 */
package ae2csl.item.linker;

import ae2csl.me.logic.MirrorPatternProviderHost;
import ae2csl.me.logic.MirroredPatternProviderTarget;
import ae2csl.item.PatternProviderBindingHelper;
import ae2csl.item.SimplePatternProviderMirrorHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;

import java.util.*;

public class MirrorLinkerItem extends Item {

    private static final int MAX_BATCH_RADIUS = 16;
    private static final int MAX_BATCH_PROVIDERS = 256;

    public MirrorLinkerItem() {
        setMaxStackSize(1);
        setTranslationKey("mirror_linker");
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                            float hitX, float hitY, float hitZ, EnumHand hand) {
        if (player == null || player.isSneaking()) return EnumActionResult.PASS;

        MirrorPatternProviderHost mirrorHost = PatternProviderBindingHelper.resolveClickedMirrorProvider(world, pos);
        if (mirrorHost != null) {
            if (world.isRemote) return EnumActionResult.SUCCESS;
            return applyStoredTarget(player.getHeldItem(hand), player, mirrorHost) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
        }

        return SimplePatternProviderMirrorHelper.tryBind(player.getHeldItem(hand), player, world, pos,
                world.provider.getDimension()) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                       EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) {
            ItemStack stack = player.getHeldItem(hand);
            if (SimplePatternProviderMirrorHelper.tryClear(stack, player)) {
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.PASS;
    }

    public static boolean applyStoredTarget(ItemStack stack, EntityPlayer player, MirrorPatternProviderHost host) {
        MirroredPatternProviderTarget target = SimplePatternProviderMirrorHelper.getTarget(stack);
        if (!player.world.isRemote) {
            host.getMirroringLogic().setMirrorTarget(target);
            player.sendMessage(new TextComponentTranslation(target == null
                    ? "ae2csl.msg.mirror_linker.cleared_provider"
                    : "ae2csl.msg.mirror_linker.applied"));
        }
        return true;
    }

    public static int applyStoredTargetToCluster(ItemStack stack, EntityPlayer player, World level,
                                                  BlockPos centerPos, Vec3d clickLocation) {
        if (level.isRemote || !level.isBlockLoaded(centerPos)) return 0;
        if (player.getDistanceSq(centerPos.getX() + 0.5, centerPos.getY() + 0.5, centerPos.getZ() + 0.5) > 64.0) return 0;

        MirrorPatternProviderHost selected = PatternProviderBindingHelper.resolveMirrorProvider(level, centerPos);
        if (selected == null) return 0;

        Set<MirrorPatternProviderHost> hosts = new LinkedHashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> pending = new ArrayDeque<>();
        pending.add(centerPos);
        hosts.add(selected);

        while (!pending.isEmpty() && hosts.size() < MAX_BATCH_PROVIDERS) {
            BlockPos current = pending.poll();
            if (!visited.add(current)) continue;
            if (!isWithinBatchBounds(centerPos, current) || !level.isBlockLoaded(current)) continue;

            List<MirrorPatternProviderHost> atCurrent = PatternProviderBindingHelper.getMirrorProvidersAt(level, current);
            hosts.addAll(atCurrent);

            for (EnumFacing dir : EnumFacing.VALUES) {
                BlockPos next = current.offset(dir);
                if (!visited.contains(next) && isWithinBatchBounds(centerPos, next)) pending.add(next);
            }
        }

        MirroredPatternProviderTarget target = SimplePatternProviderMirrorHelper.getTarget(stack);
        for (MirrorPatternProviderHost host : hosts) {
            host.getMirroringLogic().setMirrorTarget(target);
        }

        if (!player.world.isRemote) {
            player.sendMessage(new TextComponentTranslation(target == null
                    ? "ae2csl.msg.mirror_linker.cleared_batch"
                    : "ae2csl.msg.mirror_linker.applied_batch", hosts.size()));
        }
        return hosts.size();
    }

    private static boolean isWithinBatchBounds(BlockPos center, BlockPos pos) {
        return Math.abs(pos.getX() - center.getX()) <= MAX_BATCH_RADIUS
                && Math.abs(pos.getY() - center.getY()) <= MAX_BATCH_RADIUS
                && Math.abs(pos.getZ() - center.getZ()) <= MAX_BATCH_RADIUS;
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        MirroredPatternProviderTarget target = SimplePatternProviderMirrorHelper.getTarget(stack);
        if (target == null) {
            tooltip.add(TextFormatting.GRAY + net.minecraft.client.resources.I18n.format("ae2csl.tooltip.unbound"));
        } else {
            tooltip.add(TextFormatting.GRAY + net.minecraft.client.resources.I18n.format("ae2csl.tooltip.target",
                    target.getPos().getPos().getX(), target.getPos().getPos().getY(), target.getPos().getPos().getZ()));
        }
    }
}
