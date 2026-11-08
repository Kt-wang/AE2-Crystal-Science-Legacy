/*
 * AE2 Crystal Science - WirelessResonantTerminalItem (1.12.2)
 *
 * 对照 1.21.1 源码：无线谐振终端 = AE2 无线样板编码终端 + 本模组的谐振编码。
 * 1.12.2 里直接继承 fork 的 WirelessTerminalItem，并注入自己的宿主/容器/界面工厂。
 */
package ae2csl.item.linker;

import ae2.container.GuiIds;
import ae2.items.tools.powered.WirelessTerminalItem;
import ae2csl.client.gui.GuiResonantTemplateCodingTerm;
import ae2csl.container.ContainerResonantTemplateCodingTerm;
import ae2csl.me.ResonantWirelessTerminalGuiHost;

import ae2.core.gui.locator.ItemGuiHostLocator;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.List;

public class WirelessResonantTerminalItem extends WirelessTerminalItem {

    private static final double POWER_CAPACITY = 1_600_000;

    public WirelessResonantTerminalItem() {
        super(POWER_CAPACITY,
            "wireless_resonant_terminal",
            GuiIds.GuiKey.WIRELESS_PATTERN_ENCODING_TERMINAL,
            ItemStack::new,
            ResonantWirelessTerminalGuiHost::new,
            (definition, inventory, host) ->
                host instanceof ResonantWirelessTerminalGuiHost resonantHost
                    ? new ContainerResonantTemplateCodingTerm(inventory, resonantHost, resonantHost,
                        GuiIds.GuiKey.WIRELESS_PATTERN_ENCODING_TERMINAL)
                    : null,
            (definition, container, inventory) ->
                container instanceof ContainerResonantTemplateCodingTerm resonantContainer
                    ? new GuiResonantTemplateCodingTerm(resonantContainer, inventory)
                    : null,
            "wireless_resonant_terminal",
            2,
            true);
        setTranslationKey("wireless_resonant_terminal");
    }

    /**
     * 没有能量时按「未连接」处理：不开界面，并提示充电。
     * AE2 默认只检查手持物是不是自己，所以无电也会打开（能看到库存、但取不出东西）。
     */
    @Override
    protected boolean checkPreconditions(ItemStack item) {
        return super.checkPreconditions(item) && hasStoredPower(item);
    }

    private boolean hasStoredPower(ItemStack stack) {
        return getAECurrentPower(stack) > 0.0D;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World level, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!level.isRemote && !hasStoredPower(stack)) {
            player.sendStatusMessage(new TextComponentTranslation(
                "ae2csl.msg.wireless_resonant_terminal.no_power"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        return super.onItemRightClick(level, player, hand);
    }

    @Override
    protected boolean openFromInventory(EntityPlayer player, ItemGuiHostLocator locator,
                                        boolean returningFromSubmenu) {
        ItemStack stack = locator.locateItem(player);
        if (!player.world.isRemote && !stack.isEmpty() && !hasStoredPower(stack)) {
            player.sendStatusMessage(new TextComponentTranslation(
                "ae2csl.msg.wireless_resonant_terminal.no_power"), true);
            return false;
        }
        return super.openFromInventory(player, locator, returningFromSubmenu);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void addCheckedInformation(ItemStack stack, World worldIn, List tooltip, ITooltipFlag flagIn) {
        super.addCheckedInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format(
            "ae2csl.item.wireless_resonant_terminal.tooltip"));
    }
}
