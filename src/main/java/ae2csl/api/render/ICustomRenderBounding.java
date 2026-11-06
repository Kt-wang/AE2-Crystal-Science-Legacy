/*
 * AE2 Crystal Science - API ICustomRenderBounding Interface
 * Ported from 1.21.1 source
 */
package ae2csl.api.render;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public interface ICustomRenderBounding {

    boolean enableCustomRenderBounding();

    int getRange();

    default AxisAlignedBB getCustomBoundingBox(BlockPos centerPos) {
        if (enableCustomRenderBounding()) {
            return new AxisAlignedBB(
                    centerPos.getX() - getRange(),
                    centerPos.getY() - getRange(),
                    centerPos.getZ() - getRange(),
                    centerPos.getX() + getRange(),
                    centerPos.getY() + getRange(),
                    centerPos.getZ() + getRange());
        } else {
            return new AxisAlignedBB(centerPos);
        }
    }
}
