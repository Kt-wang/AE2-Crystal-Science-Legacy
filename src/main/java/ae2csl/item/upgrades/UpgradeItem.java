/*
 * AE2 Crystal Science - UpgradeItem Base Class
 * Ported from 1.21.1 source
 *
 * Base class for upgrade items that replace one block/part with another.
 * Subclasses register block/part replace mappings via registerBlockReplaceInfo / registerPartReplaceInfo.
 */
package ae2csl.item.upgrades;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class UpgradeItem extends Item {

    private final List<ReplaceEntry<Block>> pendingBlockReplacements = new ArrayList<>();
    private final Map<Block, Block> blockReplaceInfo = new IdentityHashMap<>();
    private boolean initialized = false;

    public UpgradeItem() {
        setMaxStackSize(64);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                       EnumHand hand, EnumFacing facing,
                                       float hitX, float hitY, float hitZ) {
        init();

        TileEntity originalBe = world.getTileEntity(pos);
        if (originalBe == null) return EnumActionResult.PASS;

        if (world.isRemote) return EnumActionResult.SUCCESS;

        IBlockState originState = world.getBlockState(pos);
        if (blockReplaceInfo.containsKey(originState.getBlock())) {
            Block newBlock = blockReplaceInfo.get(originState.getBlock());

            // Save original TE data
            NBTTagCompound originalData = originalBe.writeToNBT(new NBTTagCompound());

            // Remove old block and set new one
            world.removeTileEntity(pos);
            world.setBlockToAir(pos);
            world.setBlockState(pos, newBlock.getDefaultState(), 3);

            // Restore TE data if applicable
            TileEntity newBe = world.getTileEntity(pos);
            if (newBe != null) {
                newBe.readFromNBT(originalData);
                newBe.markDirty();
            }

            player.getHeldItem(hand).shrink(1);
            return EnumActionResult.SUCCESS;
        }

        return EnumActionResult.PASS;
    }

    protected void registerBlockReplaceInfo(Supplier<? extends Block> from, Supplier<? extends Block> to) {
        pendingBlockReplacements.add(new ReplaceEntry<>(from, to));
    }

    /**
     * Initialize the replacement maps. Called lazily on first use.
     */
    public void init() {
        if (initialized) return;
        for (ReplaceEntry<Block> entry : pendingBlockReplacements) {
            blockReplaceInfo.put(entry.from().get(), entry.to().get());
        }
        initialized = true;
    }

    private static final class ReplaceEntry<T> {
        private final Supplier<? extends T> from;
        private final Supplier<? extends T> to;

        ReplaceEntry(Supplier<? extends T> from, Supplier<? extends T> to) {
            this.from = from;
            this.to = to;
        }

        Supplier<? extends T> from() { return from; }
        Supplier<? extends T> to() { return to; }
    }
}
