/*
 * AE2 Crystal Science - Sound Registration
 */
package ae2csl.init;

import ae2csl.Tags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AE2CSSounds {

    public static final SoundEvent CRANK_MUSIC = create("crank_music");

    private static SoundEvent create(String name) {
        ResourceLocation id = new ResourceLocation(Tags.MOD_ID, name);
        return new SoundEvent(id).setRegistryName(id);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(CRANK_MUSIC);
    }
}
