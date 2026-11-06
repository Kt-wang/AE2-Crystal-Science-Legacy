/*
 * AE2 Crystal Science - GuiCrystalAggregator
 */
package ae2csl.client.gui;

import ae2csl.Tags;
import ae2csl.container.ContainerCrystalAggregator;
import ae2csl.tile.aggregator.TileCrystalAggregator;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiCrystalAggregator extends GuiContainer implements IUpgradePanelGui {
    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/crystal_aggregator.png");
    private final TileCrystalAggregator tile;

    public GuiCrystalAggregator(InventoryPlayer playerInv, TileCrystalAggregator tile) {
        super(new ContainerCrystalAggregator(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 178;
    }

    private static final int BTN_SIDE_CONFIG = 100;

    @Override
    public void initGui() {
        super.initGui();
        // 侧配按钮：左侧工具栏（对齐 1.21.1 源码 AECSSideConfigToggleButton）
        this.buttonList.add(new ae2csl.client.gui.widgets.AE2CSIconButton(BTN_SIDE_CONFIG,
            ae2csl.client.gui.AE2CSToolbar.iconButtonX(guiLeft), ae2csl.client.gui.AE2CSToolbar.buttonY(guiTop, 0),
            ae2csl.client.gui.icon.AE2CSIcon.SIDE_CONFIG,
            java.util.Arrays.asList(net.minecraft.client.resources.I18n.format("container.side_config"),
                net.minecraft.client.resources.I18n.format("gui.ae2csl.side_config.tooltip"))));
    }

    @Override
    protected void actionPerformed(net.minecraft.client.gui.GuiButton button) throws java.io.IOException {
        if (button.id == BTN_SIDE_CONFIG) {
            ae2csl.AE2CrystalScienceLegacy.NETWORK.sendToServer(
                new ae2csl.network.OpenGuiPacket(tile.getPos(), GuiHandler.GUI_SIDE_CONFIG));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BG);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        ae2csl.client.gui.AE2CSToolbar.draw(guiLeft, guiTop, 1);
        // Upgrade panel (ME Interface style, right edge)
        AE2CSUpgradePanel.draw(guiLeft, guiTop, xSize, ((ae2csl.container.ContainerCrystalAggregator) this.inventorySlots).getUpgradeSlots());

        // Progress bar
        int progress = tile.getField(0);
        int maxProgress = tile.getMaxProgress();
        if (maxProgress > 0 && progress > 0) {
            int w = (int)(progress / (double)maxProgress * 22);
            drawTexturedModalRect(guiLeft + 77, guiTop + 34, 199, 0, w, 33);
        }

        // Energy bar
        int energy = tile.getClientEnergyPct();
        if (energy > 0) {
            int h = energy * 18 / 100;
            drawTexturedModalRect(guiLeft + 132, guiTop + 40 + (18 - h), 176, 34, 6, h);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(tile.getDisplayName().getUnformattedText(), 8, 6, 0x404040);
        this.fontRenderer.drawString(net.minecraft.client.resources.I18n.format("container.inventory"), 8, 84, 0x404040);
    }

    @Override
    public java.awt.Rectangle getUpgradePanelArea() {
        return AE2CSUpgradePanel.getPanelBounds(guiLeft, guiTop, xSize, 4);
    }

    @Override
    protected void renderHoveredToolTip(int mx, int my) {
        super.renderHoveredToolTip(mx, my);
        net.minecraft.inventory.Slot hovered = AE2CSUpgradePanel.getHoveredUpgradeSlot(
            ((ae2csl.container.ContainerCrystalAggregator) this.inventorySlots).getUpgradeSlots(), guiLeft, guiTop, mx, my);
        if (hovered != null) {
            drawHoveringText(AE2CSUpgradePanel.getCompatibleUpgradesTooltip(true), mx, my);
        }
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        super.drawScreen(mx, my, pt);
        renderHoveredToolTip(mx, my);
        // 图标按钮 tooltip（侧配按钮等）
        for (net.minecraft.client.gui.GuiButton btn : buttonList) {
            if (btn instanceof ae2csl.client.gui.widgets.AE2CSIconButton && btn.visible && btn.isMouseOver()) {
                java.util.List<String> tt = ((ae2csl.client.gui.widgets.AE2CSIconButton) btn).getTooltip();
                if (tt != null && !tt.isEmpty()) { drawHoveringText(tt, mx, my); break; }
            }
        }
    }
}
