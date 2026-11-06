/*
 * AE2 Crystal Science - PatternProviderBindingHelper
 * Ported from 1.21.1 source
 *
 * Helper to resolve pattern providers (Mirror/Resonating) from world interaction.
 */
package ae2csl.item;

import ae2.api.parts.IPartHost;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2csl.me.logic.MirrorPatternProviderHost;
import ae2csl.me.logic.ResonatingPatternProviderHost;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class PatternProviderBindingHelper {

    private PatternProviderBindingHelper() {}

    /**
     * Resolve a pattern provider from a clicked position.
     */
    @Nullable
    public static PatternProviderLogicHost resolvePatternProvider(World level, BlockPos pos, Vec3d clickLocation) {
        TileEntity be = level.getTileEntity(pos);
        if (be instanceof IPartHost) {
            IPartHost partHost = (IPartHost) be;
            // In 1.12.2, IPartHost uses different selection API
            for (EnumFacing side : EnumFacing.VALUES) {
                Object part = partHost.getPart(side);
                if (part instanceof PatternProviderLogicHost) {
                    return (PatternProviderLogicHost) part;
                }
            }
        }

        return be instanceof PatternProviderLogicHost ? (PatternProviderLogicHost) be : null;
    }

    @Nullable
    public static ResonatingPatternProviderHost resolveClickedResonatingProvider(World level, BlockPos pos) {
        PatternProviderLogicHost host = resolvePatternProvider(level, pos, Vec3d.ZERO);
        return host instanceof ResonatingPatternProviderHost ? (ResonatingPatternProviderHost) host : null;
    }

    /**
     * 按点击面解析谐振供应器：先看该面的部件，再看方块本身（供链接器绑定用）。
     */
    @Nullable
    public static ResonatingPatternProviderHost resolveClickedResonatingProvider(World level, BlockPos pos,
                                                                               @Nullable EnumFacing clickedFace) {
        TileEntity be = level.getTileEntity(pos);
        if (be instanceof IPartHost partHost && clickedFace != null) {
            Object part = partHost.getPart(clickedFace);
            if (part instanceof ResonatingPatternProviderHost host) {
                return host;
            }
        }
        return be instanceof ResonatingPatternProviderHost ? (ResonatingPatternProviderHost) be : null;
    }

    @Nullable
    public static MirrorPatternProviderHost resolveClickedMirrorProvider(World level, BlockPos pos) {
        PatternProviderLogicHost host = resolvePatternProvider(level, pos, Vec3d.ZERO);
        return host instanceof MirrorPatternProviderHost ? (MirrorPatternProviderHost) host : null;
    }

    @Nullable
    public static MirrorPatternProviderHost resolveMirrorProvider(World level, BlockPos pos) {
        PatternProviderLogicHost host = resolvePatternProvider(level, pos, Vec3d.ZERO);
        return host instanceof MirrorPatternProviderHost ? (MirrorPatternProviderHost) host : null;
    }

    public static List<MirrorPatternProviderHost> getMirrorProvidersAt(World level, BlockPos pos) {
        List<MirrorPatternProviderHost> hosts = new ArrayList<>();
        TileEntity be = level.getTileEntity(pos);
        if (be instanceof MirrorPatternProviderHost) {
            hosts.add((MirrorPatternProviderHost) be);
        }

        if (be instanceof IPartHost) {
            IPartHost partHost = (IPartHost) be;
            for (EnumFacing side : EnumFacing.VALUES) {
                Object part = partHost.getPart(side);
                if (part instanceof MirrorPatternProviderHost) {
                    hosts.add((MirrorPatternProviderHost) part);
                }
            }
        }

        return hosts;
    }
}
