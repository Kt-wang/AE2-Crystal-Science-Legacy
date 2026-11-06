/*
 * AE2 Crystal Science - TileExtendedResonatingPatternProvider (1.12.2)
 * 扩展谐振样板供应器（36 样板槽）。
 */
package ae2csl.tile.resonating;

import ae2csl.init.AE2CSBlocks;

import net.minecraft.item.ItemStack;

public class TileExtendedResonatingPatternProvider extends TileResonatingPatternProvider {

    @Override
    public boolean isExtended() {
        return true;
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(AE2CSBlocks.EX_RESONATING_PATTERN_PROVIDER_BLOCK);
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return new ItemStack(AE2CSBlocks.EX_RESONATING_PATTERN_PROVIDER_BLOCK);
    }

    @Override
    public ae2.api.stacks.AEItemKey getTerminalIcon() {
        return ae2.api.stacks.AEItemKey.of(getMainContainerIcon());
    }
}
