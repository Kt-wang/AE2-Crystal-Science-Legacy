/*
 * AE2 Crystal Science - ChargedCertusQuartzOreBlock
 */
package ae2csl.block.ore;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class ChargedCertusQuartzOreBlock extends CertusQuartzOreBlock {

    public ChargedCertusQuartzOreBlock() {
        super();
        setLightLevel(0.5F);
    }

    @Override public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        Item it = Item.getByNameOrId("ae2:charged_certus_quartz_crystal");
        return it != null ? it : Item.getItemFromBlock(this);
    }

    @Override @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (rand.nextInt(4) != 0) return;
        double x = pos.getX() + rand.nextFloat(), y = pos.getY() + rand.nextFloat(), z = pos.getZ() + rand.nextFloat();
        switch (rand.nextInt(6)) {
            case 0: x = pos.getX() - 0.01; break; case 1: x = pos.getX() + 1.01; break;
            case 2: y = pos.getY() - 0.01; break; case 3: y = pos.getY() + 1.01; break;
            case 4: z = pos.getZ() - 0.01; break; case 5: z = pos.getZ() + 1.01; break;
        }
        world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 0.21, 0.61, 1.0);
    }
}
