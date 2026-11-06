/*
 * AE2 Crystal Science - QuartzOscillatorClockBlock (1.12.2)
 * 对照 1.21.1 源码 common/block/QuartzOscillatorClockBlock.java
 * ACTIVE 属性 + 红石输出（脉冲时对所有目标面输出 15）。
 */
package ae2csl.block.oscillator;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.tile.oscillator.TileQuartzOscillatorClock;

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
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class QuartzOscillatorClockBlock extends Block {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public QuartzOscillatorClockBlock() {
        super(Material.IRON);
        setHardness(2.5F);
        setResistance(10.0F);
        setDefaultState(blockState.getBaseState().withProperty(ACTIVE, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ACTIVE);
    }

    @Override
    public int getMetaFromState(IBlockState s) {
        return s.getValue(ACTIVE) ? 1 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int m) {
        return getDefaultState().withProperty(ACTIVE, m == 1);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos p, IBlockState s, EntityPlayer pl, EnumHand h,
                                    EnumFacing f, float x, float y, float z) {
        if (w.isRemote) {
            return true;
        }
        if (!pl.isSneaking() && w.getTileEntity(p) instanceof TileQuartzOscillatorClock) {
            pl.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_QUARTZ_OSCILLATOR_CLOCK, w,
                p.getX(), p.getY(), p.getZ());
        }
        return true;
    }

    // ---- 红石输出（对照源码 getSignal/getDirectSignal/isSignalSource） ----

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        if (!state.getValue(ACTIVE)) {
            return 0;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileQuartzOscillatorClock clock) {
            // side = 被供电的邻居相对本方块的方向
            return clock.getTargets().contains(side) ? 15 : 0;
        }
        return 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return getWeakPower(state, world, pos, side);
    }

    @Override
    public boolean shouldCheckWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        // 我们自己计算每面输出，不需要弱充能检查
        return false;
    }

    /** 破坏时掉落内容物（升级卡），对齐 AEBaseTileBlock.breakBlock */
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
        return new TileQuartzOscillatorClock();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState s) {
        return EnumBlockRenderType.MODEL;
    }
}
