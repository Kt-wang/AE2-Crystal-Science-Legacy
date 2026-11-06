/*
 * AE2 Crystal Science - TileCrankMixin (1.12.2)
 * Plays a configurable sound when a crank turn successfully starts.
 * Injects after TileCrank.markForUpdate() inside power(), which is only
 * reached on the server when a turn actually begins (canTurn() == true).
 */
package ae2csl.mixin;

import ae2.tile.misc.TileCrank;
import ae2csl.AE2CSConfig;
import ae2csl.AE2CrystalScienceLegacy;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileCrank.class, remap = false)
public abstract class TileCrankMixin extends TileEntity {

    @Unique
    private long ae2cs$musicReadyAt = 0L;

    @Unique
    private static boolean ae2cs$warnedMissingSound = false;

    @Inject(method = "power",
            at = @At(value = "INVOKE", target = "Lae2/tile/misc/TileCrank;markForUpdate()V", shift = At.Shift.AFTER),
            remap = false)
    private void ae2cs$playCrankMusic(CallbackInfo ci) {
        String soundName = AE2CSConfig.crankMusicSound;
        if (soundName == null || soundName.trim().isEmpty()) {
            return;
        }
        if (this.getWorld() == null) {
            return;
        }

        long now = this.getWorld().getTotalWorldTime();
        if (now < this.ae2cs$musicReadyAt) {
            return;
        }

        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundName.trim()));
        if (sound == null) {
            if (!ae2cs$warnedMissingSound) {
                AE2CrystalScienceLegacy.LOGGER.warn("Crank music sound '{}' is not registered; check the config.", soundName);
                ae2cs$warnedMissingSound = true;
            }
            return;
        }

        this.getWorld().playSound(null,
                this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5,
                sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        this.ae2cs$musicReadyAt = now + Math.max(0, AE2CSConfig.crankMusicCooldown);
    }
}
