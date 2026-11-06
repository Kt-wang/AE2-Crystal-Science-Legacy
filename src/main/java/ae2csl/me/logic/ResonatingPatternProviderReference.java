/*
 * AE2 Crystal Science - ResonatingPatternProviderReference (1.12.2)
 * 对照 1.21.1 common/me/logic/ResonatingPatternProviderReference。
 *
 * 记录谐振链接器当前编辑的供应器位置；部件供应器还会记录所在面。
 * 1.12.2 适配：维度用 int，GlobalPos 为自建类，side == null 表示方块形态。
 */
package ae2csl.me.logic;

import ae2.api.parts.IPartHost;
import ae2csl.api.util.GlobalPos;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Objects;

public final class ResonatingPatternProviderReference {

    /** 物品 NBT 中保存引用的键 */
    public static final String TAG_REFERENCE = "resonating_provider_ref";

    private static final String TAG_DIM = "dim";
    private static final String TAG_POS = "pos";
    private static final String TAG_SIDE = "side";

    private final GlobalPos pos;
    @Nullable
    private final EnumFacing side;

    public ResonatingPatternProviderReference(GlobalPos pos, @Nullable EnumFacing side) {
        this.pos = Objects.requireNonNull(pos, "pos");
        this.side = side;
    }

    public GlobalPos pos() {
        return pos;
    }

    @Nullable
    public EnumFacing side() {
        return side;
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(TAG_DIM, pos.getDimension());
        tag.setLong(TAG_POS, pos.getPos().toLong());
        tag.setInteger(TAG_SIDE, side == null ? -1 : side.getIndex());
        return tag;
    }

    @Nullable
    public static ResonatingPatternProviderReference read(@Nullable NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(TAG_DIM) || !tag.hasKey(TAG_POS)) {
            return null;
        }
        int dim = tag.getInteger(TAG_DIM);
        BlockPos blockPos = BlockPos.fromLong(tag.getLong(TAG_POS));
        EnumFacing side = null;
        if (tag.hasKey(TAG_SIDE)) {
            int sideIndex = tag.getInteger(TAG_SIDE);
            if (sideIndex >= 0 && sideIndex < EnumFacing.VALUES.length) {
                side = EnumFacing.byIndex(sideIndex);
            }
        }
        return new ResonatingPatternProviderReference(new GlobalPos(dim, blockPos), side);
    }

    /**
     * 解析当前已加载世界中的供应器。客户端仅解析当前维度，服务端可解析其他已加载维度。
     */
    @Nullable
    public ResonatingPatternProviderHost resolve(World currentLevel) {
        if (currentLevel == null) {
            return null;
        }

        World world = currentLevel;
        if (world.provider.getDimension() != pos.getDimension()) {
            MinecraftServer server = world.getMinecraftServer();
            if (server == null) {
                return null;
            }
            world = server.getWorld(pos.getDimension());
            if (world == null) {
                return null;
            }
        }

        BlockPos blockPos = pos.getPos();
        if (!world.isBlockLoaded(blockPos)) {
            return null;
        }

        TileEntity tile = world.getTileEntity(blockPos);
        if (side == null) {
            return tile instanceof ResonatingPatternProviderHost host ? host : null;
        }

        if (tile instanceof IPartHost partHost) {
            Object part = partHost.getPart(side);
            return part instanceof ResonatingPatternProviderHost host ? host : null;
        }
        return null;
    }
}
