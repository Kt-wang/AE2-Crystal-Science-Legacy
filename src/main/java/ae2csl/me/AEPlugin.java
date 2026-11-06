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
