/* AE2 Crystal Science - Ender Broadcaster Block */
package ae2csl.block.ender;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.tile.ender.TileEnderBroadcaster;
import net.minecraft.block.Block; import net.minecraft.block.material.Material; import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer; import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase; import net.minecraft.entity.player.EntityPlayer; import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity; import net.minecraft.util.*; import net.minecraft.util.math.BlockPos; import net.minecraft.world.World;
import javax.annotation.Nullable;
public class EnderBroadcasterBlock extends Block {
    public static final PropertyBool ACTIVE = PropertyBool.create("active"); public static final PropertyBool POWERED = PropertyBool.create("powered");
    public EnderBroadcasterBlock() { super(Material.IRON); setHardness(2.5F); setResistance(10.0F); setDefaultState(blockState.getBaseState().withProperty(ACTIVE, false).withProperty(POWERED, false)); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, ACTIVE, POWERED); }
    @Override public int getMetaFromState(IBlockState s) { return 0; } @Override public IBlockState getStateFromMeta(int m) { return getDefaultState(); }
    @Override public boolean isFullCube(IBlockState s) { return false; } @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public net.minecraft.util.BlockRenderLayer getRenderLayer() { return net.minecraft.util.BlockRenderLayer.CUTOUT; }
    @Override public boolean onBlockActivated(World w, BlockPos p, IBlockState s, EntityPlayer pl, EnumHand h, EnumFacing f, float x, float y, float z) { if (w.isRemote) return true; if (w.getTileEntity(p) instanceof TileEnderBroadcaster) pl.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_ENDER_BROADCASTER, w, p.getX(), p.getY(), p.getZ()); return true; }
    @Override public void onBlockPlacedBy(World w, BlockPos p, IBlockState s, EntityLivingBase pl, ItemStack st) {}
    @Override public boolean hasTileEntity(IBlockState s) { return true; } @Nullable @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileEnderBroadcaster(); }
    @Override public EnumBlockRenderType getRenderType(IBlockState s) { return EnumBlockRenderType.MODEL; }
}
