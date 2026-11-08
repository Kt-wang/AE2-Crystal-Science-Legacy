/*
 * AE2 Crystal Science
 */

package ae2csl.core;

import ae2csl.Tags;
import ae2csl.client.render.EnderBroadcasterTESR;
import ae2csl.client.render.EnderEmitterTESR;
import ae2csl.tile.ender.TileEnderBroadcaster;
import ae2csl.tile.ender.TileEnderEmitter;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Client-side proxy for common mod functionality.
 */
public class AppEngClient extends AppEngServer {

    public void preInit(FMLPreInitializationEvent event) {
        // Register this instance for ModelRegistryEvent (fires after block registrations)
        MinecraftForge.EVENT_BUS.register(this);

        // 谐振ME驱动器：自定义 baked model（外壳 + 舱内元件动态渲染），参考 AE2 InitBuiltInModels
        net.minecraftforge.client.model.ModelLoaderRegistry.registerLoader(new ResonatingDriveModelLoader());

        // 部件模型注册必须在 preInit（fork PartModels.registerModels 仅允许预初始化阶段）
        registerPartModels();

        // [P] 查看谐振样板：AE2 的 PatternGuiHandler 按 details 的具体类查表，
        // 自定义样板类型必须自己注册，否则按 P 无反应。
        ae2.client.gui.pattern.PatternGuiHandler.register(
            ae2csl.me.crafting.ResonatingPatternDetails.class,
            pattern -> {
                net.minecraft.entity.player.InventoryPlayer inv =
                    net.minecraft.client.Minecraft.getMinecraft().player.inventory;
                return new ae2csl.client.gui.pattern.GuiResonatingPattern(
                    new ae2csl.container.pattern.ContainerResonatingPattern(inv, pattern), inv);
            });
    }

    /** 谐振驱动器模型加载器：拦截 ae2csl:builtin/resonating_drive
     *  （AE2 的 BuiltInModelLoader 只接 ae2 域，只能自己注册一个）
     */
    private static class ResonatingDriveModelLoader
        implements net.minecraftforge.client.model.ICustomModelLoader {
        private static final String PATH = "builtin/resonating_drive";
        private final ae2csl.client.render.model.ResonatingDriveModel model =
            new ae2csl.client.render.model.ResonatingDriveModel();

        @Override
        public boolean accepts(ResourceLocation modelLocation) {
            return Tags.MOD_ID.equals(modelLocation.getNamespace())
                && modelLocation.getPath().endsWith(PATH);
        }

        @Override
        public net.minecraftforge.client.model.IModel loadModel(ResourceLocation modelLocation) {
            return model;
        }

        @Override
        public void onResourceManagerReload(net.minecraft.client.resources.IResourceManager resourceManager) {
        }
    }

    @SubscribeEvent
    public void onModelRegistry(ModelRegistryEvent event) {
        // Register item models for all blocks
        for (Block block : ForgeRegistries.BLOCKS.getValuesCollection()) {
            ResourceLocation rl = block.getRegistryName();
            if (rl != null && rl.toString().startsWith(Tags.MOD_ID + ":")) {
                Item item = Item.getItemFromBlock(block);
                if (item == null || item == net.minecraft.init.Items.AIR) {
                    continue;  // 无物品的方块（如双台阶）不注册物品模型
                }
                ModelResourceLocation mrl = new ModelResourceLocation(rl, "inventory");
                ModelLoader.setCustomModelResourceLocation(item, 0, mrl);
            }
        }
        // Register item models for all standalone items
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            ResourceLocation rl = item.getRegistryName();
            if (rl != null && rl.toString().startsWith(Tags.MOD_ID + ":")) {
                ModelResourceLocation mrl = new ModelResourceLocation(rl, "inventory");
                ModelLoader.setCustomModelResourceLocation(item, 0, mrl);
            }
        }
    }

    public void init(FMLInitializationEvent event) {
        // Bind the animated crystal-core renderer for the ender broadcaster.
        ClientRegistry.bindTileEntitySpecialRenderer(TileEnderBroadcaster.class, new EnderBroadcasterTESR());
        // Bind the antenna + link-status renderer for the ender emitter.
        ClientRegistry.bindTileEntitySpecialRenderer(TileEnderEmitter.class, new EnderEmitterTESR());
        // 末影接口：范围显示线框
        ClientRegistry.bindTileEntitySpecialRenderer(ae2csl.tile.ender.TileEnderInterface.class,
            new ae2csl.client.render.EnderInterfaceTESR());
        // 谐振ME驱动器：舱位填充百分比 LED
        ClientRegistry.bindTileEntitySpecialRenderer(ae2csl.tile.storage.TileResonatingDrive.class,
            new ae2csl.client.render.ResonatingDriveLedTESR());
    }

    private static void registerPartModels() {
        Class<?>[] partClasses = {
            ae2csl.me.part.EnderInterfacePart.class,
            ae2csl.me.part.ExtendedEnderInterfacePart.class,
            ae2csl.me.part.IntegratedInterfacePart.class,
            ae2csl.me.part.ExtendedIntegratedInterfacePart.class,
            ae2csl.me.part.MeteoritePatternProviderPart.class,
            ae2csl.me.part.MirrorPatternProviderPart.class,
            ae2csl.me.part.SimplePatternProviderPart.class,
            ae2csl.me.part.QuartzOscillatorClockPart.class,
            ae2csl.me.part.ResonatingPatternProviderPart.class,
            ae2csl.me.part.ExtendedResonatingPatternProviderPart.class,
            ae2csl.me.part.ResonantTemplateCodingTerminalPart.class,
        };
        for (Class<?> partClass : partClasses) {
            ae2.api.parts.PartModels.registerModels(
                ae2.items.parts.PartModelsHelper.createModels(partClass));
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
    }
}