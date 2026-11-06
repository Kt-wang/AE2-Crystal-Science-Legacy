/*
 * AE2 Crystal Science - ResonatingPatternProviderLogic (1.12.2)
 * 对照 1.21.1 源码 common/me/logic/ResonatingPatternProviderLogic.java 完整移植。
 * 谐振样板供应器：谐振样板的每个输入可带远程目标(维度+坐标+面)，
 * 标记输入推送到远程目标，未标记输入按普通供应器推送到相邻机器。
 *
 * 1.12.2 适配：
 * - fork PatternProviderLogic 的 doWork/sendStacksOut/onPushPatternSuccess/lock字段 全 private
 *   → 谐振推送路径自实现（sendList/锁状态复用集成接口的写法）
 * - base Ticker 不可替换（替换会断返回仓刷新）→ tickServer() 由宿主 Tile.update() 驱动；
 *   Part 形态由 AEBaseTileMixin 在线缆总线 update() 末尾驱动（见 mixin/AEBaseTileMixin）
 * - 拉动模式用 IItemHandler（仅物品，1.12.2 无流体能力统一抽象）
 */
package ae2csl.me.logic;

import ae2.api.config.Actionable;
import ae2.api.config.BlockingMode;
import ae2.api.config.LockCraftingMode;
import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.upgrades.UpgradeInventories;
import ae2.crafting.pattern.AEProcessingPattern;
import ae2.helpers.InterfaceLogicHost;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2.helpers.patternprovider.PatternProviderTarget;
import ae2.helpers.patternprovider.UnlockCraftingEvent;
import ae2.me.helpers.MachineSource;
import ae2.util.ConfigManager;
import ae2csl.api.settings.AE2CSSettings;
import ae2csl.api.settings.PullMode;
import ae2csl.me.crafting.EncodedResonatingPattern;
import ae2csl.me.crafting.ResonatingPatternDetails;
import ae2csl.me.crafting.ResonatingProviderDefaults;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ResonatingPatternProviderLogic extends PatternProviderLogic implements IUpgradeableObject {

    private static final org.apache.logging.log4j.Logger LOGGER = ae2csl.AE2CrystalScienceLegacy.LOGGER;

    private static final String TAG_DEFAULTS = "resonating_provider_defaults";
    private static final String TAG_SELECTED_INPUT = "selected_input";
    private static final String TAG_TARGETS = "targets";
    private static final String TAG_SLOT = "slot";
    private static final String TAG_DIMENSION = "dim";
    private static final String TAG_POS = "pos";
    private static final String TAG_FACE = "face";

    private final IActionSource actionSource;
    private final PatternProviderLogicHost host;
    private final IManagedGridNode mainNode;

    /**
     * 谐振（远程目标）待发列表
     */
    private final List<PendingSend> resonatingSendList = new ArrayList<>();

    /**
     * 本地（无目标）待发列表
     */
    private final List<GenericStack> localSendList = new ArrayList<>();
    @Nullable
    private EnumFacing localSendDirection;

    /**
     * 升级槽（1 槽，晶体生长卡）
     */
    private final IUpgradeInventory upgrades;

    private int localRoundRobinIndex = 0;

    // ---- 默认值（记忆卡/物品 NBT 保存的默认输入目标） ----
    private List<Optional<EncodedResonatingPattern.Target>> defaultInputTargets =
        ResonatingProviderDefaults.emptyTargets();
    private int defaultSelectedInput = 0;

    // ---- 自维护的红石锁状态（fork 父类的 lock 字段是 private） ----
    private YesNo redstoneState = YesNo.UNDECIDED;
    @Nullable
    private UnlockCraftingEvent unlockEvent;
    @Nullable
    private GenericStack unlockStack;

    /** 阻挡模式用的输入集合（fork 父类的 patternInputs 是 private） */
    private final Set<AEKey> resonatingPatternInputs = new java.util.HashSet<>();

    public ResonatingPatternProviderLogic(IManagedGridNode mainNode, PatternProviderLogicHost host) {
        this(mainNode, host, 9);
    }

    public ResonatingPatternProviderLogic(IManagedGridNode mainNode, PatternProviderLogicHost host,
                                          int patternInventorySize) {
        super(mainNode, host, patternInventorySize);

        this.host = host;
        this.mainNode = mainNode;
        this.actionSource = new MachineSource(mainNode::getNode);

        this.upgrades = UpgradeInventories.forMachine(
            Item.getItemFromBlock(ae2csl.init.AE2CSBlocks.RESONATING_PATTERN_PROVIDER), 1,
            this::onUpgradesChange);

        // fork ConfigManager 支持后置注册（源码同款写法）
        if (getConfigManager() instanceof ConfigManager cm) {
            cm.registerSetting(AE2CSSettings.PULL_MODE, PullMode.PULL_OFF);
        }
    }

    public boolean isEnablePull() {
        return getConfigManager().hasSetting(AE2CSSettings.PULL_MODE)
            && getConfigManager().getSetting(AE2CSSettings.PULL_MODE) == PullMode.PULL_ON;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    private void onUpgradesChange() {
        this.saveChanges();
    }

    // ---- NBT ----

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        this.upgrades.writeToNBT(tag, "upgrades_resonating");
        writeDefaultsToNBT(tag);

        // 锁状态
        tag.setByte("resonating_redstone_state", (byte) this.redstoneState.ordinal());
        if (unlockEvent != null) {
            tag.setByte("resonating_unlock_event", (byte) unlockEvent.ordinal());
        }
        if (unlockStack != null) {
            tag.setTag("resonating_unlock_stack", GenericStack.writeTag(unlockStack));
        }

        var list = new NBTTagList();
        for (var p : resonatingSendList) {
            var e = new NBTTagCompound();
            e.setString("dim", String.valueOf(p.target().getPos().getDimension()));
            e.setLong("pos", p.target().getPos().getPos().toLong());
            e.setByte("face", (byte) p.target().getFace().ordinal());
            e.setTag("stack", GenericStack.writeTag(p.stack()));
            list.appendTag(e);
        }
        tag.setTag("resonating_send_list", list);

        // 本地待发
        if (!localSendList.isEmpty()) {
            var localList = new NBTTagList();
            for (var stack : localSendList) {
                localList.appendTag(GenericStack.writeTag(stack));
            }
            tag.setTag("resonating_local_send_list", localList);
        }
        if (localSendDirection != null) {
            tag.setByte("resonating_local_send_dir", (byte) localSendDirection.ordinal());
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        this.upgrades.readFromNBT(tag, "upgrades_resonating");
        readDefaultsFromNBT(tag);

        this.redstoneState = YesNo.values()[tag.getByte("resonating_redstone_state") % YesNo.values().length];
        this.unlockEvent = tag.hasKey("resonating_unlock_event")
            ? UnlockCraftingEvent.values()[tag.getByte("resonating_unlock_event") % UnlockCraftingEvent.values().length]
            : null;
        this.unlockStack = tag.hasKey("resonating_unlock_stack")
            ? GenericStack.readTag(tag.getCompoundTag("resonating_unlock_stack"))
            : null;

        resonatingSendList.clear();
        if (tag.hasKey("resonating_send_list", Constants.NBT.TAG_LIST)) {
            var list = tag.getTagList("resonating_send_list", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                var e = list.getCompoundTagAt(i);
                try {
                    int dim = Integer.parseInt(e.getString("dim"));
                    BlockPos pos = BlockPos.fromLong(e.getLong("pos"));
                    EnumFacing face = EnumFacing.VALUES[e.getByte("face") % 6];
                    GenericStack stack = GenericStack.readTag(e.getCompoundTag("stack"));
                    if (stack == null || stack.amount() <= 0) {
                        continue;
                    }
                    resonatingSendList.add(new PendingSend(
                        new EncodedResonatingPattern.Target(new ae2csl.api.util.GlobalPos(dim, pos), face), stack));
                } catch (Exception ex) {
                    // 跳过损坏条目
                }
            }
        }

        localSendList.clear();
        if (tag.hasKey("resonating_local_send_list", Constants.NBT.TAG_LIST)) {
            var list = tag.getTagList("resonating_local_send_list", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                GenericStack stack = GenericStack.readTag(list.getCompoundTagAt(i));
                if (stack != null && stack.amount() > 0) {
                    localSendList.add(stack);
                }
            }
        }
        localSendDirection = tag.hasKey("resonating_local_send_dir")
            ? EnumFacing.VALUES[tag.getByte("resonating_local_send_dir") % 6]
            : null;
    }

    // ---- 默认值读写 ----

    public void readDefaultsFromItem(ItemStack stack) {
        this.defaultInputTargets = ResonatingProviderDefaults.readTargets(stack);
        this.defaultSelectedInput = ResonatingProviderDefaults.getSelectedInput(stack);
        saveChanges();
    }

    public void writeDefaultsToStack(ItemStack stack) {
        ResonatingProviderDefaults.writeTargets(stack, this.defaultInputTargets);
        ResonatingProviderDefaults.setSelectedInput(stack, this.defaultSelectedInput);
    }

    public void writeDefaultsToDrops(List<ItemStack> drops, Item selfItem) {
        for (ItemStack drop : drops) {
            if (drop.getItem() == selfItem) {
                writeDefaultsToStack(drop);
                return;
            }
        }
    }

    public List<Optional<EncodedResonatingPattern.Target>> getDefaultInputTargets() {
        return defaultInputTargets;
    }

    public int getDefaultSelectedInput() {
        return defaultSelectedInput;
    }

    /**
     * 更新一个默认输入槽的发配目标（源码 setDefaultInputTarget）。
     */
    public void setDefaultInputTarget(int input, Optional<EncodedResonatingPattern.Target> target) {
        if (input < 0 || input >= ResonatingProviderDefaults.DEFAULT_INPUT_SLOTS) {
            return;
        }

        var targets = new ArrayList<>(defaultInputTargets);
        targets.set(input, target);
        this.defaultInputTargets = targets;
        saveChanges();
    }

    /**
     * 更新链接器正在编辑的默认输入槽（源码 setDefaultSelectedInput）。
     */
    public void setDefaultSelectedInput(int input) {
        this.defaultSelectedInput = ResonatingProviderDefaults.clampSelected(input);
        saveChanges();
    }

    // ---- 记忆卡/拆解设置（NBT 版，对齐源码 writeDefaultsToSettings/readDefaultsFromSettings） ----

    public void writeDefaultsToSettings(NBTTagCompound output) {
        writeDefaultsToNBT(output);
    }

    public void readDefaultsFromSettings(NBTTagCompound input) {
        readDefaultsFromNBT(input);
        saveChanges();
    }

    private void writeDefaultsToNBT(NBTTagCompound tag) {
        var defaults = new NBTTagCompound();
        defaults.setInteger(TAG_SELECTED_INPUT, defaultSelectedInput);

        var targets = new NBTTagList();
        for (int i = 0; i < defaultInputTargets.size(); i++) {
            var target = defaultInputTargets.get(i);
            if (target.isEmpty()) {
                continue;
            }

            var e = new NBTTagCompound();
            e.setInteger(TAG_SLOT, i);
            e.setString(TAG_DIMENSION, String.valueOf(target.get().getPos().getDimension()));
            e.setLong(TAG_POS, target.get().getPos().getPos().toLong());
            e.setByte(TAG_FACE, (byte) target.get().getFace().ordinal());
            targets.appendTag(e);
        }

        tag.setTag(TAG_TARGETS, targets);
        tag.setTag(TAG_DEFAULTS, defaults);
    }

    private void readDefaultsFromNBT(NBTTagCompound tag) {
        this.defaultInputTargets = ResonatingProviderDefaults.emptyTargets();
        this.defaultSelectedInput = 0;

        if (!tag.hasKey(TAG_DEFAULTS, Constants.NBT.TAG_COMPOUND)) {
            return;
        }

        var defaults = tag.getCompoundTag(TAG_DEFAULTS);
        this.defaultSelectedInput = ResonatingProviderDefaults.clampSelected(defaults.getInteger(TAG_SELECTED_INPUT));

        if (!defaults.hasKey(TAG_TARGETS, Constants.NBT.TAG_LIST)) {
            return;
        }

        var targets = new ArrayList<>(this.defaultInputTargets);
        var list = defaults.getTagList(TAG_TARGETS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            var e = list.getCompoundTagAt(i);
            int slot = e.getInteger(TAG_SLOT);
            if (slot < 0 || slot >= ResonatingProviderDefaults.DEFAULT_INPUT_SLOTS) {
                continue;
            }

            try {
                int dim = Integer.parseInt(e.getString(TAG_DIMENSION));
                var gp = new ae2csl.api.util.GlobalPos(dim, BlockPos.fromLong(e.getLong(TAG_POS)));
                var face = EnumFacing.VALUES[e.getByte(TAG_FACE) % 6];
                targets.set(slot, Optional.of(new EncodedResonatingPattern.Target(gp, face)));
            } catch (Exception ex) {
                // 跳过损坏条目
            }
        }
        this.defaultInputTargets = targets;
    }

    // ---- 红石锁（自维护，fork 父类字段 private） ----

    @Override
    public LockCraftingMode getCraftingLockedReason() {
        LockCraftingMode lockMode = getConfigManager().getSetting(Settings.LOCK_CRAFTING_MODE);
        if (lockMode == LockCraftingMode.LOCK_WHILE_LOW && !getRedstoneState()) {
            return LockCraftingMode.LOCK_WHILE_LOW;
        } else if (lockMode == LockCraftingMode.LOCK_WHILE_HIGH && getRedstoneState()) {
            return LockCraftingMode.LOCK_WHILE_HIGH;
        } else if (unlockEvent != null) {
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

    @Override
    public @Nullable GenericStack getUnlockStack() {
        return unlockStack != null ? unlockStack : super.getUnlockStack();
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

    private void onResonatingPushSuccess(IPatternDetails pattern) {
        resetCraftingLock();

        LockCraftingMode lockMode = getConfigManager().getSetting(Settings.LOCK_CRAFTING_MODE);
        if (lockMode == LockCraftingMode.LOCK_UNTIL_PULSE) {
            if (getRedstoneState()) {
                unlockEvent = UnlockCraftingEvent.REDSTONE_PULSE;
            } else {
                unlockEvent = UnlockCraftingEvent.REDSTONE_POWER;
            }
            redstoneState = YesNo.UNDECIDED;
            saveChanges();
        } else if (lockMode == LockCraftingMode.LOCK_UNTIL_RESULT) {
            unlockEvent = UnlockCraftingEvent.RESULT;
            unlockStack = pattern.getPrimaryOutput();
            saveChanges();
        }
    }

    @Override
    public boolean resetCraftingLock() {
        if (unlockEvent != null || unlockStack != null) {
            unlockEvent = null;
            unlockStack = null;
            saveChanges();
            return true;
        }
        return super.resetCraftingLock();
    }

    @Override
    public void updateRedstoneState() {
        if (unlockEvent == UnlockCraftingEvent.REDSTONE_POWER && getRedstoneState()) {
            unlockEvent = null;
            saveChanges();
        } else if (unlockEvent == UnlockCraftingEvent.REDSTONE_PULSE && !getRedstoneState()) {
            unlockEvent = UnlockCraftingEvent.REDSTONE_POWER;
            redstoneState = YesNo.UNDECIDED;
            saveChanges();
        } else {
            redstoneState = YesNo.UNDECIDED;
        }
        super.updateRedstoneState();
    }

    // ---- 推送 ----

    /**
     * 谐振/扩展供应器的样板栏由方块/部件本身决定（9 或 36），
     * 不再受 fork「9 + 9×样板扩展卡」的限制——否则扩展版第 10~36 槽不会进网络。
     */
    @Override
    public int getActivePatternSlots() {
        return getPatternInv().size();
    }

    @Override
    public void updatePatterns() {
        super.updatePatterns();
        // 自建输入集合（fork 的 patternInputs 是 private）
        resonatingPatternInputs.clear();
        for (IPatternDetails details : getAvailablePatterns()) {
            for (IPatternDetails.IInput input : details.getInputs()) {
                for (GenericStack candidate : input.possibleInputs()) {
                    if (candidate != null) {
                        resonatingPatternInputs.add(candidate.what().dropSecondary());
                    }
                }
            }
        }
    }

    @Override
    public boolean canMergePatternPush(IPatternDetails patternDetails) {
        // 谐振样板不做合并推送（源码无此概念）
        if (patternDetails instanceof ResonatingPatternDetails) {
            return false;
        }
        return super.canMergePatternPush(patternDetails);
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, int multiplier) {
        // 谐振样板用自身 targets；普通处理样板用 provider 默认目标；都不是则交给原版
        RoutedPatternData routed = buildRoutedPatternData(patternDetails);
        if (routed == null) {
            return super.pushPattern(patternDetails, inputHolder, multiplier);
        }

        // 类原版拒绝条件
        if (super.isBusy() || !resonatingSendList.isEmpty() || !localSendList.isEmpty()
            || !this.mainNode.isActive() || !getAvailablePatterns().contains(patternDetails)) {
            return false;
        }

        // 原版锁定逻辑
        if (getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false;
        }

        var be = host.getTileEntity();
        var level = be.getWorld();
        if (level == null) {
            return false;
        }

        int times = Math.max(1, multiplier);

        // inputHolder 由 CraftingCpuHelper.extractPatternInputs 生成，已经按 multiplier 放大过
        // （inputs[x].getMultiplier() * multiplier），这里只做拷贝，不能再乘一次。
        KeyCounter[] remaining = copyKeyCounters(inputHolder);

        // 收集 marked，并从 remaining 扣掉对应数量
        record Marked(AEKey key, long amount, EncodedResonatingPattern.Target target) {
        }
        var marked = new ArrayList<Marked>();

        var sparseInputs = routed.sparseInputs();
        for (int sparseIndex = 0; sparseIndex < sparseInputs.size(); sparseIndex++) {
            var sparse = sparseInputs.get(sparseIndex);
            if (sparse == null) {
                continue;
            }

            var optTarget = routed.getTargetForSparseInputIndex(sparseIndex);
            if (optTarget.isEmpty()) {
                continue; // 无target：留给unmarked fallback
            }

            var target = optTarget.get();
            var key = sparse.what();
            long amount = sparse.amount() * times;

            if (!removeFromRemaining(remaining, key, amount)) {
                return false;
            }

            marked.add(new Marked(key, amount, target));
        }

        // 预先尝试 marked 材料是否能完整发配
        for (var m : marked) {
            var adapter = findTarget(m.target());
            if (adapter == null) {
                if (ae2csl.AE2CrystalScienceLegacy.LOGGER.isDebugEnabled()) {
                    ae2csl.AE2CrystalScienceLegacy.LOGGER.debug(
                        "[resonating] no target adapter for {} at {}/{} (dim {})",
                        m.key(), m.target().getPos().getPos(), m.target().getFace(),
                        m.target().getPos().getDimension());
                }
                return false;
            }

            // 阻挡模式
            if (isBlockedByMode(adapter)) {
                return false;
            }

            long simulated = adapter.insert(m.key(), m.amount(), Actionable.SIMULATE);
            if (simulated < m.amount()) {
                return false;
            }
        }

        // remaining 存在，则按照原版逻辑模拟发配
        EnumFacing chosenDirection = null;
        PatternProviderTarget chosenAdapter = null;

        if (!isEmpty(remaining)) {
            if (!patternDetails.supportsPushInputsToExternalInventory()) {
                return false;
            }

            record PushTarget(EnumFacing direction, PatternProviderTarget target) {
            }
            var possibleTargets = new ArrayList<PushTarget>();

            for (var dir : getActiveSidesFiltered()) {
                var adjPos = be.getPos().offset(dir);
                var adjSide = dir.getOpposite();

                var adapter = PatternProviderTarget.get(level, adjPos, adjSide, actionSource);
                if (adapter == null) {
                    continue;
                }

                BlockingMode blocking = getConfigManager().getSetting(Settings.BLOCKING_MODE);
                if (blocking == BlockingMode.YES && adapter.containsPatternInput(resonatingPatternInputs)) {
                    continue;
                }
                if (blocking == BlockingMode.STRONG && adapter.containsAnyStack()) {
                    continue;
                }

                possibleTargets.add(new PushTarget(dir, adapter));
            }

            rearrangeRoundRobin(possibleTargets);

            for (int i = 0; i < possibleTargets.size(); i++) {
                var p = possibleTargets.get(i);

                if (adapterAcceptsAllLocal(p.target(), remaining)) {
                    chosenDirection = p.direction();
                    chosenAdapter = p.target();
                    localRoundRobinIndex += i + 1;
                    break;
                }
            }

            if (chosenAdapter == null) {
                return false;
            }
        }

        // 正式执行插入
        for (var m : marked) {
            var adapter = findTarget(m.target());
            if (adapter == null) {
                if (ae2csl.AE2CrystalScienceLegacy.LOGGER.isDebugEnabled()) {
                    ae2csl.AE2CrystalScienceLegacy.LOGGER.debug(
                        "[resonating] no target adapter for {} at {}/{} (dim {})",
                        m.key(), m.target().getPos().getPos(), m.target().getFace(),
                        m.target().getPos().getDimension());
                }
                return false;
            }

            long inserted = adapter.insert(m.key(), m.amount(), Actionable.MODULATE);
            if (inserted < m.amount()) {
                var left = m.amount() - inserted;
                resonatingSendList.add(new PendingSend(m.target(), new GenericStack(m.key(), left)));
                this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
            }
        }

        // 无目的地材料走原版（直接按 remaining 推送；不走 AEProcessingPattern 的倍率推导，
        // 因为 marked 部分已从 remaining 扣除，会让它推导出 0 倍）
        if (chosenAdapter != null) {
            final var adapterFinal = chosenAdapter;

            for (var counter : remaining) {
                counter.forEach((what, amount) -> {
                    var inserted = adapterFinal.insert(what, amount, Actionable.MODULATE);
                    if (inserted < amount) {
                        addToLocalSendList(what, amount - inserted);
                    }
                });
            }

            this.localSendDirection = chosenDirection;
        }

        if (ae2csl.AE2CrystalScienceLegacy.LOGGER.isDebugEnabled()) {
            ae2csl.AE2CrystalScienceLegacy.LOGGER.debug(
                "[resonating] push {} marked={} remainingEmpty={} selectedLocal={}",
                patternDetails.getDefinition(), marked.size(), isEmpty(remaining), chosenAdapter != null);
        }

        // 成功事件/锁定逻辑沿用原版
        onResonatingPushSuccess(patternDetails);

        // 立刻尝试把缓存送出
        boolean didSomething = false;
        didSomething |= sendLocalStacksOut();
        didSomething |= sendResonatingStacksOut();

        if (didSomething) {
            this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
        }

        return true;
    }

    /**
     * 解析本次推送的路由数据：
     * - 谐振样板：自身 sparseInputs + 每个输入的 target；
     * - 普通处理样板：provider 默认目标非空时，用处理样板的 sparseInputs 与默认目标按下标映射；
     * - 其他：返回 null（走原版）。
     */
    private RoutedPatternData buildRoutedPatternData(IPatternDetails patternDetails) {
        if (patternDetails instanceof ResonatingPatternDetails resonating) {
            // 谐振样板：自身的 per-input target 优先；未标记的输入回退到 provider 默认目标。
            // 说明：1.21.1 源码对谐振样板不使用默认目标（默认目标只服务普通处理样板），
            // 但那样链接器设的目标对「已转换的谐振样板」完全无效；这里作为 port 扩展做下标合并，
            // 让「链接器设默认目标 + 谐振样板」也能精准投放。
            var targets = new ArrayList<Optional<EncodedResonatingPattern.Target>>(resonating.getInputTargets());
            for (int i = 0; i < targets.size() && i < defaultInputTargets.size(); i++) {
                if (targets.get(i).isEmpty() && defaultInputTargets.get(i).isPresent()) {
                    targets.set(i, defaultInputTargets.get(i));
                }
            }
            return new RoutedPatternData(resonating.getSparseInputs(), targets);
        }

        if (!ResonatingProviderDefaults.hasAnyTarget(defaultInputTargets)) {
            return null;
        }

        if (!(patternDetails instanceof AEProcessingPattern processing)) {
            return null;
        }

        List<GenericStack> sparseInputs = processing.getSparseInputs();
        var targets = new ArrayList<Optional<EncodedResonatingPattern.Target>>(sparseInputs.size());
        for (int i = 0; i < sparseInputs.size(); i++) {
            targets.add(i < defaultInputTargets.size() ? defaultInputTargets.get(i) : Optional.empty());
        }
        return new RoutedPatternData(sparseInputs, targets);
    }

    private record RoutedPatternData(List<GenericStack> sparseInputs,
                                     List<Optional<EncodedResonatingPattern.Target>> inputTargets) {

        private Optional<EncodedResonatingPattern.Target> getTargetForSparseInputIndex(int index) {
            return index < inputTargets.size() ? inputTargets.get(index) : Optional.empty();
        }
    }

    @Override
    public boolean isBusy() {
        return super.isBusy() || !resonatingSendList.isEmpty() || !localSendList.isEmpty();
    }

    /**
     * 每 tick 由宿主 Tile.update() 调用（服务端）：冲刷待发 + 拉动
     */
    public void tickServer() {
        if (!mainNode.isActive()) {
            return;
        }
        boolean did = sendResonatingStacksOut() | sendLocalStacksOut() | doPullWork();
        if (did) {
            saveChanges();
        }
    }

    /**
     * 从周围面抽取资源进入返回仓
     */
    private boolean doPullWork() {
        if (!isEnablePull()) {
            return false;
        }

        var hostBe = host.getTileEntity();
        if (!(hostBe.getWorld() instanceof WorldServer)) {
            return false;
        }

        if (!mainNode.isActive()) {
            return false;
        }

        var returnInv = getReturnInv();

        var sides = getActiveSidesFiltered();
        if (sides.isEmpty()) {
            return false;
        }

        // 扫描限流
        final int maxKeysPerTick = 32;
        int scanned = 0;

        for (var dir : sides) {
            var adjPos = hostBe.getPos().offset(dir);
            var adjSide = dir.getOpposite();

            TileEntity adjTile = hostBe.getWorld().getTileEntity(adjPos);
            if (adjTile == null) {
                continue;
            }

            IItemHandler handler = adjTile.getCapability(
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, adjSide);
            if (handler == null) {
                continue;
            }

            for (int s = 0; s < handler.getSlots(); s++) {
                if (scanned++ >= maxKeysPerTick) {
                    return false;
                }

                ItemStack stack = handler.getStackInSlot(s);
                if (stack.isEmpty()) {
                    continue;
                }

                AEKey key = ae2.api.stacks.AEItemKey.of(stack);
                if (key == null) {
                    continue;
                }

                long request = Math.min(stack.getCount(), 4000);

                long canBuffer = returnInv.insert(key, request, Actionable.SIMULATE, actionSource);
                if (canBuffer <= 0) {
                    continue;
                }

                ItemStack extracted = handler.extractItem(s, (int) canBuffer, false);
                if (extracted.isEmpty()) {
                    continue;
                }

                AEKey extractedKey = ae2.api.stacks.AEItemKey.of(extracted);
                if (extractedKey == null) {
                    continue;
                }

                long buffered = returnInv.insert(extractedKey, extracted.getCount(), Actionable.MODULATE,
                    actionSource);

                long leftover = extracted.getCount() - buffered;
                if (leftover > 0) {
                    // 放不回的塞回原槽位
                    ItemStack back = extracted.copy();
                    back.setCount((int) leftover);
                    handler.insertItem(s, back, false);
                }
                return true;
            }
        }
        return false;
    }

    // ------------------- 辅助方法 -------------------------

    private boolean isBlockedByMode(PatternProviderTarget adapter) {
        BlockingMode blocking = getConfigManager().getSetting(Settings.BLOCKING_MODE);
        if (blocking == BlockingMode.STRONG) {
            return adapter.containsAnyStack();
        }
        if (blocking == BlockingMode.YES) {
            return adapter.containsPatternInput(this.resonatingPatternInputs);
        }
        return false;
    }

    private static KeyCounter[] copyKeyCounters(KeyCounter[] inputHolder) {
        var out = new KeyCounter[inputHolder.length];
        for (int i = 0; i < inputHolder.length; i++) {
            out[i] = new KeyCounter();
            out[i].addAll(inputHolder[i]);
        }
        return out;
    }

    private static boolean removeFromRemaining(KeyCounter[] remaining, AEKey key, long amount) {
        long toRemove = amount;

        for (var counter : remaining) {
            long avail = counter.get(key);
            if (avail <= 0) {
                continue;
            }

            long take = Math.min(avail, toRemove);
            counter.remove(key, take);
            toRemove -= take;

            if (toRemove <= 0) {
                return true;
            }
        }

        return false;
    }

    private static boolean isEmpty(KeyCounter[] holders) {
        for (var h : holders) {
            for (var entry : h) {
                if (entry.getLongValue() > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static <T> void rearrangeRoundRobin(List<T> list, int roundRobinIndex) {
        if (list.isEmpty()) {
            return;
        }

        int idx = Math.floorMod(roundRobinIndex, list.size());
        if (idx == 0) {
            return;
        }

        var head = new ArrayList<>(list.subList(0, idx));
        list.subList(0, idx).clear();
        list.addAll(head);
    }

    private <T> void rearrangeRoundRobin(List<T> list) {
        rearrangeRoundRobin(list, localRoundRobinIndex);
    }

    private boolean adapterAcceptsAllLocal(PatternProviderTarget target, KeyCounter[] inputHolder) {
        for (var inputList : inputHolder) {
            for (var input : inputList) {
                var inserted = target.insert(input.getKey(), input.getLongValue(), Actionable.SIMULATE);
                if (inserted == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 复刻父类 getActiveSides 的过滤逻辑
     */
    private Set<EnumFacing> getActiveSidesFiltered() {
        var sides = EnumSet.copyOf(host.getTargets());

        var node = mainNode.getNode();
        if (node != null) {
            for (var entry : node.getInWorldConnections().entrySet()) {
                var otherNode = entry.getValue().getOtherSide(node);
                if (otherNode.getOwner() instanceof PatternProviderLogicHost
                    || otherNode.getOwner() instanceof IntegratedInterfaceHost
                    || (otherNode.getOwner() instanceof InterfaceLogicHost
                    && Objects.equals(otherNode.grid(), mainNode.getGrid()))) {
                    sides.remove(entry.getKey());
                }
            }
        }

        return sides;
    }

    @Nullable
    private WorldServer getTargetLevel(EncodedResonatingPattern.Target target) {
        var hostLevel = host.getTileEntity().getWorld();
        if (!(hostLevel instanceof WorldServer)) {
            return null;
        }

        return FMLCommonHandler.instance().getMinecraftServerInstance()
            .getWorld(target.getPos().getDimension());
    }

    private boolean isTargetChunkLoaded(WorldServer level, BlockPos pos) {
        return level.isBlockLoaded(pos);
    }

    @Nullable
    private PatternProviderTarget findTarget(EncodedResonatingPattern.Target target) {
        var targetLevel = getTargetLevel(target);
        if (targetLevel == null) {
            return null;
        }

        var pos = target.getPos().getPos();
        if (!isTargetChunkLoaded(targetLevel, pos)) {
            return null;
        }

        PatternProviderTarget adapter = PatternProviderTarget.get(targetLevel, pos, target.getFace(), actionSource);
        if (adapter != null) {
            return adapter;
        }

        // 兜底：AE2 的能力解析只认 ITEM_HANDLER_CAPABILITY（以及 ME_STORAGE）。
        // 很多 1.12.2 机器（包括本模组部分机器、原版箱子）只有 IInventory，没有该能力；
        // 这里用 InvWrapper 兜底，保证「标记了却不发」不会发生。
        TileEntity tile = targetLevel.getTileEntity(pos);
        if (tile instanceof IInventory inventory) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[resonating] fallback to IInventory target at {}/{} tile={}",
                    pos, target.getFace(), tile.getClass().getSimpleName());
            }
            return new InventoryFallbackTarget(new InvWrapper(inventory));
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[resonating] target probe {} side={} tile={} chunkLoaded={} itemCap={} iinventory={}",
                pos, target.getFace(),
                tile == null ? "null" : tile.getClass().getSimpleName(),
                isTargetChunkLoaded(targetLevel, pos),
                tile != null && tile.hasCapability(
                    net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, target.getFace()),
                tile instanceof IInventory);
        }

        return null;
    }

    /** 仅用于能力解析失败的兜底：把 IInventory 包成 PatternProviderTarget（插入/查询） */
    private static final class InventoryFallbackTarget implements PatternProviderTarget {
        private final InvWrapper handler;

        private InventoryFallbackTarget(InvWrapper handler) {
            this.handler = handler;
        }

        @Override
        public long insert(AEKey what, long amount, Actionable type) {
            return insert(what, amount, type, ae2.api.config.PatternProviderInsertionMode.DEFAULT);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable type,
                           ae2.api.config.PatternProviderInsertionMode insertionMode) {
            if (!(what instanceof AEItemKey itemKey) || amount <= 0) {
                return 0;
            }

            long remaining = amount;
            long insertedTotal = 0;
            int maxStack = itemKey.getReadOnlyStack().getMaxStackSize();

            while (remaining > 0) {
                int request = (int) Math.min(remaining, Math.max(1, maxStack));
                ItemStack proto = itemKey.toStack(request);
                for (int i = 0; i < handler.getSlots() && !proto.isEmpty(); i++) {
                    proto = handler.insertItem(i, proto, type == Actionable.SIMULATE);
                }
                int moved = request - proto.getCount();
                if (moved <= 0) {
                    break;
                }
                insertedTotal += moved;
                remaining -= moved;
                if (type == Actionable.SIMULATE) {
                    break;
                }
            }
            return insertedTotal;
        }

        @Override
        public boolean containsPatternInput(java.util.Set<AEKey> patternInputs) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                AEItemKey key = AEItemKey.of(stack);
                if (key != null && patternInputs.contains(key)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean containsAnyStack() {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean hasEmptySlots() {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (handler.getStackInSlot(i).isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }

    private void addToLocalSendList(AEKey what, long amount) {
        if (amount > 0) {
            localSendList.add(new GenericStack(what, amount));
            this.mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
        }
    }

    private boolean sendLocalStacksOut() {
        if (localSendDirection == null) {
            return false;
        }

        TileEntity be = host.getTileEntity();
        World level = be.getWorld();
        if (level == null) {
            return false;
        }

        BlockPos adjPos = be.getPos().offset(localSendDirection);
        PatternProviderTarget adapter = PatternProviderTarget.get(level, adjPos,
            localSendDirection.getOpposite(), actionSource);
        if (adapter == null) {
            return false;
        }

        boolean did = false;
        for (var it = localSendList.listIterator(); it.hasNext();) {
            GenericStack stack = it.next();
            long inserted = adapter.insert(stack.what(), stack.amount(), Actionable.MODULATE);
            if (inserted >= stack.amount()) {
                it.remove();
                did = true;
            } else if (inserted > 0) {
                it.set(new GenericStack(stack.what(), stack.amount() - inserted));
                did = true;
            }
        }

        if (localSendList.isEmpty()) {
            localSendDirection = null;
        }
        return did;
    }

    private boolean sendResonatingStacksOut() {
        if (resonatingSendList.isEmpty()) {
            return false;
        }

        boolean did = false;

        for (var it = resonatingSendList.listIterator(); it.hasNext();) {
            var pending = it.next();

            var adapter = findTarget(pending.target());
            if (adapter == null) {
                continue; // 维度不存在/区块未加载/能力不存在
            }

            // 阻挡模式
            if (isBlockedByMode(adapter)) {
                continue;
            }

            var what = pending.stack().what();
            var amount = pending.stack().amount();
            var inserted = adapter.insert(what, amount, Actionable.MODULATE);

            if (inserted >= amount) {
                it.remove();
                did = true;
            } else if (inserted > 0) {
                it.set(new PendingSend(pending.target(), new GenericStack(what, amount - inserted)));
                did = true;
            }
        }

        return did;
    }

    @Override
    public void addDrops(List<ItemStack> drops) {
        super.addDrops(drops);
        for (ItemStack stack : upgrades) {
            drops.add(stack);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        upgrades.clear();
        resonatingSendList.clear();
        localSendList.clear();
    }

    private record PendingSend(EncodedResonatingPattern.Target target, GenericStack stack) {
    }
}
