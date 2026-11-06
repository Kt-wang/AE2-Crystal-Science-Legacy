/*
 * AE2 Crystal Science - Item Registration
 * Ported from 1.21.1: common/init/AE2CSItems.java
 *
 * Follows source structure:
 *   17 PureCrystalItems (fuel + tool material)
 *   17 CrystalSeedItems (each grows into corresponding PureCrystal)
 *   6 dust items
 *   15 tools (3 sets × 5)
 *   + upgrades, linkers, parts, processors, misc
 */
package ae2csl.init;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.item.crystal.CrystalSeedItem;
import ae2csl.item.crystal.PureCrystalItem;
import ae2csl.item.linker.*;
import ae2csl.item.pattern.*;
import ae2csl.item.tools.ender.*;
import ae2csl.item.tools.meteor.*;
import ae2csl.item.tools.resonating.*;
import ae2csl.item.upgrades.*;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AE2CSItems {

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {

        // ==================== Pure Crystals (fuel + tool material) ====================
        // 17 types, each with different energy output per tick and burn time.
        // Values match PURE_* constants in 1.21.1 AE2CSItems.java.

        reg(event, new PureCrystalItem("purified_certus_quartz_crystal", 500, 600), "purified_certus_quartz_crystal");
        reg(event, new PureCrystalItem("purified_fluix_crystal", 1500, 1200), "purified_fluix_crystal");
        reg(event, new PureCrystalItem("purified_nether_quartz_crystal", 800, 900), "purified_nether_quartz_crystal");
        reg(event, new PureCrystalItem("purified_ender_quartz", 900, 800), "purified_ender_quartz");
        reg(event, new PureCrystalItem("purified_meteor_crystal", 1200, 1500), "purified_meteor_crystal");
        reg(event, new PureCrystalItem("purified_resonating_crystal", 2500, 2400), "purified_resonating_crystal");
        reg(event, new PureCrystalItem("purified_entro_crystal", 2000, 2400), "purified_entro_crystal");
        reg(event, new PureCrystalItem("purified_redstone_crystal", 1800, 2000), "purified_redstone_crystal");
        reg(event, new PureCrystalItem("purified_quantum_crystal", 2320, 3000), "purified_quantum_crystal");
        reg(event, new PureCrystalItem("purified_rose_quartz", 2000, 1800), "purified_rose_quartz");
        reg(event, new PureCrystalItem("purified_irradiated_crystal", 2900, 2400), "purified_irradiated_crystal");
        reg(event, new PureCrystalItem("purified_ember_crystal", 2000, 3600), "purified_ember_crystal");
        reg(event, new PureCrystalItem("purified_link_crystal", 1800, 1800), "purified_link_crystal");
        reg(event, new PureCrystalItem("purified_overload_crystal", 1000, 600), "purified_overload_crystal");
        reg(event, new PureCrystalItem("purified_data_crystal", 1000, 600), "purified_data_crystal");
        reg(event, new PureCrystalItem("purified_energized_fluix_crystal", 1000, 600), "purified_energized_fluix_crystal");
        reg(event, new PureCrystalItem("purified_energized_certus_quartz_crystal", 1000, 600), "purified_energized_certus_quartz_crystal");

        // ==================== Crystal Seeds ====================
        // Each seed grows into its corresponding Pure Crystal.
        // Uses lazy registry-name resolution (target pure crystal may register later).

        reg(event, new CrystalSeedItem("ae2csl:purified_certus_quartz_crystal", 600), "certus_quartz_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_fluix_crystal", 600), "fluix_crystal_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_nether_quartz_crystal", 600), "nether_quartz_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_ender_quartz", 600), "ender_quartz_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_meteor_crystal", 600), "meteor_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_resonating_crystal", 600), "resonating_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_entro_crystal", 600), "entro_crystal_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_redstone_crystal", 600), "redstone_crystal_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_quantum_crystal", 600), "quantum_crystal_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_rose_quartz", 600), "rose_quartz_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_irradiated_crystal", 600), "irradiated_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_ember_crystal", 600), "ember_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_link_crystal", 600), "link_crystal_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_overload_crystal", 600), "overload_crystal_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_data_crystal", 600), "data_crystal_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_energized_fluix_crystal", 600), "energized_fluix_crystal_seed");
        reg(event, new CrystalSeedItem("ae2csl:purified_energized_certus_quartz_crystal", 600), "energized_certus_quartz_seed");

        // ==================== Dusts ====================
        reg(event, new Item(), "nether_quartz_dust");
        reg(event, new Item(), "resonating_crystal_dust");
        reg(event, new Item(), "redstone_crystal_dust");
        reg(event, new Item(), "quantum_crystal_dust");
        reg(event, new Item(), "irradiated_crystal_dust");
        reg(event, new Item(), "link_crystal_dust");

        // ==================== Tools - Ender ====================
        reg(event, new EnderSwordItem(), "ender_sword");
        reg(event, new EnderPickaxeItem(), "ender_pickaxe");
        reg(event, new EnderAxeItem(), "ender_axe");
        reg(event, new EnderShovelItem(), "ender_shovel");
        reg(event, new EnderHoeItem(), "ender_hoe");

        // ==================== Tools - Meteor ====================
        reg(event, new MeteorSwordItem(), "meteor_sword");
        reg(event, new MeteorPickaxeItem(), "meteor_pickaxe");
        reg(event, new MeteorAxeItem(), "meteor_axe");
        reg(event, new MeteorShovelItem(), "meteor_shovel");
        reg(event, new MeteorHoeItem(), "meteor_hoe");

        // ==================== Tools - Resonating ====================
        reg(event, new ResonatingSwordItem(), "resonating_sword");
        reg(event, new ResonatingPickaxeItem(), "resonating_pickaxe");
        reg(event, new ResonatingAxeItem(), "resonating_axe");
        reg(event, new ResonatingShovelItem(), "resonating_shovel");
        reg(event, new ResonatingHoeItem(), "resonating_hoe");

        // ==================== Upgrades ====================
        reg(event, new EnderInterfaceUpgradeItem(), "ender_interface_upgrade");
        reg(event, new ExtendedEnderInterfaceUpgradeItem(), "extended_ender_interface_upgrade");
        reg(event, new ExtendedIntegratedInterfaceUpgradeItem(), "extended_integrated_interface_upgrade");
        reg(event, new ExtendedResonatingPatternProviderUpgradeItem(), "extended_resonating_pattern_provider_upgrade");
        reg(event, new IntegratedInterfaceUpgradeItem(), "integrated_interface_upgrade");
        reg(event, new MeteorPatternProviderUpgradeItem(), "meteor_pattern_provider_upgrade");
        reg(event, new PatternProviderUpgradeItem(), "pattern_provider_upgrade");
        reg(event, new ResonatingPatternProviderUpgradeItem(), "resonating_pattern_provider_upgrade");

        // ==================== Linkers & Memory Cards ====================
        reg(event, new EnderLinkerItem(), "ender_linker");
        reg(event, new MirrorLinkerItem(), "mirror_linker");
        reg(event, new ResonatingLinkerItem(), "resonating_linker");
        reg(event, new ResonatingMemoryCardItem(), "resonating_memory_card");
        reg(event, new ResonatingPatternConverterItem(), "resonating_pattern_converter");
        reg(event, new WirelessResonantTerminalItem(), "wireless_resonant_terminal");

        // ==================== Pattern Items ====================
        reg(event, new ResonatingPatternItem(), "resonating_pattern");
        reg(event, new ResonatingPatternProviderPartItem(), "resonating_pattern_provider_part");

        // ==================== Processors & Prints ====================
        reg(event, new Item(), "simple_circuit_print");
        reg(event, new Item(), "resonating_circuit_print");
        reg(event, new Item(), "blank_print_press");
        reg(event, new Item(), "ender_blank_print_press");
        reg(event, new Item(), "resonating_print_press");
        reg(event, new Item(), "simple_processor");
        reg(event, new Item(), "resonating_processor");

        // ==================== Misc ====================
        // 升级卡类物品必须用 fork 的 UpgradeCardItem，否则升级槽的 isUpgradeCardItem 校验不过插不进去
        reg(event, new ae2.items.materials.UpgradeCardItem(), "crystal_growth_card");
        reg(event, new ae2.items.materials.UpgradeCardItem(), "overload_card");
        reg(event, new Item(), "flour");
        reg(event, new Item(), "wooden_gear");

        // ==================== Part Items ====================
        reg(event, new SimplePatternProviderPartItem(), "simple_pattern_provider_part");
        reg(event, new MirrorPatternProviderPartItem(), "mirror_pattern_provider_part");
        reg(event, new MeteoritePatternProviderPartItem(), "meteorite_pattern_provider_part");
        reg(event, new EnderInterfacePartItem(), "ender_interface_part");
        reg(event, new IntegratedInterfacePartItem(), "integrated_interface_part");
        reg(event, new QuartzOscillatorClockPartItem(), "quartz_oscillator_clock_part");
        reg(event, new ResonantTemplateCodingTerminalPartItem(), "resonant_template_coding_terminal_part");

        // ==================== Extended Part Items ====================
        reg(event, new ExtendedEnderInterfacePartItem(), "extended_ender_interface_part");
        reg(event, new ExtendedIntegratedInterfacePartItem(), "extended_integrated_interface_part");
        reg(event, new ExtendedResonatingPatternProviderPartItem(), "extended_resonating_pattern_provider_part");

        AE2CrystalScienceLegacy.LOGGER.info("Registered {} items", 92);
    }

    private static void reg(RegistryEvent.Register<Item> event, Item item, String name) {
        item.setRegistryName(Tags.MOD_ID, name);
        item.setTranslationKey(name);
        item.setCreativeTab(AE2CSCreativeTab.INSTANCE);
        event.getRegistry().register(item);
    }
}
