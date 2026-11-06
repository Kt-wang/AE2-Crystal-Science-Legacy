/*
 * AE2 Crystal Science - GuiEnderBroadcaster
 * 176×215 — functional controls use a vertical toolbar on the left.
 * Button positions match ender_broadcaster_menu.json widgets layout.
 */
package ae2csl.client.gui;

import ae2.client.gui.style.Blitter;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.client.gui.icon.AE2CSIcon;
import ae2csl.client.gui.icon.IButtonIcon;
import ae2csl.client.gui.widgets.AE2CSButton;
import ae2csl.client.gui.widgets.AE2CSIconButton;
import ae2csl.container.ContainerEnderBroadcaster;
import ae2csl.network.EnderBroadcasterActionPacket;
import ae2csl.tile.ender.TileEnderBroadcaster;
import ae2csl.tile.ender.TileEnderBroadcaster.ConnectionType;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GuiEnderBroadcaster extends GuiContainer {
    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/ender_broadcaster.png");
    private static final ResourceLocation CLEAR_TEX = new ResourceLocation("ae2", "textures/guis/icons/clear.png");
    private final TileEnderBroadcaster tile;

    private AE2CSIconButton switchButton;

    // Button IDs
    private static final int BTN_SWITCH      = 0;
    private static final int BTN_DISCONNECT  = 1;
    private static final int BTN_BAND_VIEW   = 2;
    private static final int BTN_BAND_CREATE = 3;
    private static final int BTN_BAND_MANAGE = 4;
    private static final int BTN_CH_PLUS10   = 5;
    private static final int BTN_CH_PLUS1    = 6;
    private static final int BTN_CH_MINUS1   = 7;
    private static final int BTN_CH_MINUS10  = 8;

    // JSON widget positions (relative to 176px bg):
    // reduce_receiver_expected_channels_button_10: left=10, top=90, w=26, h=20
    // reduce_receiver_expected_channels_button_1:  left=44, top=90, w=26, h=20
    // add_receiver_expected_channels_button_1:     right=70, top=90, w=26, h=20  → x=176-70-26=80
    // add_receiver_expected_channels_button_10:    right=36, top=90, w=26, h=20  → x=176-36-26=114
    private static final int CH_BTN_Y = 90;
    private static final int CH_BTN_W = 26;
    private static final int CH_BTN_H = 20;

    public GuiEnderBroadcaster(InventoryPlayer pi, TileEnderBroadcaster t) {
        super(new ContainerEnderBroadcaster(pi, t));
        this.tile = t;
        this.xSize = 176;
        this.ySize = 215;
    }

    @Override
    public void initGui() {
        super.initGui();
        int x = guiLeft, y = guiTop;

        // Channel adjustment buttons — match JSON widget positions
        // Order: -10, -1, +1, +10
        addButton(new AE2CSButton(BTN_CH_MINUS10, x + 10,        y + CH_BTN_Y, CH_BTN_W, CH_BTN_H, "-10"));
        addButton(new AE2CSButton(BTN_CH_MINUS1,  x + 44,        y + CH_BTN_Y, CH_BTN_W, CH_BTN_H, "-1"));
        addButton(new AE2CSButton(BTN_CH_PLUS1,   x + 80,        y + CH_BTN_Y, CH_BTN_W, CH_BTN_H, "+1"));
        addButton(new AE2CSButton(BTN_CH_PLUS10,  x + 114,       y + CH_BTN_Y, CH_BTN_W, CH_BTN_H, "+10"));

        // Left toolbar buttons — icon buttons matching the source GUI's addToLeftToolbar order.
        int tbX = x - 22;
        int tbY = y + 5;
        int tbStep = 22;
        addButton(new AE2CSIconButton(BTN_BAND_VIEW, tbX, tbY, AE2CSIcon.BAND_VIEW, tooltip("ae2csl.menu.ender_broadcaster.button.open_frequency_view_menu")));
        addButton(new AE2CSIconButton(BTN_BAND_CREATE, tbX, tbY + tbStep, AE2CSIcon.BAND_CREATE, tooltip("ae2csl.menu.ender_broadcaster.button.open_frequency_create_menu")));
        addButton(new AE2CSIconButton(BTN_BAND_MANAGE, tbX, tbY + tbStep * 2, AE2CSIcon.BAND_MANAGER, tooltip("ae2csl.menu.ender_broadcaster.button.open_frequency_manager_menu")));
        addButton(switchButton = new AE2CSIconButton(BTN_SWITCH, tbX, tbY + tbStep * 3, AE2CSIcon.RECEIVER_STATE, toggleTooltip()));
        addButton(new AE2CSIconButton(BTN_DISCONNECT, tbX, tbY + tbStep * 4, clearIcon(), tooltip("ae2csl.menu.ender_broadcaster.button.clean_linker_connection")));
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        BlockPos pos = tile.getPos();
        int mult = isShiftKeyDown() ? 5 : 1;
        switch (btn.id) {
            case BTN_CH_PLUS1:   sendAction(pos, EnderBroadcasterActionPacket.CHANGE_CHANNELS, 1 * mult); break;
            case BTN_CH_MINUS1:  sendAction(pos, EnderBroadcasterActionPacket.CHANGE_CHANNELS, -1 * mult); break;
            case BTN_CH_PLUS10:  sendAction(pos, EnderBroadcasterActionPacket.CHANGE_CHANNELS, 10 * mult); break;
            case BTN_CH_MINUS10: sendAction(pos, EnderBroadcasterActionPacket.CHANGE_CHANNELS, -10 * mult); break;
            case BTN_DISCONNECT: sendAction(pos, EnderBroadcasterActionPacket.DISCONNECT, 0); break;
            case BTN_SWITCH:     sendAction(pos, EnderBroadcasterActionPacket.TOGGLE_SIDE, 0); break;
            case BTN_BAND_VIEW:  sendAction(pos, EnderBroadcasterActionPacket.OPEN_BAND_VIEW, 0); break;
            case BTN_BAND_CREATE: sendAction(pos, EnderBroadcasterActionPacket.OPEN_BAND_CREATE, 0); break;
            case BTN_BAND_MANAGE: sendAction(pos, EnderBroadcasterActionPacket.OPEN_BAND_MANAGER, 0); break;
        }
    }

    private void sendAction(BlockPos pos, byte action, int delta) {
        AE2CrystalScienceLegacy.NETWORK.sendToServer(new EnderBroadcasterActionPacket(pos, action, delta));
    }

    private static List<String> tooltip(String key) {
        return Arrays.asList(I18n.format(key).replace("\\n", "\n").split("\n", -1));
    }

    private static List<String> toggleTooltip() {
        return Arrays.asList(
                I18n.format("ae2csl.menu.ender_broadcaster.button.toggle_band_side.title"),
                I18n.format("ae2csl.menu.ender_broadcaster.button.toggle_band_side.desc"));
    }

    private static IButtonIcon clearIcon() {
        return () -> Blitter.texture(CLEAR_TEX, 16, 16).src(0, 0, 16, 16);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // Show/hide channel buttons based on connection type (only receiver mode)
        boolean isReceiver = tile.getConnectionType() == ConnectionType.AS_RECEIVER;
        boolean isSender = tile.getConnectionType() == ConnectionType.AS_SENDER;
        boolean connected = tile.getConnectionType() != ConnectionType.NO_CONNECTION;
        for (GuiButton btn : buttonList) {
            switch (btn.id) {
                case BTN_CH_PLUS10: case BTN_CH_PLUS1:
                case BTN_CH_MINUS10: case BTN_CH_MINUS1:
                    btn.visible = isReceiver;
                    break;
                case BTN_BAND_MANAGE: case BTN_DISCONNECT: case BTN_SWITCH:
                    btn.visible = connected;
                    break;
            }
        }
        if (switchButton != null) {
            switchButton.setIcon(isSender ? AE2CSIcon.SENDER_STATE : AE2CSIcon.RECEIVER_STATE);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(BG);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, 176, 215);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("tile.ender_broadcaster.name");
        fontRenderer.drawString(title, 8, 6, 4210752);

        ConnectionType ct = tile.getConnectionType();
        String band = tile.getBandName();

        if (ct == ConnectionType.NO_CONNECTION || band.isEmpty()) {
            fontRenderer.drawString(TextFormatting.GRAY + I18n.format("ae2csl.gui.no_band"), 10, 28, 4210752);
            fontRenderer.drawString(I18n.format("ae2csl.gui.no_connection"), 10, 49, 4210752);
        } else {
            // band_name position: left=10, top=28
            fontRenderer.drawString(TextFormatting.WHITE + band, 10, 28, 4210752);
            // connect_status position: left=10, top=49
            String role = ct == ConnectionType.AS_SENDER
                    ? I18n.format("ae2csl.gui.sender")
                    : I18n.format("ae2csl.gui.receiver");
            fontRenderer.drawString(TextFormatting.GREEN + role, 10, 49, 4210752);

            if (ct == ConnectionType.AS_RECEIVER) {
                // receiver_actual_channels: left=10, top=69 (channels actually allocated by the band)
                int actual = tile.isEnabledCustomChannel() ? tile.getMaxChannels() : 0;
                fontRenderer.drawString(I18n.format("ae2csl.gui.receiver_actual_channels", actual), 10, 69, 4210752);
                // receiver_expected_channels_title + count: left=10, top=80 (count follows the title inline,
                // the source's centered count at (88,97) overlaps the +1 button, so render it inline instead)
                fontRenderer.drawString(I18n.format("ae2csl.gui.expected_channels") + tile.getExpectedChannels(), 10, 80, 4210752);
            }
            if (ct == ConnectionType.AS_SENDER) {
                // sender_sent_channels: left=10, top=69
                fontRenderer.drawString(I18n.format("ae2csl.gui.sent_channels", tile.getCouldSendChannels()), 10, 69, 4210752);
            }
        }
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
