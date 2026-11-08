/*
 * AE2 Crystal Science - GuiResonantTemplateCodingTerm (1.12.2)
 *
 * 复用 AE2 样板编码终端的界面。谐振编码默认开启；额外提供三个按钮：
 *  - pullInputsToGrid      把编码栏的输入从 ME 网络抽到终端（真实槽位）
 *  - clearPulledToNetwork  把抽取的物品退回网络
 *  - clearPulledToPlayer   把抽取的物品退回玩家物品栏
 */
package ae2csl.client.gui;

import ae2.client.gui.Icon;
import ae2.client.gui.me.patternencode.GuiPatternEncodingTerm;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.IconButton;
import ae2csl.container.ContainerResonantTemplateCodingTerm;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentTranslation;

public class GuiResonantTemplateCodingTerm extends GuiPatternEncodingTerm {

    private static final String STYLE_PATH = "/screens/terminals/resonant_coding_terminal.json";

    public GuiResonantTemplateCodingTerm(ContainerResonantTemplateCodingTerm container,
                                         InventoryPlayer playerInventory) {
        super(container, playerInventory, null, GuiStyleManager.loadStyleDoc(STYLE_PATH));

        this.widgets.add("pullInputsToGrid", new SimpleIconButton(Icon.S_ARROW_DOWN,
            () -> menu().pullInputs(),
            new TextComponentTranslation("ae2csl.gui.resonant_terminal.pull_inputs")));
        this.widgets.add("clearPulledToNetwork", new SimpleIconButton(Icon.CLEAR,
            () -> menu().clearToNetwork(),
            new TextComponentTranslation("ae2csl.gui.resonant_terminal.clear_to_network")));
        this.widgets.add("clearPulledToPlayer", new SimpleIconButton(Icon.S_ARROW_UP,
            () -> menu().clearToPlayer(),
            new TextComponentTranslation("ae2csl.gui.resonant_terminal.clear_to_player")));
    }

    private ContainerResonantTemplateCodingTerm menu() {
        return (ContainerResonantTemplateCodingTerm) getContainer();
    }

    private static class SimpleIconButton extends IconButton {

        private final Icon icon;

        SimpleIconButton(Icon icon, Runnable onPress, net.minecraft.util.text.ITextComponent tooltip) {
            super(onPress);
            this.icon = icon;
            this.setTooltipOn(java.util.List.of(tooltip));
        }

        @Override
        protected Icon getIcon() {
            return this.icon;
        }

        private void setTooltipOn(java.util.List<net.minecraft.util.text.ITextComponent> lines) {
            this.tooltipLines = lines;
        }

        private java.util.List<net.minecraft.util.text.ITextComponent> tooltipLines;

        @Override
        public java.util.List<net.minecraft.util.text.ITextComponent> getTooltipMessage() {
            return this.tooltipLines == null ? java.util.Collections.emptyList() : this.tooltipLines;
        }
    }
}
