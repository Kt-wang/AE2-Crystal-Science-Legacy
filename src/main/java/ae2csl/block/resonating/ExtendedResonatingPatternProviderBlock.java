/*
 * AE2 Crystal Science - ExtendedResonatingPatternProviderBlock (1.12.2)
 */
package ae2csl.block.resonating;

import ae2csl.tile.resonating.TileExtendedResonatingPatternProvider;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ExtendedResonatingPatternProviderBlock extends ResonatingPatternProviderBlock {

    @Nullable
    @Override
    public TileEntity createTileEntity(World w, IBlockState s) {
        return new TileExtendedResonatingPatternProvider();
    }
}
