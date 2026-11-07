package ae2csl.verification;

import ae2.api.parts.IPart;
import ae2.api.parts.IPartItem;
import ae2.tile.AEBaseTile;
import ae2.tile.networking.TileCableBus;
import ae2csl.me.logic.ResonatingPatternProviderLogic;
import ae2csl.me.part.ExtendedResonatingPatternProviderPart;
import ae2csl.me.part.ResonatingPatternProviderPart;
import com.google.gson.GsonBuilder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runs only as a separate test mod against the actual installation jars. */
@Mod(modid = "ae2csl_regression", name = "AE2CSL runtime regression", version = "1",
        dependencies = "required-after:ae2csl")
public final class RuntimeRegression {
    private final Map<String, Object> report = new LinkedHashMap<>();
    private boolean serverChecked;
    private boolean clientChecked;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        report.put("cleanroom", Loader.instance().getIndexedModList().get("cleanroom").getVersion());
        report.put("ae2s", Loader.instance().getIndexedModList().get("ae2").getVersion());
        report.put("ae2csl", Loader.instance().getIndexedModList().get("ae2csl").getVersion());
        if (event.getSide().isClient()) {
            try {
                MinecraftForge.EVENT_BUS.register(Class.forName("ae2csl.verification.InputRegression")
                        .getConstructor().newInstance());
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        }
    }

    @SubscribeEvent
    public void serverTick(TickEvent.WorldTickEvent event) {
        if (!event.side.isServer() || event.phase != TickEvent.Phase.END || serverChecked) return;
        serverChecked = true;
        try {
            require(System.getProperty("ae2csl.verification.cleanroom", "0.4.0").equals(report.get("cleanroom")),
                    "Wrong Cleanroom test runtime");
            for (String name : new String[]{"ae2.tile.networking.TileCableBus", "ae2.me.GridNode",
                    "ae2.tile.misc.TileGrowthAccelerator", "ae2.tile.misc.TileCrank",
                    "ae2.crafting.execution.CraftingCpuLogic", "ae2.block.AEBaseTileBlock", "ae2.parts.AEBasePart"}) {
                Class.forName(name);
            }
            report.put("commonMixinTargetsLoaded", true);
            Method tick = method(AEBaseTile.class, new Class<?>[0], "func_73660_a", "update");
            report.put("tickMethod", tick.getName());
            ProbeBus bus = new ProbeBus();
            bus.normal = new ProbePart(item("resonating_pattern_provider_part"));
            bus.extended = new ProbeExtendedPart(item("extended_resonating_pattern_provider_part"));
            tick.invoke(bus);
            require(bus.lookups == 0, "A detached bus must not tick parts");
            setWorld(bus, event.world);
            bus.normal.calls = 0;
            bus.extended.calls = 0;
            for (int i = 0; i < 5; i++) tick.invoke(bus);
            require(bus.normal.calls == 5 && bus.extended.calls == 5 && bus.lookups == 30,
                    "Both provider variants must tick exactly once per host tick");
            report.put("serverHostTicks", 5);
            report.put("normalProviderTicks", bus.normal.calls);
            report.put("extendedProviderTicks", bus.extended.calls);
            bus.normal = null;
            bus.extended = null;
            tick.invoke(bus);
            report.put("emptyCableBusSafe", true);
            // The injected handler must also leave ordinary AE2 tiles alone.
            AEBaseTile ordinary = new AEBaseTile();
            setWorld(ordinary, event.world);
            tick.invoke(ordinary);
            report.put("ordinaryTileSafe", true);
            report.put("result", "PASS");
        } catch (Throwable error) {
            report.put("result", "FAIL");
            report.put("error", error.toString());
            error.printStackTrace();
        }
        save();
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (clientChecked || event.phase != TickEvent.Phase.END) return;
        try {
            // Reflection keeps client-only Minecraft types out of dedicated-server signatures.
            Class<?> type = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = method(type, new Class<?>[0], "func_71410_x", "getMinecraft").invoke(null);
            World world;
            try { world = (World) type.getField("field_71441_e").get(minecraft); }
            catch (NoSuchFieldException ignored) { world = (World) type.getField("world").get(minecraft); }
            if (world == null || !serverChecked) return;
            clientChecked = true;
            Class.forName("net.minecraft.entity.player.InventoryPlayer");
            ProbeBus bus = new ProbeBus();
            setWorld(bus, world);
            method(AEBaseTile.class, new Class<?>[0], "func_73660_a", "update").invoke(bus);
            require(bus.lookups == 0, "Client world must not run server provider logic");
            report.put("clientWorldGuard", true);
            report.put("clientMixinTargetLoaded", true);
        } catch (Throwable error) {
            clientChecked = true;
            report.put("result", "FAIL");
            report.put("clientError", error.toString());
            error.printStackTrace();
        }
        save();
    }

    private static IPartItem<?> item(String id) {
        Object item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("ae2csl", id));
        require(item instanceof IPartItem<?>, "Missing provider item: " + id);
        return (IPartItem<?>) item;
    }

    private static Method method(Class<?> owner, Class<?>[] parameters, String... names) throws Exception {
        for (String name : names) {
            try { return owner.getMethod(name, parameters); }
            catch (NoSuchMethodException ignored) { }
        }
        throw new NoSuchMethodException(owner.getName() + ": " + String.join(",", names));
    }

    private static void setWorld(TileEntity tile, World world) throws Exception {
        method(TileEntity.class, new Class<?>[]{World.class}, "func_145834_a", "setWorld").invoke(tile, world);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private synchronized void save() {
        try {
            Files.writeString(Path.of("ae2csl-runtime-regression.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
        } catch (Exception error) { throw new RuntimeException(error); }
    }

    public static final class ProbeBus extends TileCableBus {
        ProbePart normal;
        ProbeExtendedPart extended;
        int lookups;
        @Override public IPart getPart(EnumFacing side) {
            lookups++;
            return side == EnumFacing.NORTH ? normal : side == EnumFacing.SOUTH ? extended : null;
        }
        @Override public void clientTick() { }
    }

    public static final class ProbePart extends ResonatingPatternProviderPart {
        int calls;
        ProbePart(IPartItem<?> item) { super(item); }
        @Override public ResonatingPatternProviderLogic getResonatingLogic() {
            calls++;
            return super.getResonatingLogic();
        }
    }

    public static final class ProbeExtendedPart extends ExtendedResonatingPatternProviderPart {
        int calls;
        ProbeExtendedPart(IPartItem<?> item) { super(item); }
        @Override public ResonatingPatternProviderLogic getResonatingLogic() {
            calls++;
            return super.getResonatingLogic();
        }
    }
}
