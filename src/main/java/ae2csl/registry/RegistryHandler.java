/*
 * AE2 Crystal Science - Registry Handler
 */
package ae2csl.registry;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.init.AE2CSBlockEntities;
import ae2csl.init.AE2CSBlocks;
import ae2csl.init.AE2CSItems;
import ae2csl.init.AE2CSSounds;
import ae2.recipes.AERecipeTypes;
import ae2.recipes.handlers.InscriberProcessType;
import ae2.recipes.entropy.EntropyMode;
import ae2.recipes.entropy.EntropyRecipe;
import ae2.recipes.handlers.ChargerRecipe;
import ae2.recipes.handlers.InscriberRecipe;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

public class RegistryHandler {

    public static void preInit() {
        AE2CrystalScienceLegacy.LOGGER.info("Registering blocks, items, and tile entities...");
        AE2CSBlockEntities.register();
        MinecraftForge.EVENT_BUS.register(AE2CSBlocks.class);
        MinecraftForge.EVENT_BUS.register(AE2CSItems.class);
        MinecraftForge.EVENT_BUS.register(AE2CSSounds.class);
    }

    public static void init() {
        // Regular ore → certus quartz crystals (recipe uses oreCertusQuartz)
        OreDictionary.registerOre("oreCertusQuartz", new ItemStack(AE2CSBlocks.CERTUS_QUARTZ_ORE));
        // Charged ore → charged certus quartz crystals (recipe uses direct item match)
        OreDictionary.registerOre("oreChargedCertusQuartz", new ItemStack(AE2CSBlocks.CHARGED_CERTUS_QUARTZ_ORE));

        // Wooden gear → gearWood (used in quartz_grindstone & simple_pattern_provider recipes)
        OreDictionary.registerOre("gearWood", new ItemStack(Item.getByNameOrId("ae2csl:wooden_gear")));

        // Circuit etcher recipe ore dicts — map 1.21.1 convention tags to available items
        OreDictionary.registerOre("blockSilicon", new ItemStack(AE2CSBlocks.SILICON_BLOCK));
        OreDictionary.registerOre("blockCertusQuartz", new ItemStack(Item.getByNameOrId("ae2:quartz_block")));
        OreDictionary.registerOre("blockSkySteel", new ItemStack(Item.getByNameOrId("ae2:sky_stone_block")));
        // storage_blocks_entro → already registered as "blockEntro" by AE2, map our key too
        OreDictionary.registerOre("storage_blocks_entro", new ItemStack(Item.getByNameOrId("ae2:entro_block")));
        // quantum_alloy → use AE2 quantum ring coil (nearest equivalent)
        OreDictionary.registerOre("storage_blocks_quantum_alloy", new ItemStack(Item.getByNameOrId("ae2:quantum_ring")));
        // pure crystal blocks → use purified crystals
        OreDictionary.registerOre("storage_blocks_pure_crystal_meteor_crystal_block",
            new ItemStack(Item.getByNameOrId("ae2csl:purified_meteor_crystal")));
        OreDictionary.registerOre("storage_blocks_pure_crystal_resonating_crystal_block",
            new ItemStack(Item.getByNameOrId("ae2csl:purified_resonating_crystal")));
        // ender quartz（对齐源码 tag c:gems/ender_quartz = purified_ender_quartz）
        OreDictionary.registerOre("gems_ender_quartz",
            new ItemStack(Item.getByNameOrId("ae2csl:purified_ender_quartz")));
        // ender_quartz_block（对齐源码 tag c:storage_blocks/pure_crystal/ender_quartz_block = ae2cs:ender_quartz_block）
        OreDictionary.registerOre("storage_blocks_pure_crystal_ender_quartz_block",
            new ItemStack(AE2CSBlocks.ENDER_QUARTZ_BLOCK));
        // resonating crystal gem（聚合机 chipped/damaged_budding_quartz 等配方用）
        OreDictionary.registerOre("crystalResonating",
            new ItemStack(Item.getByNameOrId("ae2csl:purified_resonating_crystal")));
        // sky_stone_crystal = 纯陨石水晶（源码 tag c:gems/sky_stone_crystal = ae2cs:pure_crystal/meteor_crystal）
        OreDictionary.registerOre("gems_sky_stone_crystal",
            new ItemStack(Item.getByNameOrId("ae2csl:purified_meteor_crystal")));

        // Inscriber press types for blank print press recipes
        OreDictionary.registerOre("inscriberPress", new ItemStack(Item.getByNameOrId("ae2:logic_processor_press")));
        OreDictionary.registerOre("inscriberPress", new ItemStack(Item.getByNameOrId("ae2:calculation_processor_press")));
        OreDictionary.registerOre("inscriberPress", new ItemStack(Item.getByNameOrId("ae2:engineering_processor_press")));
        OreDictionary.registerOre("inscriberPress", new ItemStack(Item.getByNameOrId("ae2:silicon_press")));
        OreDictionary.registerOre("inscriberPress", new ItemStack(Item.getByNameOrId("ae2:name_press")));

        // Resonating dust ore dict — used by resonating_print_press crafting recipe
        OreDictionary.registerOre("dustResonating",
            new ItemStack(Item.getByNameOrId("ae2csl:resonating_crystal_dust")));

        // Gem resonating — used by inscriber recipes
        OreDictionary.registerOre("gemResonating",
            new ItemStack(Item.getByNameOrId("ae2csl:purified_resonating_crystal")));

        // Crystal ore dicts for pulverizer recipes — map to our purified crystals
        OreDictionary.registerOre("crystalIrradiated",
            new ItemStack(Item.getByNameOrId("ae2csl:purified_irradiated_crystal")));
        OreDictionary.registerOre("crystalQuantum",
            new ItemStack(Item.getByNameOrId("ae2csl:purified_quantum_crystal")));
        OreDictionary.registerOre("crystalRedstone",
            new ItemStack(Item.getByNameOrId("ae2csl:purified_redstone_crystal")));
        OreDictionary.registerOre("crystalEmber",
            new ItemStack(Item.getByNameOrId("ae2csl:purified_ember_crystal")));
        OreDictionary.registerOre("dustRedstoneCrystal",
            new ItemStack(Item.getByNameOrId("ae2csl:redstone_crystal_dust")));

        // Register project-wide category names, then guarantee that every AE2CS item has
        // its own stable ore dictionary name as well.
        registerProjectOreCategories();
        registerProjectOreNames();

        AE2CrystalScienceLegacy.LOGGER.info("Registry initialization complete");

        // ---- Inscriber recipes ----
        registerInscriberRecipes();
        registerChargerRecipes();
        registerEntropyRecipes();

        // ---- Smelting recipes ----
        registerSmeltingRecipes();
    }

    private static void registerProjectOreCategories() {
        registerOreForItems("crystalPure",
            "purified_certus_quartz_crystal", "purified_fluix_crystal", "purified_nether_quartz_crystal",
            "purified_ender_quartz", "purified_meteor_crystal", "purified_resonating_crystal",
            "purified_entro_crystal", "purified_redstone_crystal", "purified_quantum_crystal",
            "purified_rose_quartz", "purified_irradiated_crystal", "purified_ember_crystal",
            "purified_link_crystal", "purified_overload_crystal", "purified_data_crystal",
            "purified_energized_fluix_crystal", "purified_energized_certus_quartz_crystal");
        registerOreForItems("seedCrystal",
            "certus_quartz_seed", "fluix_crystal_seed", "nether_quartz_seed", "ender_quartz_seed",
            "meteor_seed", "resonating_seed", "entro_crystal_seed", "redstone_crystal_seed",
            "quantum_crystal_seed", "rose_quartz_seed", "irradiated_seed", "ember_seed",
            "link_crystal_seed", "overload_crystal_seed", "data_crystal_seed",
            "energized_fluix_crystal_seed", "energized_certus_quartz_seed");
        registerOreForItems("dustAE2CS", "nether_quartz_dust", "resonating_crystal_dust",
            "redstone_crystal_dust", "quantum_crystal_dust", "irradiated_crystal_dust", "link_crystal_dust");

        registerOreForItems("toolSword", "ender_sword", "meteor_sword", "resonating_sword");
        registerOreForItems("toolPickaxe", "ender_pickaxe", "meteor_pickaxe", "resonating_pickaxe");
        registerOreForItems("toolAxe", "ender_axe", "meteor_axe", "resonating_axe");
        registerOreForItems("toolShovel", "ender_shovel", "meteor_shovel", "resonating_shovel");
        registerOreForItems("toolHoe", "ender_hoe", "meteor_hoe", "resonating_hoe");

        registerOreForItems("upgradeAE2CS", "ender_interface_upgrade", "extended_ender_interface_upgrade",
            "extended_integrated_interface_upgrade", "extended_resonating_pattern_provider_upgrade",
            "integrated_interface_upgrade", "meteor_pattern_provider_upgrade", "pattern_provider_upgrade",
            "resonating_pattern_provider_upgrade", "crystal_growth_card", "overload_card");
        registerOreForItems("linkingToolAE2CS", "ender_linker", "mirror_linker", "resonating_linker",
            "resonating_memory_card", "resonating_pattern_converter", "wireless_resonant_terminal");
        registerOreForItems("patternAE2CS", "resonating_pattern");
        registerOreForItems("circuitPrintAE2CS", "simple_circuit_print", "resonating_circuit_print");
        registerOreForItems("pressAE2CS", "blank_print_press", "ender_blank_print_press", "resonating_print_press");
        registerOreForItems("inscriberPress", "resonating_print_press");
        registerOreForItems("processorAE2CS", "simple_processor", "resonating_processor");
        registerOreForItems("foodFlour", "flour");

        registerOreForItems("partAE2CS", "resonating_pattern_provider_part", "simple_pattern_provider_part",
            "mirror_pattern_provider_part", "meteorite_pattern_provider_part", "ender_interface_part",
            "integrated_interface_part", "quartz_oscillator_clock_part", "resonant_template_coding_terminal_part",
            "extended_ender_interface_part", "extended_integrated_interface_part",
            "extended_resonating_pattern_provider_part", "integrated_interface", "mirror_pattern_provider",
            "quartz_oscillator_clock", "resonating_pattern_provider", "simple_pattern_provider",
            "meteorite_pattern_provider", "ender_interface", "extended_ender_interface",
            "extended_integrated_interface", "extended_resonating_pattern_provider");
        registerOreForItems("machineAE2CS", "crystal_growth_chamber", "crystal_pulverizer", "circuit_etcher",
            "quartz_grindstone", "crystal_vibration_chamber", "crystal_aggregator",
            "entropy_variation_chamber", "ender_broadcaster", "ender_emitter");
        registerOreForItems("blockPureCrystal", "rose_quartz_block", "quantum_crystal_block",
            "redstone_crystal_block", "resonating_crystal_block", "meteor_crystal_block",
            "ender_quartz_block", "crystal_grid_block", "irradiated_crystal_block",
            "charged_certus_quartz_block", "charged_overload_crystal_block");
        registerOreForItems("oreCertusQuartz", "charged_certus_quartz_ore");
    }

    private static void registerOreForItems(String oreName, String... itemNames) {
        for (String itemName : itemNames) {
            Item item = Item.getByNameOrId(Tags.MOD_ID + ":" + itemName);
            if (item == null) {
                AE2CrystalScienceLegacy.LOGGER.warn("Cannot register ore dictionary name {}: missing item {}:{}",
                    oreName, Tags.MOD_ID, itemName);
                continue;
            }
            OreDictionary.registerOre(oreName, new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE));
        }
    }

    /**
     * Registers one private ore dictionary name for every item owned by this mod.
     *
     * <p>The names use {@code itemAE2CS...} or {@code blockAE2CS...}; for example,
     * {@code ae2csl:ender_sword} becomes {@code itemAE2CSEnderSword}. Wildcard metadata
     * keeps the entry valid for damaged tools and metadata-based item states. These private
     * names deliberately do not make unrelated materials interchangeable.</p>
     */
    private static void registerProjectOreNames() {
        int registered = 0;
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            ResourceLocation id = item.getRegistryName();
            if (id == null || !Tags.MOD_ID.equals(id.getNamespace())) {
                continue;
            }

            String prefix = item instanceof ItemBlock ? "blockAE2CS" : "itemAE2CS";
            OreDictionary.registerOre(prefix + toUpperCamel(id.getPath()),
                new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE));
            registered++;
        }
        AE2CrystalScienceLegacy.LOGGER.info("Registered private ore dictionary names for {} AE2CS items", registered);
    }

    private static String toUpperCamel(String path) {
        StringBuilder result = new StringBuilder(path.length());
        boolean capitalize = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_' || c == '-' || c == '/') {
                capitalize = true;
            } else {
                result.append(capitalize ? Character.toUpperCase(c) : c);
                capitalize = false;
            }
        }
        return result.toString();
    }

    private static void registerSmeltingRecipes() {
        // flour -> bread (1.12.2 has no JSON smelting recipes, register in code)
        FurnaceRecipes.instance().addSmeltingRecipe(
            new ItemStack(Item.getByNameOrId("ae2csl:flour")),
            new ItemStack(Item.getByNameOrId("minecraft:bread")),
            0.1f);
        AE2CrystalScienceLegacy.LOGGER.info("Registered 1 smelting recipe");
    }

    private static void registerInscriberRecipes() {
        // resonating_circuit_print: purified_resonating_crystal + resonating_print_press -> circuit print
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(
            Ingredient.fromStacks(new ItemStack(Item.getByNameOrId("ae2csl:purified_resonating_crystal"))),
            new ItemStack(Item.getByNameOrId("ae2csl:resonating_circuit_print")),
            Ingredient.fromStacks(new ItemStack(Item.getByNameOrId("ae2csl:resonating_print_press"))),
            Ingredient.EMPTY,
            InscriberProcessType.INSCRIBE
        ));

        // resonating_processor: resonating_circuit_print + sky_dust + printed_silicon -> processor
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(
            Ingredient.fromStacks(new ItemStack(Item.getByNameOrId("ae2:sky_dust"))),
            new ItemStack(Item.getByNameOrId("ae2csl:resonating_processor")),
            Ingredient.fromStacks(new ItemStack(Item.getByNameOrId("ae2csl:resonating_circuit_print"))),
            Ingredient.fromStacks(new ItemStack(Item.getByNameOrId("ae2:printed_silicon"))),
            InscriberProcessType.PRESS
        ));

        // simple_circuit_print: any quartz gem -> circuit print
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(
            Ingredient.fromStacks(OreDictionary.getOres("gemQuartz").toArray(new ItemStack[0])),
            new ItemStack(Item.getByNameOrId("ae2csl:simple_circuit_print")),
            Ingredient.EMPTY,
            Ingredient.EMPTY,
            InscriberProcessType.PRESS
        ));

        // simple_processor: simple_circuit_print + redstone dust + printed_silicon -> processor
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(
            Ingredient.fromStacks(OreDictionary.getOres("dustRedstone").toArray(new ItemStack[0])),
            new ItemStack(Item.getByNameOrId("ae2csl:simple_processor")),
            Ingredient.fromStacks(new ItemStack(Item.getByNameOrId("ae2csl:simple_circuit_print"))),
            Ingredient.fromStacks(new ItemStack(Item.getByNameOrId("ae2:printed_silicon"))),
            InscriberProcessType.PRESS
        ));

        registerQuantumInscriberRecipes();

        AE2CrystalScienceLegacy.LOGGER.info("Registered 6 inscriber recipes");
    }

    private static void registerEntropyRecipes() {
        int registered = 0;
        Block quartzBlock = Block.getBlockFromName("ae2:quartz_block");
        if (quartzBlock != null && AE2CSBlocks.CHARGED_CERTUS_QUARTZ_BLOCK != null) {
            AERecipeTypes.ENTROPY.register(new EntropyRecipe(
                EntropyMode.HEAT,
                new EntropyRecipe.Input(
                    java.util.Optional.of(new EntropyRecipe.BlockInput(quartzBlock, java.util.Collections.emptyMap())),
                    java.util.Optional.empty()),
                new EntropyRecipe.Output(
                    java.util.Optional.of(new EntropyRecipe.BlockOutput(
                        AE2CSBlocks.CHARGED_CERTUS_QUARTZ_BLOCK, -1, false, java.util.Collections.emptyMap())),
                    java.util.Optional.empty(),
                    java.util.Collections.emptyList())
            ));
            registered++;
        }

        Block overloadBlock = Block.getBlockFromName("ae2lt:overload_crystal_block");
        if (overloadBlock != null && AE2CSBlocks.CHARGED_OVERLOAD_CRYSTAL_BLOCK != null) {
            AERecipeTypes.ENTROPY.register(new EntropyRecipe(
                EntropyMode.HEAT,
                new EntropyRecipe.Input(
                    java.util.Optional.of(new EntropyRecipe.BlockInput(overloadBlock, java.util.Collections.emptyMap())),
                    java.util.Optional.empty()),
                new EntropyRecipe.Output(
                    java.util.Optional.of(new EntropyRecipe.BlockOutput(
                        AE2CSBlocks.CHARGED_OVERLOAD_CRYSTAL_BLOCK, -1, false, java.util.Collections.emptyMap())),
                    java.util.Optional.empty(),
                    java.util.Collections.emptyList())
            ));
            registered++;
        }

        if (registered > 0) {
            AE2CrystalScienceLegacy.LOGGER.info("Registered {} entropy recipes", registered);
        }
    }

    private static void registerChargerRecipes() {
        Item pureRedstone = Item.getByNameOrId("ae2csl:purified_redstone_crystal");
        Item redstoneCrystal = Item.getByNameOrId("appflux:redstone_crystal");
        if (pureRedstone == null || redstoneCrystal == null) {
            return;
        }
        // Upstream AECSCompatAFRecipeProvider.chargedRecipe(PURE_REDSTONE_CRYSTAL, REDSTONE_CRYSTAL)
        AERecipeTypes.CHARGER.register(new ChargerRecipe(
            Ingredient.fromStacks(new ItemStack(pureRedstone)),
            new ItemStack(redstoneCrystal)
        ));
        AE2CrystalScienceLegacy.LOGGER.info("Registered 1 charger recipe");
    }

    private static void registerQuantumInscriberRecipes() {
        Item quantumBlock = Item.getByNameOrId("ae2additions:quantum_alloy_block");
        Item quantumPress = Item.getByNameOrId("ae2additions:quantum_processor_press");
        Item printedBoard = Item.getByNameOrId("ae2additions:printed_quantum_processor");
        Item processor = Item.getByNameOrId("ae2additions:quantum_processor");
        if (quantumBlock == null || quantumPress == null || printedBoard == null || processor == null) {
            return;
        }

        // printed_quantum_processor: quantum alloy block + quantum processor press
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(
            Ingredient.fromStacks(new ItemStack(quantumBlock)),
            new ItemStack(printedBoard),
            Ingredient.fromStacks(new ItemStack(quantumPress)),
            Ingredient.EMPTY,
            InscriberProcessType.INSCRIBE
        ));

        // quantum_processor: printed board + redstone + printed silicon
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(
            Ingredient.fromStacks(OreDictionary.getOres("dustRedstone").toArray(new ItemStack[0])),
            new ItemStack(processor),
            Ingredient.fromStacks(new ItemStack(printedBoard)),
            Ingredient.fromStacks(new ItemStack(Item.getByNameOrId("ae2:printed_silicon"))),
            InscriberProcessType.PRESS
        ));
    }
}
