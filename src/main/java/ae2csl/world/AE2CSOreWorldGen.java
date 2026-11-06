/*
 * AE2 Crystal Science - AE2CSOreWorldGen (1.12.2)
 * Overworld ore generation for Certus Quartz Ore and Charged Certus Quartz Ore.
 * Ported from the 1.21.1 worldgen data:
 *  - certus_quartz_ore:  vein size 4, 18 veins/chunk, y -59..72  (1.12: y 0..72)
 *  - charged_certus_quartz_ore: vein size 4, 2 veins/chunk, y 0..72
 */
package ae2csl.world;

import ae2csl.init.AE2CSBlocks;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

public class AE2CSOreWorldGen implements IWorldGenerator {

    private static final int VEIN_SIZE = 4;
    private static final int CERTUS_VEINS_PER_CHUNK = 18;
    private static final int CHARGED_VEINS_PER_CHUNK = 2;
    private static final int MIN_Y = 1;
    private static final int MAX_Y = 72;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.getDimension() != 0) return; // overworld only

        generateOre(world, random, chunkX, chunkZ, AE2CSBlocks.CERTUS_QUARTZ_ORE.getDefaultState(),
            CERTUS_VEINS_PER_CHUNK);
        generateOre(world, random, chunkX, chunkZ, AE2CSBlocks.CHARGED_CERTUS_QUARTZ_ORE.getDefaultState(),
            CHARGED_VEINS_PER_CHUNK);
    }

    private static void generateOre(World world, Random random, int chunkX, int chunkZ,
                                    net.minecraft.block.state.IBlockState ore, int veinsPerChunk) {
        WorldGenMinable gen = new WorldGenMinable(ore, VEIN_SIZE,
            BlockMatcher.forBlock(Blocks.STONE));
        for (int i = 0; i < veinsPerChunk; i++) {
            int x = chunkX * 16 + random.nextInt(16);
            int y = MIN_Y + random.nextInt(MAX_Y - MIN_Y + 1);
            int z = chunkZ * 16 + random.nextInt(16);
            gen.generate(world, random, new BlockPos(x, y, z));
        }
    }
}
