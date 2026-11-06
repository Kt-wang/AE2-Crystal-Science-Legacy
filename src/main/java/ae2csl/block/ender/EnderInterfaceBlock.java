/*
 * AE2 Crystal Science - EnderInterfaceBlock (1.12.2)
 * 对照源码：末影接口使用 AE2 InterfaceBlock（无推送面属性）。
 * 1.12.2 port 用独立方块类绑定 TileEnderInterface。
 */
package ae2csl.block.ender;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.tile.ender.TileEnderInterface;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class EnderInterfaceBlock extends Block {

    public EnderInterfaceBlock() {
        super(Material.IRON);
        setHardness(2.5F);
        setResistance(10.0F);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEnderInterface) {
            ((TileEnderInterface) tile).onNeighborChanged();
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        if (world.getTileEntity(pos) instanceof TileEnderInterface) {
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_ENDER_INTERFACE, world,
                pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    /** 破坏时掉落内容物（样板/升级卡），对齐 AEBaseTileBlock.breakBlock */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof ae2.tile.AEBaseTile && !world.isRemote) {
            java.util.List<net.minecraft.item.ItemStack> drops = new java.util.ArrayList<>();
            ((ae2.tile.AEBaseTile) tile).addAdditionalDrops(drops);
            ((ae2.tile.AEBaseTile) tile).clearContent();
            ae2.util.Platform.spawnDrops(world, pos, drops);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean hasTileEntity(IBlockState s) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World w, IBlockState s) {
        return new TileEnderInterface();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState s) {
        return EnumBlockRenderType.MODEL;
    }
}
