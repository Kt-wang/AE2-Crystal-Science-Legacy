/*
 * AE2 Crystal Science - EnderEmitterLinkEvents (1.12.2)
 * Ported from 1.21.1 EnderEmitterBlockEntity#onBlockPlace/onBlockBreak.
 *
 * Proactive auto-link: when any in-world grid node host block (machine, cable bus)
 * is placed near an ender emitter, add it to that emitter's pending link list; when
 * it is broken, remove it. This is what makes the emitter "主动链接范围内的机器".
 */
package ae2csl.tile.ender;

import ae2csl.api.ids.AE2CSConstants;

import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = AE2CSConstants.MODID)
public final class EnderEmitterLinkEvents {

    private EnderEmitterLinkEvents() {}

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getWorld().isRemote) return;
        TileEnderEmitter.addPosToRecentEmitter(event.getWorld(), event.getPos());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote) return;
        TileEnderEmitter.removePosFromRecentEmitter(event.getWorld(), event.getPos());
    }
}
