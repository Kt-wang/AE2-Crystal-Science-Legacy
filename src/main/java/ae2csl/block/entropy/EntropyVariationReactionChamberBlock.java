/*
 * AE2 Crystal Science - Entropy Variation Reaction Chamber Block
 */
package ae2csl.block.entropy;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;

import ae2csl.tile.entropy.TileEntropyVariationChamber;
import net.minecraft.block.Block; import net.minecraft.block.material.Material; import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer; import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase; import net.minecraft.entity.player.EntityPlayer; import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity; import net.minecraft.util.*; import net.minecraft.util.math.BlockPos; import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;
import javax.annotation.Nullable;

public class EntropyVariationReactionChamberBlock extends Block {
    public static final PropertyBool ACTIVE = PropertyBool.create("active"); public static final PropertyBool POWERED = PropertyBool.create("powered");
    public EntropyVariationReactionChamberBlock() { super(Material.IRON); setHardness(2.5F); setResistance(10.0F); setDefaultState(blockState.getBaseState().withProperty(ACTIVE, false).withProperty(POWERED, false)); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, ACTIVE, POWERED); }
    @Override public int getMetaFromState(IBlockState s) { return 0; } @Override public IBlockState getStateFromMeta(int m) { return getDefaultState(); }
    @Override public boolean isFullCube(IBlockState s) { return false; } @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean onBlockActivated(World w, BlockPos p, IBlockState s, EntityPlayer pl, EnumHand h, EnumFacing f, float x, float y, float z) { if (w.isRemote) return true; if (w.getTileEntity(p) instanceof TileEntropyVariationChamber) { if (FluidUtil.interactWithFluidHandler(pl, h, w, p, f)) return true; pl.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_ENTROPY_VARIATION_CHAMBER, w, p.getX(), p.getY(), p.getZ()); } return true; }
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

    @Override public boolean hasTileEntity(IBlockState s) { return true; } @Nullable @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileEntropyVariationChamber(); }
    @Override public EnumBlockRenderType getRenderType(IBlockState s) { return EnumBlockRenderType.MODEL; }
}
