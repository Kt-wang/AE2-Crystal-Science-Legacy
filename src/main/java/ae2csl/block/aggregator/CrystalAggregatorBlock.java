/*
 * AE2 Crystal Science
 * Crystal Aggregator Block - Aggregates crystals
 */
package ae2csl.block.aggregator;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;

import ae2csl.tile.aggregator.TileCrystalAggregator;
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

public class CrystalAggregatorBlock extends Block {
    public static final PropertyBool ACTIVE = PropertyBool.create("active");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public CrystalAggregatorBlock() {
        super(Material.IRON);
        this.setHardness(2.5F); this.setResistance(10.0F);
        this.setDefaultState(this.blockState.getBaseState().withProperty(ACTIVE, false).withProperty(POWERED, false));
    }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, ACTIVE, POWERED); }
    @Override public int getMetaFromState(IBlockState s) { return 0; }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState(); }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean onBlockActivated(World w, BlockPos p, IBlockState s, EntityPlayer pl, EnumHand h, EnumFacing f, float x, float y, float z) {
        if (w.isRemote) return true;
        TileEntity t = w.getTileEntity(p);
        if (t instanceof TileCrystalAggregator) pl.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_CRYSTAL_AGGREGATOR, w, p.getX(), p.getY(), p.getZ());
        return true;
    }
    @Override public void onBlockPlacedBy(World w, BlockPos p, IBlockState s, EntityLivingBase pl, ItemStack st) {}
    /** 破坏时掉落物品栏内容（输入/输出/升级卡），防止拆机器吞物品 */
    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof net.minecraft.inventory.IInventory) {
            net.minecraft.inventory.InventoryHelper.dropInventoryItems(world, pos,
                (net.minecraft.inventory.IInventory) tile);
        }
        super.breakBlock(world, pos, state);
    }

    @Override public boolean hasTileEntity(IBlockState s) { return true; }
    @Nullable @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileCrystalAggregator(); }
    @Override public EnumBlockRenderType getRenderType(IBlockState s) { return EnumBlockRenderType.MODEL; }
}
