package ae2csl.item.tools;

import ae2csl.api.ids.AE2CSConstants;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = AE2CSConstants.MODID, value = Side.CLIENT)
public class LinkableToolTooltipEvents {
    private LinkableToolTooltipEvents() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        if (!ToolLinkableHandler.INSTANCE.canLink(stack)) return;
        int[] pos = LinkableTool.getLinkedPosition(stack);
        if (pos != null) {
            event.getToolTip().add(TextFormatting.GREEN + I18n.format("ae2csl.tooltip.tool.linked", pos[0], pos[1], pos[2], pos[3]));
        } else {
            event.getToolTip().add(TextFormatting.RED + I18n.format("ae2csl.tooltip.tool.unlinked"));
        }
    }
}
