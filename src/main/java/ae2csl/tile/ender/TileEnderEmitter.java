/*
 * AE2 Crystal Science - TileEnderEmitter (1.12.2)
 * Full port from 1.21.1 EnderEmitterBlockEntity.
 *
 * Wireless connection hub: automatically links nearby ME grid nodes into its own
 * network, and joins a frequency band as a receiver. Client sync, the global
 * emitter index, and the scan/destroy helpers mirror the 1.21.1 source.
 */
package ae2csl.tile.ender;

import ae2.api.networking.*;
import ae2.api.networking.pathing.ControllerState;
import ae2.api.parts.IPart;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.me.GridNode;
import ae2.parts.CableBusContainer;
import ae2.tile.networking.TileCableBus;
import ae2.tile.networking.TileController;
import ae2csl.api.CustomChannelProviderHost;
import ae2csl.api.linker.broadcast.*;
import ae2csl.api.render.ICustomRenderBounding;
import ae2csl.api.util.GlobalChunkPos;
import ae2csl.api.util.GlobalPos;
import ae2csl.init.AE2CSBlocks;
import ae2csl.tile.base.AENetworkedTileBase;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.*;

public class TileEnderEmitter extends AENetworkedTileBase
        implements IConfigurableObject, ICustomRenderBounding,
                   CustomChannelProviderHost, BroadcastReceiverHost {

    public static final int AUTO_AREA_FACTOR = 2;

    /**
     * 以全局区块坐标为索引的发信器位置表，每个区块 key 对应周围 3x3 区块范围内所有发信器
     */
    public static final Map<GlobalChunkPos, Set<BlockPos>> EMITTER_CHUNK_POSITIONS = new HashMap<>();

    private static MinecraftServer boundServer = null;

    public static int maxLinkDistance() { return 16 * AUTO_AREA_FACTOR; }

    private boolean autoMode = false; // 默认关闭自动连接，需玩家在 GUI 中手动开启或用末影连接器手动链接
    private boolean allowAutoLinkCableLike = false;
    private boolean showLinkStatus = false;
    private int linkDistance = 8;
    private final Set<BlockPos> pendingLinkPositions = new HashSet<>();
    private final Set<BlockPos> linkedPositions = new HashSet<>();
    private final Map<BlockPos, List<IGridConnection>> linkedConnections = new HashMap<>();
    private int recentAddedPosCountdown = 2;
    /** 标记节点将被(重新)创建；onGridReady 据此区分「区块重载重建节点」与「机器加入触发的 grid re-boot」。 */
    private boolean nodeRecreated = false;

    // 客户端显示字段
    private boolean active = false;
    private String bandId = "";
    private int bandUsedChannelsForClient = 0;
    private int bandTotalChannelsForClient = 0;
    private int usedChannelsForClient = 0;
    private int maxLinkDistanceForClient = maxLinkDistance();
    private int customMaxChannels = 0;
    private boolean enabledCustomChannel = false;

    public TileEnderEmitter() { super(); }

    @Override protected ItemStack getMachineRepresentation() { return new ItemStack(AE2CSBlocks.ENDER_EMITTER); }

    // 主节点必须是致密频道载波（对齐 1.21.1 源码 EnderEmitterBlockEntity 构造器
    // `getMainNode().setFlags(GridFlags.DENSE_CAPACITY)`）。作为频段接收端时，它需要承载频段
    // 分配的 customMaxChannels 个频道给下游被链接的设备；若用默认 REQUIRE_CHANNEL（普通耗频道
    // 设备）则只能承载 1 个频道，导致连接频段后机器无法运转。
    @Override protected GridFlags[] getMainNodeFlags() { return new GridFlags[]{GridFlags.DENSE_CAPACITY}; }

    // ---- 状态 getters ----
    public boolean isActive() { return active; }
    public boolean isAutoMode() { return autoMode; }
    public int getLinkDistance() { return linkDistance; }
    public boolean allowAutoLinkCableLike() { return allowAutoLinkCableLike; }
    public boolean isShowLinkStatus() { return showLinkStatus; }
    public boolean isConnectedToBand() { return bandId != null && !bandId.isEmpty(); }
    public String getBandName() { return bandId == null ? "" : bandId; }
    public int getBandUsedChannelsForClient() { return bandUsedChannelsForClient; }
    public int getBandTotalChannelsForClient() { return bandTotalChannelsForClient; }
    public int getMaxLinkDistanceForClient() { return maxLinkDistanceForClient; }

    public List<BlockPos> getPendingRenderPositionsSnapshot() { return new ArrayList<>(pendingLinkPositions); }
    public List<BlockPos> getLinkedRenderPositionsSnapshot() { return new ArrayList<>(linkedPositions); }

    public int getUsedChannelsForClient() { return usedChannelsForClient; }

    public int getMaxLinkChannels() { IGridNode n = getMainNode().getNode(); return n == null ? 0 : n.getMaxChannels(); }
    public int getUsedLinkChannels() { IGridNode n = getMainNode().getNode(); return n == null ? 0 : n.getUsedChannels(); }

    // ---- ICustomRenderBounding ----
    @Override public boolean enableCustomRenderBounding() { return showLinkStatus; }
    @Override public int getRange() { return maxLinkDistanceForClient * 2; }
    @Override public AxisAlignedBB getCustomBoundingBox(BlockPos cp) {
        if (enableCustomRenderBounding()) return ICustomRenderBounding.super.getCustomBoundingBox(cp);
        else return new AxisAlignedBB(cp).grow(0, 1, 0);
    }

    // ---- 渲染包围盒 / 渲染距离 (1.12.2 TESR 裁剪控制, 对齐 1.21.1 EnderEmitterRenderer 的
    //      getRenderBoundingBox / shouldRenderOffScreen / getViewDistance) ----
    @Override public AxisAlignedBB getRenderBoundingBox() {
        return getCustomBoundingBox(getPos());
    }

    @Override public double getMaxRenderDistanceSquared() {
        // 对齐源码 getViewDistance() = 96
        return 96.0 * 96.0;
    }

    // ---- IConfigurableObject (1.12.2 简化：直接字段切换，无 IConfigManager) ----
    @Override public IConfigManager getConfigManager() { return null; }

    // ---- CustomChannelProviderHost ----
    @Override public int getMaxChannels() { return Math.max(0, customMaxChannels); }
    @Override public void setMaxChannels(int v) { customMaxChannels = Math.max(0, v); }
    @Override public boolean isEnabledCustomChannel() { return enabledCustomChannel; }
    @Override public void setEnabledCustomChannel(boolean v) { enabledCustomChannel = v; }

    // ---- BroadcastReceiverHost ----
    @Override public int getExpectedChannels() {
        // 对齐 1.21.1：返回已用频道数，有 pending 时额外 +1 用于建连。
        int usedChannels = Math.max(0, getUsedLinkChannels());
        if (usedChannels == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return pendingLinkPositions.isEmpty() ? usedChannels : usedChannels + 1;
    }

    private static int clampChannelCount(long c) { return c <= 0 ? 0 : c >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) c; }

    // ---- 连接 / 断开频段 ----
    public void connectToBand(String id) {
        if (world == null || world.isRemote) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        BroadcastFrequencyBand newBand = FrequencyBandManager.getBand(id);
        if (newBand == null) return;
        GlobalPos gp = new GlobalPos(world.provider.getDimension(), pos);
        if (!bandId.isEmpty()) {
            BroadcastFrequencyBand oldBand = FrequencyBandManager.getBand(bandId);
            if (oldBand != null) {
                oldBand.onReceiverOffline(server, gp);
                if (oldBand != newBand) oldBand.undeclareReceiver(gp);
            }
        }
        newBand.declareReceiver(gp);
        setEnabledCustomChannel(true);
        setMaxChannels(0);
        IGridNode node = getMainNode().getNode();
        if (node != null) newBand.onReceiverOnline(server, gp, node, this);
        bandId = newBand.getName();
        updateExposedSides();
        markDirty();
        markForClientUpdate();
    }

    @Override public void cleanConnectionPermanent() {
        if (world == null || world.isRemote) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        if (!bandId.isEmpty()) {
            BroadcastFrequencyBand band = FrequencyBandManager.getBand(bandId);
            if (band != null) {
                GlobalPos gp = new GlobalPos(world.provider.getDimension(), pos);
                band.onReceiverOffline(server, gp);
                band.undeclareReceiver(gp);
            }
        }
        bandId = "";
        updateExposedSides();
        setEnabledCustomChannel(false);
        setMaxChannels(0);
        markDirty();
        markForClientUpdate();
    }

    /**
     * 对齐 1.21.1 源码 `getGridConnectableSides()`：连接频段后返回空集合(不与物理线缆相连，
     * 频道/供电全部走频段虚拟连接)，未连接时恢复全方向。1.12.2 无该接口，改用
     * {@code ManagedGridNode.setExposedOnSides} 等价实现——被设为空的节点不会被相邻线缆
     * 的 `GridHelper.getExposedNode` 找到(其内部检查 `isExposedOnSide`)，避免接收端误连到
     * 无电的物理线缆网络导致 `isPowered()==false`(active 恒 false)。
     */
    private void updateExposedSides() {
        if (getMainNode() == null) return;
        getMainNode().setExposedOnSides(isConnectedToBand()
                ? EnumSet.noneOf(EnumFacing.class)
                : EnumSet.allOf(EnumFacing.class));
    }

    // ---- 距离 / 模式调整（由 action packet 服务端调用）----
    public void setLinkDistance(int d) {
        int n = Math.max(0, Math.min(d, maxLinkDistance()));
        if (n != linkDistance) { linkDistance = n; markDirty(); markForClientUpdate(); }
    }

    public void toggleAutoMode() { autoMode = !autoMode; markDirty(); markForClientUpdate(); }
    public void toggleShowRange() { showLinkStatus = !showLinkStatus; markDirty(); markForClientUpdate(); }
    public void toggleAutoLinkCableLike() { allowAutoLinkCableLike = !allowAutoLinkCableLike; markDirty(); markForClientUpdate(); }

    // ---- Tick ----
    @Override public void update() {
        if (world == null || world.isRemote) return;
        if (isRecentAddedPos()) return;
        IGridNode selfNode = getMainNode().getNode();
        if (selfNode == null) return;
        refreshClientDisplayState();
        if (active != selfNode.isActive()) {
            active = selfNode.isActive();
            markForClientUpdate();
        }
        if (!active) return;
        if (selfNode.getUsedChannels() >= getMaxLinkChannels()) {
            if (isConnectedToBand()) {
                // 频段模式会在下一次重算中按已用频道扩容，保留待连接项以便继续处理。
                return;
            }
            // 非频段模式：无可用频道时清除 pending
            if (!pendingLinkPositions.isEmpty()) {
                pendingLinkPositions.clear();
                markDirty();
                markForClientUpdate();
            }
            return;
        }

        int pendingSize = pendingLinkPositions.size();
        Iterator<BlockPos> it = pendingLinkPositions.iterator();
        while (it.hasNext()) {
            BlockPos tp = it.next();
            // 不处理未加载区块
            if (!world.isBlockLoaded(tp)) continue;

            List<IGridConnection> old = linkedConnections.remove(tp);
            if (old != null) for (IGridConnection c : old) { if (c != null) c.destroy(); }

            List<IGridNode> targetNodes = getConnectableNodes(world, tp);
            if (targetNodes.isEmpty()) { it.remove(); continue; }

            List<IGridConnection> newConns = new ArrayList<>();
            for (IGridNode tn : targetNodes) {
                if (!(tn instanceof GridNode) || !(selfNode instanceof GridNode)) continue;
                IGrid tg;
                IGrid sg;
                try {
                    tg = ((GridNode) tn).grid();
                    sg = ((GridNode) selfNode).grid();
                } catch (IllegalStateException destroyed) {
                    continue;
                }
                if (tg != null && sg != null && tg.getPathingService().getControllerState() != ControllerState.NO_CONTROLLER) {
                    if (tg != sg) continue; // 不连接导致冲突的网络
                    else if (tn.meetsChannelRequirements()) continue; // 同一网络内对方已有频道则不连
                }
                try { newConns.add(GridHelper.createConnection(selfNode, tn)); } catch (IllegalStateException ignored) {}
            }

            if (newConns.isEmpty()) { it.remove(); continue; }

            linkedConnections.put(tp, newConns);
            it.remove();
            linkedPositions.add(tp);
            break; // 本轮成功一个链接即结束，其余等待下一轮
        }

        if (pendingSize != pendingLinkPositions.size()) { markDirty(); markForClientUpdate(); }
    }

    private boolean isRecentAddedPos() {
        if (recentAddedPosCountdown > 0) { recentAddedPosCountdown--; return true; }
        return false;
    }

    private void addPosToPending(BlockPos pos) {
        pendingLinkPositions.add(pos);
        recentAddedPosCountdown = 2;
        markBandRuntimeDirty();
    }

    private void addListPosToPending(Collection<BlockPos> posList) {
        pendingLinkPositions.addAll(posList);
        recentAddedPosCountdown = 2;
        markBandRuntimeDirty();
    }

    /** 通知频段本发信器需求已变化，触发下一 tick 重新分配额度。 */
    private void markBandRuntimeDirty() {
        if (world == null || world.isRemote || bandId.isEmpty()) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) FrequencyBandManager.markRuntimeDirty(server, bandId);
    }

    private void refreshClientDisplayState() {
        long u = 0, t = 0;
        if (!bandId.isEmpty()) {
            BroadcastFrequencyBand b = FrequencyBandManager.getBand(bandId);
            if (b != null) { u = b.getUsedChannels(); t = b.getUsableChannels(); }
        }
        int nu = clampChannelCount(u), nt = clampChannelCount(t);
        int nuu = getUsedLinkChannels();
        if (bandUsedChannelsForClient != nu || bandTotalChannelsForClient != nt || usedChannelsForClient != nuu) {
            bandUsedChannelsForClient = nu;
            bandTotalChannelsForClient = nt;
            usedChannelsForClient = nuu;
            markForClientUpdate();
        }
    }

    private void markForClientUpdate() {
        World w = getWorld();
        BlockPos p = getPos();
        if (w != null && !w.isRemote) { IBlockState s = w.getBlockState(p); w.notifyBlockUpdate(p, s, s, 3); }
    }

    // ---- 获取可被无线连接的目标节点 ----
    public static List<IGridNode> getConnectableNodes(World level, BlockPos pos) {
        List<IGridNode> nodes = new ArrayList<>();
        TileEntity te = level.getTileEntity(pos);
        if (!(te instanceof IInWorldGridNodeHost)) return nodes;
        IInWorldGridNodeHost nodeHost = (IInWorldGridNodeHost) te;

        // 无线连接拒绝控制器
        if (nodeHost instanceof TileController) return nodes;
        else if (nodeHost instanceof TileCableBus) {
            CableBusContainer cableBus = ((TileCableBus) nodeHost).getCableBus();
            IPart center = cableBus.getPart(null);
            if (center != null) {
                IGridNode n = center.getGridNode(); if (n != null) nodes.add(n);
            } else {
                for (EnumFacing d : EnumFacing.VALUES) {
                    IPart part = cableBus.getPart(d);
                    if (part != null) { IGridNode n = part.getGridNode(); if (n != null) nodes.add(n); }
                }
            }
        } else {
            for (EnumFacing d : EnumFacing.VALUES) {
                IGridNode node = nodeHost.getGridNode(d);
                if (node != null) { nodes.add(node); break; }
            }
        }
        return nodes;
    }

    // ---- 自动链接 helpers（服务端）----
    public static boolean addPosToEmitter(TileEnderEmitter emitter, BlockPos pos, boolean byManual, boolean forceAuto) {
        if (emitter.getPos().equals(pos)) return false;

        // 对齐 1.21.1 源码：手动链接使用 maxLinkDistance()（16×AUTO_AREA_FACTOR=32），
        // 不受 GUI 设定的 linkDistance 限制（linkDistance 仅约束自动链接）。
        boolean valid = byManual && closerThanChebyshev(emitter.getPos(), pos, maxLinkDistance());

        if (!valid) {
            if (emitter.world == null) return false;
            IGridNode emitterNode = emitter.getMainNode().getNode();
            if (emitterNode == null) return false;
            // 对齐 1.21.1：未激活时若已连频段仍允许收集（频段会按需求分配额度）
            if (!emitterNode.isActive() && !emitter.isConnectedToBand()) return false;
            TileEntity targetTe = emitter.world.getTileEntity(pos);
            boolean cableLike = targetTe instanceof TileCableBus;
            boolean hasAvailableChannels = emitterNode.getUsedChannels() < emitter.getMaxLinkChannels();
            valid = (forceAuto || emitter.autoMode)
                    && (hasAvailableChannels || emitter.isConnectedToBand())
                    && (!cableLike || emitter.allowAutoLinkCableLike)
                    && closerThanChebyshev(emitter.getPos(), pos, emitter.linkDistance);
        }

        if (valid) {
            emitter.addPosToPending(pos);
            emitter.markForClientUpdate();
            emitter.markDirty();
            return true;
        }
        return false;
    }

    public static void removePosFromEmitter(TileEnderEmitter emitter, BlockPos targetPos) {
        emitter.pendingLinkPositions.remove(targetPos);
        emitter.linkedPositions.remove(targetPos);
        List<IGridConnection> conns = emitter.linkedConnections.remove(targetPos);
        if (conns != null && !conns.isEmpty()) {
            for (IGridConnection c : conns) { if (c != null) c.destroy(); }
        }
        emitter.markDirty();
        emitter.markForClientUpdate();
        emitter.markBandRuntimeDirty();
    }

    /**
     * 自动将当前位置添加到最近一个可用发信器（通过全局索引快速定位）
     */
    public static void addPosToRecentEmitter(World world, BlockPos targetPos) {
        TileEntity te = world.getTileEntity(targetPos);
        if (!(te instanceof IInWorldGridNodeHost)) return;

        GlobalChunkPos targetChunk = new GlobalChunkPos(world.provider.getDimension(), targetPos);
        Set<BlockPos> positions = EMITTER_CHUNK_POSITIONS.get(targetChunk);
        if (positions == null || positions.isEmpty()) return;

        List<BlockPos> available = new ArrayList<>(positions.size());
        for (BlockPos p : positions) {
            if (closerThanChebyshev(p, targetPos, maxLinkDistance())) available.add(p);
        }
        if (available.isEmpty()) return;

        available.sort(Comparator.comparingDouble(pos -> pos.distanceSq(targetPos)));
        for (BlockPos p : available) {
            if (world.getTileEntity(p) instanceof TileEnderEmitter emitter) {
                if (addPosToEmitter(emitter, targetPos, false, false)) return;
            }
        }
    }

    public static void removePosFromRecentEmitter(World world, BlockPos targetPos) {
        TileEntity te = world.getTileEntity(targetPos);
        if (!(te instanceof IInWorldGridNodeHost)) return;

        GlobalChunkPos targetChunk = new GlobalChunkPos(world.provider.getDimension(), targetPos);
        Set<BlockPos> positions = EMITTER_CHUNK_POSITIONS.get(targetChunk);
        if (positions == null || positions.isEmpty()) return;
        for (BlockPos p : positions) {
            if (world.getTileEntity(p) instanceof TileEnderEmitter emitter) {
                removePosFromEmitter(emitter, targetPos);
            }
        }
    }

    /**
     * 主动将附近已加载区块的所有 BE 加入发信器（scan all）
     */
    public static void addAllRecentBEtoEmitter(TileEnderEmitter emitter) {
        World level = emitter.world;
        if (level == null) return;
        int centerX = emitter.pos.getX() >> 4, centerZ = emitter.pos.getZ() >> 4;
        for (int ox = -AUTO_AREA_FACTOR; ox <= AUTO_AREA_FACTOR; ox++) {
            for (int oz = -AUTO_AREA_FACTOR; oz <= AUTO_AREA_FACTOR; oz++) {
                Chunk chunk = level.getChunkProvider().getLoadedChunk(centerX + ox, centerZ + oz);
                if (chunk == null) continue;
                for (TileEntity be : chunk.getTileEntityMap().values()) {
                    if (be != null) addPosToEmitter(emitter, be.getPos(), false, true);
                }
            }
        }
    }

    /**
     * 手动显式清除所有连接（destroy all）
     */
    public static void removeAllLinkedFromEmitter(TileEnderEmitter emitter) {
        for (List<IGridConnection> conns : emitter.linkedConnections.values()) {
            for (IGridConnection c : conns) { if (c != null) c.destroy(); }
        }
        emitter.linkedConnections.clear();
        emitter.linkedPositions.clear();
        emitter.pendingLinkPositions.clear();
        emitter.markBandRuntimeDirty();
        emitter.markDirty();
        emitter.markForClientUpdate();
    }

    private static boolean closerThanChebyshev(BlockPos a, BlockPos b, int maxDistance) {
        int dx = Math.abs(a.getX() - b.getX());
        if (dx > maxDistance) return false;
        int dy = Math.abs(a.getY() - b.getY());
        if (dy > maxDistance) return false;
        int dz = Math.abs(a.getZ() - b.getZ());
        return dz <= maxDistance;
    }

    // ---- 全局索引生命周期 ----
    public static void ensureBound(@Nullable MinecraftServer server) {
        if (server == null) { EMITTER_CHUNK_POSITIONS.clear(); boundServer = null; return; }
        if (boundServer != server) { EMITTER_CHUNK_POSITIONS.clear(); boundServer = server; }
    }

    private void registerToIndex() {
        if (world == null || world.isRemote) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        ensureBound(server);
        ChunkPos center = new ChunkPos(pos);
        int dim = world.provider.getDimension();
        for (int ox = -AUTO_AREA_FACTOR; ox <= AUTO_AREA_FACTOR; ox++) {
            for (int oz = -AUTO_AREA_FACTOR; oz <= AUTO_AREA_FACTOR; oz++) {
                EMITTER_CHUNK_POSITIONS
                        .computeIfAbsent(new GlobalChunkPos(dim, center.x + ox, center.z + oz), k -> new HashSet<>())
                        .add(pos);
            }
        }
    }

    private void removeFromIndex(World w, BlockPos p) {
        if (w == null || w.isRemote) return;
        ChunkPos center = new ChunkPos(p);
        int dim = w.provider.getDimension();
        for (int ox = -AUTO_AREA_FACTOR; ox <= AUTO_AREA_FACTOR; ox++) {
            for (int oz = -AUTO_AREA_FACTOR; oz <= AUTO_AREA_FACTOR; oz++) {
                GlobalChunkPos key = new GlobalChunkPos(dim, center.x + ox, center.z + oz);
                Set<BlockPos> set = EMITTER_CHUNK_POSITIONS.get(key);
                if (set != null) {
                    set.remove(p);
                    if (set.isEmpty()) EMITTER_CHUNK_POSITIONS.remove(key);
                }
            }
        }
    }

    @Override public void onLoad() {
        // 节点将被(重新)创建：区块重载后 onGridReady 需要做 linked→pending 迁移重连。
        nodeRecreated = true;
        super.onLoad();
    }

    @Override public void invalidate() {
        World w = world;
        BlockPos p = pos;
        super.invalidate();
        removeFromIndex(w, p);
    }

    // ---- Grid 生命周期 ----
    @Override protected void onGridReady() {
        // 节点准备好后加入缓存表
        registerToIndex();
        // 节点重建后按当前频段连接状态恢复物理线缆连接策略
        updateExposedSides();

        // 仅在节点真正重建(区块重载)时，把 linked 转入 pending 等待重连。
        // 机器加入导致的 grid re-boot 也会触发 onGridReady，此时绝不能清空 pending。
        if (nodeRecreated) {
            nodeRecreated = false;
            pendingLinkPositions.clear();
            addListPosToPending(linkedPositions);
            linkedPositions.clear();
        }

        if (world == null || world.isRemote || bandId.isEmpty()) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        BroadcastFrequencyBand band = FrequencyBandManager.getBand(bandId);
        if (band == null) { setEnabledCustomChannel(false); return; }
        GlobalPos gp = new GlobalPos(world.provider.getDimension(), pos);
        band.declareReceiver(gp);
        setEnabledCustomChannel(true);
        IGridNode node = getMainNode().getNode();
        if (node != null) band.onReceiverOnline(server, gp, node, this);
    }

    @Override public void onChunkUnload() {
        if (world != null && !world.isRemote && !bandId.isEmpty()) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            BroadcastFrequencyBand band = FrequencyBandManager.getBand(bandId);
            if (server != null && band != null) {
                band.onReceiverOffline(server, new GlobalPos(world.provider.getDimension(), pos));
            }
        }
        super.onChunkUnload();
    }

    @Override public void onStateChanged(AENetworkedTileBase owner, IGridNode node, State state) {
        super.onStateChanged(owner, node, state);
        if (state == State.CHANNEL || state == State.GRID_BOOT) {
            if (world != null && !world.isRemote && !bandId.isEmpty()) {
                MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                if (server != null) FrequencyBandManager.markRuntimeDirty(server, bandId);
            }
        }
    }

    // ---- NBT ----
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        linkDistance = tag.getInteger("link_distance");
        bandId = tag.getString("band_id");
        enabledCustomChannel = tag.getBoolean("enabled_custom_channel");
        customMaxChannels = tag.getInteger("custom_max_channels");
        autoMode = tag.getBoolean("auto_mode");
        allowAutoLinkCableLike = tag.getBoolean("auto_link_cable");
        showLinkStatus = tag.getBoolean("show_range");
        linkedPositions.clear();
        NBTTagList list = tag.getTagList("linked_positions", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound pt = list.getCompoundTagAt(i);
            linkedPositions.add(new BlockPos(pt.getInteger("x"), pt.getInteger("y"), pt.getInteger("z")));
        }
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("link_distance", linkDistance);
        tag.setString("band_id", bandId);
        tag.setBoolean("enabled_custom_channel", enabledCustomChannel);
        tag.setInteger("custom_max_channels", customMaxChannels);
        tag.setBoolean("auto_mode", autoMode);
        tag.setBoolean("auto_link_cable", allowAutoLinkCableLike);
        tag.setBoolean("show_range", showLinkStatus);
        NBTTagList list = new NBTTagList();
        for (BlockPos p : linkedPositions) {
            NBTTagCompound pt = new NBTTagCompound();
            pt.setInteger("x", p.getX()); pt.setInteger("y", p.getY()); pt.setInteger("z", p.getZ());
            list.appendTag(pt);
        }
        tag.setTag("linked_positions", list);
        return tag;
    }

    // ---- 客户端同步 ----
    @Override public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        writeClientState(tag);
        return tag;
    }

    @Override public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        applyClientState(tag);
    }

    @Override public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        applyClientState(pkt.getNbtCompound());
        // 重新渲染方块模型，使 getActualState 派生的 active 属性(off_form→on_form)及时切换
        World w = getWorld();
        if (w != null && w.isRemote) w.markBlockRangeForRenderUpdate(getPos(), getPos());
    }

    private void writeClientState(NBTTagCompound tag) {
        tag.setBoolean("c_auto_mode", autoMode);
        tag.setBoolean("c_active", active);
        tag.setBoolean("c_show_range", showLinkStatus);
        tag.setBoolean("c_auto_link_cable", allowAutoLinkCableLike);
        tag.setString("c_band_id", bandId);
        tag.setInteger("c_band_used", bandUsedChannelsForClient);
        tag.setInteger("c_band_total", bandTotalChannelsForClient);
        tag.setInteger("c_used", usedChannelsForClient);
        tag.setInteger("c_max_distance", maxLinkDistanceForClient);
        tag.setInteger("c_link_distance", linkDistance);

        NBTTagList pending = new NBTTagList();
        for (BlockPos p : pendingLinkPositions) {
            NBTTagCompound pt = new NBTTagCompound();
            pt.setInteger("x", p.getX()); pt.setInteger("y", p.getY()); pt.setInteger("z", p.getZ());
            pending.appendTag(pt);
        }
        tag.setTag("c_pending", pending);

        NBTTagList linked = new NBTTagList();
        for (BlockPos p : linkedPositions) {
            NBTTagCompound pt = new NBTTagCompound();
            pt.setInteger("x", p.getX()); pt.setInteger("y", p.getY()); pt.setInteger("z", p.getZ());
            linked.appendTag(pt);
        }
        tag.setTag("c_linked", linked);
    }

    private void applyClientState(NBTTagCompound tag) {
        if (tag.hasKey("c_auto_mode")) autoMode = tag.getBoolean("c_auto_mode");
        if (tag.hasKey("c_active")) active = tag.getBoolean("c_active");
        if (tag.hasKey("c_show_range")) showLinkStatus = tag.getBoolean("c_show_range");
        if (tag.hasKey("c_auto_link_cable")) allowAutoLinkCableLike = tag.getBoolean("c_auto_link_cable");
        if (tag.hasKey("c_band_id")) bandId = tag.getString("c_band_id");
        if (tag.hasKey("c_band_used")) bandUsedChannelsForClient = tag.getInteger("c_band_used");
        if (tag.hasKey("c_band_total")) bandTotalChannelsForClient = tag.getInteger("c_band_total");
        if (tag.hasKey("c_used")) usedChannelsForClient = tag.getInteger("c_used");
        if (tag.hasKey("c_max_distance")) maxLinkDistanceForClient = tag.getInteger("c_max_distance");
        if (tag.hasKey("c_link_distance")) linkDistance = tag.getInteger("c_link_distance");

        if (tag.hasKey("c_pending")) {
            pendingLinkPositions.clear();
            NBTTagList list = tag.getTagList("c_pending", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound pt = list.getCompoundTagAt(i);
                pendingLinkPositions.add(new BlockPos(pt.getInteger("x"), pt.getInteger("y"), pt.getInteger("z")));
            }
        }
        if (tag.hasKey("c_linked")) {
            linkedPositions.clear();
            NBTTagList list = tag.getTagList("c_linked", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound pt = list.getCompoundTagAt(i);
                linkedPositions.add(new BlockPos(pt.getInteger("x"), pt.getInteger("y"), pt.getInteger("z")));
            }
        }
    }

    public ItemStack getMainMenuIcon() { return new ItemStack(AE2CSBlocks.ENDER_EMITTER); }
}
