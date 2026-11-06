/*
 * AE2 Crystal Science - ExtendedEnderInterfaceBlock (1.12.2)
 */
package ae2csl.block.ender;

import ae2csl.tile.ender.TileExtendedEnderInterface;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ExtendedEnderInterfaceBlock extends EnderInterfaceBlock {

    @Nullable
    @Override
    public TileEntity createTileEntity(World w, IBlockState s) {
        return new TileExtendedEnderInterface();
    }
}
