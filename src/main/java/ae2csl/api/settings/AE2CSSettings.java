/*
 * AE2 Crystal Science - API Settings Registration
 * Ported from 1.21.1 source
 *
 * Registers custom machine settings with AE2's setting system.
 */
package ae2csl.api.settings;

import ae2.api.config.RedstoneMode;
import ae2.api.config.Setting;
import ae2.recipes.entropy.EntropyMode;

import java.util.EnumSet;

public final class AE2CSSettings {

    public static final Setting<RedstoneMode> REDSTONE_CONTROLLED_NO_PULSE =
            new Setting<>("aecs_redstone_controlled_no_pulse", RedstoneMode.class,
                    EnumSet.of(RedstoneMode.IGNORE, RedstoneMode.HIGH_SIGNAL, RedstoneMode.LOW_SIGNAL));

    public static final Setting<ShowRangeMode> SHOW_RANGE_MODE =
            new Setting<>("aecs_show_range_mode", ShowRangeMode.class);

    public static final Setting<BlackListMode> BLACK_LIST_MODE =
            new Setting<>("aecs_black_list_mode", BlackListMode.class);

    public static final Setting<PullMode> PULL_MODE =
            new Setting<>("aecs_pull_mode", PullMode.class);

    public static final Setting<EntropyMode> ENTROPY_CHANGE_MODE =
            new Setting<>("aecs_entropy_change_mode", EntropyMode.class);

    public static final Setting<AutoLinkMode> AUTO_LINK_MODE =
            new Setting<>("aecs_auto_link_mode", AutoLinkMode.class);

    public static final Setting<AutoLinkCableMode> AUTO_LINK_CABLE_MODE =
            new Setting<>("aecs_auto_link_cable_mode", AutoLinkCableMode.class);

    public static final Setting<SoundMode> SOUND_MODE =
            new Setting<>("aecs_sound_mode", SoundMode.class);

    private AE2CSSettings() {}
}
