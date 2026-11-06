/*
 * AE2 Crystal Science - MeteoritePatternProviderLogic (1.12.2)
 * 对照 1.21.1 源码 common/me/logic/MeteoritePatternProviderLogic.java
 * 陨石样板供应器：对工作台类样板(IAssemblerPattern)直接快速完成（消耗能量），
 * 速度卡提升每 tick 工作次数。
 *
 * 1.12.2 适配：
 * - IMolecularAssemblerSupportedPattern → fork IAssemblerPattern(assemble/fillCraftingGrid/getRemainingItems)
 * - CraftingInput.of → InventoryCrafting
 * - 源码的 Ticker 扩展方案在 fork 不可行（fork PatternProviderLogic 的 doWork/hasWorkToDo/sendStacksOut
 *   都是 private，且 addService 同接口会覆盖）→ 改为 tickServer() 由宿主 Tile.update() 每 tick 调用；
 *   返回仓/待发列表仍由父类自带的 Ticker 处理。
 */
package ae2csl.me.logic;

import ae2.api.config.Actionable;
import ae2.api.config.PowerMultiplier;
import ae2.api.crafting.IAssemblerPattern;
import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGrid;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.energy.IEnergyService;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.upgrades.UpgradeInventories;
import ae2.core.definitions.AEItems;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.me.helpers.MachineSource;
import ae2csl.init.AE2CSBlocks;
import ae2csl.util.KeyCounterHelper;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MeteoritePatternProviderLogic extends PatternProviderLogic
    implements IUpgradeableObject, ae2csl.api.crafting.IFreeMergePushProvider {

    private final IManagedGridNode mainNode;
    private final IActionSource actionSource;
    private final MeteoritePatternProviderHost meteoriteHost;

    /**
     * 在没有任何加速卡的情况下，我们每次合成的能量消耗
     */
    public static int energyPerWork = 50;

    /**
     * 升级槽
     */
    public final IUpgradeInventory upgrades;

    /**
     * 完成产物表，即将对外输出的物品会被临时存放在此，等待tick中再实际对外发送
     */
    final Object2LongOpenHashMap<AEKey> craftedContents = new Object2LongOpenHashMap<>();

    /**
     * 本tick已经完成过的工作次数
     */
    private int worksInRound = 0;

    /**
     * 本轮最大可完成工作次数
     */
    private int maxWorksInRound = 8;

    /** 缓存的加速卡数量（装超频卡时归零；升级变更很少，热路径不扫槽位。对齐上游） */
    private int speedCards = 0;

    /** 缓存的陨石超频卡数量 */
    private int overclockCards = 0;

    /** 上一次工作轮对应的世界时刻（用世界时间驱动轮次，方块/部件形态都可靠） */
    private long lastWorkRoundTick = -1;

    /**
     * 缓存表上限
     */
    private static final int OUTPUT_CACHE_LIMIT = 10;

    /**
     * 快速缓存，用于对同一个样板进行快速判定
     */
    private final Reference2ObjectArrayMap<IPatternDetails, List<GenericStack>> outputCache =
        new Reference2ObjectArrayMap<>(OUTPUT_CACHE_LIMIT);

    /**
     * 记录缓存顺序，用于顺序清理
     */
    private final ReferenceArrayList<IPatternDetails> outputCacheOrder =
        new ReferenceArrayList<>(OUTPUT_CACHE_LIMIT + 4);

    /**
     * 全量样板列表（含合成样板）；fork 私有 patterns 列表只含处理样板
     */
    private final List<IPatternDetails> allPatterns = new ArrayList<>();

    private static final Container NULL_CONTAINER = new Container() {
        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return false;
        }
    };

    public MeteoritePatternProviderLogic(IManagedGridNode mainNode, MeteoritePatternProviderHost host,
                                         int patternInventorySize) {
        super(mainNode, host, Item.getItemFromBlock(AE2CSBlocks.METEORITE_PATTERN_PROVIDER_BLOCK),
            patternInventorySize);

        this.meteoriteHost = host;
        // 进行一些额外设置，并保存重要信息在子类
        this.mainNode = mainNode;
        mainNode.setFlags(GridFlags.REQUIRE_CHANNEL, GridFlags.DENSE_CAPACITY);
        this.actionSource = new MachineSource(mainNode::getNode);
        this.upgrades = UpgradeInventories.forMachine(
            Item.getItemFromBlock(AE2CSBlocks.METEORITE_PATTERN_PROVIDER_BLOCK), 4, this::onUpgradesChange);

        // fork 的样板栏过滤器拒绝合成样板(IAssemblerPattern)，与源码行为不符——替换为全收
        // （陨石供应器的快速合成路径必须要能放合成样板）
        getPatternInv().setFilter(new PatternInventoryAllFilter(host));
    }

    private boolean canAcceptPattern(@Nullable IPatternDetails details) {
        if (details == null) {
            return false;
        }
        return details instanceof IAssemblerPattern;
    }

    /**
     * 维护全量样板列表（fork 的 updatePatterns 会排除合成样板）
     * 注意顺序：必须先重建 allPatterns 再调 super——super 末尾的 requestUpdate
     * 会立即让合成服务快照 getAvailablePatterns()，快照必须拿到新列表。
     */
    @Override
    public void updatePatterns() {
        allPatterns.clear();
        for (int slot = 0; slot < getActivePatternSlots(); slot++) {
            ItemStack stack = getPatternInv().getStackInSlot(slot);
            IPatternDetails details = ae2.api.crafting.PatternDetailsHelper.decodePattern(stack,
                meteoriteHost.getTileEntity().getWorld());
            if (details != null) {
                allPatterns.add(details);
            }
        }

        super.updatePatterns(); // 维护 fork 私有列表（处理样板）+ 末尾触发网络刷新快照
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        if (!this.mainNode.isActive()) {
            return Collections.emptyList();
        }
        return this.allPatterns;
    }

    /**
     * 63 槽全部激活——fork 的容量公式是 9+9/扩展卡，会把我们限到 9 槽；
     * 源码陨石供应器固定 63 槽、无扩展卡概念
     */
    @Override
    public int getActivePatternSlots() {
        return getPatternInv().size();
    }

    @Override
    public boolean canMergePatternPush(IPatternDetails patternDetails) {
        // 合成样板（快速合成通道）必须允许合并推送：否则合成 CPU 每刻只推 1 次，
        // 大批量合成（如 1000 个箱子）会慢到肉眼可见。倍率由 getMaxPatternPushMultiplier 按本轮剩余容量上报。
        if (patternDetails instanceof IAssemblerPattern) {
            return true;
        }
        return super.canMergePatternPush(patternDetails);
    }

    @Override
    public int getMaxPatternPushMultiplier(IPatternDetails patternDetails, int maxMultiplier) {
        if (patternDetails instanceof IAssemblerPattern) {
            resetWorkRoundIfNeeded();
            // 上报本轮剩余容量；返回 0 = 本轮已满，CPU 本轮跳过此供应器（不会退化为单推）
            return Math.max(0, Math.min(maxMultiplier, this.maxWorksInRound - this.worksInRound));
        }
        return super.getMaxPatternPushMultiplier(patternDetails, maxMultiplier);
    }

    private void onUpgradesChange() {
        // 超频卡：有任一超频卡时加速卡失效；每轮工作量 (8 << speed) + 128 * overclock
        this.overclockCards = Math.min(4, getInstalledUpgrades(getOverloadCardItem()));
        this.speedCards = overclockCards > 0 ? 0 : getInstalledUpgrades(AEItems.SPEED_CARD.item());
        this.maxWorksInRound = (8 << speedCards) + 128 * overclockCards;
        this.saveChanges();
    }

    private static Item getOverloadCardItem() {
        return Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation("ae2csl", "overload_card"));
    }

    private double getEnergyPerWorkAfterSpeed() {
        // 装超频卡后加速卡不再生效（含能耗侧），用缓存值
        return energyPerWork << this.speedCards;
    }

    /**
     * 每 tick 由宿主 Tile.update() 调用（服务端）。
     * 对应源码 Ticker 中的 workCraftedContents 部分；返回仓/待发列表由父类 Ticker 处理。
     */
    public void tickServer() {
        if (mainNode.isActive()) {
            workCraftedContents();
        }
    }

    private boolean workCraftedContents() {
        boolean worked = false;
        this.worksInRound = 0; // 每tick重置

        if (!craftedContents.isEmpty()) {
            @Nullable MEStorage gridInv = null;

            IGrid grid = getGrid();
            if (grid != null) {
                gridInv = grid.getStorageService().getInventory();
            }

            // 完成sendContent内容
            ObjectIterator<Object2LongMap.Entry<AEKey>> it = craftedContents.object2LongEntrySet().iterator();
            while (it.hasNext()) {
                Object2LongMap.Entry<AEKey> entry = it.next();
                AEKey key = entry.getKey();
                long remaining = entry.getLongValue();
                if (key == null || remaining <= 0) {
                    it.remove();
                    continue;
                }

                // 尝试塞入可用库存
                long allInserted = 0;
                if (gridInv != null) {
                    long inserted = gridInv.insert(key, remaining, Actionable.MODULATE, actionSource);
                    allInserted += inserted;
                    remaining -= inserted;
                }
                if (remaining > 0) {
                    long inserted = getReturnInv().insert(key, remaining, Actionable.MODULATE, actionSource);
                    allInserted += inserted;
                    remaining -= inserted;
                }

                // 更新 craftedContents并决定返回值
                if (remaining <= 0) {
                    it.remove();
                } else {
                    entry.setValue(remaining);
                }
                if (allInserted > 0) {
                    worked = true;
                }
            }
        }

        if (worked) {
            saveChanges();
        }
        return worked;
    }

    /**
     * 当前是否繁忙（有未发完的合成产物）
     */
    public boolean hasCraftedContents() {
        return !craftedContents.isEmpty();
    }

    /** 世界时间跨刻时重置本轮计数（方块和部件形态都安全） */
    private void resetWorkRoundIfNeeded() {
        net.minecraft.tileentity.TileEntity tile = this.meteoriteHost.getTileEntity();
        if (tile == null || tile.getWorld() == null) {
            return;
        }
        long now = tile.getWorld().getTotalWorldTime();
        if (now != this.lastWorkRoundTick) {
            this.lastWorkRoundTick = now;
            this.worksInRound = 0;
        }
    }

    /**
     * 如果是可通过陨石样板供应器快速完成的样板，则直接完成，否则回退到普通供应器逻辑
     */
    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, int multiplier) {
        if (!(patternDetails instanceof IAssemblerPattern pattern) || !canAcceptPattern(patternDetails)) {
            return super.pushPattern(patternDetails, inputHolder, multiplier);
        }

        // 成员校验 + 红石锁（fork 私有 patterns 列表不含合成样板，需自行校验）
        if (!mainNode.isActive() || !allPatterns.contains(patternDetails)
            || getCraftingLockedReason() != ae2.api.config.LockCraftingMode.NONE) {
            return false;
        }

        int times = Math.max(1, multiplier);
        // 按世界时间重置工作轮：部件形态没有 Tile.update 驱动 tickServer，
        // 轮次重置不能只靠 tick 通道，否则累计 8 次后快速合成永久停摆
        resetWorkRoundIfNeeded();
        // 如果本轮工作轮数不足，则直接返回（对齐源码：超出上限返回 false）
        if (worksInRound + times > maxWorksInRound) {
            return false;
        }

        // 单次产物
        List<GenericStack> output = getAssemblerPatternOutput(pattern, inputHolder);
        if (output == null) {
            return false;
        }

        // 如果能量不足则返回
        double neededEnergy = getEnergyPerWorkAfterSpeed() * times;
        if (!tryConsumeEnergyFromGrid(neededEnergy)) {
            return false;
        }

        // 记录一下当前发送表状态
        boolean wasEmpty = craftedContents.isEmpty();

        // 将产物与剩余物添加入表（×次数）
        for (GenericStack stack : output) {
            if (stack == null || stack.what() == null || stack.amount() <= 0) {
                continue;
            }
            craftedContents.addTo(stack.what(), stack.amount() * times);
        }

        // 提交、计数、唤醒ticker
        saveChanges();
        worksInRound += times;

        if (wasEmpty && !craftedContents.isEmpty()) {
            mainNode.ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
        }
        return true;
    }

    /**
     * 尝试从网络中扣除指定数量的能量，如果不足不扣除
     */
    private boolean tryConsumeEnergyFromGrid(double energy) {
        IGrid grid = getGrid();
        if (grid == null) {
            return false;
        }
        IEnergyService energyService = grid.getEnergyService();
        if (energyService == null) {
            return false;
        }

        double extracted = energyService.extractAEPower(energy, Actionable.MODULATE, PowerMultiplier.ONE);
        if (extracted + 1.0e-9 >= energy) {
            return true;
        }

        try {
            energyService.injectPower(extracted, Actionable.MODULATE);
        } catch (Throwable ignored) {
            // 默许损耗
        }
        return false;
    }

    @Nullable
    private List<GenericStack> getAssemblerPatternOutput(IAssemblerPattern pattern, KeyCounter[] inputHolder) {
        List<GenericStack> cachedOutput = outputCache.get(pattern);
        if (cachedOutput != null) {
            return cachedOutput;
        }

        World level = meteoriteHost.getTileEntity().getWorld();
        if (level == null) {
            return null;
        }

        // 计算真实输出和剩余物
        final ItemStack[] grid3x3 = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            grid3x3[i] = ItemStack.EMPTY;
        }
        try {
            KeyCounter[] inputHolderCopy = new KeyCounter[inputHolder.length];
            for (int i = 0; i < inputHolder.length; i++) {
                KeyCounter kc = inputHolder[i];
                inputHolderCopy[i] = (kc == null) ? new KeyCounter() : KeyCounterHelper.deepCopy(kc);
            }
            pattern.fillCraftingGrid(inputHolderCopy, (slot, stack) -> {
                if (slot >= 0 && slot < 9) {
                    grid3x3[slot] = (stack == null) ? ItemStack.EMPTY : stack;
                }
            });
        } catch (RuntimeException e) {
            // 出现任何异常，此时便不稳定，直接返回null
            return null;
        }

        // fork 的 AECraftingPattern.assemble 强制要求 3×3 网格（非3x3直接返回EMPTY），
        // 因此不能像 1.21.1 源码那样压缩边距，直接填 3×3
        InventoryCrafting input = new InventoryCrafting(NULL_CONTAINER, 3, 3);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = grid3x3[slot];
            input.setInventorySlotContents(slot, stack == null ? ItemStack.EMPTY : stack);
        }

        ItemStack output = pattern.assemble(input, level);
        if (output == null || output.isEmpty()) {
            // 无输出
            return null;
        }
        NonNullList<ItemStack> remainders = pattern.getRemainingItems(input);

        // 构造结果
        List<GenericStack> finalOutput = new ArrayList<>();
        GenericStack outputStack = GenericStack.fromItemStack(output);
        if (outputStack != null) {
            finalOutput.add(outputStack);
        }
        for (ItemStack stack : remainders) {
            GenericStack remainingStack = GenericStack.fromItemStack(stack);
            if (remainingStack != null) {
                finalOutput.add(remainingStack);
            }
        }

        // 写入并清理缓存
        outputCache.put(pattern, finalOutput);
        outputCacheOrder.add(pattern);
        while (outputCache.size() > OUTPUT_CACHE_LIMIT && !outputCacheOrder.isEmpty()) {
            IPatternDetails oldest = outputCacheOrder.remove(0);
            outputCache.remove(oldest);
        }
        return finalOutput;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        upgrades.writeToNBT(tag, "upgrades_meteorite");
        // craftedContents → NBTTagList(GenericStack)
        NBTTagList list = new NBTTagList();
        for (var entry : craftedContents.object2LongEntrySet()) {
            list.appendTag(GenericStack.writeTag(new GenericStack(entry.getKey(), entry.getLongValue())));
        }
        tag.setTag("crafted_contents", list);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        upgrades.readFromNBT(tag, "upgrades_meteorite");
        craftedContents.clear();
        if (tag.hasKey("crafted_contents", Constants.NBT.TAG_LIST)) {
            NBTTagList list = tag.getTagList("crafted_contents", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                GenericStack stack = GenericStack.readTag(list.getCompoundTagAt(i));
                if (stack != null && stack.what() != null && stack.amount() > 0) {
                    craftedContents.addTo(stack.what(), stack.amount());
                }
            }
        }
        onUpgradesChange();
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
        craftedContents.clear();
    }
}
