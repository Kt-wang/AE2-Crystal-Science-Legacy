/*
 * AE2 Crystal Science - GuiFrequencyBandCreate
 * Create a new frequency band.
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
import ae2csl.container.ContainerFrequencyBandCreate;
import ae2csl.network.EnderBroadcasterActionPacket;
import ae2csl.network.FrequencyBandActionPacket;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GuiFrequencyBandCreate extends GuiBroadcastSubMenu {

    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/frequency_band_create_menu.png");
    private static final int TEXTURE_W = 256, TEXTURE_H = 256;
    private static final ResourceLocation BACK_TEX = new ResourceLocation("ae2", "textures/guis/icons/back.png");

    private static final int BTN_PUBLIC   = 0;
    private static final int BTN_MEMCARD  = 1;
    private static final int BTN_CONFIRM  = 2;
    private static final int BTN_BACK     = 3;

    private AE2CSTextField nameField;
    private AE2CSTextField passwordField;
    private boolean isPublic;
    private boolean allowMemoryCard;
    private AE2CSCheckbox publicBox;
    private AE2CSCheckbox memcardBox;

    public GuiFrequencyBandCreate(InventoryPlayer pi, TileEntity t) {
        super(new ContainerFrequencyBandCreate(pi, t), t);
        this.xSize = 176;
        this.ySize = 119;
    }

    @Override
    public void initGui() {
        super.initGui();
        nameField = new AE2CSTextField(0, fontRenderer, guiLeft + 10, guiTop + 20, 100, 12);
        nameField.setMaxStringLength(64);
        passwordField = new AE2CSTextField(1, fontRenderer, guiLeft + 10, guiTop + 40, 100, 12);
        passwordField.setMaxStringLength(64);

        publicBox = new AE2CSCheckbox(BTN_PUBLIC, guiLeft + 10, guiTop + 58, 156, 14, I18n.format("ae2csl.menu.frequency_band_create_menu.is_public"));
        publicBox.setSelected(isPublic);
        addButton(publicBox);
        memcardBox = new AE2CSCheckbox(BTN_MEMCARD, guiLeft + 10, guiTop + 74, 156, 14, I18n.format("ae2csl.menu.frequency_band_create_menu.allow_memory_card_copy"));
        memcardBox.setSelected(allowMemoryCard);
        addButton(memcardBox);
        addButton(new AE2CSButton(BTN_CONFIRM, guiLeft + 115, guiTop + 19, 52, 14, I18n.format("ae2csl.menu.frequency_band_create_menu.confirm")));
        addButton(new AE2CSIconButton(BTN_BACK, guiLeft + 132, guiTop - 5, backIcon(), Arrays.asList(I18n.format("ae2csl.gui.back"))));
    }

    private static IButtonIcon backIcon() {
        return () -> Blitter.texture(BACK_TEX, 16, 16).src(0, 0, 16, 16);
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        switch (btn.id) {
            case BTN_PUBLIC:
                isPublic = !isPublic;
                publicBox.setSelected(isPublic);
                break;
            case BTN_MEMCARD:
                allowMemoryCard = !allowMemoryCard;
                memcardBox.setSelected(allowMemoryCard);
                break;
            case BTN_CONFIRM:
                confirm();
                break;
            case BTN_BACK:
                navigate(EnderBroadcasterActionPacket.OPEN_MAIN);
                break;
        }
    }

    private void confirm() {
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) return;
        AE2CrystalScienceLegacy.NETWORK.sendToServer(
                FrequencyBandActionPacket.withCreate(tilePos, name.trim(), passwordField.getText(), isPublic, allowMemoryCard));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (nameField.isFocused()) {
            nameField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (passwordField.isFocused()) {
            passwordField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        nameField.mouseClicked(mx, my, button);
        passwordField.mouseClicked(mx, my, button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        nameField.drawTextBox();
        passwordField.drawTextBox();
        drawTextFieldPlaceholder(nameField, "ae2csl.menu.frequency_band_create_menu.input_band_name");
        drawTextFieldPlaceholder(passwordField, "ae2csl.menu.frequency_band_create_menu.input_password");
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
        fontRenderer.drawString(I18n.format("ae2csl.menu.title.frequency_band_create"), 8, 6, 4210752);
    }
}
