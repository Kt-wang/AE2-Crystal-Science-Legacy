/*
 * AE2 Crystal Science - API AE2CSGridHelper
 * Ported from 1.21.1 source
 *
 * Utility methods for interacting with AE2 grid networks.
 */
package ae2csl.api.util;

import ae2.api.networking.GridFlags;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.pathing.ChannelMode;
import ae2.api.networking.pathing.IPathingService;
import ae2.me.GridNode;
import ae2.me.pathfinding.IPathItem;
import ae2.tile.networking.TileController;

import javax.annotation.Nullable;
import java.util.Iterator;

public final class AE2CSGridHelper {

    /**
     * Get a random controller node from the network.
     */
    @Nullable
    public static IGridNode getControlNode(IGrid grid) {
        Iterator<IGridNode> it = grid.getMachineNodes(TileController.class).iterator();
        return it.hasNext() ? it.next() : null;
    }

    /**
     * Get the distributable channels on the path to the controller for a node.
     * - Returns 0 during network booting/repathing
     * - Returns Integer.MAX_VALUE for INFINITE mode
     * - Endpoint devices start calculation from their parent node
     */
    public static int getDistributableChannelsOnControllerPath(IGridNode node) {
        if (!(node instanceof GridNode)) return 0;

        GridNode gn = (GridNode) node;
        IGrid grid = gn.grid();
        if (grid == null) return 0;

        IPathingService ps = grid.getPathingService();
        if (ps.channelMode() == ChannelMode.INFINITE) return Integer.MAX_VALUE;

        // During booting/repathing, conservatively return 0
        if (!gn.isActive()) return 0;

        // Controller itself is not a bottleneck
        if (gn.getOwner() instanceof TileController) return Integer.MAX_VALUE;

        // If not a cable-like node, start from parent toward controller
        GridNode cur = isCableLike(gn) ? gn : parentTowardController(gn);
        if (cur == null) return 0;

        int available = Integer.MAX_VALUE;

        // Walk up toward controller, taking min slack on path
        for (int guard = 0; guard < 2048 && cur != null; guard++) {
            if (cur.getOwner() instanceof TileController) break;

            if (!cur.hasFlag(GridFlags.CANNOT_CARRY)) {
                int slack = cur.getMaxChannels() - cur.getUsedChannels();
                if (slack < available) {
                    available = slack;
                    if (available <= 0) return 0;
                }
            }

            cur = parentTowardController(cur);
        }

        return Math.max(0, available);
    }

    /**
     * Check if a node is cable-like (dense cable or regular cable).
     */
    private static boolean isCableLike(GridNode n) {
        return n.hasFlag(GridFlags.DENSE_CAPACITY) || n.hasFlag(GridFlags.PREFERRED);
    }

    /**
     * Get the parent of this grid node on the path toward the controller.
     * In 1.12.2 AE2, getControllerRoute() returns an IPathItem which is the next
     * GridNode on the path toward the controller.
     */
    @Nullable
    private static GridNode parentTowardController(GridNode n) {
        try {
            IPathItem route = n.getControllerRoute();
            return (route instanceof GridNode) ? (GridNode) route : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private AE2CSGridHelper() {}
}
