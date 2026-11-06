/*
 * AE2 Crystal Science - GuiEnderEmitter
 * 176×168 — functional controls use a vertical toolbar on the left, matching the
 * 1.21.1 source EnderEmitterGUI (addToLeftToolbar + icon distance buttons).
 */
package ae2csl.client.gui;

import ae2.client.gui.style.Blitter;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.client.gui.icon.AE2CSIcon;
import ae2csl.client.gui.icon.IButtonIcon;
import ae2csl.client.gui.widgets.AE2CSIconButton;
import ae2csl.container.ContainerEnderEmitter;
import ae2csl.network.EnderEmitterActionPacket;
import ae2csl.tile.ender.TileEnderEmitter;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GuiEnderEmitter extends GuiContainer {
    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/ender_emitter.png");
    private static final ResourceLocation CLEAR_TEX = new ResourceLocation("ae2", "textures/guis/icons/clear.png");
    private static final ResourceLocation OVERLAY_ON_TEX = new ResourceLocation("ae2", "textures/guis/icons/overlay_on.png");
    private static final ResourceLocation OVERLAY_OFF_TEX = new ResourceLocation("ae2", "textures/guis/icons/overlay_off.png");

    private final TileEnderEmitter tile;

    // Toggle buttons whose icon/tooltip reflect live tile state.
    private AE2CSIconButton showRangeButton;
    private AE2CSIconButton autoModeButton;
    private AE2CSIconButton autoLinkCableButton;

    // Button IDs
    private static final int BTN_DIST_PLUS        = 0;
    private static final int BTN_DIST_MINUS       = 1;
    private static final int BTN_SHOW_RANGE       = 2;
    private static final int BTN_AUTO_MODE        = 3;
    private static final int BTN_AUTO_LINK_CABLE  = 4;
    private static final int BTN_SCAN_ALL         = 5;
    private static final int BTN_DESTROY_ALL      = 6;
    private static final int BTN_BAND             = 7;
    private static final int BTN_CLEAN_BAND       = 8;

    // Distance buttons: JSON right=26 → x = 176 - 26 = 150 (add_distance_button top=16, reduce top=48)
    private static final int DIST_BTN_X  = 150;
    private static final int DIST_BTN_Y1 = 16;
    private static final int DIST_BTN_Y2 = 48;

    public GuiEnderEmitter(InventoryPlayer pi, TileEnderEmitter t) {
        super(new ContainerEnderEmitter(pi, t));
        this.tile = t;
        this.xSize = 176;
        this.ySize = 168;
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = guiLeft, y = guiTop;

        // Distance adjustment — icon buttons matching the source's add/reduce_distance_button widgets.
        addButton(new AE2CSIconButton(BTN_DIST_PLUS, x + DIST_BTN_X, y + DIST_BTN_Y1, AE2CSIcon.ADDITION_SIGN,
                tooltip("ae2csl.menu.ender_emitter.button.add_distance")));
        addButton(new AE2CSIconButton(BTN_DIST_MINUS, x + DIST_BTN_X, y + DIST_BTN_Y2, AE2CSIcon.SUBTRACTION_SIGN,
                tooltip("ae2csl.menu.ender_emitter.button.reduce_distance")));

        // Left toolbar buttons — matching the source GUI's addToLeftToolbar order.
        int tbX = x - 22;
        int tbY = y + 5;
        int tbStep = 22;
        addButton(showRangeButton = new AE2CSIconButton(BTN_SHOW_RANGE, tbX, tbY, showRangeIcon(), showRangeTooltip()));
        addButton(autoModeButton = new AE2CSIconButton(BTN_AUTO_MODE, tbX, tbY + tbStep, autoModeIcon(), autoModeTooltip()));
        addButton(autoLinkCableButton = new AE2CSIconButton(BTN_AUTO_LINK_CABLE, tbX, tbY + tbStep * 2,
                autoLinkCableIcon(), autoLinkCableTooltip()));
        addButton(new AE2CSIconButton(BTN_SCAN_ALL, tbX, tbY + tbStep * 3, AE2CSIcon.LINK_TO_ALL,
                tooltip("ae2csl.menu.ender_emitter.button.try_sacn_all")));
        addButton(new AE2CSIconButton(BTN_DESTROY_ALL, tbX, tbY + tbStep * 4, AE2CSIcon.BREAK_ALL_LINKS,
                tooltip("ae2csl.menu.ender_emitter.button.destroy_all")));
        addButton(new AE2CSIconButton(BTN_BAND, tbX, tbY + tbStep * 5, AE2CSIcon.BAND_VIEW,
                tooltip("ae2csl.menu.ender_emitter.button.open_frequency_view_menu")));
        addButton(new AE2CSIconButton(BTN_CLEAN_BAND, tbX, tbY + tbStep * 6, clearIcon(),
                tooltip("ae2csl.menu.ender_emitter.button.clean_band_connection")));
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        int mult = isShiftKeyDown() ? 5 : 1;
        switch (btn.id) {
            case BTN_DIST_PLUS:       sendAction(EnderEmitterActionPacket.CHANGE_DISTANCE, 1 * mult); break;
            case BTN_DIST_MINUS:      sendAction(EnderEmitterActionPacket.CHANGE_DISTANCE, -1 * mult); break;
            case BTN_SHOW_RANGE:      sendAction(EnderEmitterActionPacket.TOGGLE_SHOW_RANGE); break;
            case BTN_AUTO_MODE:       sendAction(EnderEmitterActionPacket.TOGGLE_AUTO_MODE); break;
            case BTN_AUTO_LINK_CABLE: sendAction(EnderEmitterActionPacket.TOGGLE_AUTO_LINK_CABLE); break;
            case BTN_SCAN_ALL:        sendAction(EnderEmitterActionPacket.SCAN_ALL); break;
            case BTN_DESTROY_ALL:     sendAction(EnderEmitterActionPacket.DESTROY_ALL); break;
            case BTN_BAND:            sendAction(EnderEmitterActionPacket.OPEN_BAND_MENU); break;
            case BTN_CLEAN_BAND:      sendAction(EnderEmitterActionPacket.CLEAN_BAND_CONNECTION); break;
        }
    }

    private void sendAction(byte action) {
        AE2CrystalScienceLegacy.NETWORK.sendToServer(new EnderEmitterActionPacket(tile.getPos(), action));
    }

    private void sendAction(byte action, int delta) {
        AE2CrystalScienceLegacy.NETWORK.sendToServer(new EnderEmitterActionPacket(tile.getPos(), action, delta));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        boolean hasBand = tile.isConnectedToBand();
        for (GuiButton btn : buttonList) {
            if (btn.id == BTN_CLEAN_BAND) {
                btn.visible = hasBand;
            }
        }
        if (showRangeButton != null) {
            showRangeButton.setIcon(showRangeIcon());
            showRangeButton.setTooltip(showRangeTooltip());
        }
        if (autoModeButton != null) {
            autoModeButton.setIcon(autoModeIcon());
            autoModeButton.setTooltip(autoModeTooltip());
        }
        if (autoLinkCableButton != null) {
            autoLinkCableButton.setIcon(autoLinkCableIcon());
            autoLinkCableButton.setTooltip(autoLinkCableTooltip());
        }
    }

    // ---- Toggle button icon/tooltip helpers ----
    private IButtonIcon showRangeIcon() {
        return tile.isShowLinkStatus() ? overlayOnIcon() : overlayOffIcon();
    }

    private List<String> showRangeTooltip() {
        return toggleTooltip("ae2csl.machine_settings.show_range_mode",
                tile.isShowLinkStatus() ? "show.desc" : "hide.desc");
    }

    private IButtonIcon autoModeIcon() {
        return tile.isAutoMode() ? AE2CSIcon.AUTO_LINK_ENABLE : AE2CSIcon.AUTO_LINK_DISABLE;
    }

    private List<String> autoModeTooltip() {
        return toggleTooltip("ae2csl.machine_settings.auto_link_mode",
                tile.isAutoMode() ? "enable.desc" : "disable.desc");
    }

    private IButtonIcon autoLinkCableIcon() {
        return tile.allowAutoLinkCableLike() ? AE2CSIcon.AUTO_LINK_CABLE_ENABLE : AE2CSIcon.AUTO_LINK_CABLE_DISABLE;
    }

    private List<String> autoLinkCableTooltip() {
        return toggleTooltip("ae2csl.machine_settings.auto_link_cable_mode",
                tile.allowAutoLinkCableLike() ? "enable.desc" : "disable.desc");
    }

    private static List<String> toggleTooltip(String keyPrefix, String descSuffix) {
        return Arrays.asList(
                I18n.format(keyPrefix + ".title"),
                I18n.format(keyPrefix + "." + descSuffix));
    }

    private static List<String> tooltip(String key) {
        return Arrays.asList(I18n.format(key).replace("\\n", "\n").split("\n", -1));
    }

    private static IButtonIcon overlayOnIcon() {
        return () -> Blitter.texture(OVERLAY_ON_TEX, 16, 16).src(0, 0, 16, 16);
    }

    private static IButtonIcon overlayOffIcon() {
        return () -> Blitter.texture(OVERLAY_OFF_TEX, 16, 16).src(0, 0, 16, 16);
    }

    private static IButtonIcon clearIcon() {
        return () -> Blitter.texture(CLEAR_TEX, 16, 16).src(0, 0, 16, 16);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(BG);
        // Main background: 176×168
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, 176, 168);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // dialog_title: left=8, top=6
        fontRenderer.drawString(I18n.format("tile.ender_emitter.name"), 8, 6, 4210752);

        // distance: left=10, top=62 (JSON says top=62)
        fontRenderer.drawString(I18n.format("ae2csl.gui.distance") + ": " + tile.getLinkDistance(), 10, 62, 4210752);

        if (tile.isConnectedToBand()) {
            // band_name: left=10, top=20
            fontRenderer.drawString(tile.getBandName(), 10, 20, 0x55FF55);
            // band_channels: left=10, top=34
            fontRenderer.drawString(I18n.format("ae2csl.gui.band_channels") + ": "
                    + tile.getBandUsedChannelsForClient() + "/" + tile.getBandTotalChannelsForClient(), 10, 34, 4210752);
        } else {
            // band_name: left=10, top=20 (shows "none")
            fontRenderer.drawString(I18n.format("ae2csl.gui.no_band"), 10, 20, 0xAAAAAA);
        }

        // emitter_channels: left=10, top=48
        fontRenderer.drawString(I18n.format("ae2csl.gui.emitter_channels") + ": " + tile.getUsedChannelsForClient(), 10, 48, 4210752);

        // max_distance
        fontRenderer.drawString(I18n.format("ae2csl.gui.max_distance") + ": " + tile.getMaxLinkDistanceForClient(), 10, 76, 4210752);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        for (GuiButton btn : buttonList) {
            if (btn instanceof AE2CSIconButton && btn.visible && btn.isMouseOver()) {
                List<String> tt = ((AE2CSIconButton) btn).getTooltip();
                if (tt != null && !tt.isEmpty()) {
                    drawHoveringText(tt, mouseX, mouseY);
                    break;
                }
            }
        }
    }
}
