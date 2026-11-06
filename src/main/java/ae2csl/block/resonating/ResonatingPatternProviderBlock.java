/*
 * AE2 Crystal Science - ResonatingPatternProviderBlock (1.12.2)
 * 对照源码：extends PatternProviderBlock（PUSH_DIRECTION 定向 + 扳手改面）。
 * 放置时读取物品 NBT 里的默认目标，破坏时写回掉落物。
 */
package ae2csl.block.resonating;

import ae2.block.crafting.PatternProviderBlock;
import ae2.block.crafting.PushDirection;
import ae2.util.InteractionUtil;
import ae2.util.Platform;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.item.linker.ResonatingMemoryCardHelper;
import ae2csl.tile.resonating.TileResonatingPatternProvider;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ResonatingPatternProviderBlock extends Block {

    public static final PropertyEnum<PushDirection> PUSH_DIRECTION = PatternProviderBlock.PUSH_DIRECTION;

    public ResonatingPatternProviderBlock() {
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
        if (tile instanceof TileResonatingPatternProvider) {
            ((TileResonatingPatternProvider) tile).getLogic().updateRedstoneState();
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // 本方块直接 extends Block（未继承 AEBaseTileBlock），记忆卡交互需要自己接
        if (ResonatingMemoryCardHelper.tryUseOnBlock(world, pos, player.getHeldItem(hand), player)) {
            return true;
        }

        if (InteractionUtil.canWrenchRotate(player, player.getHeldItem(hand), pos, state)) {
            if (!world.isRemote) {
                setSide(world, pos, facing);
            }
            return true;
        }

        if (world.isRemote) {
            return true;
        }
        if (world.getTileEntity(pos) instanceof TileResonatingPatternProvider) {
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_RESONATING_PATTERN_PROVIDER, world,
                pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    /**
     * 放置时读取物品内的默认目标（对齐源码 setPlacedBy）
     */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer,
                                ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileResonatingPatternProvider tile) {
            tile.getResonatingLogic().readDefaultsFromItem(stack);
            // 快捷栏中有数据的谐振记忆卡：放置时自动应用当前槽配置
            if (placer instanceof EntityPlayer player) {
                ResonatingMemoryCardHelper.tryApplyToBlockEntity(player, tile);
            }
            tile.markDirty();
        }
    }

    /**
     * 破坏时把默认目标写回掉落物（对齐源码 getDrops）
     */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state,
                         int fortune) {
        super.getDrops(drops, world, pos, state, fortune);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileResonatingPatternProvider resonating) {
            resonating.getResonatingLogic().writeDefaultsToDrops(drops,
                net.minecraft.item.Item.getItemFromBlock(this));
        }
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
        if (tile instanceof TileResonatingPatternProvider) {
            ((TileResonatingPatternProvider) tile).onPushDirectionChanged();
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
        return new TileResonatingPatternProvider();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState s) {
        return EnumBlockRenderType.MODEL;
    }
}
