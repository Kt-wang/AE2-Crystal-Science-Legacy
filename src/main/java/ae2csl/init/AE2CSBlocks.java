/*
 * AE2 Crystal Science - Block Registration
 * Ported from 1.21.1: common/init/AE2CSBlocks.java
 */
package ae2csl.init;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.block.aggregator.CrystalAggregatorBlock;
import ae2csl.block.crystal.CrystalGrowthChamberBlock;
import ae2csl.block.drive.ResonatingDriveBlock;
import ae2csl.block.ender.EnderBroadcasterBlock;
import ae2csl.block.ender.EnderEmitterBlock;
import ae2csl.block.entropy.EntropyVariationReactionChamberBlock;
import ae2csl.block.etcher.CircuitEtcherBlock;
import ae2csl.block.grindstone.QuartzGrindstoneBlock;
import ae2csl.block.integrated.IntegratedInterfaceBlock;
import ae2csl.block.mirror.MirrorPatternProviderBlock;
import ae2csl.block.ore.CertusQuartzOreBlock;
import ae2csl.block.ore.ChargedCertusQuartzOreBlock;
import ae2csl.block.oscillator.QuartzOscillatorClockBlock;
import ae2csl.block.pulverizer.CrystalPulverizerBlock;
import ae2csl.block.resonating.ResonatingPatternProviderBlock;
import ae2csl.block.vibration.CrystalVibrationChamberBlock;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Block registration. Uses @SubscribeEvent on RegistryEvent.Register<Block>. */
public class AE2CSBlocks {

    public static CrystalGrowthChamberBlock CRYSTAL_GROWTH_CHAMBER;
    public static CertusQuartzOreBlock CERTUS_QUARTZ_ORE;
    public static ChargedCertusQuartzOreBlock CHARGED_CERTUS_QUARTZ_ORE;
    public static CrystalPulverizerBlock CRYSTAL_PULVERIZER;
    public static CircuitEtcherBlock CIRCUIT_ETCHER;
    public static QuartzGrindstoneBlock QUARTZ_GRINDSTONE;
    public static CrystalVibrationChamberBlock CRYSTAL_VIBRATION_CHAMBER;
    public static CrystalAggregatorBlock CRYSTAL_AGGREGATOR;
    public static EntropyVariationReactionChamberBlock ENTROPY_VARIATION_CHAMBER;
    public static EnderBroadcasterBlock ENDER_BROADCASTER;
    public static EnderEmitterBlock ENDER_EMITTER;
    public static IntegratedInterfaceBlock INTEGRATED_INTERFACE;
    public static MirrorPatternProviderBlock MIRROR_PATTERN_PROVIDER;
    public static QuartzOscillatorClockBlock QUARTZ_OSCILLATOR_CLOCK;
    public static ResonatingPatternProviderBlock RESONATING_PATTERN_PROVIDER;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        CRYSTAL_GROWTH_CHAMBER = reg(event, new CrystalGrowthChamberBlock(), "crystal_growth_chamber");
        CERTUS_QUARTZ_ORE = reg(event, new CertusQuartzOreBlock(), "certus_quartz_ore");
        CHARGED_CERTUS_QUARTZ_ORE = reg(event, new ChargedCertusQuartzOreBlock(), "charged_certus_quartz_ore");
        CRYSTAL_PULVERIZER = reg(event, new CrystalPulverizerBlock(), "crystal_pulverizer");
        CIRCUIT_ETCHER = reg(event, new CircuitEtcherBlock(), "circuit_etcher");
        QUARTZ_GRINDSTONE = reg(event, new QuartzGrindstoneBlock(), "quartz_grindstone");
        CRYSTAL_VIBRATION_CHAMBER = reg(event, new CrystalVibrationChamberBlock(), "crystal_vibration_chamber");
        CRYSTAL_AGGREGATOR = reg(event, new CrystalAggregatorBlock(), "crystal_aggregator");
        ENTROPY_VARIATION_CHAMBER = reg(event, new EntropyVariationReactionChamberBlock(), "entropy_variation_chamber");
        ENDER_BROADCASTER = reg(event, new EnderBroadcasterBlock(), "ender_broadcaster");
        ENDER_EMITTER = reg(event, new EnderEmitterBlock(), "ender_emitter");
        INTEGRATED_INTERFACE = reg(event, new IntegratedInterfaceBlock(), "integrated_interface");
        MIRROR_PATTERN_PROVIDER = reg(event, new MirrorPatternProviderBlock(), "mirror_pattern_provider");
        QUARTZ_OSCILLATOR_CLOCK = reg(event, new QuartzOscillatorClockBlock(), "quartz_oscillator_clock");
        RESONATING_PATTERN_PROVIDER = reg(event, new ResonatingPatternProviderBlock(), "resonating_pattern_provider");
        AE2CrystalScienceLegacy.LOGGER.info("Registered {} blocks", 117);
    }

    // Storage blocks + extended block forms
    public static Block SILICON_BLOCK;
    public static Block ROSE_QUARTZ_BLOCK;
    public static Block QUANTUM_CRYSTAL_BLOCK;
    public static Block REDSTONE_CRYSTAL_BLOCK;
    public static Block RESONATING_CRYSTAL_BLOCK;
    public static Block METEOR_CRYSTAL_BLOCK;
    public static Block ENDER_QUARTZ_BLOCK;
    public static Block CRYSTAL_GRID_BLOCK;
    public static Block IRRADIATED_CRYSTAL_BLOCK;
    public static Block CHARGED_CERTUS_QUARTZ_BLOCK;
    public static Block CHARGED_OVERLOAD_CRYSTAL_BLOCK;
    public static Block SIMPLE_PATTERN_PROVIDER_BLOCK;
    public static Block METEORITE_PATTERN_PROVIDER_BLOCK;
    public static Block ENDER_INTERFACE_BLOCK;
    public static Block EX_ENDER_INTERFACE_BLOCK;
    public static Block EX_INTEGRATED_INTERFACE_BLOCK;
    public static Block EX_RESONATING_PATTERN_PROVIDER_BLOCK;
    public static Block RESONATING_DRIVE_BLOCK;

    @SubscribeEvent
    public static void registerExtraBlocks(RegistryEvent.Register<Block> event) {
        SILICON_BLOCK = reg(event, new Block(Material.ROCK).setHardness(3.0F).setResistance(5.0F), "silicon_block");
        ROSE_QUARTZ_BLOCK = reg(event, new Block(Material.ROCK).setHardness(3.0F).setResistance(5.0F), "rose_quartz_block");
        QUANTUM_CRYSTAL_BLOCK = reg(event, new Block(Material.ROCK).setHardness(3.0F).setResistance(5.0F), "quantum_crystal_block");
        REDSTONE_CRYSTAL_BLOCK = reg(event, new Block(Material.ROCK).setHardness(3.0F).setResistance(5.0F), "redstone_crystal_block");
        RESONATING_CRYSTAL_BLOCK = reg(event, new Block(Material.ROCK).setHardness(3.0F).setResistance(5.0F), "resonating_crystal_block");
        METEOR_CRYSTAL_BLOCK = reg(event, new Block(Material.ROCK).setHardness(3.0F).setResistance(5.0F), "meteor_crystal_block");
        ENDER_QUARTZ_BLOCK = reg(event, new Block(Material.ROCK).setHardness(3.0F).setResistance(5.0F), "ender_quartz_block");
        CRYSTAL_GRID_BLOCK = reg(event, new Block(Material.ROCK).setHardness(3.0F).setResistance(5.0F), "crystal_grid_block");
        IRRADIATED_CRYSTAL_BLOCK = reg(event, new Block(Material.ROCK).setHardness(3.0F).setResistance(5.0F), "irradiated_crystal_block");
        CHARGED_CERTUS_QUARTZ_BLOCK = reg(event, new Block(Material.ROCK).setHardness(3.0F).setResistance(5.0F), "charged_certus_quartz_block");
        CHARGED_OVERLOAD_CRYSTAL_BLOCK = reg(event, new Block(Material.ROCK).setHardness(3.0F).setResistance(5.0F), "charged_overload_crystal_block");
        SIMPLE_PATTERN_PROVIDER_BLOCK = reg(event, new ae2csl.block.mirror.SimplePatternProviderBlock(), "simple_pattern_provider");
        METEORITE_PATTERN_PROVIDER_BLOCK = reg(event, new ae2csl.block.mirror.MeteoritePatternProviderBlock(), "meteorite_pattern_provider");
        ENDER_INTERFACE_BLOCK = reg(event, new ae2csl.block.ender.EnderInterfaceBlock(), "ender_interface");
        EX_ENDER_INTERFACE_BLOCK = reg(event, new ae2csl.block.ender.ExtendedEnderInterfaceBlock(), "extended_ender_interface");
        EX_INTEGRATED_INTERFACE_BLOCK = reg(event, new ae2csl.block.integrated.ExtendedIntegratedInterfaceBlock(), "extended_integrated_interface");
        EX_RESONATING_PATTERN_PROVIDER_BLOCK = reg(event, new ae2csl.block.resonating.ExtendedResonatingPatternProviderBlock(), "extended_resonating_pattern_provider");
        // 谐振ME驱动器（占位：无 TE/GUI，先让方块进世界）
        RESONATING_DRIVE_BLOCK = reg(event, new ResonatingDriveBlock(), "resonating_drive");
    }

    private static <T extends Block> T reg(RegistryEvent.Register<Block> evt, T block, String name) {
        block.setRegistryName(Tags.MOD_ID, name);
        block.setTranslationKey(name);
        block.setCreativeTab(AE2CSCreativeTab.INSTANCE);
        evt.getRegistry().register(block);
        return block;
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        AE2CrystalScienceLegacy.LOGGER.info("Registering item blocks...");
        regItem(event, CRYSTAL_GROWTH_CHAMBER, "crystal_growth_chamber");
        regItem(event, CERTUS_QUARTZ_ORE, "certus_quartz_ore");
        regItem(event, CHARGED_CERTUS_QUARTZ_ORE, "charged_certus_quartz_ore");
        regItem(event, CRYSTAL_PULVERIZER, "crystal_pulverizer");
        regItem(event, CIRCUIT_ETCHER, "circuit_etcher");
        regItem(event, QUARTZ_GRINDSTONE, "quartz_grindstone");
        regItem(event, CRYSTAL_VIBRATION_CHAMBER, "crystal_vibration_chamber");
        regItem(event, CRYSTAL_AGGREGATOR, "crystal_aggregator");
        regItem(event, ENTROPY_VARIATION_CHAMBER, "entropy_variation_chamber");
        regItem(event, ENDER_BROADCASTER, "ender_broadcaster");
        regItem(event, ENDER_EMITTER, "ender_emitter");
        regItem(event, INTEGRATED_INTERFACE, "integrated_interface");
        regItem(event, MIRROR_PATTERN_PROVIDER, "mirror_pattern_provider");
        regItem(event, QUARTZ_OSCILLATOR_CLOCK, "quartz_oscillator_clock");
        // 谐振供应器用自定义方块物品（手持可标记目标，对齐源码 ResonatingPatternProviderBlockItem）
        regCustomItem(event, new ae2csl.item.pattern.ResonatingPatternProviderBlockItem(RESONATING_PATTERN_PROVIDER),
            "resonating_pattern_provider");
        regItem(event, SILICON_BLOCK, "silicon_block");
        regItem(event, ROSE_QUARTZ_BLOCK, "rose_quartz_block");
        regItem(event, QUANTUM_CRYSTAL_BLOCK, "quantum_crystal_block");
        regItem(event, REDSTONE_CRYSTAL_BLOCK, "redstone_crystal_block");
        regItem(event, RESONATING_CRYSTAL_BLOCK, "resonating_crystal_block");
        regItem(event, METEOR_CRYSTAL_BLOCK, "meteor_crystal_block");
        regItem(event, ENDER_QUARTZ_BLOCK, "ender_quartz_block");
        regItem(event, CRYSTAL_GRID_BLOCK, "crystal_grid_block");
        regItem(event, IRRADIATED_CRYSTAL_BLOCK, "irradiated_crystal_block");
        regItem(event, CHARGED_CERTUS_QUARTZ_BLOCK, "charged_certus_quartz_block");
        regItem(event, CHARGED_OVERLOAD_CRYSTAL_BLOCK, "charged_overload_crystal_block");
        regItem(event, SIMPLE_PATTERN_PROVIDER_BLOCK, "simple_pattern_provider");
        regItem(event, METEORITE_PATTERN_PROVIDER_BLOCK, "meteorite_pattern_provider");
        regItem(event, ENDER_INTERFACE_BLOCK, "ender_interface");
        regItem(event, EX_ENDER_INTERFACE_BLOCK, "extended_ender_interface");
        regItem(event, EX_INTEGRATED_INTERFACE_BLOCK, "extended_integrated_interface");
        regItem(event, EX_RESONATING_PATTERN_PROVIDER_BLOCK, "extended_resonating_pattern_provider");
        regItem(event, RESONATING_DRIVE_BLOCK, "resonating_drive");
        AE2CrystalScienceLegacy.LOGGER.info("Registered {} item blocks", 34);
    }

    private static void regItem(RegistryEvent.Register<Item> event, Block block, String name) {
        if (block == null) { AE2CrystalScienceLegacy.LOGGER.error("Block is null for {}", name); return; }
        ItemBlock item = new ItemBlock(block);
        item.setRegistryName(Tags.MOD_ID, name);
        item.setCreativeTab(AE2CSCreativeTab.INSTANCE);
        event.getRegistry().register(item);
    }

    private static void regCustomItem(RegistryEvent.Register<Item> event, Item item, String name) {
        item.setRegistryName(Tags.MOD_ID, name);
        item.setCreativeTab(AE2CSCreativeTab.INSTANCE);
        event.getRegistry().register(item);
    }
}
