/*
 * AE2 Crystal Science - ResonatingPatternProviderBlockItem (1.12.2)
 * 对照 1.21.1 source：供应器方块物品就是普通 BlockItem，没有任何"标记目标/滚轮选槽"交互。
 * 默认目标由谐振连接器在已放置的供应器上编辑；拆掉方块时 defaults 会写进掉落物，
 * 放置时由 ResonatingPatternProviderBlock#onBlockPlacedBy 读回（与源码一致）。
 */
package ae2csl.item.pattern;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;

public class ResonatingPatternProviderBlockItem extends ItemBlock {

    public ResonatingPatternProviderBlockItem(Block block) {
        super(block);
    }
}
