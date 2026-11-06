/*
 * AE2 Crystal Science - ToolLinkableHandler (1.12.2)
 * Ported from 1.21.1 source
 *
 * Implements AE2's IGridLinkableHandler to support wireless linking of AE2CS tools
 * to ME networks via Wireless Access Points.
 */
package ae2csl.item.tools;

import ae2.api.config.Actionable;
import ae2.api.features.IGridLinkableHandler;
import ae2.api.implementations.blockentities.IWirelessAccessPoint;
import ae2.api.stacks.AEKey;
import ae2.api.storage.MEStorage;
import ae2.me.helpers.PlayerSource;
import ae2csl.init.AE2CSEnchantments;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;

public class ToolLinkableHandler implements IGridLinkableHandler {

    public static final ToolLinkableHandler INSTANCE = new ToolLinkableHandler();

    private ToolLinkableHandler() {}

    @Override
    public boolean canLink(ItemStack stack) {
        if (stack.getItem() instanceof LinkableTool)
            return true;

        // Check for ENDER_LINK enchantment
        int level = EnchantmentHelper.getEnchantmentLevel(AE2CSEnchantments.ENDER_LINK, stack);
        return level > 0;
    }

    @Override
    public void link(ItemStack itemStack, World world, BlockPos pos) {
        LinkableTool.setLinkedPosition(itemStack, world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void unlink(ItemStack itemStack) {
        LinkableTool.unlink(itemStack);
    }

    /**
     * Get the MEStorage linked to this tool stack, via Wireless Access Point.
     */
    @Nullable
    public static MEStorage getLinkMEStorage(ItemStack stack, EntityPlayer player) {
        if (!INSTANCE.canLink(stack)) return null;

        int[] targetPos = LinkableTool.getLinkedPosition(stack);
        if (targetPos == null || targetPos.length < 4) return null;

        if (!(player.world instanceof WorldServer)) return null;

        WorldServer playerLevel = (WorldServer) player.world;
        World linkedLevel = playerLevel;
        if (linkedLevel.provider.getDimension() != targetPos[0]) {
            linkedLevel = DimensionManager.getWorld(targetPos[0]);
        }
        if (linkedLevel == null) return null;

        BlockPos bp = new BlockPos(targetPos[1], targetPos[2], targetPos[3]);
        if (!linkedLevel.isBlockLoaded(bp)) return null;

        TileEntity be = linkedLevel.getTileEntity(bp);
        if (!(be instanceof IWirelessAccessPoint)) return null;

        IWirelessAccessPoint accessPoint = (IWirelessAccessPoint) be;
        ae2.api.networking.IGrid grid = accessPoint.getGrid();
        if (grid == null) return null;

        return grid.getStorageService().getInventory();
    }

    /**
     * Insert items into the linked ME storage.
     */
    public static long insert(EntityPlayer player, ItemStack toolStack, AEKey what, long amount, Actionable mode) {
        if (player.world.isRemote) return 0;

        MEStorage inv = getLinkMEStorage(toolStack, player);
        if (inv == null) return 0;

        return inv.insert(what, amount, mode, new PlayerSource(player));
    }
}
