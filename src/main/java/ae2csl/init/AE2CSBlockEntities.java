/*
 * AE2 Crystal Science - Block Entity Registration
 * Ported from 1.21.1: common/init/AE2CSBlockEntities.java
 */
package ae2csl.init;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.tile.aggregator.TileCrystalAggregator;
import ae2csl.tile.crystal.TileCrystalGrowthChamber;
import ae2csl.tile.ender.TileEnderBroadcaster;
import ae2csl.tile.ender.TileEnderEmitter;
import ae2csl.tile.ender.TileEnderInterface;
import ae2csl.tile.ender.TileExtendedEnderInterface;
import ae2csl.tile.entropy.TileEntropyVariationChamber;
import ae2csl.tile.etcher.TileCircuitEtcher;
import ae2csl.tile.grindstone.TileQuartzGrindstone;
import ae2csl.tile.integrated.TileExtendedIntegratedInterface;
import ae2csl.tile.integrated.TileIntegratedInterface;
import ae2csl.tile.mirror.TileMeteoritePatternProvider;
import ae2csl.tile.mirror.TileMirrorPatternProvider;
import ae2csl.tile.mirror.TileSimplePatternProvider;
import ae2csl.tile.oscillator.TileQuartzOscillatorClock;
import ae2csl.tile.pulverizer.TileCrystalPulverizer;
import ae2csl.tile.resonating.TileExtendedResonatingPatternProvider;
import ae2csl.tile.resonating.TileResonatingPatternProvider;
import ae2csl.tile.storage.TileResonatingDrive;
import ae2csl.tile.vibration.TileCrystalVibrationChamber;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class AE2CSBlockEntities {

    public static void register() {
        reg(TileCrystalGrowthChamber.class, "crystal_growth_chamber");
        reg(TileCrystalAggregator.class, "crystal_aggregator");
        reg(TileCircuitEtcher.class, "circuit_etcher");
        reg(TileQuartzGrindstone.class, "quartz_grindstone");
        reg(TileCrystalPulverizer.class, "crystal_pulverizer");
        reg(TileCrystalVibrationChamber.class, "crystal_vibration_chamber");
        reg(TileEnderBroadcaster.class, "ender_broadcaster");
        reg(TileEnderEmitter.class, "ender_emitter");
        reg(TileEnderInterface.class, "ender_interface");
        reg(TileExtendedEnderInterface.class, "extended_ender_interface");
        reg(TileIntegratedInterface.class, "integrated_interface");
        reg(TileExtendedIntegratedInterface.class, "extended_integrated_interface");
        reg(TileMirrorPatternProvider.class, "mirror_pattern_provider");
        reg(TileMeteoritePatternProvider.class, "meteorite_pattern_provider");
        reg(TileSimplePatternProvider.class, "simple_pattern_provider");
        reg(TileQuartzOscillatorClock.class, "quartz_oscillator_clock");
        reg(TileResonatingPatternProvider.class, "resonating_pattern_provider");
        reg(TileExtendedResonatingPatternProvider.class, "extended_resonating_pattern_provider");
        reg(TileEntropyVariationChamber.class, "entropy_variation_chamber");
        reg(TileResonatingDrive.class, "resonating_drive");
        AE2CrystalScienceLegacy.LOGGER.info("Registered {} tile entities", 17);
    }

    private static void reg(Class<? extends net.minecraft.tileentity.TileEntity> cls, String name) {
        GameRegistry.registerTileEntity(cls, Tags.MOD_ID + ":" + name);
    }
}
