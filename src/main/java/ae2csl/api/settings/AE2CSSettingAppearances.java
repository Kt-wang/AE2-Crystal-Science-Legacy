/*
 * AE2 Crystal Science - API Setting Appearances
 * Ported from 1.21.1 source
 *
 * Registers visual appearances (icons, tooltips) for custom machine settings.
 */
package ae2csl.api.settings;

import ae2.api.config.RedstoneMode;
import ae2.api.config.Setting;
import ae2.recipes.entropy.EntropyMode;
import net.minecraft.item.Item;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.*;

public final class AE2CSSettingAppearances {

    private static final Map<AppearanceKey<?>, Appearance> APPEARANCES = new HashMap<>();

    static {
        // NOTE: Icons (IButtonIcon) are not yet ported - using null Item for now.
        // Icon references will be wired when the client GUI icon system is ported.

        // REDSTONE_CONTROLLED_NO_PULSE
        register(null, AE2CSSettings.REDSTONE_CONTROLLED_NO_PULSE, RedstoneMode.IGNORE,
                new TextComponentTranslation("ae2csl.machine_settings.redstone_controlled_no_pulse.title"),
                new TextComponentTranslation("ae2csl.machine_settings.redstone_controlled_no_pulse.ignore.desc"));
        register(null, AE2CSSettings.REDSTONE_CONTROLLED_NO_PULSE, RedstoneMode.LOW_SIGNAL,
                new TextComponentTranslation("ae2csl.machine_settings.redstone_controlled_no_pulse.title"),
                new TextComponentTranslation("ae2csl.machine_settings.redstone_controlled_no_pulse.low_signal.desc"));
        register(null, AE2CSSettings.REDSTONE_CONTROLLED_NO_PULSE, RedstoneMode.HIGH_SIGNAL,
                new TextComponentTranslation("ae2csl.machine_settings.redstone_controlled_no_pulse.title"),
                new TextComponentTranslation("ae2csl.machine_settings.redstone_controlled_no_pulse.high_signal.desc"));

        register(null, AE2CSSettings.SHOW_RANGE_MODE, ShowRangeMode.SHOW_RANGE,
                new TextComponentTranslation("ae2csl.machine_settings.show_range_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.show_range_mode.show.desc"));
        register(null, AE2CSSettings.SHOW_RANGE_MODE, ShowRangeMode.HIDE_RANGE,
                new TextComponentTranslation("ae2csl.machine_settings.show_range_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.show_range_mode.hide.desc"));

        register(null, AE2CSSettings.BLACK_LIST_MODE, BlackListMode.WHITELIST,
                new TextComponentTranslation("ae2csl.machine_settings.black_list_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.black_list_mode.white_list.desc"));
        register(null, AE2CSSettings.BLACK_LIST_MODE, BlackListMode.BLACKLIST,
                new TextComponentTranslation("ae2csl.machine_settings.black_list_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.black_list_mode.black_list.desc"));

        register(null, AE2CSSettings.PULL_MODE, PullMode.PULL_ON,
                new TextComponentTranslation("ae2csl.machine_settings.pull_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.pull_mode.on.desc"));
        register(null, AE2CSSettings.PULL_MODE, PullMode.PULL_OFF,
                new TextComponentTranslation("ae2csl.machine_settings.pull_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.pull_mode.off.desc"));

        register(null, AE2CSSettings.ENTROPY_CHANGE_MODE, EntropyMode.HEAT,
                new TextComponentTranslation("ae2csl.machine_settings.entropy_change_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.entropy_change_mode.increase.desc"));
        register(null, AE2CSSettings.ENTROPY_CHANGE_MODE, EntropyMode.COOL,
                new TextComponentTranslation("ae2csl.machine_settings.entropy_change_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.entropy_change_mode.decrease.desc"));

        register(null, AE2CSSettings.AUTO_LINK_MODE, AutoLinkMode.ENABLE,
                new TextComponentTranslation("ae2csl.machine_settings.auto_link_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.auto_link_mode.enable.desc"));
        register(null, AE2CSSettings.AUTO_LINK_MODE, AutoLinkMode.DISABLE,
                new TextComponentTranslation("ae2csl.machine_settings.auto_link_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.auto_link_mode.disable.desc"));

        register(null, AE2CSSettings.AUTO_LINK_CABLE_MODE, AutoLinkCableMode.ENABLE,
                new TextComponentTranslation("ae2csl.machine_settings.auto_link_cable_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.auto_link_cable_mode.enable.desc"));
        register(null, AE2CSSettings.AUTO_LINK_CABLE_MODE, AutoLinkCableMode.DISABLE,
                new TextComponentTranslation("ae2csl.machine_settings.auto_link_cable_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.auto_link_cable_mode.disable.desc"));

        register(null, AE2CSSettings.SOUND_MODE, SoundMode.MUTE,
                new TextComponentTranslation("ae2csl.machine_settings.sound_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.sound_mode.mute.desc"));
        register(null, AE2CSSettings.SOUND_MODE, SoundMode.UNMUTE,
                new TextComponentTranslation("ae2csl.machine_settings.sound_mode.title"),
                new TextComponentTranslation("ae2csl.machine_settings.sound_mode.unmute.desc"));
    }

    private AE2CSSettingAppearances() {}

    /**
     * Register an appearance with an IButtonIcon. The icon parameter is nullable
     * for deferred wiring - use the Item overload if you have an ItemLike.
     */
    public static <T extends Enum<T>> void register(@Nullable Object icon, Setting<T> setting, T val,
                                                     ITextComponent title, ITextComponent... extraLines) {
        List<ITextComponent> lines = new ArrayList<>(1 + (extraLines == null ? 0 : extraLines.length));
        lines.add(title);
        if (extraLines != null && extraLines.length > 0) {
            Collections.addAll(lines, extraLines);
        }

        APPEARANCES.put(new AppearanceKey<>(setting, val),
                new Appearance(null, List.copyOf(lines)));
    }

    /**
     * Register an appearance with an ItemLike for the icon.
     */
    public static <T extends Enum<T>> void register(Item item, Setting<T> setting, T val,
                                                     ITextComponent title, ITextComponent... extraLines) {
        List<ITextComponent> lines = new ArrayList<>(1 + (extraLines == null ? 0 : extraLines.length));
        lines.add(title);
        if (extraLines != null && extraLines.length > 0) {
            Collections.addAll(lines, extraLines);
        }

        APPEARANCES.put(new AppearanceKey<>(setting, val),
                new Appearance(item, List.copyOf(lines)));
    }

    @Nullable
    public static <T extends Enum<T>> Appearance getOrNull(Setting<T> setting, T value) {
        return APPEARANCES.get(new AppearanceKey<>(setting, value));
    }

    public static <T extends Enum<T>> Appearance getOrDefault(Setting<T> setting, T value, Appearance fallback) {
        Appearance a = getOrNull(setting, value);
        return a != null ? a : fallback;
    }

    public static void clear() {
        APPEARANCES.clear();
    }

    public static Map<AppearanceKey<?>, Appearance> view() {
        return Collections.unmodifiableMap(APPEARANCES);
    }

    /**
     * Key for the appearance map, using identity comparison on setting and value.
     */
    public static final class AppearanceKey<T extends Enum<T>> {
        private final Setting<T> setting;
        private final T value;

        public AppearanceKey(Setting<T> setting, T value) {
            this.setting = setting;
            this.value = value;
        }

        public Setting<T> getSetting() { return setting; }
        public T getValue() { return value; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof AppearanceKey<?> other)) return false;
            return this.setting == other.setting && this.value == other.value;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(setting) ^ System.identityHashCode(value);
        }
    }

    /**
     * Appearance data for a setting value.
     */
    public static final class Appearance {
        @Nullable private final Item item;
        private final List<ITextComponent> tooltipLines;

        public Appearance(@Nullable Item item, List<ITextComponent> tooltipLines) {
            this.item = item;
            this.tooltipLines = tooltipLines;
        }

        @Nullable
        public Item getItem() { return item; }

        public List<ITextComponent> getTooltipLines() { return tooltipLines; }
    }
}
