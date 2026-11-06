/*
 * AE2 Crystal Science - API GlobalPos (1.12.2 compatible)
 *
 * In 1.21.1, GlobalPos uses ResourceKey<Level> for dimension.
 * In 1.12.2, dimensions are identified by int IDs.
 * This class serves as a replacement.
 */
package ae2csl.api.util;

import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public final class GlobalPos {

    private final int dimension;
    private final BlockPos pos;

    public GlobalPos(int dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = Objects.requireNonNull(pos, "pos");
    }

    public int getDimension() {
        return dimension;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GlobalPos that)) return false;
        return dimension == that.dimension && pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        return 31 * dimension + pos.hashCode();
    }

    @Override
    public String toString() {
        return "GlobalPos{dim=" + dimension + ", " + pos + "}";
    }

    /**
     * Create a GlobalPos from dimension and block position.
     */
    public static GlobalPos of(int dimension, BlockPos pos) {
        return new GlobalPos(dimension, pos);
    }
}
