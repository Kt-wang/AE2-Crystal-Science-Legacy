/*
 * AE2 Crystal Science - GuiEntropyVariationChamber
 */
package ae2csl.client.gui;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.container.ContainerEntropyVariationChamber;
import ae2csl.network.EntropyModeTogglePacket;
import ae2csl.tile.entropy.TileEntropyVariationChamber;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class GuiEntropyVariationChamber extends GuiContainer implements IUpgradePanelGui {
    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/entropy_variation_reaction_chamber_menu.png");
    private static final int TANK_CAPACITY = 8000;
    // Source layout: fluidInput (8,20) / fluidOutput (150,20), 18x60 tanks
    private static final int TANK_LEFT_X = 8;
    private static final int TANK_RIGHT_X = 150;
    private static final int TANK_TOP = 20;
    private static final int TANK_WIDTH = 18;
    private static final int TANK_HEIGHT = 60;

    private final TileEntropyVariationChamber tile;
    private ae2csl.client.gui.widgets.AE2CSIconButton modeButton;

    public GuiEntropyVariationChamber(InventoryPlayer playerInv, TileEntropyVariationChamber tile) {
        super(new ContainerEntropyVariationChamber(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 178;
    }

    @Override
    public void initGui() {
        super.initGui();
        // Mode toggle: icon button on the left edge (source: left toolbar icon button)
        boolean heat = tile.getField(2) == 0;
        this.modeButton = new ae2csl.client.gui.widgets.AE2CSIconButton(0,
            ae2csl.client.gui.AE2CSToolbar.iconButtonX(guiLeft), ae2csl.client.gui.AE2CSToolbar.buttonY(guiTop, 0),
            heat ? ae2csl.client.gui.icon.AE2CSIcon.ENTROPY_INCREASE : ae2csl.client.gui.icon.AE2CSIcon.ENTROPY_DECREASE,
            java.util.Arrays.asList(
                I18n.format("ae2csl.machine_settings.entropy_change_mode.title"),
                I18n.format(heat ? "ae2csl.machine_settings.entropy_change_mode.increase.desc"
                                 : "ae2csl.machine_settings.entropy_change_mode.decrease.desc")));
        this.buttonList.add(modeButton);
        // 侧配按钮：左侧工具栏（对齐 1.21.1 源码 AECSSideConfigToggleButton）
        this.buttonList.add(new ae2csl.client.gui.widgets.AE2CSIconButton(1,
            ae2csl.client.gui.AE2CSToolbar.iconButtonX(guiLeft), ae2csl.client.gui.AE2CSToolbar.buttonY(guiTop, 1),
            ae2csl.client.gui.icon.AE2CSIcon.SIDE_CONFIG,
            java.util.Arrays.asList(
                I18n.format("container.side_config"),
                I18n.format("gui.ae2csl.side_config.tooltip"))));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            AE2CrystalScienceLegacy.NETWORK.sendToServer(new EntropyModeTogglePacket(tile.getPos()));
        } else if (button.id == 1) {
            AE2CrystalScienceLegacy.NETWORK.sendToServer(
                new ae2csl.network.OpenGuiPacket(tile.getPos(), GuiHandler.GUI_SIDE_CONFIG));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BG);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        ae2csl.client.gui.AE2CSToolbar.draw(guiLeft, guiTop, 2);
        // Upgrade panel (ME Interface style, right edge)
        AE2CSUpgradePanel.draw(guiLeft, guiTop, xSize, ((ae2csl.container.ContainerEntropyVariationChamber) this.inventorySlots).getUpgradeSlots());

        // Progress bar（进度按能量累积，同步百分比 0-100）
        int progress = tile.getClientProgressPct();
        if (progress > 0) {
            int w = progress * 16 / 100;
            drawTexturedModalRect(guiLeft + 75, guiTop + 43, 179, 18, w, 15);
        }

        // Energy bar (percentage, 0-100)
        int energyPct = tile.getField(1);
        if (energyPct > 0) {
            int h = (int)(energyPct / 100.0 * 18);
            drawTexturedModalRect(guiLeft + 135, guiTop + 40 + (18 - h), 176, 34, 6, h);
        }

        // Input fluid tank (water/lava), frame comes from the background texture
        int inAmount = tile.getField(3);
        int inType = tile.getField(4);
        if (inAmount > 0) {
            int h = Math.max(1, (int)((double)inAmount / TANK_CAPACITY * (TANK_HEIGHT - 2)));
            int color = inType == 2 ? 0xFFFF6600 : 0xFF3F76E4;
            drawRect(guiLeft + TANK_LEFT_X + 1, guiTop + TANK_TOP + TANK_HEIGHT - 1 - h,
                    guiLeft + TANK_LEFT_X + TANK_WIDTH - 1, guiTop + TANK_TOP + TANK_HEIGHT - 1, color);
        }

        // Output fluid tank (water)
        int outAmount = tile.getField(5);
        if (outAmount > 0) {
            int h = Math.max(1, (int)((double)outAmount / TANK_CAPACITY * (TANK_HEIGHT - 2)));
            drawRect(guiLeft + TANK_RIGHT_X + 1, guiTop + TANK_TOP + TANK_HEIGHT - 1 - h,
                    guiLeft + TANK_RIGHT_X + TANK_WIDTH - 1, guiTop + TANK_TOP + TANK_HEIGHT - 1, 0xFF3F76E4);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(tile.getDisplayName().getUnformattedText(), 8, 6, 0x404040);
        this.fontRenderer.drawString(net.minecraft.client.resources.I18n.format("container.inventory"), 8, 82, 0x404040);
    }

    @Override
    public java.awt.Rectangle getUpgradePanelArea() {
        return AE2CSUpgradePanel.getPanelBounds(guiLeft, guiTop, xSize, 4);
    }

    @Override
    protected void renderHoveredToolTip(int mx, int my) {
        super.renderHoveredToolTip(mx, my);
        net.minecraft.inventory.Slot hovered = AE2CSUpgradePanel.getHoveredUpgradeSlot(
            ((ae2csl.container.ContainerEntropyVariationChamber) this.inventorySlots).getUpgradeSlots(), guiLeft, guiTop, mx, my);
        if (hovered != null) {
            drawHoveringText(AE2CSUpgradePanel.getCompatibleUpgradesTooltip(true), mx, my);
            return;
        }

        // Fluid tank tooltips
        drawTankTooltip(mx, my, TANK_LEFT_X, tile.getField(3), tile.getField(4));
        drawTankTooltip(mx, my, TANK_RIGHT_X, tile.getField(5), 1);
    }

    private void drawTankTooltip(int mx, int my, int tankX, int amount, int type) {
        if (mx < guiLeft + tankX || mx >= guiLeft + tankX + TANK_WIDTH
                || my < guiTop + TANK_TOP || my >= guiTop + TANK_TOP + TANK_HEIGHT) return;
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(I18n.format(type == 2 ? "gui.ae2csl.entropy.fluid.lava" : "gui.ae2csl.entropy.fluid.water"));
        lines.add(I18n.format("gui.ae2csl.entropy.fluid.amount", amount, TANK_CAPACITY));
        drawHoveringText(lines, mx, my);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        if (modeButton != null) {
            boolean heat = tile.getField(2) == 0;
            modeButton.setIcon(heat ? ae2csl.client.gui.icon.AE2CSIcon.ENTROPY_INCREASE
                                    : ae2csl.client.gui.icon.AE2CSIcon.ENTROPY_DECREASE);
            modeButton.setTooltip(java.util.Arrays.asList(
                I18n.format("ae2csl.machine_settings.entropy_change_mode.title"),
                I18n.format(heat ? "ae2csl.machine_settings.entropy_change_mode.increase.desc"
                                 : "ae2csl.machine_settings.entropy_change_mode.decrease.desc")));
        }
        super.drawScreen(mx, my, pt);
        renderHoveredToolTip(mx, my);
        // Icon button tooltips（模式切换 / 侧配按钮）
        for (GuiButton btn : buttonList) {
            if (btn instanceof ae2csl.client.gui.widgets.AE2CSIconButton && btn.visible && btn.isMouseOver()) {
                java.util.List<String> tt = ((ae2csl.client.gui.widgets.AE2CSIconButton) btn).getTooltip();
                if (tt != null && !tt.isEmpty()) { drawHoveringText(tt, mx, my); break; }
            }
        }
    }
}
