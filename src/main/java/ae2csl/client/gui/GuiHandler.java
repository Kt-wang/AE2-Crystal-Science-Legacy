/*
 * AE2 Crystal Science
 * GUI Handler - dispatches Container and GUI creation for all 13 machines
 */
package ae2csl.client.gui;

import ae2csl.container.*;
import ae2csl.tile.aggregator.TileCrystalAggregator;
import ae2csl.tile.crystal.TileCrystalGrowthChamber;
import ae2csl.tile.ender.TileEnderBroadcaster;
import ae2csl.tile.ender.TileEnderEmitter;
import ae2csl.tile.ender.TileEnderInterface;
import ae2csl.tile.entropy.TileEntropyVariationChamber;
import ae2csl.tile.etcher.TileCircuitEtcher;
import ae2csl.tile.grindstone.TileQuartzGrindstone;
import ae2csl.tile.integrated.TileIntegratedInterface;
import ae2csl.tile.mirror.TileMeteoritePatternProvider;
import ae2csl.tile.oscillator.TileQuartzOscillatorClock;
import ae2csl.tile.pulverizer.TileCrystalPulverizer;
import ae2csl.tile.resonating.TileResonatingPatternProvider;
import ae2csl.tile.vibration.TileCrystalVibrationChamber;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import javax.annotation.Nullable;

public class GuiHandler implements IGuiHandler {

    public static final int GUI_CRYSTAL_GROWTH_CHAMBER = 0;
    public static final int GUI_CIRCUIT_ETCHER = 1;
    public static final int GUI_QUARTZ_GRINDSTONE = 2;
    public static final int GUI_CRYSTAL_PULVERIZER = 3;
    public static final int GUI_CRYSTAL_VIBRATION_CHAMBER = 4;
    public static final int GUI_CRYSTAL_AGGREGATOR = 5;
    public static final int GUI_ENDER_BROADCASTER = 6;
    public static final int GUI_ENDER_EMITTER = 7;
    public static final int GUI_ENTROPY_VARIATION_CHAMBER = 8;
    public static final int GUI_INTEGRATED_INTERFACE = 9;
    public static final int GUI_QUARTZ_OSCILLATOR_CLOCK = 11;
    public static final int GUI_RESONATING_PATTERN_PROVIDER = 12;
    public static final int GUI_ENDER_INTERFACE = 13;
    public static final int GUI_METEORITE_PATTERN_PROVIDER = 14;
    public static final int GUI_SIDE_CONFIG = 15;
    public static final int GUI_FREQUENCY_BAND = 16;
    public static final int GUI_FREQUENCY_BAND_CREATE = 17;
    public static final int GUI_FREQUENCY_BAND_LINK = 18;
    public static final int GUI_FREQUENCY_BAND_MANAGER = 19;
    public static final int GUI_BAND_WHITELIST = 20;
    public static final int GUI_ENDER_EMITTER_FREQUENCY_BAND = 21;
    public static final int GUI_ENDER_EMITTER_FREQUENCY_BAND_LINK = 22;
    public static final int GUI_II_SET_STOCK_AMOUNT = 23;
    public static final int GUI_SIMPLE_PATTERN_PROVIDER = 24;
    public static final int GUI_RESONATING_DRIVE = 25;
    public static final int GUI_RESONATING_DRIVE_SETTINGS = 26;
    /** 部件形态石英振荡时钟 GUI（id = 本值 + side 序号 0-5） */
    public static final int GUI_QUARTZ_OSCILLATOR_CLOCK_PART = 56;
    /** 部件形态初级样板供应器 GUI（id = 本值 + side 序号 0-5） */
    public static final int GUI_SIMPLE_PATTERN_PROVIDER_PART = 64;
    /** 部件形态陨石样板供应器 GUI（id = 本值 + side 序号 0-5） */
    public static final int GUI_METEORITE_PATTERN_PROVIDER_PART = 72;
    /** 部件形态谐振样板供应器 GUI（id = 本值 + side 序号 0-5） */
    public static final int GUI_RESONATING_PATTERN_PROVIDER_PART = 80;
    /** 谐振样板转换器（手持物品 GUI） */
    public static final int GUI_RESONATING_PATTERN_CONVERTER = 88;
    /** 部件形态集成接口 GUI（id = 本值 + side 序号 0-5） */
    public static final int GUI_INTEGRATED_INTERFACE_PART = 32;
    /** 部件形态设置数量子GUI（id = 本值 + side 序号 0-5） */
    public static final int GUI_II_SET_STOCK_AMOUNT_PART = 40;
    /** 部件形态末影接口 GUI（id = 本值 + side 序号 0-5） */
    public static final int GUI_ENDER_INTERFACE_PART = 48;

    @Nullable @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tile = world.getTileEntity(pos);
        switch (id) {
            case GUI_CRYSTAL_GROWTH_CHAMBER: if (tile instanceof TileCrystalGrowthChamber) return new ContainerCrystalGrowthChamber(player.inventory, (TileCrystalGrowthChamber) tile); break;
            case GUI_CIRCUIT_ETCHER: if (tile instanceof TileCircuitEtcher) return new ContainerCircuitEtcher(player.inventory, (TileCircuitEtcher) tile); break;
            case GUI_QUARTZ_GRINDSTONE: if (tile instanceof TileQuartzGrindstone) return new ContainerQuartzGrindstone(player.inventory, (TileQuartzGrindstone) tile); break;
            case GUI_CRYSTAL_PULVERIZER: if (tile instanceof TileCrystalPulverizer) return new ContainerCrystalPulverizer(player.inventory, (TileCrystalPulverizer) tile); break;
            case GUI_CRYSTAL_VIBRATION_CHAMBER: if (tile instanceof TileCrystalVibrationChamber) return new ContainerCrystalVibrationChamber(player.inventory, (TileCrystalVibrationChamber) tile); break;
            case GUI_CRYSTAL_AGGREGATOR: if (tile instanceof TileCrystalAggregator) return new ContainerCrystalAggregator(player.inventory, (TileCrystalAggregator) tile); break;
            case GUI_ENDER_BROADCASTER: if (tile instanceof TileEnderBroadcaster) return new ContainerEnderBroadcaster(player.inventory, (TileEnderBroadcaster) tile); break;
            case GUI_ENDER_EMITTER: if (tile instanceof TileEnderEmitter) return new ContainerEnderEmitter(player.inventory, (TileEnderEmitter) tile); break;
            case GUI_ENTROPY_VARIATION_CHAMBER: if (tile instanceof TileEntropyVariationChamber) return new ContainerEntropyVariationChamber(player.inventory, (TileEntropyVariationChamber) tile); break;
            case GUI_INTEGRATED_INTERFACE: if (tile instanceof TileIntegratedInterface) return createIntegratedInterfaceContainer(player, (TileIntegratedInterface) tile); break;
            case GUI_QUARTZ_OSCILLATOR_CLOCK: if (tile instanceof TileQuartzOscillatorClock) return createOscillatorContainer(player, (TileQuartzOscillatorClock) tile); break;
            case GUI_RESONATING_PATTERN_PROVIDER: if (tile instanceof TileResonatingPatternProvider) return new ContainerResonatingPatternProvider(player.inventory, (TileResonatingPatternProvider) tile); break;
            case GUI_ENDER_INTERFACE: if (tile instanceof TileEnderInterface) return createEnderInterfaceContainer(player, (TileEnderInterface) tile); break;
            case GUI_METEORITE_PATTERN_PROVIDER: if (tile instanceof TileMeteoritePatternProvider) return createMeteoriteContainer(player, (TileMeteoritePatternProvider) tile); break;
            case GUI_SIDE_CONFIG: return new ContainerSideConfig(player.inventory, tile);
            case GUI_FREQUENCY_BAND: return new ContainerFrequencyBand(player.inventory, tile);
            case GUI_FREQUENCY_BAND_CREATE: return new ContainerFrequencyBandCreate(player.inventory, tile);
            case GUI_FREQUENCY_BAND_LINK: return new ContainerFrequencyBandLink(player.inventory, tile);
            case GUI_FREQUENCY_BAND_MANAGER: return new ContainerFrequencyBandManager(player.inventory, tile);
            case GUI_BAND_WHITELIST: return new ContainerBandWhiteListManager(player.inventory, tile);
            case GUI_ENDER_EMITTER_FREQUENCY_BAND: return new ContainerEnderEmitterFrequencyBand(player.inventory, tile);
            case GUI_ENDER_EMITTER_FREQUENCY_BAND_LINK: return new ContainerEnderEmitterFrequencyBandLink(player.inventory, tile);
            case GUI_II_SET_STOCK_AMOUNT: if (tile instanceof TileIntegratedInterface) return ContainerSetStockAmount.createFromPending(player.inventory, (TileIntegratedInterface) tile); break;
            case GUI_RESONATING_PATTERN_CONVERTER: return new ContainerResonatingPatternConverter(player.inventory);
            case GUI_SIMPLE_PATTERN_PROVIDER: if (tile instanceof ae2csl.tile.mirror.TileSimplePatternProvider) return createSimplePatternProviderContainer(player, (ae2csl.tile.mirror.TileSimplePatternProvider) tile); break;
            case GUI_RESONATING_DRIVE: if (tile instanceof ae2csl.tile.storage.TileResonatingDrive) return createResonatingDriveContainer(player, (ae2csl.tile.storage.TileResonatingDrive) tile); break;
            case GUI_RESONATING_DRIVE_SETTINGS: if (tile instanceof ae2csl.tile.storage.TileResonatingDrive) return new ae2csl.container.ContainerResonatingDriveSettings(player.inventory, (ae2csl.tile.storage.TileResonatingDrive) tile); break;
        }
        return createPartServerGui(id, player, tile);
    }

    private static Object createIntegratedInterfaceContainer(EntityPlayer player, TileIntegratedInterface tile) {
        var container = new ContainerIntegratedInterface(player.inventory, tile);
        // 设置 locator 使 fork 的子GUI系统（优先级界面等）能定位宿主
        container.setLocator(ae2.core.gui.locator.GuiHostLocators.forTile(tile));
        return container;
    }

    /** 部件形态集成接口的服务端容器（按面索引） */
    @Nullable
    private static Object createPartServerGui(int id, EntityPlayer player, TileEntity tile) {
        if (id >= GUI_INTEGRATED_INTERFACE_PART && id < GUI_INTEGRATED_INTERFACE_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_INTEGRATED_INTERFACE_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.IntegratedInterfacePart part) {
                var container = new ContainerIntegratedInterface(player.inventory, part);
                container.setLocator(ae2.core.gui.locator.GuiHostLocators.forPart(part));
                return container;
            }
            return null;
        }
        if (id >= GUI_II_SET_STOCK_AMOUNT_PART && id < GUI_II_SET_STOCK_AMOUNT_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_II_SET_STOCK_AMOUNT_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.IntegratedInterfacePart part) {
                return ContainerSetStockAmount.createFromPending(player.inventory, part);
            }
        }
        if (id >= GUI_ENDER_INTERFACE_PART && id < GUI_ENDER_INTERFACE_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_ENDER_INTERFACE_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.EnderInterfacePart part) {
                var container = new ContainerEnderInterface(player.inventory, part);
                container.setLocator(ae2.core.gui.locator.GuiHostLocators.forPart(part));
                return container;
            }
        }
        if (id >= GUI_QUARTZ_OSCILLATOR_CLOCK_PART && id < GUI_QUARTZ_OSCILLATOR_CLOCK_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_QUARTZ_OSCILLATOR_CLOCK_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.QuartzOscillatorClockPart part) {
                var container = new ContainerQuartzOscillatorClock(player.inventory, part);
                container.setLocator(ae2.core.gui.locator.GuiHostLocators.forPart(part));
                return container;
            }
        }
        if (id >= GUI_METEORITE_PATTERN_PROVIDER_PART && id < GUI_METEORITE_PATTERN_PROVIDER_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_METEORITE_PATTERN_PROVIDER_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.MeteoritePatternProviderPart part) {
                var container = new ae2csl.container.ContainerMeteoritePatternProvider(player.inventory, part);
                container.setLocator(ae2.core.gui.locator.GuiHostLocators.forPart(part));
                return container;
            }
        }
        if (id >= GUI_RESONATING_PATTERN_PROVIDER_PART && id < GUI_RESONATING_PATTERN_PROVIDER_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_RESONATING_PATTERN_PROVIDER_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.ResonatingPatternProviderPart part) {
                var container = new ContainerResonatingPatternProvider(player.inventory, part);
                container.setLocator(ae2.core.gui.locator.GuiHostLocators.forPart(part));
                return container;
            }
        }
        return null;
    }

    private static Object createEnderInterfaceContainer(EntityPlayer player,
                                                        ae2csl.tile.ender.TileEnderInterface tile) {
        var container = new ContainerEnderInterface(player.inventory, tile);
        container.setLocator(ae2.core.gui.locator.GuiHostLocators.forTile(tile));
        return container;
    }

    private static Object createSimplePatternProviderContainer(EntityPlayer player,
                                                               ae2csl.tile.mirror.TileSimplePatternProvider tile) {
        // 初级供应器用 fork 原生容器（只收处理样板，合成样板由分子装配室自持）
        var container = new ae2.container.implementations.ContainerPatternProvider(player.inventory, tile);
        // 设置 locator 使优先级子界面等能定位宿主
        container.setLocator(ae2.core.gui.locator.GuiHostLocators.forTile(tile));
        return container;
    }

    private static Object createResonatingDriveContainer(EntityPlayer player,
                                                         ae2csl.tile.storage.TileResonatingDrive tile) {
        var container = new ae2csl.container.ContainerResonatingDrive(player.inventory, tile);
        // 设置 locator 使 fork 的子GUI系统（优先级界面等）能定位宿主
        container.setLocator(ae2.core.gui.locator.GuiHostLocators.forTile(tile));
        return container;
    }

    private static Object createOscillatorContainer(EntityPlayer player, TileQuartzOscillatorClock tile) {
        var container = new ContainerQuartzOscillatorClock(player.inventory, tile);
        container.setLocator(ae2.core.gui.locator.GuiHostLocators.forTile(tile));
        return container;
    }

    private static Object createMeteoriteContainer(EntityPlayer player, TileMeteoritePatternProvider tile) {
        var container = new ae2csl.container.ContainerMeteoritePatternProvider(player.inventory, (TileMeteoritePatternProvider) tile);
        container.setLocator(ae2.core.gui.locator.GuiHostLocators.forTile(tile));
        return container;
    }

    @Nullable @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tile = world.getTileEntity(pos);
        switch (id) {
            case GUI_CRYSTAL_GROWTH_CHAMBER: if (tile instanceof TileCrystalGrowthChamber) return new GuiCrystalGrowthChamber(player.inventory, (TileCrystalGrowthChamber) tile); break;
            case GUI_CIRCUIT_ETCHER: if (tile instanceof TileCircuitEtcher) return new GuiCircuitEtcher(player.inventory, (TileCircuitEtcher) tile); break;
            case GUI_QUARTZ_GRINDSTONE: if (tile instanceof TileQuartzGrindstone) return new GuiQuartzGrindstone(player.inventory, (TileQuartzGrindstone) tile); break;
            case GUI_CRYSTAL_PULVERIZER: if (tile instanceof TileCrystalPulverizer) return new GuiCrystalPulverizer(player.inventory, (TileCrystalPulverizer) tile); break;
            case GUI_CRYSTAL_VIBRATION_CHAMBER: if (tile instanceof TileCrystalVibrationChamber) return new GuiCrystalVibrationChamber(player.inventory, (TileCrystalVibrationChamber) tile); break;
            case GUI_CRYSTAL_AGGREGATOR: if (tile instanceof TileCrystalAggregator) return new GuiCrystalAggregator(player.inventory, (TileCrystalAggregator) tile); break;
            case GUI_ENDER_BROADCASTER: if (tile instanceof TileEnderBroadcaster) return new GuiEnderBroadcaster(player.inventory, (TileEnderBroadcaster) tile); break;
            case GUI_ENDER_EMITTER: if (tile instanceof TileEnderEmitter) return new GuiEnderEmitter(player.inventory, (TileEnderEmitter) tile); break;
            case GUI_ENTROPY_VARIATION_CHAMBER: if (tile instanceof TileEntropyVariationChamber) return new GuiEntropyVariationChamber(player.inventory, (TileEntropyVariationChamber) tile); break;
            case GUI_INTEGRATED_INTERFACE: if (tile instanceof TileIntegratedInterface) return new GuiIntegratedInterface(player.inventory, (TileIntegratedInterface) tile); break;
            case GUI_II_SET_STOCK_AMOUNT: if (tile instanceof TileIntegratedInterface) return new GuiSetStockAmount(new ContainerSetStockAmount(player.inventory, (TileIntegratedInterface) tile), player.inventory); break;
            case GUI_RESONATING_PATTERN_CONVERTER: return new GuiResonatingPatternConverter(player.inventory);
            case GUI_RESONATING_DRIVE: if (tile instanceof ae2csl.tile.storage.TileResonatingDrive) return new GuiResonatingDrive(player.inventory, (ae2csl.tile.storage.TileResonatingDrive) tile); break;
            case GUI_RESONATING_DRIVE_SETTINGS: if (tile instanceof ae2csl.tile.storage.TileResonatingDrive) return new GuiResonatingDriveSettings(player.inventory, (ae2csl.tile.storage.TileResonatingDrive) tile); break;
            case GUI_SIMPLE_PATTERN_PROVIDER: if (tile instanceof ae2csl.tile.mirror.TileSimplePatternProvider simpleTile) {
                // 初级供应器用 fork 原生容器（只收处理样板，合成样板由分子装配室自持）
                var container = new ae2.container.implementations.ContainerPatternProvider(player.inventory, simpleTile);
                return new GuiSimplePatternProvider(container, player.inventory);
            } break;
            case GUI_QUARTZ_OSCILLATOR_CLOCK: if (tile instanceof TileQuartzOscillatorClock) return new GuiQuartzOscillatorClock(player.inventory, (TileQuartzOscillatorClock) tile); break;
            case GUI_RESONATING_PATTERN_PROVIDER: if (tile instanceof TileResonatingPatternProvider) return new GuiResonatingPatternProvider(player.inventory, (TileResonatingPatternProvider) tile); break;
            case GUI_ENDER_INTERFACE: if (tile instanceof TileEnderInterface) return new GuiEnderInterface(player.inventory, (TileEnderInterface) tile); break;
            case GUI_METEORITE_PATTERN_PROVIDER: if (tile instanceof TileMeteoritePatternProvider) return new GuiMeteoritePatternProvider(player.inventory, (TileMeteoritePatternProvider) tile); break;
            case GUI_SIDE_CONFIG: return new GuiSideConfig(player.inventory, tile);
            case GUI_FREQUENCY_BAND: return new GuiFrequencyBand(player.inventory, tile);
            case GUI_FREQUENCY_BAND_CREATE: return new GuiFrequencyBandCreate(player.inventory, tile);
            case GUI_FREQUENCY_BAND_LINK: return new GuiFrequencyBandLink(player.inventory, tile);
            case GUI_FREQUENCY_BAND_MANAGER: return new GuiFrequencyBandManager(player.inventory, tile);
            case GUI_BAND_WHITELIST: return new GuiBandWhiteListManager(player.inventory, tile);
            case GUI_ENDER_EMITTER_FREQUENCY_BAND: return new GuiEnderEmitterFrequencyBand(player.inventory, tile);
            case GUI_ENDER_EMITTER_FREQUENCY_BAND_LINK: return new GuiEnderEmitterFrequencyBandLink(player.inventory, tile);
        }
        // 部件形态集成接口（按面索引）
        if (id >= GUI_INTEGRATED_INTERFACE_PART && id < GUI_INTEGRATED_INTERFACE_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_INTEGRATED_INTERFACE_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.IntegratedInterfacePart part) {
                return new GuiIntegratedInterface(player.inventory, part);
            }
            return null;
        }
        if (id >= GUI_II_SET_STOCK_AMOUNT_PART && id < GUI_II_SET_STOCK_AMOUNT_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_II_SET_STOCK_AMOUNT_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.IntegratedInterfacePart part) {
                return new GuiSetStockAmount(new ContainerSetStockAmount(player.inventory, part), player.inventory);
            }
            return null;
        }
        if (id >= GUI_ENDER_INTERFACE_PART && id < GUI_ENDER_INTERFACE_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_ENDER_INTERFACE_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.EnderInterfacePart part) {
                return new GuiEnderInterface(player.inventory, part);
            }
            return null;
        }
        if (id >= GUI_QUARTZ_OSCILLATOR_CLOCK_PART && id < GUI_QUARTZ_OSCILLATOR_CLOCK_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_QUARTZ_OSCILLATOR_CLOCK_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.QuartzOscillatorClockPart part) {
                return new GuiQuartzOscillatorClock(player.inventory, part);
            }
            return null;
        }
        if (id >= GUI_METEORITE_PATTERN_PROVIDER_PART && id < GUI_METEORITE_PATTERN_PROVIDER_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_METEORITE_PATTERN_PROVIDER_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.MeteoritePatternProviderPart part) {
                return new GuiMeteoritePatternProvider(player.inventory, part);
            }
            return null;
        }
        if (id >= GUI_RESONATING_PATTERN_PROVIDER_PART && id < GUI_RESONATING_PATTERN_PROVIDER_PART + 6) {
            EnumFacing side = EnumFacing.VALUES[id - GUI_RESONATING_PATTERN_PROVIDER_PART];
            if (tile instanceof ae2.api.parts.IPartHost partHost
                && partHost.getPart(side) instanceof ae2csl.me.part.ResonatingPatternProviderPart part) {
                return new GuiResonatingPatternProvider(player.inventory, part);
            }
            return null;
        }
        return null;
    }
}
