/*
 * AE2 Crystal Science - PatternProviderBindingHelper
 * Ported from 1.21.1 source
 *
 * Helper to resolve pattern providers (Mirror/Resonating) from world interaction.
 */
package ae2csl.item;

import ae2.api.parts.IPart;
import ae2.api.parts.IPartHost;
import ae2.api.parts.SelectedPart;
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
     * 优先用点击位置选择部件（对齐 1.21.1 源码的 selectPartWorld），
     * 否则退回「遍历所有面」的旧行为。
     */
    @Nullable
    public static PatternProviderLogicHost resolvePatternProvider(World level, BlockPos pos, Vec3d clickLocation) {
        TileEntity be = level.getTileEntity(pos);
        if (be instanceof IPartHost partHost) {
            if (clickLocation != null && clickLocation.lengthSquared() > 0) {
                SelectedPart selected = partHost.selectPartWorld(clickLocation);
                if (selected != null && selected.part instanceof PatternProviderLogicHost host) {
                    return host;
                }
            }
            for (EnumFacing side : EnumFacing.VALUES) {
                Object part = partHost.getPart(side);
                if (part instanceof PatternProviderLogicHost host) {
                    return host;
                }
            }
        }

        return be instanceof PatternProviderLogicHost host ? host : null;
    }

    /** 以点击面选部件（对齐源码：先按点击位置选，再退回点击面） */
    @Nullable
    public static SelectedPart selectPart(IPartHost partHost, @Nullable EnumFacing clickedFace,
                                          @Nullable Vec3d clickLocation) {
        if (clickLocation != null && clickLocation.lengthSquared() > 0) {
            SelectedPart selected = partHost.selectPartWorld(clickLocation);
            if (selected != null && selected.part != null) {
                return selected;
            }
        }
        if (clickedFace != null) {
            Object part = partHost.getPart(clickedFace);
            if (part instanceof IPart ipart) {
                return new SelectedPart(ipart, clickedFace);
            }
        }
        return null;
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
        return resolveClickedResonatingProvider(level, pos, clickedFace, null);
    }

    /**
     * 带点击位置的版本：部件优先用 selectPartWorld 精确定位（对齐 1.21.1 源码）。
     */
    @Nullable
    public static ResonatingPatternProviderHost resolveClickedResonatingProvider(World level, BlockPos pos,
                                                                               @Nullable EnumFacing clickedFace,
                                                                               @Nullable Vec3d clickLocation) {
        TileEntity be = level.getTileEntity(pos);
        if (be instanceof IPartHost partHost) {
            SelectedPart selected = selectPart(partHost, clickedFace, clickLocation);
            if (selected != null && selected.part instanceof ResonatingPatternProviderHost host) {
                return host;
            }
        }
        return be instanceof ResonatingPatternProviderHost host ? host : null;
    }

    /** 解析部件供应器所在的面（绑定引用需要它） */
    @Nullable
    public static EnumFacing resolvePartSide(World level, BlockPos pos, @Nullable EnumFacing clickedFace,
                                             @Nullable Vec3d clickLocation) {
        TileEntity be = level.getTileEntity(pos);
        if (!(be instanceof IPartHost partHost)) {
            return null;
        }
        SelectedPart selected = selectPart(partHost, clickedFace, clickLocation);
        if (selected != null && selected.part instanceof ResonatingPatternProviderHost) {
            return selected.side != null ? selected.side : clickedFace;
        }
        return null;
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
