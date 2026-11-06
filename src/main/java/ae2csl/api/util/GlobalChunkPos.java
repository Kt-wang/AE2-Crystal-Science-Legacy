/*
 * AE2 Crystal Science - API GlobalChunkPos
 * Ported from 1.21.1 source
 *
 * Represents a chunk position within a dimension.
 * In 1.12.2, dimensions are identified by int IDs.
 */
package ae2csl.api.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class GlobalChunkPos {

    private final int dimension;
    private final ChunkPos chunkPos;

    public GlobalChunkPos(int dimension, ChunkPos chunkPos) {
        this.dimension = dimension;
        this.chunkPos = chunkPos;
    }

    public GlobalChunkPos(int dimension, BlockPos pos) {
        this(dimension, new ChunkPos(pos));
    }

    public GlobalChunkPos(int dimension, int x, int z) {
        this(dimension, new ChunkPos(x, z));
    }

    /**
     * Construct from a packed ChunkPos long (upper 32 bits = x, lower 32 bits = z).
     */
    public GlobalChunkPos(int dimension, long packedPos) {
        this(dimension, (int)(packedPos >> 32), (int) packedPos);
    }

    public int getDimension() {
        return dimension;
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GlobalChunkPos)) return false;
        GlobalChunkPos that = (GlobalChunkPos) o;
        return dimension == that.dimension && chunkPos.equals(that.chunkPos);
    }

    @Override
    public int hashCode() {
        return 31 * dimension + chunkPos.hashCode();
    }

    @Override
    public String toString() {
        return "GlobalChunkPos{dim=" + dimension + ", " + chunkPos + "}";
    }
}
