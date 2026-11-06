/*
 * AE2 Crystal Science - IntegratedInterfaceLogic (1.12.2)
 * 对照 1.21.1 源码 common/me/logic/IntegratedInterfaceLogic.java 完整移植。
 * 同时包含 ME 接口(备货/存储)与样板供应器(推送样板)能力。
 *
 * 与源码差异(1.12.2 适配):
 * - DataComponentMap → NBTTagCompound
 * - PatternProviderTargetCache(fork 包私有) → PatternProviderTarget.get() 直接获取
 * - 1.21.1 BLOCKING_MODE 为 YesNo → fork 为 BlockingMode 三态枚举(NO/YES/STRONG)，
 *   语义对齐 fork PatternProviderLogic: YES=含样板输入则跳过, STRONG=含任意物品则跳过
 * - level.invalidateCapabilities → Platform.notifyBlocksOfNeighbors
 *
 * Port 扩展(源码没有, 予以保留):
 * - multiplier 合并推送(fork ICraftingProvider 接口自带 canMergePatternPush/getMaxPatternPushMultiplier)
 * - pull 模式(从相邻容器按 config 拉取物品)
 */
package ae2csl.me.logic;

import ae2.api.config.Actionable;
import ae2.api.config.BlockingMode;
import ae2.api.config.FuzzyMode;
import ae2.api.config.LockCraftingMode;
import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.implementations.blockentities.ICraftingMachine;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.crafting.ICraftingLink;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.networking.crafting.ICraftingRequester;
import ae2.api.networking.energy.IEnergySource;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.api.storage.StorageHelper;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.core.definitions.AEItems;
import ae2.core.settings.TickRates;
import ae2.helpers.InterfaceLogicHost;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2.helpers.patternprovider.PatternProviderTarget;
import ae2.helpers.patternprovider.UnlockCraftingEvent;
import ae2.me.helpers.MachineSource;
import ae2.util.ConfigInventory;
import ae2.util.Platform;
import ae2.util.inv.AppEngInternalInventory;
import ae2csl.init.AE2CSBlocks;

import com.google.common.collect.ImmutableSet;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class IntegratedInterfaceLogic implements IConfigurableObject, IUpgradeableObject,
    ICraftingProvider, ICraftingRequester {

    /**
     * 实际载体，可能是特定 tile，也可能是 ae 的线缆部件
     */
    protected final IntegratedInterfaceHost host;

    protected final IManagedGridNode mainNode;

    private final ConfigInventory configInv;

    private final ConfigInventory storage;

    private final AppEngInternalInventory patternInventory;

    private final List<IPatternDetails> patterns = new ArrayList<>();

    /** 样板终端用：当前所有已编码样板的 key */
    private final Set<AEItemKey> patternKeys = new HashSet<>();

    private final IConfigManager configManager;

    private final IUpgradeInventory upgrades;

    private final Set<AEKey> patternInputs = new HashSet<>();

    private final List<GenericStack> sendList = new ArrayList<>();

    @Nullable
    private EnumFacing sendDirection;

    private int roundRobinIndex = 0;

    /**
     * 缓存的红石状态。只有在推送完成后才需要跟踪红石状态
     */
    private YesNo redstoneState = YesNo.UNDECIDED;

    @Nullable
    private UnlockCraftingEvent unlockEvent;

    @Nullable
    private GenericStack unlockStack;

    protected final IActionSource actionSource;

    private final AE2CSMultiCraftingTracker craftingTracker;

    private final GenericStack[] plannedWork;

    private boolean hasConfig = false;

    private final int storageSize;

    private final int patternSize;

    // ---- Port 扩展: 合并推送状态 ----
    @Nullable
    private MergeInfo pendingMerge;

    // ---- Port 扩展: pull 模式 ----
    private boolean pullEnabled;

    /** 合并推送暂存信息。 */
    private record MergeInfo(IPatternDetails pattern, PatternProviderTarget target,
                             EnumFacing direction, int multiplier) {
        boolean matches(IPatternDetails other) {
            return pattern.equals(other);
        }
    }

    public IntegratedInterfaceLogic(IManagedGridNode mainNode, IntegratedInterfaceHost host,
                                    int storageSize, int patternSize) {
        this.host = host;

        this.mainNode = mainNode.setFlags(GridFlags.REQUIRE_CHANNEL) // 需要频道
            .addService(ICraftingProvider.class, this) // 可以提供样板（样板供应器）
            .addService(ICraftingRequester.class, this) // 可以请求合成（合成卡请求）
            .addService(IGridTickable.class, new Ticker()); // tick行为

        this.configManager = createConfigManager();

        this.storageSize = storageSize;
        this.patternSize = patternSize;

        this.configInv = ConfigInventory.configStacks(storageSize)
            .changeListener(this::onConfigRowChanged)
            .allowOverstacking(true)
            .build();
        this.storage = ConfigInventory.storage(storageSize)
            .slotFilter(this::isAllowedInStorageSlot)
            .changeListener(this::onStorageChanged)
            .allowOverstacking(true)
            .build();
        this.plannedWork = new GenericStack[storageSize];

        this.patternInventory = new AppEngInternalInventory(null, patternSize, 1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (!super.isItemValid(slot, stack) || !PatternDetailsHelper.isEncodedPattern(stack)) {
                    return false;
                }
                // fork 架构下合成样板由分子装配室自持自答，供应器/接口不接收
                World level = host.getTileEntity().getWorld();
                if (level == null) {
                    return false;
                }
                IPatternDetails details = PatternDetailsHelper.decodePattern(stack, level);
                return !(details instanceof ae2.api.crafting.IAssemblerPattern);
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                updatePatterns();
            }
        };

        this.actionSource = new MachineSource(mainNode::getNode);
        this.craftingTracker = new AE2CSMultiCraftingTracker(this, storageSize);
        this.upgrades = ae2.api.upgrades.UpgradeInventories.forMachine(getMachineItem(), 1,
            this::onUpgradesChanged);

        // 为GenericStackInv的单槽位容量使用AE默认注册容量
        this.configInv.useRegisteredCapacities();
        this.storage.useRegisteredCapacities();
    }

    public @Nullable GenericStack getUnlockStack() {
        return unlockStack;
    }

    @Override
    public IConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return this.patterns;
    }

    @Override
    public int getPatternPriority() {
        return this.host.getPriority();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, int multiplier) {
        if (!sendList.isEmpty() || !this.mainNode.isActive() || !this.patterns.contains(patternDetails)) {
            return false;
        }

        TileEntity tileEntity = host.getTileEntity();
        World level = tileEntity.getWorld();

        if (level == null) {
            return false;
        }

        if (getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false;
        }

        // 合并推送快速通道 — 使用预算好的目标（port 扩展）
        MergeInfo merge = this.pendingMerge;
        this.pendingMerge = null;
        if (merge != null && merge.matches(patternDetails) && multiplier > 1) {
            return doPushToTarget(patternDetails, inputHolder, multiplier, merge.target(), merge.direction());
        }

        record PushTarget(EnumFacing direction, PatternProviderTarget target) {
        }
        List<PushTarget> possibleTargets = new ArrayList<>();

        // 首先尝试给ICraftingMachine推送合成
        // 如果没有，则再找可能的目标，并加入possibleTargets
        for (EnumFacing direction : getActiveSides()) {
            BlockPos adjPos = tileEntity.getPos().offset(direction);
            EnumFacing adjBeSide = direction.getOpposite();

            ICraftingMachine craftingMachine = ICraftingMachine.of(level, adjPos, adjBeSide);
            if (craftingMachine != null && craftingMachine.acceptsPlans()) {
                if (craftingMachine.pushPattern(patternDetails, inputHolder, multiplier, adjBeSide)) {
                    onPushPatternSuccess(patternDetails);
                    return true;
                }
                continue;
            }

            PatternProviderTarget adapter = findAdapter(direction);
            if (adapter == null) {
                continue;
            }

            possibleTargets.add(new PushTarget(direction, adapter));
        }

        // 如果没有合适的ICraftingMachine，且此配方并不支持外部仓库运行，则到此停止
        if (!patternDetails.supportsPushInputsToExternalInventory()) {
            return false;
        }

        // 再安排轮询顺序
        rearrangeRoundRobin(possibleTargets);

        // 向其他类型的方块推送样板
        for (int i = 0; i < possibleTargets.size(); ++i) {
            PushTarget target = possibleTargets.get(i);
            EnumFacing direction = target.direction();
            PatternProviderTarget adapter = target.target();

            // 如果开启了阻塞模式，且对方包含我们的某个样板输入
            if (isTargetBlocked(adapter)) {
                continue;
            }

            // 只要目标能接收样板中所有种类的输入，我们就推送
            // 无论本次是否真的完全推送了所有资源，都视为完成，所有未完成资源添加到sendList，等待其他逻辑重新推送
            if (this.adapterAcceptsAll(adapter, inputHolder)) {
                doPushToTarget(patternDetails, inputHolder, multiplier, adapter, direction);
                roundRobinIndex += i + 1;
                return true;
            }
        }

        return false;
    }

    /**
     * 执行实际的推送（含 multiplier 次数），并立即尝试清空 sendList。
     */
    private boolean doPushToTarget(IPatternDetails patternDetails, KeyCounter[] inputHolder,
                                   int multiplier, PatternProviderTarget adapter, EnumFacing direction) {
        for (int m = 0; m < multiplier; m++) {
            patternDetails.pushInputsToExternalInventory(inputHolder, (what, amount) -> {
                long inserted = adapter.insert(what, amount, Actionable.MODULATE);
                if (inserted < amount) {
                    this.addToSendList(what, amount - inserted);
                }
            });
        }
        onPushPatternSuccess(patternDetails);
        this.sendDirection = direction;
        this.sendStacksOut(); // 在这里立刻对外发送一次sendList中的东西
        return true;
    }

    @Override
    public boolean canMergePatternPush(IPatternDetails patternDetails) {
        if (!sendList.isEmpty() || !mainNode.isActive() || !patterns.contains(patternDetails)
            || getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false;
        }
        TileEntity tile = host.getTileEntity();
        World level = tile.getWorld();
        if (level == null) {
            return false;
        }
        // 检查是否至少有一个目标能接收该样板
        for (EnumFacing direction : getActiveSides()) {
            PatternProviderTarget adapter = findAdapter(direction);
            if (adapter == null || isTargetBlocked(adapter)) {
                continue;
            }
            KeyCounter[] inputs = buildInputHolder(patternDetails);
            if (adapterAcceptsAll(adapter, inputs)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getMaxPatternPushMultiplier(IPatternDetails patternDetails, int maxMultiplier) {
        this.pendingMerge = null;
        if (maxMultiplier <= 0 || !sendList.isEmpty() || !mainNode.isActive()
            || !patterns.contains(patternDetails)
            || getCraftingLockedReason() != LockCraftingMode.NONE) {
            return 0;
        }

        TileEntity tile = host.getTileEntity();
        World level = tile.getWorld();
        if (level == null) {
            return 0;
        }

        KeyCounter[] inputs = buildInputHolder(patternDetails);
        int bestMultiplier = 0;
        PatternProviderTarget bestTarget = null;
        EnumFacing bestDirection = null;

        for (EnumFacing direction : getActiveSides()) {
            PatternProviderTarget adapter = findAdapter(direction);
            if (adapter == null || isTargetBlocked(adapter)) {
                continue;
            }
            int accepted = calcMaxAccepted(adapter, inputs, maxMultiplier);
            if (accepted > bestMultiplier) {
                bestMultiplier = accepted;
                bestTarget = adapter;
                bestDirection = direction;
                if (accepted >= maxMultiplier) {
                    break; // 不可能更好了
                }
            }
        }

        if (bestMultiplier > 1 && bestTarget != null) {
            this.pendingMerge = new MergeInfo(patternDetails, bestTarget, bestDirection, bestMultiplier);
        }
        return bestMultiplier;
    }

    /** 为样板构建 KeyCounter[] 输入容器（用于模拟）。 */
    private KeyCounter[] buildInputHolder(IPatternDetails patternDetails) {
        IPatternDetails.IInput[] inputs = patternDetails.getInputs();
        KeyCounter[] holder = new KeyCounter[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            holder[i] = new KeyCounter();
            for (GenericStack candidate : inputs[i].possibleInputs()) {
                if (candidate != null) {
                    holder[i].add(candidate.what(), candidate.amount());
                }
            }
        }
        return holder;
    }

    /** 通过模拟插入计算目标能接受的最大倍数。 */
    private int calcMaxAccepted(PatternProviderTarget target, KeyCounter[] inputs, int maxMultiplier) {
        int accepted = 0;
        for (int m = 1; m <= maxMultiplier; m++) {
            boolean allOk = true;
            for (KeyCounter kc : inputs) {
                for (var entry : kc) {
                    long needed = entry.getLongValue() * m;
                    long inserted = target.insert(entry.getKey(), needed, Actionable.SIMULATE);
                    if (inserted < needed) {
                        allOk = false;
                        break;
                    }
                }
                if (!allOk) {
                    break;
                }
            }
            if (allOk) {
                accepted = m;
            } else {
                break;
            }
        }
        return accepted;
    }

    /**
     * 重新解析样板信息，并通知ae网络获取
     */
    public void updatePatterns() {
        patterns.clear();
        patternInputs.clear();
        patternKeys.clear();

        TileEntity tileEntity = host.getTileEntity();
        World level = tileEntity != null ? tileEntity.getWorld() : null;

        for (ItemStack stack : this.patternInventory) {
            if (stack.isEmpty()) {
                continue;
            }
            AEItemKey patternKey = AEItemKey.of(stack);
            if (patternKey != null) {
                patternKeys.add(patternKey);
            }
            IPatternDetails details = PatternDetailsHelper.decodePattern(stack, level);

            // fork 架构下合成样板由分子装配室自持自答，接口不提供给网络
            if (details != null && !(details instanceof ae2.api.crafting.IAssemblerPattern)) {
                patterns.add(details);

                for (IPatternDetails.IInput iinput : details.getInputs()) {
                    for (GenericStack inputCandidate : iinput.possibleInputs()) {
                        if (inputCandidate != null) {
                            patternInputs.add(inputCandidate.what().dropSecondary());
                        }
                    }
                }
            }
        }

        ICraftingProvider.requestUpdate(mainNode);
    }

    /**
     * 获取当前合成供应被锁定的原因
     *
     * @return 如果未被锁定则返回 NONE
     */
    public LockCraftingMode getCraftingLockedReason() {
        if (!configManager.hasSetting(Settings.LOCK_CRAFTING_MODE)) {
            return LockCraftingMode.NONE;
        }
        var lockMode = configManager.getSetting(Settings.LOCK_CRAFTING_MODE);
        if (lockMode == LockCraftingMode.LOCK_WHILE_LOW && !getRedstoneState()) {
            // 被红石信号锁定
            return LockCraftingMode.LOCK_WHILE_LOW;
        } else if (lockMode == LockCraftingMode.LOCK_WHILE_HIGH && getRedstoneState()) {
            return LockCraftingMode.LOCK_WHILE_HIGH;
        } else if (unlockEvent != null) {
            // 等待解锁事件
            switch (unlockEvent) {
                case REDSTONE_POWER, REDSTONE_PULSE -> {
                    return LockCraftingMode.LOCK_UNTIL_PULSE;
                }
                case RESULT -> {
                    return LockCraftingMode.LOCK_UNTIL_RESULT;
                }
            }
        }
        return LockCraftingMode.NONE;
    }

    /**
     * 当样板推送完成，刷新一次锁定状态
     */
    private void onPushPatternSuccess(IPatternDetails pattern) {
        resetCraftingLock();

        LockCraftingMode lockMode = configManager.getSetting(Settings.LOCK_CRAFTING_MODE);
        switch (lockMode) {
            case LOCK_UNTIL_PULSE -> {
                if (getRedstoneState()) {
                    // 已经有信号，等待无信号时切换到 REDSTONE_POWER
                    unlockEvent = UnlockCraftingEvent.REDSTONE_PULSE;
                } else {
                    // 当前无信号，等待信号
                    unlockEvent = UnlockCraftingEvent.REDSTONE_POWER;
                }
                redstoneState = YesNo.UNDECIDED; // 在下次更新时检查红石状态
                saveChanges();
            }
            case LOCK_UNTIL_RESULT -> {
                unlockEvent = UnlockCraftingEvent.RESULT;
                unlockStack = pattern.getPrimaryOutput();
                saveChanges();
            }
        }
    }

    /**
     * 将目标方向的MEStorage或者其他合适能力包装成PatternProviderTarget
     */
    @Nullable
    private PatternProviderTarget findAdapter(EnumFacing side) {
        TileEntity tile = host.getTileEntity();
        World level = tile.getWorld();
        if (level == null) {
            return null;
        }
        BlockPos adjPos = tile.getPos().offset(side);
        if (!level.isBlockLoaded(adjPos)) {
            return null;
        }
        return PatternProviderTarget.get(level, adjPos, side.getOpposite(), actionSource);
    }

    /**
     * 对列表应用轮询
     */
    private <T> void rearrangeRoundRobin(List<T> list) {
        if (list.isEmpty()) {
            return;
        }

        roundRobinIndex %= list.size();
        for (int i = 0; i < roundRobinIndex; ++i) {
            list.add(list.get(i));
        }
        list.subList(0, roundRobinIndex).clear();
    }

    public boolean isBlocking() {
        return this.configManager.getSetting(Settings.BLOCKING_MODE) != BlockingMode.NO;
    }

    /**
     * 阻塞模式判定（语义对齐 fork PatternProviderLogic）:
     * YES=目标含我们的样板输入则阻塞, STRONG=目标含任意物品则阻塞
     */
    private boolean isTargetBlocked(PatternProviderTarget target) {
        return switch (this.configManager.getSetting(Settings.BLOCKING_MODE)) {
            case NO -> false;
            case STRONG -> target.containsAnyStack();
            case YES -> target.containsPatternInput(this.patternInputs);
        };
    }

    /**
     * 过滤掉同网络直连的 AE 机器目标面，保持与原版样板供应器一致。
     */
    private Set<EnumFacing> getActiveSides() {
        EnumSet<EnumFacing> sides = EnumSet.copyOf(host.getTargets());

        IGridNode node = mainNode.getNode();
        if (node != null) {
            for (var entry : node.getInWorldConnections().entrySet()) {
                IGridNode otherNode = entry.getValue().getOtherSide(node);
                Object otherOwner = otherNode.getOwner();
                if (otherOwner instanceof PatternProviderLogicHost
                    || otherOwner instanceof IntegratedInterfaceHost
                    || (otherOwner instanceof InterfaceLogicHost
                    && Objects.equals(otherNode.grid(), mainNode.getGrid()))) {
                    sides.remove(entry.getKey());
                }
            }
        }

        return sides;
    }

    /**
     * 检查目标是否能接收此样板所有输入种类（不包含具体的数量验证）
     */
    private boolean adapterAcceptsAll(PatternProviderTarget target, KeyCounter[] inputHolder) {
        for (KeyCounter inputList : inputHolder) {
            for (var input : inputList) {
                long inserted = target.insert(input.getKey(), input.getLongValue(), Actionable.SIMULATE);
                if (inserted == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 将物品加入待发送列表，然后加速接口运行
     */
    private void addToSendList(AEKey what, long amount) {
        if (amount > 0) {
            this.sendList.add(new GenericStack(what, amount));

            this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
        }
    }

    /**
     * 尝试将sendList中的东西向外发送
     */
    private boolean sendStacksOut() {
        if (sendDirection == null) {
            return false;
        }

        PatternProviderTarget adapter = findAdapter(sendDirection);
        if (adapter == null) {
            return false;
        }

        boolean didSomething = false;

        for (ListIterator<GenericStack> it = sendList.listIterator(); it.hasNext();) {
            GenericStack stack = it.next();
            AEKey what = stack.what();
            long amount = stack.amount();

            long inserted = adapter.insert(what, amount, Actionable.MODULATE);
            if (inserted >= amount) {
                it.remove();
                didSomething = true;
            } else if (inserted > 0) {
                it.set(new GenericStack(what, amount - inserted));
                didSomething = true;
            }
        }

        if (sendList.isEmpty()) {
            sendDirection = null;
        }

        return didSomething;
    }

    private boolean getRedstoneState() {
        if (redstoneState == YesNo.UNDECIDED) {
            TileEntity tile = this.host.getTileEntity();
            World level = tile.getWorld();
            if (level == null) {
                redstoneState = YesNo.UNDECIDED;
            } else {
                redstoneState = level.isBlockPowered(tile.getPos()) ? YesNo.YES : YesNo.NO;
            }
        }
        return redstoneState == YesNo.YES;
    }

    public void resetCraftingLock() {
        if (unlockEvent != null) {
            unlockEvent = null;
            unlockStack = null;
            saveChanges();
        }
    }

    /**
     * 当前供应器是否繁忙
     */
    @Override
    public boolean isBusy() {
        return !sendList.isEmpty();
    }

    // ICraftingRequester
    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.craftingTracker.getRequestedJobs();
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        int slot = this.craftingTracker.getSlot(link);
        return storage.insert(slot, what, amount, mode);
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        this.craftingTracker.jobStateChange(link);
    }

    private boolean isAllowedInStorageSlot(int slot, AEKey what) {
        if (slot < configInv.size()) {
            var configured = configInv.getKey(slot);
            if (configured == null || configured.equals(what)) {
                return true;
            }
            if (upgrades.isInstalled(AEItems.FUZZY_CARD.item())) {
                var fuzzyMode = configManager.getSetting(Settings.FUZZY_MODE);
                return configured.fuzzyEquals(what, fuzzyMode);
            }
            return false;
        }
        return true;
    }

    protected void onConfigChanged() {
        this.saveChanges();
        // FUZZY_MODE 变了也会走这里，所以要重算计划
        updatePlan();
    }

    private void onConfigRowChanged() {
        this.saveChanges();
        readConfig();
    }

    private void onStorageChanged() {
        this.saveChanges();
        updatePlan();
    }

    private void onUpgradesChanged() {
        this.saveChanges();

        // 移除合成卡时，取消所有挂起的合成任务
        if (!upgrades.isInstalled(AEItems.CRAFTING_CARD.item())) {
            cancelCrafting();
        }

        // fuzzy 卡插拔也会改变匹配规则
        updatePatterns();
        updatePlan();
        ICraftingProvider.requestUpdate(mainNode);
    }

    private void readConfig() {
        this.hasConfig = !this.configInv.isEmpty();
        updatePlan();

        // hasConfig 影响对外暴露的库存形态，通知邻居刷新（1.12.2 无 invalidateCapabilities）
        TileEntity tile = this.host.getTileEntity();
        World level = tile.getWorld();
        if (level != null) {
            Platform.notifyBlocksOfNeighbors(level, tile.getPos());
        }
    }

    private boolean hasStorageWork() {
        for (GenericStack work : this.plannedWork) {
            if (work != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 重新计算所有槽位的 plannedWork，并根据有无工作唤醒/睡眠设备
     */
    private void updatePlan() {
        boolean hadWork = hasStorageWork();

        for (int i = 0; i < this.configInv.size(); i++) {
            updatePlan(i);
        }

        boolean hasWork = hasStorageWork();
        if (hadWork != hasWork) {
            mainNode.ifPresent((grid, node) -> {
                if (hasWork) {
                    grid.getTickManager().alertDevice(node);
                } else {
                    grid.getTickManager().sleepDevice(node);
                }
            });
        }
    }

    /**
     * 单槽位：根据 config vs storage 生成 plannedWork[slot]
     */
    private void updatePlan(int slot) {
        var req = this.configInv.getStack(slot); // 想要的
        var stored = this.storage.getStack(slot); // 现在有的

        if (req == null && stored != null) {
            // 不想存，但现在有东西 -> 全部退回网络
            this.plannedWork[slot] = new GenericStack(stored.what(), -stored.amount());
        } else if (req != null) {
            if (stored == null) {
                // 想存但现在空 -> 从网络拉 req
                this.plannedWork[slot] = req;
            } else if (storedRequestEquals(req.what(), stored.what())) {
                long delta = req.amount() - stored.amount();
                this.plannedWork[slot] = (delta != 0) ? new GenericStack(req.what(), delta) : null;
            } else {
                // 槽里是别的东西 -> 先全部退回网络
                this.plannedWork[slot] = new GenericStack(stored.what(), -stored.amount());
            }
        } else {
            // req == null && stored == null
            this.plannedWork[slot] = null;
        }
    }

    private boolean storedRequestEquals(AEKey request, AEKey stored) {
        if (upgrades.isInstalled(AEItems.FUZZY_CARD.item()) && request.supportsFuzzyRangeSearch()) {
            return request.fuzzyEquals(stored, configManager.getSetting(Settings.FUZZY_MODE));
        } else {
            return request.equals(stored);
        }
    }

    /**
     * 执行存储相关的计划（plannedWork），返回本 tick 是否做成了任何事情
     */
    private boolean updateStorage() {
        boolean didSomething = false;

        for (int slot = 0; slot < plannedWork.length; slot++) {
            GenericStack work = plannedWork[slot];
            if (work != null) {
                if (usePlan(slot, work.what(), work.amount())) {
                    didSomething = true;
                }
            }
        }

        return didSomething;
    }

    private boolean usePlan(int slot, AEKey what, long amount) {
        boolean changed = tryUsePlan(slot, what, amount);
        if (changed) {
            updatePlan(slot);
        }
        return changed;
    }

    /**
     * 根据配置plan拉取/退回资源
     */
    private boolean tryUsePlan(int slot, AEKey what, long amount) {
        var grid = mainNode.getGrid();
        if (grid == null) {
            return false;
        }

        MEStorage networkInv = grid.getStorageService().getInventory();
        IEnergySource energySrc = grid.getEnergyService();

        // amount < 0 - 从接口推回网络
        if (amount < 0) {
            amount = -amount;

            GenericStack inSlot = storage.getStack(slot);
            if (inSlot == null || !what.matches(inSlot) || inSlot.amount() < amount) {
                // 槽位状态改了，要求重新规划
                return true;
            }

            // 注入网络
            long inserted = StorageHelper.poweredInsert(
                energySrc,
                networkInv,
                what,
                amount,
                this.actionSource);

            if (inserted > 0) {
                // 从本地仓扣掉
                storage.extract(slot, what, inserted, Actionable.MODULATE);

                // 通知RESULT锁
                onStackReturnedToNetwork(new GenericStack(what, inserted));
            }

            return inserted > 0;
        }

        // 如果该槽位已经有挂起的合成任务，优先走合成处理
        if (this.craftingTracker.isBusy(slot)) {
            return handleCrafting(slot, what, amount);
        }

        // amount > 0 - 从网络拉货进接口
        if (amount > 0) {
            // 先模拟是否能插满 amount，避免计划过期
            long canInsert = storage.insert(slot, what, amount, Actionable.SIMULATE);
            if (canInsert != amount) {
                return true; // 槽位状态变了，重新规划
            }

            // 尝试直接从网络抽取指定 key
            if (acquireFromNetwork(energySrc, networkInv, slot, what, amount)) {
                return true;
            }

            // 如果这个槽现在还空，并且有 FUZZY 卡，则尝试模糊拉货
            if (storage.getStack(slot) == null && upgrades.isInstalled(AEItems.FUZZY_CARD.item())) {
                FuzzyMode fuzzyMode = configManager.getSetting(Settings.FUZZY_MODE);
                for (var entry : grid.getStorageService().getCachedInventory().findFuzzy(what, fuzzyMode)) {
                    long maxAmount = storage.insert(slot, entry.getKey(), amount, Actionable.SIMULATE);
                    if (acquireFromNetwork(energySrc, networkInv, slot, entry.getKey(), maxAmount)) {
                        return true;
                    }
                }
            }

            // 网络里没有现货 - 尝试拉起合成任务
            return handleCrafting(slot, what, amount);
        }

        return false;
    }

    /**
     * 从网络中拉指定 key 进入 storage[slot]，返回是否成功拉到一点
     */
    private boolean acquireFromNetwork(IEnergySource energySrc, MEStorage networkInv,
                                       int slot, AEKey what, long amount) {
        long acquired = StorageHelper.poweredExtraction(energySrc, networkInv, what, amount, this.actionSource);
        if (acquired > 0) {
            long inserted = storage.insert(slot, what, acquired, Actionable.MODULATE);
            if (inserted < acquired) {
                // 计划外丢失，静默吞掉（源码注释掉异常）
            }
            return true;
        }
        return false;
    }

    private boolean handleCrafting(int slot, AEKey key, long amount) {
        IGrid grid = mainNode.getGrid();
        if (grid != null && upgrades.isInstalled(AEItems.CRAFTING_CARD.item()) && key != null) {
            return this.craftingTracker.handleCrafting(
                slot,
                key,
                amount,
                host.getTileEntity().getWorld(),
                grid.getCraftingService(),
                this.actionSource);
        }
        return false;
    }

    private void cancelCrafting() {
        this.craftingTracker.cancel();
    }

    @Override
    public @Nullable IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    /**
     * 获取样板管理终端中显示的分组，使用与推送相同的有效目标面。
     */
    @Override
    public PatternContainerGroup getTerminalGroup() {
        TileEntity tileEntity = host.getTileEntity();
        World hostLevel = tileEntity.getWorld();

        var groups = new java.util.LinkedHashSet<PatternContainerGroup>();
        if (hostLevel != null) {
            for (EnumFacing side : getActiveSides()) {
                BlockPos sidePos = tileEntity.getPos().offset(side);
                PatternContainerGroup group = PatternContainerGroup.fromMachine(hostLevel, sidePos,
                    side.getOpposite());
                if (group != null) {
                    groups.add(group);
                }
            }
        }

        // 如果附近有了一个组，使用它的组
        if (groups.size() == 1) {
            return groups.iterator().next();
        }

        List<net.minecraft.util.text.ITextComponent> tooltip = new ArrayList<>();
        // 如果有多个组，一起显示出来
        if (groups.size() > 1) {
            tooltip.add(ae2.core.localization.GuiText.AdjacentToDifferentMachines.text());
            for (PatternContainerGroup group : groups) {
                tooltip.add(group.name());
                tooltip.addAll(group.tooltip());
            }
        }

        // 如果没有任何东西，则自己显示
        AEItemKey hostIcon = this.host.getTerminalIcon();
        return new PatternContainerGroup(
            hostIcon,
            hostIcon.getDisplayName(),
            tooltip);
    }

    private class Ticker implements IGridTickable {

        @Override
        public TickingRequest getTickingRequest(IGridNode node) {
            // 有工作就醒着，没有工作就可以睡
            boolean idle = !hasStorageWork() && sendList.isEmpty() && !hasExtraWork();
            return new TickingRequest(TickRates.Interface, idle);
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
            if (!mainNode.isActive()) {
                return TickRateModulation.SLEEP;
            }

            boolean didSomething = updateStorage() | sendStacksOut() | doExtraWork();

            boolean stillHasWork = hasStorageWork() || !sendList.isEmpty() || hasExtraWork();

            if (!stillHasWork) {
                return TickRateModulation.SLEEP;
            } else if (didSomething) {
                return TickRateModulation.URGENT;
            } else {
                return TickRateModulation.SLOWER;
            }
        }
    }

    /**
     * 子类额外工作钩子（EnderInterface 的掉落物吸收）。
     * 源码 EnderInterfaceLogic 的 Ticker 把吸收逻辑合并在 updateStorage 一起。
     */
    protected boolean doExtraWork() {
        return false;
    }

    /**
     * 子类额外工作存在性钩子（影响 Ticker 睡眠判断）
     */
    protected boolean hasExtraWork() {
        return false;
    }

    // ---- NBT ----

    public void writeToNBT(NBTTagCompound tag) {
        // 配置 / 存储
        this.configInv.writeToChildTag(tag, "config");
        this.storage.writeToChildTag(tag, "storage");

        // 升级
        this.upgrades.writeToNBT(tag, "upgrades");

        // 机器配置（FUZZY_MODE / BLOCKING / LOCK_CRAFTING_MODE / PATTERN_ACCESS_TERMINAL）
        this.configManager.writeToNBT(tag);

        // 样板槽
        this.patternInventory.writeToNBT(tag, "patterns");

        // 合成跟踪
        this.craftingTracker.writeToNBT(tag);

        // 样板推送相关
        tag.setInteger("roundRobinIndex", this.roundRobinIndex);
        tag.setByte("redstoneState", (byte) this.redstoneState.ordinal());

        if (unlockEvent != null) {
            tag.setByte("unlockEvent", (byte) unlockEvent.ordinal());
        }
        if (unlockStack != null) {
            tag.setTag("unlockStack", GenericStack.writeTag(unlockStack));
        }

        if (!sendList.isEmpty()) {
            var list = new NBTTagList();
            for (var gs : sendList) {
                list.appendTag(GenericStack.writeTag(gs));
            }
            tag.setTag("sendList", list);
        }
        if (sendDirection != null) {
            tag.setByte("sendDirection", (byte) sendDirection.ordinal());
        }

        // Port 扩展
        tag.setBoolean("pullEnabled", pullEnabled);
    }

    public void readFromNBT(NBTTagCompound tag) {
        this.configInv.readFromChildTag(tag, "config");
        this.storage.readFromChildTag(tag, "storage");
        this.upgrades.readFromNBT(tag, "upgrades");
        this.configManager.readFromNBT(tag);
        this.patternInventory.readFromNBT(tag, "patterns");
        this.craftingTracker.readFromNBT(tag);

        this.roundRobinIndex = tag.getInteger("roundRobinIndex");
        this.redstoneState = YesNo.values()[tag.getByte("redstoneState") % YesNo.values().length];

        this.unlockEvent = tag.hasKey("unlockEvent")
            ? UnlockCraftingEvent.values()[tag.getByte("unlockEvent") % UnlockCraftingEvent.values().length]
            : null;
        this.unlockStack = tag.hasKey("unlockStack") ? GenericStack.readTag(tag.getCompoundTag("unlockStack")) : null;

        this.sendList.clear();
        if (tag.hasKey("sendList", Constants.NBT.TAG_LIST)) {
            NBTTagList list = tag.getTagList("sendList", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                GenericStack stack = GenericStack.readTag(list.getCompoundTagAt(i));
                if (stack != null) {
                    this.sendList.add(stack);
                }
            }
        }
        this.sendDirection = tag.hasKey("sendDirection")
            ? EnumFacing.VALUES[tag.getByte("sendDirection") % 6]
            : null;

        // Port 扩展
        this.pullEnabled = tag.getBoolean("pullEnabled");

        updatePatterns();
        readConfig();
    }

    /**
     * 红石状态变化时由方块邻居更新调用
     */
    public void updateRedstoneState() {
        if (unlockEvent == UnlockCraftingEvent.REDSTONE_POWER && getRedstoneState()) {
            // 等待有电 -> 现在有电 -> 解锁
            unlockEvent = null;
            saveChanges();
        } else if (unlockEvent == UnlockCraftingEvent.REDSTONE_PULSE && !getRedstoneState()) {
            // 等待断电再等下一次上电
            unlockEvent = UnlockCraftingEvent.REDSTONE_POWER;
            redstoneState = YesNo.UNDECIDED;
            saveChanges();
        } else {
            // 其他情况仅仅重置缓存，下次读取重新测红石
            redstoneState = YesNo.UNDECIDED;
        }
    }

    /**
     * 对外暴露的库存：无配置时暴露整个网络，有配置时暴露本地仓
     */
    public MEStorage getExposedMEStorage(EnumFacing side) {
        var grid = mainNode.getGrid();
        if (!hasConfig && grid != null) {
            // 没配备货，就直接暴露整个网络
            return grid.getStorageService().getInventory();
        }
        // 有 config，则暴露本地仓
        return storage;
    }

    private void onStackReturnedToNetwork(GenericStack stack) {
        // 当前不是 RESULT 模式，直接忽略
        if (unlockEvent != UnlockCraftingEvent.RESULT) {
            return;
        }

        if (unlockStack == null) {
            // 状态不一致，直接解锁防止死锁
            unlockEvent = null;
            saveChanges();
            return;
        }

        // 只统计主输出类型
        if (!unlockStack.what().equals(stack.what())) {
            return;
        }

        long remaining = unlockStack.amount() - stack.amount();

        if (remaining <= 0) {
            // 数量足够了，解锁
            unlockStack = null;
            unlockEvent = null;
        } else {
            // 继续等待
            unlockStack = new GenericStack(unlockStack.what(), remaining);
        }

        saveChanges();
    }

    public void saveChanges() {
        this.host.saveChanges();
    }

    public void onMainNodeStateChanged() {
        if (this.mainNode.isActive()) {
            this.mainNode.ifPresent((grid, node) -> {
                grid.getTickManager().alertDevice(node);
            });
        }
    }

    public ConfigInventory getConfigInv() {
        return configInv;
    }

    public ConfigInventory getStorageInv() {
        return storage;
    }

    public AppEngInternalInventory getPatternInventory() {
        return patternInventory;
    }

    /**
     * 样板终端用：是否包含指定已编码样板
     */
    public boolean containsPattern(AEItemKey pattern) {
        return this.patternKeys.contains(pattern);
    }

    public int getStorageSize() {
        return storageSize;
    }

    public int getPatternSize() {
        return patternSize;
    }

    @Nullable
    public IGrid getGrid() {
        return mainNode.getGrid();
    }

    public void addDrops(List<ItemStack> drops) {
        for (ItemStack stack : this.patternInventory) {
            drops.add(stack);
        }

        for (ItemStack stack : upgrades) {
            drops.add(stack);
        }

        var level = this.host.getTileEntity().getWorld();
        var pos = this.host.getTileEntity().getPos();
        if (level != null) {
            for (GenericStack stack : this.sendList) {
                stack.what().addDrops(stack.amount(), drops, level, pos);
            }

            for (GenericStack stack : this.getStorageInv().toList()) {
                if (stack == null) {
                    continue;
                }
                stack.what().addDrops(stack.amount(), drops, level, pos);
            }
        }
    }

    public void clearContent() {
        this.patternInventory.clear();
        this.upgrades.clear();
        this.sendList.clear();
        this.storage.clear();
    }

    // ---- 记忆卡设置导入导出（对照源码 exportSettings/importSettings，NBT 版） ----

    private static final String MEMORY_CARD_SETTINGS = "settings";
    private static final String MEMORY_CARD_PATTERNS = "patterns";

    public void exportSettings(NBTTagCompound output) {
        NBTTagCompound settings = new NBTTagCompound();
        for (var entry : this.configManager.exportSettings().entrySet()) {
            settings.setString(entry.getKey(), entry.getValue());
        }
        output.setTag(MEMORY_CARD_SETTINGS, settings);
        this.patternInventory.writeToNBT(output, MEMORY_CARD_PATTERNS);
    }

    public void importSettings(NBTTagCompound input, @Nullable net.minecraft.entity.player.EntityPlayer player) {
        if (input.hasKey(MEMORY_CARD_SETTINGS, Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound settingsTag = input.getCompoundTag(MEMORY_CARD_SETTINGS);
            Map<String, String> settings = new java.util.HashMap<>();
            for (String key : settingsTag.getKeySet()) {
                settings.put(key, settingsTag.getString(key));
            }
            this.configManager.importSettings(settings);
        }

        if (player == null || player.world.isRemote
            || !input.hasKey(MEMORY_CARD_PATTERNS, Constants.NBT.TAG_LIST)) {
            return;
        }

        clearPatternInventory(player);

        AppEngInternalInventory desiredPatterns = new AppEngInternalInventory(this.patternInventory.size());
        desiredPatterns.readFromNBT(input, MEMORY_CARD_PATTERNS);

        // 从玩家背包将空白样板恢复出来
        int blankPatternsAvailable = player.capabilities.isCreativeMode
            ? Integer.MAX_VALUE
            : countItem(player.inventory, AEItems.BLANK_PATTERN.stack());
        int blankPatternsUsed = 0;
        for (int i = 0; i < desiredPatterns.size(); i++) {
            ItemStack desiredPattern = desiredPatterns.getStackInSlot(i);
            if (desiredPattern.isEmpty()) {
                continue;
            }

            IPatternDetails pattern = PatternDetailsHelper.decodePattern(desiredPattern,
                this.host.getTileEntity().getWorld());
            if (pattern == null) {
                continue;
            }

            ++blankPatternsUsed;
            if (blankPatternsAvailable >= blankPatternsUsed) {
                if (!this.patternInventory.addItems(pattern.getDefinition().toStack()).isEmpty()) {
                    blankPatternsUsed--;
                }
            }
        }

        if (blankPatternsUsed > 0 && !player.capabilities.isCreativeMode) {
            new ae2.util.inv.PlayerInternalInventory(player.inventory).removeItems(blankPatternsUsed,
                AEItems.BLANK_PATTERN.stack(), null);
        }

        // 如果无法恢复所有样板，则发出警告
        if (blankPatternsUsed > blankPatternsAvailable) {
            player.sendMessage(
                ae2.core.localization.PlayerMessages.MissingBlankPatterns.text(blankPatternsUsed - blankPatternsAvailable));
        }

        this.updatePatterns();
        this.saveChanges();
    }

    /**
     * 把所有样板转成空白样板并给玩家
     */
    private void clearPatternInventory(net.minecraft.entity.player.EntityPlayer player) {
        if (player.capabilities.isCreativeMode) {
            for (int i = 0; i < patternInventory.size(); i++) {
                patternInventory.setItemDirect(i, ItemStack.EMPTY);
            }
            return;
        }

        // 清空样板并给玩家
        int blankPatternCount = 0;
        for (int i = 0; i < patternInventory.size(); i++) {
            ItemStack pattern = patternInventory.getStackInSlot(i);
            if (pattern.isEmpty()) {
                continue;
            }

            if (pattern.getItem() == AEItems.CRAFTING_PATTERN.item()
                || pattern.getItem() == AEItems.PROCESSING_PATTERN.item()
                || pattern.getItem() == AEItems.BLANK_PATTERN.item()) {
                blankPatternCount += pattern.getCount();
            } else if (!player.inventory.addItemStackToInventory(pattern.copy())) {
                player.dropItem(pattern.copy(), false);
            }
            patternInventory.setItemDirect(i, ItemStack.EMPTY);
        }

        if (blankPatternCount > 0) {
            ItemStack stack = AEItems.BLANK_PATTERN.stack(blankPatternCount);
            if (!player.inventory.addItemStackToInventory(stack)) {
                player.dropItem(stack, false);
            }
        }
    }

    private static int countItem(net.minecraft.entity.player.InventoryPlayer inventory, ItemStack needle) {
        int total = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.areItemsEqual(stack, needle)
                && ItemStack.areItemStackTagsEqual(stack, needle)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    // ---- Port 扩展: pull 模式（源码没有此功能） ----

    public boolean isPullEnabled() {
        return pullEnabled;
    }

    public void setPullEnabled(boolean enabled) {
        this.pullEnabled = enabled;
        saveChanges();
    }

    /**
     * 按 config 从相邻容器拉取物品进本地仓（port 自研扩展）。
     * 由 host tile 的 update() 周期性调用。
     */
    public void doPullWork() {
        if (!pullEnabled || !mainNode.isActive()) {
            return;
        }

        TileEntity tile = host.getTileEntity();
        World level = tile.getWorld();
        if (level == null || level.isRemote) {
            return;
        }

        int maxPerTick = 8;

        for (EnumFacing dir : EnumFacing.VALUES) {
            if (maxPerTick <= 0) {
                break;
            }
            BlockPos adjPos = tile.getPos().offset(dir);
            TileEntity adjTile = level.getTileEntity(adjPos);
            if (adjTile == null) {
                continue;
            }

            IItemHandler handler = adjTile.getCapability(
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite());
            if (handler == null) {
                continue;
            }

            // 检查每个配置槽，看是否能拉取匹配的物品
            for (int ci = 0; ci < configInv.size() && maxPerTick > 0; ci++) {
                AEKey configKey = configInv.getKey(ci);
                if (!(configKey instanceof AEItemKey itemKey)) {
                    continue;
                }

                // 检查本地仓该槽还有多少空间
                long canInsert = storage.insert(ci, configKey, 1, Actionable.SIMULATE);
                if (canInsert <= 0) {
                    continue;
                }

                // 扫描相邻容器寻找匹配物品
                for (int s = 0; s < handler.getSlots() && maxPerTick > 0; s++) {
                    ItemStack stack = handler.getStackInSlot(s);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    AEItemKey stackKey = AEItemKey.of(stack);
                    if (stackKey == null || !stackKey.equals(itemKey)) {
                        continue;
                    }

                    long amount = Math.min(stack.getCount(), 64);
                    ItemStack extracted = handler.extractItem(s, (int) amount, false);
                    if (!extracted.isEmpty()) {
                        AEItemKey extractedKey = AEItemKey.of(extracted);
                        if (extractedKey != null) {
                            storage.insert(ci, extractedKey, extracted.getCount(), Actionable.MODULATE);
                            maxPerTick--;
                            break; // 每个配置槽每轮只拉一组
                        }
                    }
                }
            }
        }
    }

    /**
     * 子类工厂：机器配置管理器（EnderInterface 会追加 SHOW_RANGE_MODE/BLACK_LIST_MODE）
     */
    protected IConfigManager createConfigManager() {
        return IConfigManager.builder(this::onConfigChanged)
            .registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL)
            .registerSetting(Settings.BLOCKING_MODE, BlockingMode.NO)
            .registerSetting(Settings.PATTERN_ACCESS_TERMINAL, YesNo.YES)
            .registerSetting(Settings.LOCK_CRAFTING_MODE, LockCraftingMode.NONE)
            .build();
    }

    /**
     * 子类工厂：升级库存对应的机器物品（决定支持哪些升级卡）
     */
    protected Item getMachineItem() {
        return Item.getItemFromBlock(AE2CSBlocks.INTEGRATED_INTERFACE);
    }
}
