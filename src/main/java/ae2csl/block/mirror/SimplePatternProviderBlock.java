/*
 * AE2 Crystal Science - SimplePatternProviderBlock (1.12.2)
 * 对照源码：注册类型为 AE2 的 PatternProviderBlock（PUSH_DIRECTION 定向推送 + 扳手改面）。
 * 1.12.2 port 用独立方块类绑定 TileSimplePatternProvider。
 */
package ae2csl.block.mirror;

import ae2.block.crafting.PatternProviderBlock;
import ae2.block.crafting.PushDirection;
import ae2.util.InteractionUtil;
import ae2.util.Platform;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.tile.mirror.TileSimplePatternProvider;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
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

public class SimplePatternProviderBlock extends Block {

    public static final PropertyEnum<PushDirection> PUSH_DIRECTION = PatternProviderBlock.PUSH_DIRECTION;

    public SimplePatternProviderBlock() {
        super(Material.IRON);
        setHardness(2.5F);
        setResistance(10.0F);
        setDefaultState(blockState.getBaseState().withProperty(PUSH_DIRECTION, PushDirection.ALL));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, PUSH_DIRECTION);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(PUSH_DIRECTION).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(PUSH_DIRECTION,
            PushDirection.values()[meta % PushDirection.values().length]);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileSimplePatternProvider) {
            // 通知更新红石信号缓存（锁定合成用）
            ((TileSimplePatternProvider) tile).getLogic().updateRedstoneState();
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (InteractionUtil.canWrenchRotate(player, player.getHeldItem(hand), pos, state)) {
            if (!world.isRemote) {
                setSide(world, pos, facing);
            }
            return true;
        }

        if (world.isRemote) {
            return true;
        }
        if (world.getTileEntity(pos) instanceof TileSimplePatternProvider) {
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_SIMPLE_PATTERN_PROVIDER, world,
                pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    /**
     * 扳手修改推送面（逻辑对齐 fork PatternProviderBlock.setSide）
     */
    public void setSide(World world, BlockPos pos, EnumFacing facing) {
        IBlockState currentState = world.getBlockState(pos);
        EnumFacing pushSide = currentState.getValue(PUSH_DIRECTION).getDirection();

        PushDirection newPushDirection;
        if (pushSide == facing.getOpposite()) {
            newPushDirection = PushDirection.fromDirection(facing);
        } else if (pushSide == facing) {
            newPushDirection = PushDirection.ALL;
        } else if (pushSide == null) {
            newPushDirection = PushDirection.fromDirection(facing.getOpposite());
        } else {
            newPushDirection = PushDirection.fromDirection(Platform.rotateAround(pushSide, facing));
        }

        world.setBlockState(pos, currentState.withProperty(PUSH_DIRECTION, newPushDirection), 3);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileSimplePatternProvider) {
            ((TileSimplePatternProvider) tile).onPushDirectionChanged();
        }
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
        return new TileSimplePatternProvider();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState s) {
        return EnumBlockRenderType.MODEL;
    }
}
