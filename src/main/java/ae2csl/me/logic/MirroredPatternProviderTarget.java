/*
 * AE2 Crystal Science - MirroredPatternProviderTarget
 * Ported from 1.21.1 source
 *
 * Holds the target position (dimension + blockpos + optional side) for a mirror pattern provider.
 * Used by MirrorLinkerItem to bind providers.
 */
package ae2csl.me.logic;

import ae2.api.parts.IPartHost;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2csl.api.util.GlobalPos;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;

public final class MirroredPatternProviderTarget {

    private static final String TAG_MIRROR = "mirror_target";
    private static final String TAG_DIMENSION = "dimension";
    private static final String TAG_POS = "pos";
    private static final String TAG_HAS_SIDE = "has_side";
    private static final String TAG_SIDE = "side";

    private final GlobalPos pos;
    @Nullable
    private final EnumFacing side;

    public MirroredPatternProviderTarget(GlobalPos pos, @Nullable EnumFacing side) {
        this.pos = pos;
        this.side = side;
    }

    public GlobalPos getPos() { return pos; }

    @Nullable
    public EnumFacing getSide() { return side; }

    // ---- NBT ----

    @Nullable
    public static MirroredPatternProviderTarget read(NBTTagCompound root) {
        if (!root.hasKey(TAG_MIRROR, 10)) return null;

        NBTTagCompound tag = root.getCompoundTag(TAG_MIRROR);
        if (!tag.hasKey(TAG_DIMENSION, 3) || !tag.hasKey(TAG_POS, 10)) return null;

        int dim = tag.getInteger(TAG_DIMENSION);
        NBTTagCompound posTag = tag.getCompoundTag(TAG_POS);
        BlockPos bp = new BlockPos(posTag.getInteger("x"), posTag.getInteger("y"), posTag.getInteger("z"));
        EnumFacing readSide = null;
        if (tag.getBoolean(TAG_HAS_SIDE) && tag.hasKey(TAG_SIDE, 3)) {
            readSide = EnumFacing.byIndex(tag.getInteger(TAG_SIDE));
        }

        return new MirroredPatternProviderTarget(new GlobalPos(dim, bp), readSide);
    }

    public static void write(@Nullable MirroredPatternProviderTarget target, NBTTagCompound root) {
        if (target == null) {
            root.removeTag(TAG_MIRROR);
            return;
        }

        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(TAG_DIMENSION, target.pos.getDimension());

        NBTTagCompound posTag = new NBTTagCompound();
        posTag.setInteger("x", target.pos.getPos().getX());
        posTag.setInteger("y", target.pos.getPos().getY());
        posTag.setInteger("z", target.pos.getPos().getZ());
        tag.setTag(TAG_POS, posTag);

        tag.setBoolean(TAG_HAS_SIDE, target.side != null);
        if (target.side != null) {
            tag.setInteger(TAG_SIDE, target.side.getIndex());
        }
        root.setTag(TAG_MIRROR, tag);
    }

    // ---- PacketBuffer ----

    public void write(PacketBuffer buf) {
        buf.writeInt(pos.getDimension());
        buf.writeInt(pos.getPos().getX());
        buf.writeInt(pos.getPos().getY());
        buf.writeInt(pos.getPos().getZ());
        buf.writeBoolean(side != null);
        if (side != null) {
            buf.writeEnumValue(side);
        }
    }

    public static MirroredPatternProviderTarget read(PacketBuffer buf) {
        int dim = buf.readInt();
        BlockPos bp = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        EnumFacing side = buf.readBoolean() ? buf.readEnumValue(EnumFacing.class) : null;
        return new MirroredPatternProviderTarget(new GlobalPos(dim, bp), side);
    }

    /**
     * Resolve the target to a PatternProviderLogicHost in the given world.
     */
    @Nullable
    public PatternProviderLogicHost resolve(World currentLevel) {
        World level = currentLevel;
        if (level == null || level.provider.getDimension() != pos.getDimension()) {
            level = DimensionManager.getWorld(pos.getDimension());
        }

        if (!(level instanceof WorldServer) || !level.isBlockLoaded(pos.getPos())) {
            return null;
        }

        TileEntity be = level.getTileEntity(pos.getPos());
        if (side == null) {
            return be instanceof PatternProviderLogicHost ? (PatternProviderLogicHost) be : null;
        }

        if (be instanceof IPartHost) {
            IPartHost partHost = (IPartHost) be;
            Object part = partHost.getPart(side);
            if (part instanceof PatternProviderLogicHost) {
                return (PatternProviderLogicHost) part;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MirroredPatternProviderTarget)) return false;
        MirroredPatternProviderTarget that = (MirroredPatternProviderTarget) o;
        return pos.equals(that.pos) && (side == that.side || (side != null && side.equals(that.side)));
    }

    @Override
    public int hashCode() {
        return 31 * pos.hashCode() + (side != null ? side.hashCode() : 0);
    }
}
