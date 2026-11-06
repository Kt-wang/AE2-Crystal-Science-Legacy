/*
 * AE2 Crystal Science
 * Crystal Growth Chamber Block
 * A machine that accelerates crystal growth using AE power
 */
package ae2csl.block.crystal;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.tile.crystal.TileCrystalGrowthChamber;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class CrystalGrowthChamberBlock extends Block {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public CrystalGrowthChamberBlock() {
        super(Material.IRON);
        this.setHardness(2.2F);
        this.setResistance(11.0F);
        this.setDefaultState(this.blockState.getBaseState().withProperty(ACTIVE, Boolean.FALSE).withProperty(POWERED, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ACTIVE, POWERED);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = 0;
        if (state.getValue(ACTIVE)) {
            meta |= 1;
        }
        if (state.getValue(POWERED)) {
            meta |= 2;
        }
        return meta;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(ACTIVE, (meta & 1) != 0)
                .withProperty(POWERED, (meta & 2) != 0);
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
        if (tile instanceof TileCrystalGrowthChamber) {
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, ae2csl.client.gui.GuiHandler.GUI_CRYSTAL_GROWTH_CHAMBER, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    /** 破坏时掉落物品栏内容（种子/升级卡），防止拆机器吞物品 */
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
        return new TileCrystalGrowthChamber();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }
}