/*
 * AE2 Crystal Science - GuiResonatingPatternConverter (1.12.2)
 * 对照源码 ResonatingPatternConverterGUI：转换按钮 + 1.21.1 样式 JSON。
 */
package ae2csl.client.gui;

import ae2.client.gui.AEBaseGui;
import ae2.client.gui.Icon;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.IconButton;
import ae2csl.container.ContainerResonatingPatternConverter;

import net.minecraft.entity.player.InventoryPlayer;

public class GuiResonatingPatternConverter extends AEBaseGui<ContainerResonatingPatternConverter> {

    private static final String STYLE_PATH = "/screens/resonating_pattern_converter_menu.json";

    private final IconButton converterButton;

    public GuiResonatingPatternConverter(InventoryPlayer playerInv) {
        super(new ContainerResonatingPatternConverter(playerInv), playerInv,
            GuiStyleManager.loadStyleDoc(STYLE_PATH));

        this.converterButton = new IconButton(() -> getContainer().onConverterPattern()) {
            @Override
            protected Icon getIcon() {
                return Icon.SCHEDULING_RANDOM;
            }
        };
        this.widgets.add("converter", converterButton);
    }
}
