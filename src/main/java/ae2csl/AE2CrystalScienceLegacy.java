/*
 * AE2 Crystal Science
 * A mod that adds crystal-related machines and items to Applied Energistics 2
 */

package ae2csl;

import ae2csl.client.gui.GuiHandler;
import ae2csl.network.EnderBroadcasterActionPacket;
import ae2csl.network.EnderEmitterActionPacket;
import ae2csl.network.EntropyModeTogglePacket;
import ae2csl.network.FrequencyBandActionPacket;
import ae2csl.network.FrequencyBandSyncPacket;
import ae2csl.network.MirrorLinkerBatchApplyPacket;
import ae2csl.network.OpenGuiPacket;
import ae2csl.network.ScrollResonatingPatternSelectPacket;
import ae2csl.network.SideConfigActionPacket;
import ae2csl.network.SideConfigSyncPacket;
import ae2csl.core.AppEngServer;
import ae2csl.recipe.AE2CSRecipeRegistry;
import ae2csl.registry.RegistryHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID,
     name = Tags.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:ae2;after:crafttweaker")
public final class AE2CrystalScienceLegacy {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    @Mod.Instance(Tags.MOD_ID)
    public static AE2CrystalScienceLegacy INSTANCE;

    @SuppressWarnings("unused")
    @SidedProxy(clientSide = "ae2csl.core.AppEngClient", serverSide = "ae2csl.core.AppEngServer")
    private static AppEngServer runtime;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("{} preInit", Tags.MOD_NAME);
        MinecraftForge.EVENT_BUS.register(RegistryHandler.class);
        RegistryHandler.preInit();
        ae2csl.me.part.AE2CSPartModelRegistration.register();
        net.minecraftforge.fml.common.registry.GameRegistry.registerWorldGenerator(new ae2csl.world.AE2CSOreWorldGen(), 0);
        if (event.getSide().isClient()) {
            runtime.preInit(event);
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("{} init", Tags.MOD_NAME);
        RegistryHandler.init();       // Register ore dicts FIRST — recipes may reference them
        AE2CSRecipeRegistry.init();
        // Apply CraftTweaker actions queued during preInit (scripts run before our recipes are loaded)
        ae2csl.integration.crafttweaker.CrTBridge.applyPending();
        NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, new GuiHandler());
        registerPackets();
        runtime.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("{} postInit", Tags.MOD_NAME);
        runtime.postInit(event);
    }

    private void registerPackets() {
        int id = 0;
        NETWORK.registerMessage(MirrorLinkerBatchApplyPacket.Handler.class, MirrorLinkerBatchApplyPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(ScrollResonatingPatternSelectPacket.Handler.class, ScrollResonatingPatternSelectPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(EntropyModeTogglePacket.Handler.class, EntropyModeTogglePacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(EnderBroadcasterActionPacket.Handler.class, EnderBroadcasterActionPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(EnderEmitterActionPacket.Handler.class, EnderEmitterActionPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(FrequencyBandActionPacket.Handler.class, FrequencyBandActionPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(FrequencyBandSyncPacket.Handler.class, FrequencyBandSyncPacket.class, id++, Side.CLIENT);
        NETWORK.registerMessage(OpenGuiPacket.Handler.class, OpenGuiPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(SideConfigActionPacket.Handler.class, SideConfigActionPacket.class, id++, Side.SERVER);
        NETWORK.registerMessage(SideConfigSyncPacket.Handler.class, SideConfigSyncPacket.class, id++, Side.CLIENT);
    }

    public static AE2CrystalScienceLegacy instance() {
        return INSTANCE;
    }
}