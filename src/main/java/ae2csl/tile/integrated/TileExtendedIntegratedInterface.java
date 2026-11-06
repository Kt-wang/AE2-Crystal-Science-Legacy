/*
 * AE2 Crystal Science - TileExtendedIntegratedInterface (1.12.2)
 * 扩展版集成接口（36槽）。对照源码 EX_INTEGRATED_INTERFACE_BLOCK_ENTITY。
 */
package ae2csl.tile.integrated;

import ae2csl.init.AE2CSBlocks;

import net.minecraft.item.ItemStack;

public class TileExtendedIntegratedInterface extends TileIntegratedInterface {

    @Override
    public boolean isExtended() {
        return true;
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(AE2CSBlocks.EX_INTEGRATED_INTERFACE_BLOCK);
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(AE2CSBlocks.EX_INTEGRATED_INTERFACE_BLOCK);
    }
}
