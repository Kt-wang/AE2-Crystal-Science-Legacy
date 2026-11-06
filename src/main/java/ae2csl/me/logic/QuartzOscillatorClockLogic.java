/*
 * AE2 Crystal Science - QuartzOscillatorClockLogic (1.12.2)
 * 对照 1.21.1 源码 common/me/logic/QuartzOscillatorClockLogic.java 完整移植。
 * 石英振荡时钟：按 间隔(hold)+脉宽(width) 输出红石脉冲。
 *
 * 1.12.2 适配：
 * - Level.getGameTime() → World.getTotalWorldTime()（不受 /time set 影响，对齐源码语义）
 * - SoundEvents.COPPER_BULB_TURN_ON/OFF（1.21 音效）→ BLOCK_STONE_BUTTON_CLICK_ON/OFF
 * - playSound 补音量/音高参数
 */
package ae2csl.me.logic;

import ae2.api.config.RedstoneMode;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.upgrades.UpgradeInventories;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.core.definitions.AEItems;
import ae2.core.settings.TickRates;
import ae2csl.api.settings.AE2CSSettings;
import ae2csl.api.settings.SoundMode;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class QuartzOscillatorClockLogic implements IUpgradeableObject, IConfigurableObject {

    /**
     * 输出翻转后，输入采样冷却（避免输出回灌）
     */
    private static final int INPUT_SAMPLE_COOLDOWN_TICKS = 1;

    protected final QuartzOscillatorClockHost host;
    protected final IManagedGridNode mainNode;
    protected final IUpgradeInventory upgrades;
    private final IConfigManager cm;

    /**
     * 脉冲宽度
     */
    private int pulseWidthTicks = 1;

    /**
     * 每次脉冲的间隔
     */
    private int currentHold = 10;

    /**
     * 脉冲结束后到下一次脉冲开始的剩余tick数，字段主要目的在于读档后修正nextPulseGameTime
     */
    private int currentCountDown = currentHold;

    /**
     * 红石模式
     */
    private RedstoneMode currentRedStoneMode = RedstoneMode.IGNORE;

    /**
     * 下一次脉冲"允许开始"的世界时间点（从"脉冲结束"开始计时）
     * -1 表示尚未排程/将在脉冲结束时重新排程
     */
    private long nextPulseGameTime = -1;

    /**
     * 脉冲结束的世界时间
     */
    private long pulseEndGameTime = -1;

    /**
     * 读档/初始化时不直接碰世界层输出，在tick里统一同步
     */
    private boolean pendingPulseSync = false;

    /**
     * 外部红石输入缓存
     */
    private boolean cachedExternalPowered = false;

    /**
     * 输出翻转后若干tick内，不采样输入
     */
    private int inputSampleCooldown = 0;

    public QuartzOscillatorClockLogic(IManagedGridNode gridNode, QuartzOscillatorClockHost host, Item is) {
        this.host = host;
        this.mainNode = gridNode
            .setFlags()
            .addService(IGridTickable.class, new Ticker());

        this.cm = IConfigManager.builder(this::onConfigManagerChange)
            .registerSetting(AE2CSSettings.REDSTONE_CONTROLLED_NO_PULSE, RedstoneMode.IGNORE)
            .registerSetting(AE2CSSettings.SOUND_MODE, SoundMode.UNMUTE)
            .build();

        this.upgrades = UpgradeInventories.forMachine(is, 1, this::onUpgradesChange);
    }

    public int getCurrentHold() {
        return currentHold;
    }

    public void setCurrentHold(int currentHold) {
        int newHold = Math.max(1, currentHold);
        newHold = Math.min(newHold, 12000);
        if (this.currentHold == newHold) {
            return;
        }

        this.currentHold = newHold;

        // 只保证倒计时合法；真正的排程由tickServer统一修正
        if (this.currentCountDown <= 0 || this.currentCountDown > this.currentHold) {
            this.currentCountDown = this.currentHold;
        }

        this.host.saveChanges();
    }

    public int getPulseWidthTicks() {
        return pulseWidthTicks;
    }

    public void setPulseWidthTicks(int newValue) {
        newValue = Math.max(1, newValue);
        newValue = Math.min(newValue, 12000);
        if (this.pulseWidthTicks == newValue) {
            return;
        }

        this.pulseWidthTicks = newValue;

        this.host.saveChanges();
    }

    private void onUpgradesChange() {
        if (this.upgrades.isInstalled(AEItems.REDSTONE_CARD.item())) {
            this.currentRedStoneMode = this.cm.getSetting(AE2CSSettings.REDSTONE_CONTROLLED_NO_PULSE);
        } else {
            this.currentRedStoneMode = RedstoneMode.IGNORE;
        }
        this.host.saveChanges();
    }

    private void onConfigManagerChange() {
        if (!this.upgrades.isInstalled(AEItems.REDSTONE_CARD.item())) {
            return;
        }

        this.currentRedStoneMode = this.cm.getSetting(AE2CSSettings.REDSTONE_CONTROLLED_NO_PULSE);
        this.host.saveChanges();
    }

    private boolean isPulsing(World level) {
        return this.pulseEndGameTime >= 0 && level.getTotalWorldTime() < this.pulseEndGameTime;
    }

    /**
     * 获取外部红石输入：
     * - 自己正在输出（或刚翻转输出）时，不采样邻居信号，直接返回缓存值，避免回灌死锁
     * - 自己不输出时：允许采样并刷新缓存
     */
    private boolean getExternalPowered(World level, BlockPos pos) {
        // 脉冲期间不采样，避免读到自己输出形成的回灌
        if (this.pulseEndGameTime >= 0) {
            return this.cachedExternalPowered;
        }

        // 输出刚翻转后的冷却期不采样
        if (this.inputSampleCooldown > 0) {
            this.inputSampleCooldown--;
            return this.cachedExternalPowered;
        }

        boolean powered = level.isBlockPowered(pos);
        this.cachedExternalPowered = powered;
        return powered;
    }

    private boolean isAllowedByRedstoneMode(World level, BlockPos pos) {
        if (this.currentRedStoneMode == RedstoneMode.IGNORE) {
            return true;
        }

        boolean powered = getExternalPowered(level, pos);

        return switch (this.currentRedStoneMode) {
            case HIGH_SIGNAL -> powered;
            case LOW_SIGNAL -> !powered;
            default -> true;
        };
    }

    /**
     * 开始一次脉冲：只负责拉高输出并设置 pulseEndGameTime。
     * 注意：下一次脉冲的倒计时从"脉冲结束"开始，所以这里不推进/不消耗 countdown。
     */
    private void beginPulse(World level, BlockPos pos) {
        if (isPulsing(level)) {
            return;
        }

        this.pulseEndGameTime = level.getTotalWorldTime() + Math.max(1, pulseWidthTicks);

        // 具体输出交给host
        this.host.setPulseActive(true);
        notifyRedstone(level, pos);

        // 脉冲期间不排程下一次；等结束时再排程
        this.nextPulseGameTime = -1;

        // 短暂不采样输入
        this.inputSampleCooldown = INPUT_SAMPLE_COOLDOWN_TICKS;

        // 声效
        if (getConfigManager().getSetting(AE2CSSettings.SOUND_MODE) == SoundMode.UNMUTE) {
            level.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.3F, 0.6F);
        }
    }

    /**
     * 结束脉冲，并从"当前时刻"开始排程下一次脉冲：now + hold
     */
    private void endPulse(World level, BlockPos pos) {
        if (this.pulseEndGameTime < 0) {
            return;
        }

        this.pulseEndGameTime = -1;
        this.host.setPulseActive(false);
        notifyRedstone(level, pos);

        // 短暂不采样输入
        this.inputSampleCooldown = INPUT_SAMPLE_COOLDOWN_TICKS;

        int hold = Math.max(1, this.currentHold);
        this.currentCountDown = hold;
        this.nextPulseGameTime = level.getTotalWorldTime() + hold;

        // 声效
        if (getConfigManager().getSetting(AE2CSSettings.SOUND_MODE) == SoundMode.UNMUTE) {
            level.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.3F, 0.5F);
        }
    }

    private void tickServer(World level, BlockPos pos, int ticksSinceLastCall) {
        // 统一在首次server tick把世界输出对齐为未脉冲
        if (this.pendingPulseSync) {
            this.pendingPulseSync = false;

            this.pulseEndGameTime = -1;
            this.host.setPulseActive(false);
            notifyRedstone(level, pos);

            // 读档后：把 nextPulseGameTime 对齐为 "now + currentCountDown"
            int hold = Math.max(1, this.currentHold);
            if (this.currentCountDown <= 0 || this.currentCountDown > hold) {
                this.currentCountDown = hold;
            }
            this.nextPulseGameTime = level.getTotalWorldTime() + this.currentCountDown;

            // 添加一点冷却
            this.inputSampleCooldown = INPUT_SAMPLE_COOLDOWN_TICKS;
        }

        // 如果正在脉冲：只负责收尾，不推进倒计时（倒计时从脉冲结束开始）
        if (this.pulseEndGameTime >= 0) {
            if (level.getTotalWorldTime() >= this.pulseEndGameTime) {
                endPulse(level, pos);
            }
            return;
        }

        // 红石模式不允许：不走倒计时、不触发新脉冲
        if (!isAllowedByRedstoneMode(level, pos)) {
            return;
        }

        // 满足红石模式：走倒计时
        int hold = Math.max(1, this.currentHold);

        // 未排程则用当前倒计时排一次
        if (this.nextPulseGameTime < 0) {
            if (this.currentCountDown <= 0 || this.currentCountDown > hold) {
                this.currentCountDown = hold;
            }
            this.nextPulseGameTime = level.getTotalWorldTime() + this.currentCountDown;
        }

        long now = level.getTotalWorldTime();
        long remaining = this.nextPulseGameTime - now;

        if (remaining > 0) {
            // 只在非脉冲时更新 countdown 显示/存档值
            this.currentCountDown = (int) Math.min(hold, remaining);
            return;
        }

        // 到点：触发一次脉冲；注意这次脉冲的"下一次倒计时"会在 endPulse 里排程
        beginPulse(level, pos);
    }

    /**
     * 为了防止setPulseActive在某些实现可能跳过邻居通知，我们在这里手动进行通知，必须跟在setPulseActive后调用
     */
    private void notifyRedstone(World level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }

        IBlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // 通知宿主方块周围
        level.notifyNeighborsOfStateChange(pos, block, false);
        for (var dir : host.getTargets()) {
            level.notifyNeighborsOfStateChange(pos.offset(dir), block, false);
        }
    }

    public void writeToNBT(NBTTagCompound tag) {
        this.upgrades.writeToNBT(tag, "upgrades");
        this.cm.writeToNBT(tag);

        tag.setInteger("pulse_width_ticks", pulseWidthTicks);
        tag.setInteger("current_hold", this.currentHold);
        tag.setInteger("current_countdown", this.currentCountDown);
    }

    public void readFromNBT(NBTTagCompound tag) {
        this.upgrades.readFromNBT(tag, "upgrades");
        this.cm.readFromNBT(tag);

        this.pulseWidthTicks = tag.getInteger("pulse_width_ticks");
        this.currentHold = Math.max(1, tag.getInteger("current_hold"));
        this.currentCountDown = tag.getInteger("current_countdown");

        // 重置红石状态
        onUpgradesChange();

        // 读档时 逻辑层强制关闭脉冲
        this.pulseEndGameTime = -1;

        // 读档后 nextPulseGameTime 需要在 server tick 里用 world time 对齐
        this.nextPulseGameTime = -1;

        // 标记需要重置状态
        this.pendingPulseSync = true;

        // 输入采样缓存重置
        this.cachedExternalPowered = false;
        this.inputSampleCooldown = INPUT_SAMPLE_COOLDOWN_TICKS;

        int hold = Math.max(1, this.currentHold);
        if (this.currentCountDown <= 0 || this.currentCountDown > hold) {
            this.currentCountDown = hold;
        }
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.cm;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    public void addDrops(List<ItemStack> drops) {
        for (ItemStack stack : upgrades) {
            drops.add(stack);
        }
    }

    public void clearContent() {
        upgrades.clear();
    }

    private class Ticker implements IGridTickable {

        @Override
        public TickingRequest getTickingRequest(IGridNode node) {
            return new TickingRequest(TickRates.IOPort, false);
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
            TileEntity be = host.getTileEntity();
            World level = be.getWorld();

            if (level == null || level.isRemote) {
                return TickRateModulation.IDLE;
            }

            BlockPos pos = be.getPos();
            tickServer(level, pos, ticksSinceLastCall);

            if (isPulsing(level)) {
                return TickRateModulation.URGENT;
            }

            if (isAllowedByRedstoneMode(level, pos)) {
                return TickRateModulation.URGENT;
            }

            return TickRateModulation.SAME;
        }
    }
}
