package ae2csl;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = Tags.MOD_ID)
public class AE2CSConfig {

    @Config.Name("Ender Emitter Auto Area Factor")
    @Config.Comment("Controls auto-link search area. Final area = 16 * value.")
    @Config.RangeInt(min = 1, max = 100)
    public static int enderEmitterAutoAreaFactor = 2;

    @Config.Name("Resonating Drive Cell Count")
    @Config.Comment("Storage cell slots of the Resonating ME Drive. Rounded down to a multiple of 63 (one page). Default 630 = 10 pages x 63.")
    @Config.RangeInt(min = 63, max = 6300)
    public static int resonatingDriveCellCount = 630;

    @Config.Name("Crank Music Sound")
    @Config.Comment("Sound event played when a wooden crank successfully starts a turn. Set empty to disable. Any registered sound event id works, e.g. ae2csl:crank_music or minecraft:record_cat.")
    public static String crankMusicSound = "ae2csl:crank_music";

    @Config.Name("Crank Music Cooldown")
    @Config.Comment("Ticks before the crank music can trigger again. Default 2000 = 100s, matching the bundled track length. Set 0 to retrigger on every crank turn.")
    @Config.RangeInt(min = 0, max = 72000)
    public static int crankMusicCooldown = 2000;

    @Config.Name("Pure Crystal Burn Multiplier")
    @Config.Comment("Global multiplier for Pure Crystal energy generation (AE/t).")
    @Config.RangeDouble(min = 0.01, max = 100.0)
    public static double pureCrystalBurnMultiplier = 1.0;

    @Mod.EventBusSubscriber(modid = Tags.MOD_ID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Tags.MOD_ID)) {
                ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }
}
