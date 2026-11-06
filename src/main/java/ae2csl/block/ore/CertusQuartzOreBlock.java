/*
 * AE2 Crystal Science - CertusQuartzOreBlock
 * Drops AE2 certus_quartz_crystal (1-2, fortune applies). Silk touch drops itself.
 */
package ae2csl.block.ore;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class CertusQuartzOreBlock extends Block {

    public CertusQuartzOreBlock() {
        super(Material.ROCK);
        setHardness(3.0F); setResistance(5.0F); setHarvestLevel("pickaxe", 2);
    }

    private Item getDropItem() {
        Item it = Item.getByNameOrId("ae2:certus_quartz_crystal");
        return it != null ? it : Item.getItemFromBlock(this);
    }

    @Override public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return getDropItem();
    }

    @Override public int quantityDropped(Random rand) { return 1 + rand.nextInt(2); }

    @Override public int quantityDroppedWithBonus(int fortune, Random rand) {
        if (fortune > 0) {
            int bonus = Math.max(0, rand.nextInt(fortune + 2) - 1);
            return quantityDropped(rand) * (bonus + 1);
        }
        return quantityDropped(rand);
    }

    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Random rand = world instanceof World ? ((World) world).rand : RANDOM;
        int count = quantityDroppedWithBonus(fortune, rand);
        Item item = getItemDropped(state, rand, fortune);
        for (int i = 0; i < count; i++) drops.add(new ItemStack(item, 1, damageDropped(state)));
    }

    @Override protected boolean canSilkHarvest() { return true; }

    @Override public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune) {
        Random rand = world instanceof World ? ((World) world).rand : new Random();
        return 2 + rand.nextInt(4);
    }
}
