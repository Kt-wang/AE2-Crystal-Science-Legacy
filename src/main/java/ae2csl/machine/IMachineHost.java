/*
 * AE2 Crystal Science - IMachineHost (1.12.2)
 */
package ae2csl.machine;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import javax.annotation.Nullable;

public interface IMachineHost {

    MachineComponentContainer getMachineComponents();

    @Nullable World getLevel();

    BlockPos getBlockPos();

    IBlockState getBlockState();

    boolean isClientSide();

    void markChanged();

    void markForClientUpdate();

    void invalidCap();
}
