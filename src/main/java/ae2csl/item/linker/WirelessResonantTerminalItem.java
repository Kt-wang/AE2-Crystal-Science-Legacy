package ae2csl.item.linker;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;

import java.util.List;

public class WirelessResonantTerminalItem extends Item {
    public WirelessResonantTerminalItem() {
        setTranslationKey("wireless_resonant_terminal");
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format("ae2csl.item.wireless_resonant_terminal.tooltip"));
    }
}
