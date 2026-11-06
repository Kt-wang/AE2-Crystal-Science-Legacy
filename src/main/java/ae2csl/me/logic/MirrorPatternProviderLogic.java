/*
 * AE2 Crystal Science - MirrorPatternProviderLogic (1.12.2)
 * 对照 1.21.1 源码 common/me/logic/MirrorPatternProviderLogic.java
 *
 * 1.12.2 适配说明：
 * - fork 的 PatternProviderLogic.patterns/patternInputs/configManager 是 private，
 *   无法像源码那样直接改写；等效方案：
 *   · getAvailablePatterns() 直接实时代理目标的样板列表（网络刷新时会重新调用，功能等价）
 *   · getConfigManager() 覆写返回 MirrorConfigManager 包装（源码是替换 configManager 字段）
 * - 阻塞模式差异：fork 的 pushPattern 阻塞检查用的是自身 patternInputs（恒空），
 *   镜像状态下阻塞模式不生效（源码有效）。fork 私有字段限制，暂无等效方案。
 */
package ae2csl.me.logic;

import ae2.api.config.Actionable;
import ae2.api.config.BlockingMode;
import ae2.api.config.Settings;
import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.IManagedGridNode;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.api.util.IConfigManager;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2.helpers.patternprovider.PatternProviderTarget;
import ae2.me.helpers.MachineSource;
import ae2csl.util.MirrorConfigManager;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MirrorPatternProviderLogic extends PatternProviderLogic {

    private final IManagedGridNode mainNode;
    private final PatternProviderLogicHost host;
    private final MirrorConfigManager mirrorConfigManager;
    private final MachineSource actionSource;
    private @Nullable MirroredPatternProviderTarget mirrorTarget;
    public WeakReference<PatternProviderLogicHost> cachedTarget = new WeakReference<>(null);

    /** 镜像的处理样板列表（本地推送用；fork 私有 patterns 因 0 槽恒空不可用） */
    private final List<IPatternDetails> mirroredPatterns = new ArrayList<>();
    private final Set<AEKey> mirrorPatternInputs = new HashSet<>();
    private int roundRobinIndex = 0;

    public MirrorPatternProviderLogic(IManagedGridNode mainNode, PatternProviderLogicHost host) {
        super(mainNode, host, 0);
        this.mainNode = mainNode;
        this.host = host;
        // fork 字段为 private，改为覆写 getConfigManager()
        this.mirrorConfigManager = new MirrorConfigManager(this, super.getConfigManager());
        this.actionSource = new MachineSource(mainNode::getNode);
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.mirrorConfigManager;
    }

    public boolean isMirroring() {
        return mirrorTarget != null;
    }

    public @Nullable MirroredPatternProviderTarget getMirrorTarget() {
        return mirrorTarget;
    }

    public void setMirrorTarget(@Nullable MirroredPatternProviderTarget mirrorTarget) {
        this.mirrorTarget = mirrorTarget;
        this.cachedTarget = new WeakReference<>(null);
        updatePatterns();
        host.saveChanges();
        if (host instanceof MirrorPatternProviderHost mirrorHost) {
            mirrorHost.markForLogicClientUpdate();
        }
    }

    /**
     * 客户端同步入口（对照源码 readMirrorTargetFromStream）
     *
     * @return 目标是否发生变化
     */
    public boolean readMirrorTargetFromStream(@Nullable MirroredPatternProviderTarget mirrorTarget) {
        boolean changed = !Objects.equals(this.mirrorTarget, mirrorTarget);
        this.mirrorTarget = mirrorTarget;
        this.cachedTarget = new WeakReference<>(null);
        return changed;
    }

    public @Nullable PatternProviderLogicHost resolveMirrorTargetHost() {
        if (mirrorTarget == null) {
            return null;
        }

        PatternProviderLogicHost target = cachedTarget.get();
        if (target == null || target.getTileEntity().isInvalid() || target.getTileEntity().getWorld() == null) {
            target = mirrorTarget.resolve(host.getTileEntity().getWorld());
            if (target != null) {
                cachedTarget = new WeakReference<>(target);
            }
        }

        // 防止镜像链：目标本身也是镜像供应器且正在镜像时，视为无效目标
        if (target instanceof MirrorPatternProviderHost mirrored && mirrored.getMirroringLogic().isMirroring()) {
            cachedTarget = new WeakReference<>(null);
            return null;
        }

        return target;
    }

    @Override
    public int getPriority() {
        PatternProviderLogicHost target = resolveMirrorTargetHost();
        if (target != null) {
            return target.getLogic().getPriority();
        }

        return super.getPriority();
    }

    @Override
    public int getPatternPriority() {
        return getPriority();
    }

    /**
     * 本地推送（镜像从自己位置向相邻机器推送，而不是从目标位置）。
     * fork 父类的 pushPattern 成员校验查私有 patterns（镜像 0 槽恒空），不可用。
     * 安全策略：先全量模拟，目标放不下则整批不推（CPU 下 tick 重试），杜绝部分推送丢失。
     */
    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, int multiplier) {
        if (isMirroring()) {
            updatePatterns();
        }

        if (!mainNode.isActive() || !mirroredPatterns.contains(patternDetails)
            || getCraftingLockedReason() != ae2.api.config.LockCraftingMode.NONE) {
            return false;
        }

        TileEntity be = host.getTileEntity();
        World level = be.getWorld();
        if (level == null) {
            return false;
        }

        if (!patternDetails.supportsPushInputsToExternalInventory()) {
            return false;
        }

        int times = Math.max(1, multiplier);

        // 收集目标（对齐 fork 的阻塞模式语义）
        List<PatternProviderTarget> targets = new ArrayList<>();
        for (EnumFacing dir : host.getTargets()) {
            BlockPos adjPos = be.getPos().offset(dir);
            if (!level.isBlockLoaded(adjPos)) {
                continue;
            }
            PatternProviderTarget adapter = PatternProviderTarget.get(level, adjPos, dir.getOpposite(),
                actionSource);
            if (adapter == null) {
                continue;
            }
            BlockingMode blocking = getConfigManager().getSetting(Settings.BLOCKING_MODE);
            if (blocking == BlockingMode.YES && adapter.containsPatternInput(mirrorPatternInputs)) {
                continue;
            }
            if (blocking == BlockingMode.STRONG && adapter.containsAnyStack()) {
                continue;
            }
            targets.add(adapter);
        }
        if (targets.isEmpty()) {
            return false;
        }

        // 轮询
        if (!targets.isEmpty()) {
            roundRobinIndex %= targets.size();
            for (int i = 0; i < roundRobinIndex; i++) {
                targets.add(targets.get(i));
            }
            if (roundRobinIndex > 0) {
                targets.subList(0, roundRobinIndex).clear();
            }
        }

        for (int i = 0; i < targets.size(); i++) {
            PatternProviderTarget adapter = targets.get(i);
            if (!adapterAcceptsAllExact(adapter, inputHolder, times)) {
                continue;
            }
            for (int m = 0; m < times; m++) {
                patternDetails.pushInputsToExternalInventory(inputHolder, (what, amount) -> {
                    long inserted = adapter.insert(what, amount, Actionable.MODULATE);
                    if (inserted < amount) {
                        // 理论不会发生（已全量模拟），兜底进返回仓由父类 Ticker 退回网络
                        getReturnInv().insert(what, amount - inserted, Actionable.MODULATE, actionSource);
                    }
                });
            }
            roundRobinIndex += i + 1;
            host.saveChanges();
            return true;
        }
        return false;
    }

    /**
     * 全量模拟：目标必须能装下所有输入×次数，否则不推（防止部分推送）
     */
    private boolean adapterAcceptsAllExact(PatternProviderTarget target, KeyCounter[] inputHolder, int times) {
        for (KeyCounter inputList : inputHolder) {
            for (var entry : inputList) {
                long needed = entry.getLongValue() * times;
                if (needed <= 0) {
                    continue;
                }
                long inserted = target.insert(entry.getKey(), needed, Actionable.SIMULATE);
                if (inserted < needed) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void updatePatterns() {
        // 注意顺序：先重建镜像列表，再调 super——super 末尾的 requestUpdate
        // 会立即让合成服务快照 getAvailablePatterns()，快照必须拿到新列表
        mirroredPatterns.clear();
        mirrorPatternInputs.clear();

        PatternProviderLogicHost target = resolveMirrorTargetHost();
        if (target != null) {
            for (IPatternDetails details : target.getLogic().getAvailablePatterns()) {
                // fork 架构下镜像无法推送合成样板（装配室自持自答，不收外部推送），只代理处理样板
                if (details instanceof ae2.api.crafting.IAssemblerPattern) {
                    continue;
                }
                mirroredPatterns.add(details);
                for (IPatternDetails.IInput input : details.getInputs()) {
                    for (GenericStack candidate : input.possibleInputs()) {
                        if (candidate != null) {
                            mirrorPatternInputs.add(candidate.what().dropSecondary());
                        }
                    }
                }
            }
        }

        // 自身样板栏为 0 槽，super.updatePatterns() 恒为空解析（仅末尾触发网络刷新）
        super.updatePatterns();
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        if (!mainNode.isActive()) {
            return java.util.Collections.emptyList();
        }
        return mirroredPatterns;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        MirroredPatternProviderTarget.write(mirrorTarget, tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.mirrorTarget = MirroredPatternProviderTarget.read(tag);
        this.cachedTarget = new WeakReference<>(null);
        if (isMirroring()) {
            updatePatterns();
        }
    }

    // ---- 记忆卡/拆解设置（对照源码 writeMirrorSettings/readMirrorSettings，NBT 版） ----

    public void writeMirrorSettings(NBTTagCompound output) {
        MirroredPatternProviderTarget.write(mirrorTarget, output);
    }

    public void readMirrorSettings(NBTTagCompound input) {
        this.mirrorTarget = MirroredPatternProviderTarget.read(input);
        this.cachedTarget = new WeakReference<>(null);
        updatePatterns();
        if (host instanceof MirrorPatternProviderHost mirrorHost) {
            mirrorHost.markForLogicClientUpdate();
        }
    }
}
