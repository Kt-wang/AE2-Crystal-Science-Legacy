/*
 * AE2 Crystal Science - Ender Emitter Block (1.12.2)
 * Double-block (lower + upper) with horizontal facing, matching 1.21.1 source.
 *
 * The lower half holds the TileEnderEmitter; the upper half is an empty placeholder
 * (`upper.json`). The rotating antenna is rendered on the lower half by EnderEmitterTESR.
 */
package ae2csl.block.ender;

import ae2.block.AEBaseBlock;
import ae2.api.orientation.IOrientationStrategy;
import ae2.api.orientation.OrientationStrategies;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.tile.ender.TileEnderEmitter;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class EnderEmitterBlock extends AEBaseBlock {

    public enum Half implements IStringSerializable {
        LOWER("lower"), UPPER("upper");
        private final String name;
        Half(String name) { this.name = name; }
        @Override public String getName() { return name; }
        @Override public String toString() { return name; }
    }

    public static final PropertyBool ACTIVE = PropertyBool.create("active");
    public static final PropertyEnum<Half> HALF = PropertyEnum.create("half", Half.class);

    public EnderEmitterBlock() {
        super(Material.IRON);
        setHardness(2.5F);
        setResistance(10.0F);
        // Non-opaque, non-full cube (cutout model with an antenna extending above)
        setOpaque();
        setFullSize();
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.horizontalFacing();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return createBlockState(ACTIVE, HALF);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        // facing uses 3 bits (index 2-5), half uses bit 3
        int facing = getOrientationStrategy().getFacing(state).getIndex();
        int half = state.getValue(HALF) == Half.UPPER ? 8 : 0;
        return facing | half;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byIndex(meta & 7);
        if (facing == null || facing.getAxis() == EnumFacing.Axis.Y) {
            facing = EnumFacing.NORTH;
        }
        IBlockState state = getOrientationStrategy().setOrientation(getDefaultState(), facing, 0);
        return state.withProperty(HALF, (meta & 8) != 0 ? Half.UPPER : Half.LOWER);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEnderEmitter) {
            return state.withProperty(ACTIVE, ((TileEnderEmitter) te).isActive());
        }
        return state;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos)
                && world.getBlockState(pos.up()).getBlock().isReplaceable(world, pos.up());
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        if (!world.getBlockState(pos.up()).getBlock().isReplaceable(world, pos.up())) {
            return null;
        }
        IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand);
        return state.withProperty(HALF, Half.LOWER);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (state.getValue(HALF) == Half.LOWER) {
            world.setBlockState(pos.up(), state.withProperty(HALF, Half.UPPER), 3);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        boolean isLower = state.getValue(HALF) == Half.LOWER;
        // Remove the other half silently (its own breakBlock is a no-op via the ==-this guard).
        BlockPos other = isLower ? pos.up() : pos.down();
        if (world.getBlockState(other).getBlock() == this) {
            world.setBlockToAir(other);
        }
        // 敲掉发信器时先从频段断开（undeclareReceiver + onReceiverOffline），否则频段不会
        // recompute，其余发信器拿不到被释放的频道、GUI 也不会刷新。
        BlockPos tilePos = isLower ? pos : other;
        TileEntity te = world.getTileEntity(tilePos);
        if (te instanceof TileEnderEmitter) {
            ((TileEnderEmitter) te).cleanConnectionPermanent();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        // Upper half depends on the lower half beneath it.
        if (state.getValue(HALF) == Half.UPPER) {
            IBlockState below = world.getBlockState(pos.down());
            if (below.getBlock() != this || below.getValue(HALF) != Half.LOWER) {
                world.setBlockToAir(pos);
            }
        }
    }

    @Override
    public boolean isFullCube(IBlockState state) { return false; }

    @Override
    public boolean isOpaqueCube(IBlockState state) { return false; }

    @Override
    public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        // Redirect upper-half interaction to the lower half that holds the tile.
        if (state.getValue(HALF) == Half.UPPER) {
            pos = pos.down();
        }
        if (world.isRemote) return true;
        if (world.getTileEntity(pos) instanceof TileEnderEmitter) {
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_ENDER_EMITTER, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) { return true; }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return state.getValue(HALF) == Half.LOWER ? new TileEnderEmitter() : null;
    }
}
