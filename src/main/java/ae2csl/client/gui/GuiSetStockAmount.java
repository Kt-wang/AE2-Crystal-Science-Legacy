/*
 * AE2 Crystal Science - GuiSetStockAmount (1.12.2)
 * 移植自 fork 的 GuiSetStockAmount（容器类型换成本 port 的 ContainerSetStockAmount）。
 * 样式复用 AE2 自带的 /screens/set_stock_amount.json。
 */
package ae2csl.client.gui;

import ae2.client.gui.AEBaseGui;
import ae2.client.gui.NumberEntryType;
import ae2.client.gui.implementations.AESubGui;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.NumberEntryWidget;
import ae2.core.localization.GuiText;
import ae2csl.container.ContainerSetStockAmount;

import net.minecraft.entity.player.InventoryPlayer;

public class GuiSetStockAmount extends AEBaseGui<ContainerSetStockAmount> {

    private static final String STYLE_PATH = "/screens/set_stock_amount.json";

    private final NumberEntryWidget amount;
    private boolean amountInitialized;

    public GuiSetStockAmount(ContainerSetStockAmount container, InventoryPlayer playerInventory) {
        super(container, playerInventory, GuiStyleManager.loadStyleDoc(STYLE_PATH));

        widgets.addButton("save", GuiText.Set.text(), this::confirm);
        AESubGui.addBackButton(container, "back", widgets);

        this.amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.UNITLESS);
        this.amount.setLongValue(1);
        this.amount.setTextFieldStyle(this.style.getWidget("amountToStockInput"));
        this.amount.setPreviewFieldStyle(this.style.getWidget("amountToStockPreview"));
        this.amount.setMinValue(0);
        this.amount.setOnConfirm(this::confirm);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!this.amountInitialized) {
            var whatToStock = getContainer().getWhatToStock();
            if (whatToStock != null) {
                this.amount.setType(NumberEntryType.of(whatToStock));
                this.amount.setLongValue(getContainer().getInitialAmount());
                this.amount.setMaxValue(getContainer().getMaxAmount());
                this.amountInitialized = true;
            }
        }
    }

    private void confirm() {
        this.amount.getIntValue().ifPresent(getContainer()::confirm);
    }
}
