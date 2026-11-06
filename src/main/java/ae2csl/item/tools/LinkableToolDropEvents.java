/*
 * AE2 Crystal Science - LinkableToolDropEvents (1.12.2)
 * Ported from 1.21.1 source
 *
 * Listens for mob kill drops and block break drops.
 * If the player is holding a LinkableTool, tries to insert drops into ME storage.
 */
package ae2csl.item.tools;

import ae2.api.config.Actionable;
import ae2.api.stacks.AEItemKey;
import ae2csl.api.ids.AE2CSConstants;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Iterator;

@Mod.EventBusSubscriber(modid = AE2CSConstants.MODID)
public final class LinkableToolDropEvents {

    private LinkableToolDropEvents() {}

    /**
     * Kill mobs: try to insert drops into ME; anything leftover drops normally.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().world.isRemote) return;

        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

        ItemStack tool = player.getHeldItemMainhand();
        if (!ToolLinkableHandler.INSTANCE.canLink(tool)) {
            tool = player.getHeldItemOffhand();
            if (!ToolLinkableHandler.INSTANCE.canLink(tool)) return;
        }

        tryInsertDrops(player, tool, event.getDrops());
    }

    /**
     * Break blocks: try to insert drops into ME.
     */
    @SubscribeEvent
    public static void onBlockDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.getWorld().isRemote) return;

        EntityPlayer player = event.getHarvester();
        if (player == null) return;

        ItemStack tool = player.getHeldItemMainhand();
        if (!ToolLinkableHandler.INSTANCE.canLink(tool)) return;

        tryInsertDropsItems(player, tool, event.getDrops().iterator());
    }

    /**
     * Try to insert each EntityItem drop into ME storage (living drops).
     */
    private static void tryInsertDrops(EntityPlayer player, ItemStack tool, java.util.List<EntityItem> drops) {
        Iterator<EntityItem> it = drops.iterator();
        while (it.hasNext()) {
            EntityItem entityItem = it.next();
            ItemStack stack = entityItem.getItem();
            if (stack.isEmpty()) continue;

            long amount = stack.getCount();
            if (amount <= 0) continue;

            AEItemKey key = AEItemKey.of(stack);
            if (key == null) continue;

            long inserted = ToolLinkableHandler.insert(player, tool, key, amount, Actionable.MODULATE);
            if (inserted <= 0) continue;

            long remaining = amount - inserted;
            if (remaining <= 0) {
                it.remove();
                entityItem.setDead();
            } else {
                stack.setCount((int) remaining);
                entityItem.setItem(stack);
            }
        }
    }

    /**
     * Try to insert each ItemStack drop into ME storage (block drops).
     */
    private static void tryInsertDropsItems(EntityPlayer player, ItemStack tool, Iterator<ItemStack> it) {
        while (it.hasNext()) {
            ItemStack stack = it.next();
            if (stack.isEmpty()) continue;

            long amount = stack.getCount();
            if (amount <= 0) continue;

            AEItemKey key = AEItemKey.of(stack);
            if (key == null) continue;

            long inserted = ToolLinkableHandler.insert(player, tool, key, amount, Actionable.MODULATE);
            if (inserted <= 0) continue;

            long remaining = amount - inserted;
            if (remaining <= 0) {
                it.remove();
            } else {
                stack.setCount((int) remaining);
            }
        }
    }
}
