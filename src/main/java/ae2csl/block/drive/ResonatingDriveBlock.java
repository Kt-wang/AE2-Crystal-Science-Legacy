/*
 * AE2 Crystal Science - ResonatingDriveBlock (1.12.2)
 * 谐振ME驱动器：大容量存储元件驱动器（默认 10 页 × 63 格 = 630 元件槽）。
 * 继承 AEBaseTileBlock：自动处理 TE 创建、破坏时掉落库存内容。
 * 实施计划见 .claude/memory/resonating-drive-plan.md。
 */
package ae2csl.block.drive;

import ae2.block.AEBaseTileBlock;
import ae2.block.storage.DriveBlock;
import ae2.api.orientation.IOrientationStrategy;
import ae2.api.orientation.OrientationStrategies;
import ae2.client.render.model.DriveModelData;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.tile.storage.TileResonatingDrive;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

public class ResonatingDriveBlock extends AEBaseTileBlock<TileResonatingDrive> {

    public ResonatingDriveBlock() {
        super(Material.IRON);
        setHardness(2.5F);
        setResistance(10.0F);
        setTileEntity(TileResonatingDrive.class);
    }

    /** 正面朝向玩家放置（熔炉式水平朝向），扳手可旋转 */
    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.horizontalFacing();
    }

    /** 复用 AE2 驱动器外壳模型（镂空框架），与原版一致需要 CUTOUT 层 */
    @Override
    public net.minecraft.util.BlockRenderLayer getRenderLayer() {
        return net.minecraft.util.BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, net.minecraft.util.BlockRenderLayer layer) {
        return layer == net.minecraft.util.BlockRenderLayer.CUTOUT;
    }

    /**
     * 复用原版 DriveBlock.RENDER_STATE 未列出属性：DriveBakedModel 从这里读舱内元件数据。
     * 方块状态需声明为 ExtendedBlockState 并带上该属性（对照 DriveBlock.createBlockState）。
     */
    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this,
            getOrientationStrategy().getProperties().toArray(new IProperty<?>[0]),
            new IUnlistedProperty<?>[]{DriveBlock.RENDER_STATE});
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (!(state instanceof IExtendedBlockState)) {
            return state;
        }
        TileEntity tile = world.getTileEntity(pos);
        DriveModelData data = tile instanceof TileResonatingDrive drive
            ? DriveModelData.fromDrive(drive)
            : DriveModelData.createEmpty(10);
        return ((IExtendedBlockState) state).withProperty(DriveBlock.RENDER_STATE, data);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ)) {
            return true;
        }

        TileResonatingDrive tile = this.getTileEntity(world, pos);
        if (tile != null) {
            if (!world.isRemote) {
                player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_RESONATING_DRIVE, world,
                    pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return false;
    }
}
