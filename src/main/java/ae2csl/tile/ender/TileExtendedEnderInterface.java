/*
 * AE2 Crystal Science - TileExtendedEnderInterface (1.12.2)
 * 扩展版末影接口（吸收配置 36 槽）。对照源码 EX_ENDER_INTERFACE_BLOCK_ENTITY。
 */
package ae2csl.tile.ender;

import ae2csl.init.AE2CSBlocks;

import net.minecraft.item.ItemStack;

public class TileExtendedEnderInterface extends TileEnderInterface {

    @Override
    public boolean isExtended() {
        return true;
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(AE2CSBlocks.EX_ENDER_INTERFACE_BLOCK);
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(AE2CSBlocks.EX_ENDER_INTERFACE_BLOCK);
    }
}
