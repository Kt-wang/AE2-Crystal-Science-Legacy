/*
 * AE2 Crystal Science - GuiCircuitEtcher
 */
package ae2csl.client.gui;

import ae2csl.Tags;
import ae2csl.container.ContainerCircuitEtcher;
import ae2csl.tile.etcher.TileCircuitEtcher;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiCircuitEtcher extends GuiContainer implements IUpgradePanelGui {
    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/circuit_etcher.png");
    /** 进度箭头素材所在贴图（上游屏幕 JSON 引用的文件；BG 里 (199,0) 位置是坏的） */
    private static final ResourceLocation BG_SPRITES = new ResourceLocation(Tags.MOD_ID, "textures/gui/circuit_etcher_menu.png");
    private final TileCircuitEtcher tile;

    public GuiCircuitEtcher(InventoryPlayer playerInv, TileCircuitEtcher tile) {
        super(new ContainerCircuitEtcher(playerInv, tile));
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
        AE2CSUpgradePanel.draw(guiLeft, guiTop, xSize, ((ae2csl.container.ContainerCircuitEtcher) this.inventorySlots).getUpgradeSlots());

        // Progress bar：上游 FillMode 为 TOP_BOTTOM_TO_CENTER（上下两端向中间填充），
        // 素材在 circuit_etcher_menu.png 的 (199,0,22,33)
        int progress = tile.getClientProgressPct(); // 0-100（进度按能量累积，同步百分比）
        if (progress > 0) {
            this.mc.getTextureManager().bindTexture(BG_SPRITES);
            int fill = progress * 33 / 100;
            int top = (fill + 1) / 2, bottom = fill / 2;
            if (top > 0) drawTexturedModalRect(guiLeft + 77, guiTop + 34, 199, 0, 22, top);
            if (bottom > 0) drawTexturedModalRect(guiLeft + 77, guiTop + 34 + (33 - bottom), 199, 33 - bottom, 22, bottom);
            this.mc.getTextureManager().bindTexture(BG);
        }

        // Energy bar：固定整条紫色能量条（不做填充动画，用户要求与粉碎机截图一致）
        // 注意：升级面板绘制后绑的是面板贴图，箭头不画时不会重绑 BG，必须在这里重绑
        this.mc.getTextureManager().bindTexture(BG);
        drawTexturedModalRect(guiLeft + 132, guiTop + 40, 176, 34, 6, 18);
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
            ((ae2csl.container.ContainerCircuitEtcher) this.inventorySlots).getUpgradeSlots(), guiLeft, guiTop, mx, my);
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
