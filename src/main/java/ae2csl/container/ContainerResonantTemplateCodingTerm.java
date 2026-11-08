/*
 * AE2 Crystal Science - ContainerResonantTemplateCodingTerm (1.12.2)
 *
 * 复用 AE2 的样板编码终端容器（编码网格、样板槽、模式切换等），并按本模组开关
 * 额外支持「编码为谐振样板」：开启后，编码按钮产出 ae2csl:resonating_pattern。
 */
package ae2csl.container;

import ae2.api.stacks.GenericStack;
import ae2.container.slot.FakeSlot;
import ae2.parts.encoding.PatternEncodingLogic;
import ae2.util.ConfigGuiInventory;
import ae2csl.me.ResonantPatternEncodingCapacity;
import ae2.container.me.patternencode.ContainerPatternEncodingTerm;
import ae2.helpers.IPatternTerminalLogicHost;
import ae2.parts.encoding.PatternEncodingLogic;
import ae2csl.me.crafting.ResonatingPatternDetails;
import ae2csl.me.part.IResonantTemplateCodingTerminalHost;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ContainerResonantTemplateCodingTerm extends ContainerPatternEncodingTerm {

    /** 与 fork 的 ACTION_ENCODE 同名（客户端动作靠字符串路由） */
    private static final String ACTION_ENCODE = "encode";
    private static final String BLANK_PATTERN_ID = "ae2:blank_pattern";
    private static final String RESONATING_PATTERN_ID = "ae2csl:resonating_pattern";

    private final IResonantTemplateCodingTerminalHost resonantHost;

    /** 扩容后追加的槽位（AE2 原本只有 81 输入 / 27 输出） */
    private FakeSlot[] extraInputSlots = new FakeSlot[0];
    private FakeSlot[] extraOutputSlots = new FakeSlot[0];
    private FakeSlot[] allInputSlots;
    private FakeSlot[] allOutputSlots;

    public ContainerResonantTemplateCodingTerm(InventoryPlayer ip,
                                               ae2.helpers.IPatternTerminalGuiHost guiHost,
                                               IResonantTemplateCodingTerminalHost host) {
        super(ip, guiHost);
        this.resonantHost = host;
        // 顶部标题清空：界面里只保留样式中的「样板编码」区域标题，避免出现两个终端名。
        // （AE2 在 AEBaseGui 构造时用 container.getGuiTitle() 覆盖样式里的 dialog_title，
        //   所以这里给空串才会真的不显示。）
        setGuiTitle(new net.minecraft.util.text.TextComponentString(""));
        expandEncodingSlots();
    }

    public IResonantTemplateCodingTerminalHost getResonantHost() {
        return resonantHost;
    }

    /**
     * 编码库存扩容到 144/36 后，父类只按原数组长度创建了 81/27 个槽，
     * 这里补齐剩余槽位，并让面板通过 getProcessingInputSlots() 看到完整数组
     * （ProcessingEncodingPanel 按数组长度动态计算滚动范围，无需改 AE2 面板）。
     */
    private void expandEncodingSlots() {
        FakeSlot[] baseInputs = super.getProcessingInputSlots();
        FakeSlot[] baseOutputs = super.getProcessingOutputSlots();
        this.allInputSlots = baseInputs;
        this.allOutputSlots = baseOutputs;

        if (!(this.resonantHost instanceof IPatternTerminalLogicHost logicHost)) {
            return;
        }
        PatternEncodingLogic logic = logicHost.getLogic();
        if (logic == null) {
            return;
        }

        ConfigGuiInventory inputs = logic.getEncodedInputInv().createGuiWrapper();
        if (inputs.size() > baseInputs.length) {
            this.extraInputSlots = new FakeSlot[inputs.size() - baseInputs.length];
            for (int i = 0; i < this.extraInputSlots.length; i++) {
                FakeSlot slot = new FakeSlot(inputs, baseInputs.length + i, 0, 0);
                slot.setHideAmount(true);
                this.extraInputSlots[i] = slot;
                addSlot(slot, ae2.container.SlotSemantics.PROCESSING_INPUTS);
            }
            this.allInputSlots = concat(baseInputs, this.extraInputSlots);
        }

        ConfigGuiInventory outputs = logic.getEncodedOutputInv().createGuiWrapper();
        if (outputs.size() > baseOutputs.length) {
            this.extraOutputSlots = new FakeSlot[outputs.size() - baseOutputs.length];
            for (int i = 0; i < this.extraOutputSlots.length; i++) {
                this.extraOutputSlots[i] = new FakeSlot(outputs, baseOutputs.length + i, 0, 0);
                addSlot(this.extraOutputSlots[i], ae2.container.SlotSemantics.PROCESSING_OUTPUTS);
            }
            this.allOutputSlots = concat(baseOutputs, this.extraOutputSlots);
        }
    }

    private static FakeSlot[] concat(FakeSlot[] base, FakeSlot[] extra) {
        FakeSlot[] merged = new FakeSlot[base.length + extra.length];
        System.arraycopy(base, 0, merged, 0, base.length);
        System.arraycopy(extra, 0, merged, base.length, extra.length);
        return merged;
    }

    @Override
    public FakeSlot[] getProcessingInputSlots() {
        return this.allInputSlots != null ? this.allInputSlots : super.getProcessingInputSlots();
    }

    @Override
    public FakeSlot[] getProcessingOutputSlots() {
        return this.allOutputSlots != null ? this.allOutputSlots : super.getProcessingOutputSlots();
    }


    @Override
    public void encode(boolean preferModifierAndInventory) {
        if (this.resonantHost == null || !this.resonantHost.isEncodeResonatingPattern()) {
            super.encode(preferModifierAndInventory);
            return;
        }

        if (isClientSide()) {
            sendClientAction(ACTION_ENCODE, preferModifierAndInventory);
            return;
        }

        if (!encodeResonatingPattern()) {
            // 条件不足（没输入 / 没空样板 / 物品缺失）时退回原版样板编码
            super.encode(preferModifierAndInventory);
        }
    }

    /**
     * 用当前编码栏位产出谐振样板：
     * 1) 从编码逻辑读取稀疏输入/输出；
     * 2) 消耗一个空样板；
     * 3) 成品进玩家背包（放不下就掉在玩家脚下）。
     */
    private boolean encodeResonatingPattern() {
        if (!(this.resonantHost instanceof IPatternTerminalLogicHost logicHost)) {
            return false;
        }
        PatternEncodingLogic logic = logicHost.getLogic();
        if (logic == null) {
            return false;
        }

        List<GenericStack> inputs = collect(logic.getEncodedInputInv());
        List<GenericStack> outputs = collect(logic.getEncodedOutputInv());
        if (inputs.stream().noneMatch(Objects::nonNull)) {
            return false;
        }
        if (outputs.isEmpty() || outputs.get(0) == null) {
            return false;
        }

        Item resonating = Item.REGISTRY.getObject(new ResourceLocation(RESONATING_PATTERN_ID));
        if (resonating == null) {
            return false;
        }
        ItemStack pattern = new ItemStack(resonating);
        try {
            ResonatingPatternDetails.encode(pattern, inputs, outputs);
        } catch (IllegalArgumentException e) {
            return false;
        }

        // 输出槽必须可用：空着，或放着已编码样板（会被覆盖）时才允许编码
        net.minecraft.inventory.Slot outputSlot = findSlot(ae2.container.SlotSemantics.ENCODED_PATTERN);
        if (outputSlot == null) {
            return false;
        }
        ItemStack current = outputSlot.getStack();
        if (!current.isEmpty() && !ae2.api.crafting.PatternDetailsHelper.isEncodedPattern(current)) {
            return false;
        }

        if (!consumeBlankPattern()) {
            return false;
        }

        outputSlot.putStack(pattern.copy());
        outputSlot.onSlotChanged();
        detectAndSendChanges();
        return true;
    }

    private static List<GenericStack> collect(ae2.util.ConfigInventory inv) {
        List<GenericStack> list = new ArrayList<>(inv.size());
        for (int i = 0; i < inv.size(); i++) {
            list.add(inv.getStack(i));
        }
        return list;
    }

    /** 通过容器语义找到槽位（空样板 / 已编码样板） */
    private net.minecraft.inventory.Slot findSlot(ae2.container.SlotSemantic semantic) {
        java.util.List<net.minecraft.inventory.Slot> slots = getSlots(semantic);
        return slots.isEmpty() ? null : slots.get(0);
    }

    /** 从空样板槽扣除 1 个 ae2:blank_pattern */
    private boolean consumeBlankPattern() {
        Item blank = Item.REGISTRY.getObject(new ResourceLocation(BLANK_PATTERN_ID));
        if (blank == null) {
            return false;
        }
        net.minecraft.inventory.Slot blankSlot = findSlot(ae2.container.SlotSemantics.BLANK_PATTERN);
        if (blankSlot == null) {
            return false;
        }
        ItemStack stack = blankSlot.getStack();
        if (stack.isEmpty() || stack.getItem() != blank) {
            return false;
        }
        stack.shrink(1);
        blankSlot.putStack(stack.isEmpty() ? ItemStack.EMPTY : stack);
        blankSlot.onSlotChanged();
        return true;
    }
}
