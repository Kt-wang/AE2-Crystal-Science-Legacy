package ae2csl.verification;

import ae2csl.item.linker.ResonatingMemoryCardItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Synthetic Forge input events on the real client thread; never shipped in the mod. */
public final class InputRegression {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private boolean preflight;
    private long lastRequest = -1;

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            Class<?> type = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = method(type, new Class<?>[0], "func_71410_x", "getMinecraft").invoke(null);
            if (!preflight) {
                preflight = true;
                Map<String, Object> report = new LinkedHashMap<>();
                try {
                    for (int i = 0; i < 8; i++) MinecraftForge.EVENT_BUS.post(new InputEvent.KeyInputEvent());
                    report.put("result", "PASS");
                    report.put("events", 8);
                } catch (Throwable error) { failure(report, error); }
                save("ae2csl-input-preflight.json", report);
            }
            Path requestFile = Path.of("ae2csl-input-request.json");
            if (!Files.exists(requestFile)) return;
            JsonObject request = JSON.fromJson(Files.readString(requestFile), JsonObject.class);
            long id = request.get("id").getAsLong();
            if (id == lastRequest) return;
            lastRequest = id;
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", id);
            try {
                String action = request.get("action").getAsString();
                if (action.equals("key")) {
                    Field hit = field(type, "field_71476_x", "objectMouseOver");
                    Object previous = hit.get(minecraft);
                    try {
                        if (request.has("hitBlock") && request.get("hitBlock").getAsBoolean()) {
                            hit.set(minecraft, new RayTraceResult(new Vec3d(3.5, 4.5, 3.5), EnumFacing.NORTH,
                                    new BlockPos(3, 4, 3)));
                        }
                        for (int i = 0; i < 8; i++) MinecraftForge.EVENT_BUS.post(new InputEvent.KeyInputEvent());
                        response.put("events", 8);
                    } finally { hit.set(minecraft, previous); }
                } else if (action.equals("wheel")) {
                    MouseEvent mouse = new MouseEvent();
                    Field wheel = MouseEvent.class.getDeclaredField("dwheel");
                    wheel.setAccessible(true);
                    wheel.setInt(mouse, request.get("delta").getAsInt());
                    Field button = MouseEvent.class.getDeclaredField("button");
                    button.setAccessible(true);
                    button.setInt(mouse, -1);
                    response.put("canceled", MinecraftForge.EVENT_BUS.post(mouse));
                    // Universal Tweaks can forward mouse input as keyboard input too.
                    MinecraftForge.EVENT_BUS.post(new InputEvent.KeyInputEvent());
                    MinecraftForge.EVENT_BUS.post(new InputEvent.MouseInputEvent());
                } else if (action.equals("pollMouse")) {
                    // Public LWJGL 2 references must follow the loader's backend redirection.
                    response.put("wheel", org.lwjgl.input.Mouse.getEventDWheel());
                    response.put("x", org.lwjgl.input.Mouse.getEventX());
                    response.put("y", org.lwjgl.input.Mouse.getEventY());
                } else if (!action.equals("inspect")) {
                    throw new IllegalArgumentException(action);
                }
                Object player = field(type, "field_71439_g", "player").get(minecraft);
                if (player != null) {
                    Object inventory = field(player.getClass(), "field_71071_by", "inventory").get(player);
                    response.put("hotbar", field(inventory.getClass(), "field_70461_c", "currentItem").getInt(inventory));
                    ItemStack stack = (ItemStack) method(player.getClass(), new Class<?>[0],
                            "func_184614_ca", "getHeldItemMainhand").invoke(player);
                    Object item = method(ItemStack.class, new Class<?>[0], "func_77973_b", "getItem").invoke(stack);
                    if (item instanceof ResonatingMemoryCardItem card) response.put("cardSlot", card.getSelectedSlot(stack));
                }
                response.put("result", "PASS");
            } catch (Throwable error) { failure(response, error); }
            save("ae2csl-input-response.json", response);
        } catch (Throwable error) { error.printStackTrace(); }
    }

    private static Method method(Class<?> type, Class<?>[] parameters, String... names) throws Exception {
        for (String name : names) {
            try { return type.getMethod(name, parameters); } catch (NoSuchMethodException ignored) { }
        }
        throw new NoSuchMethodException(type.getName());
    }

    private static Field field(Class<?> type, String... names) throws Exception {
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            for (String name : names) {
                try {
                    Field field = owner.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) { }
            }
        }
        throw new NoSuchFieldException(type.getName());
    }

    private static void failure(Map<String, Object> response, Throwable error) {
        response.put("result", "FAIL");
        java.io.StringWriter stack = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(stack));
        response.put("error", stack.toString());
    }

    private static void save(String name, Map<String, Object> data) throws Exception {
        Path target = Path.of(name);
        Path temporary = target.resolveSibling(name + ".tmp");
        Files.writeString(temporary, JSON.toJson(data));
        Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
