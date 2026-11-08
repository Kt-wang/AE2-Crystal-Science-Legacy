package ae2csl.me;

import ae2.api.features.GridLinkables;
import ae2.api.upgrades.Upgrades;
import ae2csl.api.ids.AE2CSConstants;
import ae2csl.init.AE2CSBlocks;
import ae2csl.item.tools.ToolLinkableHandler;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = AE2CSConstants.MODID)
public class AEPlugin {

    private static boolean initialized = false;

    /**
     * 部件形态的陨石样板供应器逻辑集合（弱引用自动清理）。
     * 部件没有 Tile.update()，其逻辑的 tickServer（产物入仓 + 工作轮刷新）由这里统一驱动。
     */
    private static final Set<ae2csl.me.logic.MeteoritePatternProviderLogic> PART_LOGICS =
        Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public static void registerMeteoritePartLogic(ae2csl.me.logic.MeteoritePatternProviderLogic logic) {
        PART_LOGICS.add(logic);
    }

    public static void unregisterMeteoritePartLogic(ae2csl.me.logic.MeteoritePatternProviderLogic logic) {
        PART_LOGICS.remove(logic);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!initialized) {
            initialized = true;
            onCommonSetup();
        }
        // 驱动部件形态供应器逻辑（方块形态由 Tile.update 驱动，不在这里）
        synchronized (PART_LOGICS) {
            for (var logic : PART_LOGICS) {
                logic.tickServer();
            }
        }
    }

    public static void onCommonSetup() {
        // Grid linkable: register all tools as linkable
        for (Item item : Item.REGISTRY) {
            if (item instanceof ae2csl.item.tools.LinkableTool && GridLinkables.get(item) == null) {
                GridLinkables.register(item, ToolLinkableHandler.INSTANCE);
            }
        }

        // 升级卡注册（对齐源码 AEPlugin: 集成接口 1×合成卡）
        Item integratedInterfaceItem = Item.getItemFromBlock(AE2CSBlocks.INTEGRATED_INTERFACE);
        Item partItem = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
            AE2CSConstants.MODID, "integrated_interface_part"));
        Upgrades.add(ae2.core.definitions.AEItems.CRAFTING_CARD.item(), integratedInterfaceItem, 1,
            "block.ae2csl.integrated_interface");
        if (partItem != null) {
            Upgrades.add(ae2.core.definitions.AEItems.CRAFTING_CARD.item(), partItem, 1,
                "block.ae2csl.integrated_interface");
        }

        // 末影接口：1×合成卡 + 1×模糊卡（含扩展版）
        addEnderInterfaceUpgrades(AE2CSBlocks.ENDER_INTERFACE_BLOCK, "ender_interface_part",
            "block.ae2csl.ender_interface");
        addEnderInterfaceUpgrades(AE2CSBlocks.EX_ENDER_INTERFACE_BLOCK, "extended_ender_interface_part",
            "block.ae2csl.extended_ender_interface");

        // 初级样板供应器：对齐 fork 对样板供应器的升级支持。
        // 注意不注册样板扩展卡——本机样板栏固定 5 槽，扩展卡无效果（fork getActivePatternSlots 取 min(库存,9+卡*9)）
        Item simpleProviderItem = Item.getItemFromBlock(AE2CSBlocks.SIMPLE_PATTERN_PROVIDER_BLOCK);
        Item simpleProviderPart = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
            AE2CSConstants.MODID, "simple_pattern_provider_part"));
        Upgrades.add(ae2.core.definitions.AEItems.PSEUDO_CRAFTING_CARD.item(), simpleProviderItem, 1,
            "block.ae2csl.simple_pattern_provider");
        if (simpleProviderPart != null) {
            Upgrades.add(ae2.core.definitions.AEItems.PSEUDO_CRAFTING_CARD.item(), simpleProviderPart, 1,
                "block.ae2csl.simple_pattern_provider");
        }

        // 石英振荡时钟：1×红石卡（解锁红石控制模式，对齐源码 AEPlugin）
        Item clockItem = Item.getItemFromBlock(AE2CSBlocks.QUARTZ_OSCILLATOR_CLOCK);
        Item clockPart = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
            AE2CSConstants.MODID, "quartz_oscillator_clock_part"));
        Upgrades.add(ae2.core.definitions.AEItems.REDSTONE_CARD.item(), clockItem, 1,
            "block.ae2csl.quartz_oscillator_clock");
        if (clockPart != null) {
            Upgrades.add(ae2.core.definitions.AEItems.REDSTONE_CARD.item(), clockPart, 1,
                "block.ae2csl.quartz_oscillator_clock");
        }

        // 陨石样板供应器：4×加速卡 + 1×晶体生长卡（对齐源码 AEPlugin）
        Item meteoriteItem = Item.getItemFromBlock(AE2CSBlocks.METEORITE_PATTERN_PROVIDER_BLOCK);
        Item meteoritePart = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
            AE2CSConstants.MODID, "meteorite_pattern_provider_part"));
        Item growthCard = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
            AE2CSConstants.MODID, "crystal_growth_card"));
        Upgrades.add(ae2.core.definitions.AEItems.SPEED_CARD.item(), meteoriteItem, 4,
            "block.ae2csl.meteorite_pattern_provider");
        if (meteoritePart != null) {
            Upgrades.add(ae2.core.definitions.AEItems.SPEED_CARD.item(), meteoritePart, 4,
                "block.ae2csl.meteorite_pattern_provider");
        }
        // 陨石超频卡（对齐源码 AEPlugin）：蚀刻器/熵变仓/催生仓/谐振仓/粉碎机/聚合器 2 张，
        // 自装配样板供应器 4 张
        Item overloadCard = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
            AE2CSConstants.MODID, "overload_card"));
        if (overloadCard != null) {
            Upgrades.add(overloadCard, Item.getItemFromBlock(AE2CSBlocks.CIRCUIT_ETCHER), 2,
                "block.ae2csl.circuit_etcher");
            Upgrades.add(overloadCard, Item.getItemFromBlock(AE2CSBlocks.ENTROPY_VARIATION_CHAMBER), 2,
                "block.ae2csl.entropy_variation_reaction_chamber");
            // 催生仓/谐振仓/粉碎机/聚合器：各 2 张（一次操作最多 4/1 刻）
            Upgrades.add(overloadCard, Item.getItemFromBlock(AE2CSBlocks.CRYSTAL_GROWTH_CHAMBER), 2,
                "block.ae2csl.crystal_growth_chamber");
            Upgrades.add(overloadCard, Item.getItemFromBlock(AE2CSBlocks.CRYSTAL_VIBRATION_CHAMBER), 2,
                "block.ae2csl.crystal_vibration_chamber");
            Upgrades.add(overloadCard, Item.getItemFromBlock(AE2CSBlocks.CRYSTAL_PULVERIZER), 2,
                "block.ae2csl.crystal_pulverizer");
            Upgrades.add(overloadCard, Item.getItemFromBlock(AE2CSBlocks.CRYSTAL_AGGREGATOR), 2,
                "block.ae2csl.crystal_aggregator");
            Upgrades.add(overloadCard, meteoriteItem, 4, "block.ae2csl.meteorite_pattern_provider");
            if (meteoritePart != null) {
                Upgrades.add(overloadCard, meteoritePart, 4, "block.ae2csl.meteorite_pattern_provider");
            }
        }

        registerMachineUpgrades();
        addGrowthCardSupport();

        if (growthCard != null) {
            Upgrades.add(growthCard, meteoriteItem, 1, "block.ae2csl.meteorite_pattern_provider");
            if (meteoritePart != null) {
                Upgrades.add(growthCard, meteoritePart, 1, "block.ae2csl.meteorite_pattern_provider");
            }

            // 谐振样板供应器：1×晶体生长卡（对齐源码 AEPlugin L98-99）
            Item resonatingItem = Item.getItemFromBlock(AE2CSBlocks.RESONATING_PATTERN_PROVIDER);
            Item resonatingPart = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
                AE2CSConstants.MODID, "resonating_pattern_provider_part"));
            Item exResonatingItem = Item.getItemFromBlock(AE2CSBlocks.EX_RESONATING_PATTERN_PROVIDER_BLOCK);
            Item exResonatingPart = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
                AE2CSConstants.MODID, "extended_resonating_pattern_provider_part"));
            Upgrades.add(growthCard, resonatingItem, 1, "block.ae2csl.resonating_pattern_provider");
            if (resonatingPart != null) {
                Upgrades.add(growthCard, resonatingPart, 1, "block.ae2csl.resonating_pattern_provider");
            }
            Upgrades.add(growthCard, exResonatingItem, 1, "block.ae2csl.extended_resonating_pattern_provider");
            if (exResonatingPart != null) {
                Upgrades.add(growthCard, exResonatingPart, 1, "block.ae2csl.extended_resonating_pattern_provider");
            }
        }
    }

    /**
     * 晶体生长卡登记（对齐 1.21.1 源码 AEPlugin.addGrowthCardSupport）。
     * 效果：催生仓会检查四周相邻的方块/部件，装了生长卡的设备等同于一台晶体催生加速器。
     */
    private static void addGrowthCardSupport() {
        Item growthCard = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
            AE2CSConstants.MODID, "crystal_growth_card"));
        if (growthCard == null) {
            return;
        }

        // AE2 原版总线：各自独立显示名字（不给 group，避免 tooltip 被合并成一行）
        Upgrades.add(growthCard, ae2.core.definitions.AEParts.STORAGE_BUS.item(), 1);
        Upgrades.add(growthCard, ae2.core.definitions.AEParts.IMPORT_BUS.item(), 1);
        Upgrades.add(growthCard, ae2.core.definitions.AEParts.EXPORT_BUS.item(), 1);

        // AE2 接口（部件 + 方块）：共用一个「已存在」的翻译键，tooltip 合并成「ME接口」。
        // 注意 1.12.2 fork 没有 block.ae2.interface 键，机器名在 gui.ae2.Interface / tile.ae2.interface.name。
        String interfaceGroup = "gui.ae2.Interface";
        Upgrades.add(growthCard, ae2.core.definitions.AEParts.INTERFACE.item(), 1, interfaceGroup);
        Upgrades.add(growthCard, ae2.core.definitions.AEBlocks.INTERFACE.item(), 1, interfaceGroup);

        // 本模组：集成接口 / 末影接口（方块 + 部件）
        addGrowthCardToMachineAndPart(growthCard, AE2CSBlocks.INTEGRATED_INTERFACE,
            "integrated_interface_part", "block.ae2csl.integrated_interface");
        addGrowthCardToMachineAndPart(growthCard, AE2CSBlocks.ENDER_INTERFACE_BLOCK,
            "ender_interface_part", "block.ae2csl.ender_interface");
        addGrowthCardToMachineAndPart(growthCard, AE2CSBlocks.EX_ENDER_INTERFACE_BLOCK,
            "extended_ender_interface_part", "block.ae2csl.extended_ender_interface");
    }

    private static void addGrowthCardToMachineAndPart(Item growthCard, net.minecraft.block.Block block,
                                                      String partName, String group) {
        Item blockItem = Item.getItemFromBlock(block);
        if (blockItem != null) {
            Upgrades.add(growthCard, blockItem, 1, group);
        }
        Item part = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
            AE2CSConstants.MODID, partName));
        if (part != null) {
            Upgrades.add(growthCard, part, 1, group);
        }
    }

    /**
     * 六台处理器机器的加速卡登记（对齐 1.21.1 源码 AEPlugin.onCommonSetup）。
     * 源码里机器接受哪些升级卡完全由这里决定：其它 mod 的卡想支持某台机器，
     * 由它自己调用 Upgrades.add(card, 机器物品, 上限) 登记即可，无需改本模组代码。
     */
    private static void registerMachineUpgrades() {
        Item speedCard = ae2.core.definitions.AEItems.SPEED_CARD.item();

        // 机器 -> 升级槽数量
        Object[][] machines = {
            { AE2CSBlocks.CRYSTAL_AGGREGATOR, 4, "block.ae2csl.crystal_aggregator" },
            { AE2CSBlocks.CRYSTAL_PULVERIZER, 4, "block.ae2csl.crystal_pulverizer" },
            { AE2CSBlocks.CIRCUIT_ETCHER, 4, "block.ae2csl.circuit_etcher" },
            { AE2CSBlocks.CRYSTAL_GROWTH_CHAMBER, 4, "block.ae2csl.crystal_growth_chamber" },
            { AE2CSBlocks.CRYSTAL_VIBRATION_CHAMBER, 3, "block.ae2csl.crystal_vibration_chamber" },
            { AE2CSBlocks.ENTROPY_VARIATION_CHAMBER, 4, "block.ae2csl.entropy_variation_reaction_chamber" },
        };
        for (Object[] entry : machines) {
            Item machine = Item.getItemFromBlock((net.minecraft.block.Block) entry[0]);
            int slots = (Integer) entry[1];
            String group = (String) entry[2];
            if (machine == null) {
                continue;
            }
            Upgrades.add(speedCard, machine, slots, group);
        }
    }

    private static void addEnderInterfaceUpgrades(net.minecraft.block.Block block, String partName,
                                                  String group) {
        Item blockItem = Item.getItemFromBlock(block);
        Item part = Item.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(
            AE2CSConstants.MODID, partName));
        Upgrades.add(ae2.core.definitions.AEItems.CRAFTING_CARD.item(), blockItem, 1, group);
        Upgrades.add(ae2.core.definitions.AEItems.FUZZY_CARD.item(), blockItem, 1, group);
        if (part != null) {
            Upgrades.add(ae2.core.definitions.AEItems.CRAFTING_CARD.item(), part, 1, group);
            Upgrades.add(ae2.core.definitions.AEItems.FUZZY_CARD.item(), part, 1, group);
        }
    }
}
