/*
 * AE2 Crystal Science - GuiFrequencyBandLink
 * Link this broadcaster to a chosen band (password + sender/receiver selection).
 */
package ae2csl.client.gui;

import ae2.client.gui.style.Blitter;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.client.gui.icon.IButtonIcon;
import ae2csl.client.gui.widgets.AE2CSButton;
import ae2csl.client.gui.widgets.AE2CSCheckbox;
import ae2csl.client.gui.widgets.AE2CSIconButton;
import ae2csl.client.gui.widgets.AE2CSTextField;
import ae2csl.container.ContainerFrequencyBandLink;
import ae2csl.network.EnderBroadcasterActionPacket;
import ae2csl.network.FrequencyBandActionPacket;
import ae2csl.tile.ender.TileEnderBroadcaster;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GuiFrequencyBandLink extends GuiBroadcastSubMenu {

    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/frequency_band_link_menu.png");
    private static final int TEXTURE_W = 256, TEXTURE_H = 256;
    private static final ResourceLocation BACK_TEX = new ResourceLocation("ae2", "textures/guis/icons/back.png");

    private static final int BTN_SENDER   = 0;
    private static final int BTN_RECEIVER = 1;
    private static final int BTN_CONFIRM  = 2;
    private static final int BTN_BACK     = 3;

    /** Set by GuiFrequencyBand before opening the link menu (client-side only). */
    public static String pendingBand;

    private AE2CSTextField passwordField;
    private AE2CSCheckbox senderBox;
    private AE2CSCheckbox receiverBox;
    private String selectedBand = "";
    private boolean asSender = true;

    public GuiFrequencyBandLink(InventoryPlayer pi, TileEntity t) {
        super(new ContainerFrequencyBandLink(pi, t), t);
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

        senderBox = new AE2CSCheckbox(BTN_SENDER, guiLeft + 10, guiTop + 35, 67, 14, I18n.format("ae2csl.menu.frequency_band_menu.as_sender"));
        senderBox.setRadio(true);
        senderBox.setSelected(asSender);
        addButton(senderBox);
        receiverBox = new AE2CSCheckbox(BTN_RECEIVER, guiLeft + 85, guiTop + 35, 67, 14, I18n.format("ae2csl.menu.frequency_band_menu.as_receiver"));
        receiverBox.setRadio(true);
        receiverBox.setSelected(!asSender);
        addButton(receiverBox);

        addButton(new AE2CSButton(BTN_CONFIRM, guiLeft + 115, guiTop + 19, 52, 14, I18n.format("ae2csl.menu.frequency_band_menu.confirm")));
        addButton(new AE2CSIconButton(BTN_BACK, guiLeft + 132, guiTop - 5, backIcon(), Arrays.asList(I18n.format("ae2csl.gui.back"))));
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        switch (btn.id) {
            case BTN_SENDER:
                asSender = true;
                senderBox.setSelected(true);
                receiverBox.setSelected(false);
                break;
            case BTN_RECEIVER:
                asSender = false;
                senderBox.setSelected(false);
                receiverBox.setSelected(true);
                break;
            case BTN_CONFIRM:
                confirm();
                break;
            case BTN_BACK:
                navigate(EnderBroadcasterActionPacket.OPEN_BAND_VIEW);
                break;
        }
    }

    private void confirm() {
        if (selectedBand == null || selectedBand.isEmpty()) return;
        AE2CrystalScienceLegacy.NETWORK.sendToServer(
                FrequencyBandActionPacket.withLink(tilePos, selectedBand, passwordField.getText(), asSender));
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

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mx, int my) {
        mc.getTextureManager().bindTexture(BG);
        drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize, TEXTURE_W, TEXTURE_H);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRenderer.drawString(I18n.format("ae2csl.menu.title.frequency_band_link"), 8, 6, 4210752);
        boolean connected = !selectedBand.isEmpty() && tile instanceof TileEnderBroadcaster
                && selectedBand.equals(((TileEnderBroadcaster) tile).getBandName());
        fontRenderer.drawString(I18n.format(connected
                ? "ae2csl.menu.frequency_band_menu.connected"
                : "ae2csl.menu.frequency_band_menu.not_connected"), 84, 6, 4210752);
    }

    private static IButtonIcon backIcon() {
        return () -> Blitter.texture(BACK_TEX, 16, 16).src(0, 0, 16, 16);
    }
}
