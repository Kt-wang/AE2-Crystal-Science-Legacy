/*
 * AE2 Crystal Science - GuiEnderEmitterFrequencyBandLink
 * Link this emitter to a chosen band (password prompt). Emitter is always a receiver.
 */
package ae2csl.client.gui;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.client.gui.widgets.AE2CSTextField;
import ae2csl.container.ContainerEnderEmitterFrequencyBandLink;
import ae2csl.network.EnderEmitterActionPacket;
import ae2csl.tile.ender.TileEnderEmitter;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class GuiEnderEmitterFrequencyBandLink extends GuiBroadcastSubMenu {

    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/frequency_band_link_menu.png");
    private static final int TEXTURE_W = 256, TEXTURE_H = 256;

    private static final int BTN_CONFIRM = 0;
    private static final int BTN_BACK    = 1;

    /** Set by GuiEnderEmitterFrequencyBand before opening the link menu (client-side only). */
    public static String pendingBand;

    private AE2CSTextField passwordField;
    private String selectedBand = "";

    public GuiEnderEmitterFrequencyBandLink(InventoryPlayer pi, TileEntity t) {
        super(new ContainerEnderEmitterFrequencyBandLink(pi, t), t);
        this.xSize = 176;
        this.ySize = 64;
    }

    @Override
    public void initGui() {
        super.initGui();
        selectedBand = pendingBand != null ? pendingBand : "";
        pendingBand = null;

        passwordField = new AE2CSTextField(0, fontRenderer, guiLeft + 10, guiTop + 20, 100, 12);
        passwordField.setMaxStringLength(64);

        addButton(new GuiButton(BTN_CONFIRM, guiLeft + 115, guiTop + 19, 52, 14, I18n.format("ae2csl.menu.frequency_band_menu.confirm")));
        addButton(new GuiButton(BTN_BACK, guiLeft + 140, guiTop + 5, 30, 12, I18n.format("ae2csl.gui.back")));
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        switch (btn.id) {
            case BTN_CONFIRM:
                confirm();
                break;
            case BTN_BACK:
                AE2CrystalScienceLegacy.NETWORK.sendToServer(
                        new EnderEmitterActionPacket(tilePos, EnderEmitterActionPacket.OPEN_BAND_MENU));
                break;
        }
    }

    private void confirm() {
        if (selectedBand == null || selectedBand.isEmpty()) return;
        AE2CrystalScienceLegacy.NETWORK.sendToServer(
                new EnderEmitterActionPacket(tilePos, EnderEmitterActionPacket.CONNECT_BAND,
                        selectedBand, passwordField.getText()));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (passwordField.isFocused()) {
            passwordField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        passwordField.mouseClicked(mx, my, button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        passwordField.drawTextBox();
        drawTextFieldPlaceholder(passwordField, "ae2csl.menu.frequency_band_menu.input_password");
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mx, int my) {
        mc.getTextureManager().bindTexture(BG);
        drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize, TEXTURE_W, TEXTURE_H);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRenderer.drawString(I18n.format("ae2csl.menu.title.frequency_band_link"), 8, 6, 4210752);
        boolean connected = !selectedBand.isEmpty() && tile instanceof TileEnderEmitter
                && selectedBand.equals(((TileEnderEmitter) tile).getBandName());
        fontRenderer.drawString(I18n.format(connected
                ? "ae2csl.menu.frequency_band_menu.connected"
                : "ae2csl.menu.frequency_band_menu.not_connected"), 84, 6, 4210752);
    }
}
