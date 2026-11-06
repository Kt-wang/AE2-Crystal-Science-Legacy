/*
 * AE2 Crystal Science - MirrorPatternProviderBlock (1.12.2)
 * 对照源码：MirrorPatternProviderBlock extends PatternProviderBlock（PUSH_DIRECTION 定向 + 扳手改面）。
 * 右键无 GUI——重定向到被镜像目标的 GUI（见 MirrorPatternProviderGui）。
 */
package ae2csl.block.mirror;

import ae2.block.crafting.PatternProviderBlock;
import ae2.block.crafting.PushDirection;
import ae2.util.InteractionUtil;
import ae2.util.Platform;
import ae2csl.tile.mirror.TileMirrorPatternProvider;

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

public class MirrorPatternProviderBlock extends Block {

    public static final PropertyEnum<PushDirection> PUSH_DIRECTION = PatternProviderBlock.PUSH_DIRECTION;

    public MirrorPatternProviderBlock() {
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
        if (tile instanceof TileMirrorPatternProvider) {
            ((TileMirrorPatternProvider) tile).getLogic().updateRedstoneState();
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
        // 对齐源码：镜像样板供应器没有自己的 GUI，重定向到被镜像目标的 GUI
        if (world.getTileEntity(pos) instanceof TileMirrorPatternProvider tile) {
            ae2csl.me.logic.MirrorPatternProviderGui.openTargetGui(player, tile.getMirroringLogic());
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
        if (tile instanceof TileMirrorPatternProvider) {
            ((TileMirrorPatternProvider) tile).onPushDirectionChanged();
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
        return new TileMirrorPatternProvider();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState s) {
        return EnumBlockRenderType.MODEL;
    }
}
