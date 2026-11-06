/*
 * AE2 Crystal Science
 * Quartz Grindstone Block - Manual crystal processing with crank
 */
package ae2csl.block.grindstone;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.tile.grindstone.TileQuartzGrindstone;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class QuartzGrindstoneBlock extends Block {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public QuartzGrindstoneBlock() {
        super(Material.ROCK);
        this.setHardness(2.0F);
        this.setResistance(8.0F);
        this.setDefaultState(this.blockState.getBaseState().withProperty(ACTIVE, Boolean.FALSE).withProperty(POWERED, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ACTIVE, POWERED);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState();
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileQuartzGrindstone) {
            // Open GUI - placeholder
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, ae2csl.client.gui.GuiHandler.GUI_QUARTZ_GRINDSTONE, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        // Could set custom name here
    }

    /** 破坏时掉落物品栏内容，防止拆机器吞物品 */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof net.minecraft.inventory.IInventory) {
            net.minecraft.inventory.InventoryHelper.dropInventoryItems(world, pos,
                (net.minecraft.inventory.IInventory) tile);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileQuartzGrindstone();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }
}