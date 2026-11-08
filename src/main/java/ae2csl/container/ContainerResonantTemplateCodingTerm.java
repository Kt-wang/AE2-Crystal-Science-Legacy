/*
 * AE2 Crystal Science - ContainerResonantTemplateCodingTerm (1.12.2)
 *
 * 复用 AE2 的样板编码终端容器（编码网格、样板槽、模式切换等），并按本模组开关
 * 额外支持「编码为谐振样板」：开启后，编码按钮产出 ae2csl:resonating_pattern。
 */
package ae2csl.container;

import ae2.api.stacks.GenericStack;
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

    public ContainerResonantTemplateCodingTerm(InventoryPlayer ip,
                                               ae2.helpers.IPatternTerminalGuiHost guiHost,
                                               IResonantTemplateCodingTerminalHost host) {
        super(ip, guiHost);
        this.resonantHost = host;
    }

    public IResonantTemplateCodingTerminalHost getResonantHost() {
        return resonantHost;
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
