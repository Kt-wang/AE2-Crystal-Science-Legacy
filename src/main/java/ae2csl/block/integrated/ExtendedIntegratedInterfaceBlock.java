/*
 * AE2 Crystal Science - ExtendedIntegratedInterfaceBlock (1.12.2)
 * 扩展版集成接口方块（36槽），对照源码 extended_integrated_interface。
 */
package ae2csl.block.integrated;

import ae2csl.tile.integrated.TileExtendedIntegratedInterface;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ExtendedIntegratedInterfaceBlock extends IntegratedInterfaceBlock {

    public ExtendedIntegratedInterfaceBlock() {
        super();
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World w, IBlockState s) {
        return new TileExtendedIntegratedInterface();
    }
}
