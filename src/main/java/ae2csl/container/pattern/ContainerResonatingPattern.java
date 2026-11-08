/*
 * AE2 Crystal Science - ContainerResonatingPattern (1.12.2)
 *
 * [P] 查看谐振样板的展示界面：复用 AE2 的处理样板展示布局，
 * 但按 ResonatingPatternDetails 的稀疏输入/输出填格。
 */
package ae2csl.container.pattern;

import ae2.api.stacks.GenericStack;
import ae2.container.pattern.ContainerPattern;
import ae2csl.me.crafting.ResonatingPatternDetails;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ContainerResonatingPattern extends ContainerPattern {

    public ContainerResonatingPattern(InventoryPlayer playerInventory, ItemStack stack) {
        super(playerInventory, stack);
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                addDisplaySlot(this.inputs, row * 9 + column, 8 + column * 18, 9 + row * 18);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addDisplaySlot(this.outputs, row * 9 + column, 8 + column * 18, 189 + row * 18);
            }
        }
    }

    @Override
    protected void analyse() {
        if (!(this.details instanceof ResonatingPatternDetails pattern)) {
            invalidate();
            return;
        }
        addContents(pattern.getSparseInputs(), this.inputs);
        addContents(pattern.getSparseOutputs(), this.outputs);
    }

    private void addContents(List<GenericStack> stacks, List<GenericStack[]> slots) {
        for (GenericStack stack : stacks) {
            if (stack == null) {
                slots.add(new GenericStack[0]);
            } else {
                slots.add(new GenericStack[] { new GenericStack(stack.what(), stack.amount()) });
            }
        }
    }
}
