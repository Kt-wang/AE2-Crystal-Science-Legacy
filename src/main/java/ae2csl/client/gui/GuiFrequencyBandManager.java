/*
 * AE2 Crystal Science - GuiFrequencyBandManager
 * Manage a single band: view senders/receivers, change password/public/memory-card,
 * open whitelist, delete the band.
 */
package ae2csl.client.gui;

import ae2.client.gui.style.Blitter;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.api.linker.broadcast.BroadcastFrequencyBand;
import ae2csl.api.linker.broadcast.networking.FrequencyBandDetailInfo;
import ae2csl.api.util.GlobalPos;
import ae2csl.client.gui.icon.AE2CSIcon;
import ae2csl.client.gui.icon.IButtonIcon;
import ae2csl.client.gui.widgets.AE2CSButton;
import ae2csl.client.gui.widgets.AE2CSIconButton;
import ae2csl.client.gui.widgets.AE2CSTextField;
import ae2csl.container.ContainerFrequencyBandManager;
import ae2csl.network.EnderBroadcasterActionPacket;
import ae2csl.network.FrequencyBandActionPacket;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiFrequencyBandManager extends GuiBroadcastSubMenu {

    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/frequency_band_manager_menu.png");
    private static final int TEXTURE_W = 512, TEXTURE_H = 512;
    private static final ResourceLocation LOCKED_TEX = new ResourceLocation("ae2", "textures/guis/icons/locked.png");
    private static final ResourceLocation UNLOCKED_TEX = new ResourceLocation("ae2", "textures/guis/icons/unlocked.png");
    private static final ResourceLocation CLEAR_TEX = new ResourceLocation("ae2", "textures/guis/icons/clear.png");
    private static final ResourceLocation BACK_TEX = new ResourceLocation("ae2", "textures/guis/icons/back.png");

    private static final int LIST_X = 9, LIST_Y = 21, LIST_W = 158, LIST_H = 119;
    private static final int ROW_H = 17;
    private static final int DELETE_AREA_X = 142; // right 16px of each 158-wide row (source DELETE_AREA_BOUND)

    private static final int BTN_SET_PW   = 0;
    private static final int BTN_PUBLIC   = 1;
    private static final int BTN_MEMCARD  = 2;
    private static final int BTN_WHITELIST = 3;
    private static final int BTN_DELETE   = 4;
    private static final int BTN_BACK     = 5;

    private static class Entry {
        final GlobalPos pos;
        final boolean sender;
        Entry(GlobalPos pos, boolean sender) { this.pos = pos; this.sender = sender; }
    }

    private AE2CSTextField passwordField;
    private AE2CSIconButton publicToggle;
    private AE2CSIconButton memcardToggle;
    private AE2CSIconButton deleteButton;
    private final List<Entry> entries = new ArrayList<>();
    private String bandName = "";
    private boolean isPublic;
    private boolean allowMemoryCard;
    private BroadcastFrequencyBand.BandError errorState = BroadcastFrequencyBand.BandError.NO_SENDER;
    private long usableChannels;
    private long usedChannels;
    private int scroll;
    private boolean removeArmed;
    private int removeTicks;

    public GuiFrequencyBandManager(InventoryPlayer pi, TileEntity t) {
        super(new ContainerFrequencyBandManager(pi, t), t);
        this.xSize = 195;
        this.ySize = 226;
    }

    @Override
    public void initGui() {
        super.initGui();
        passwordField = new AE2CSTextField(0, fontRenderer, guiLeft + 8, guiTop + 155, 89, 12);
        passwordField.setMaxStringLength(64);

        addButton(new AE2CSButton(BTN_SET_PW, guiLeft + 100, guiTop + 154, 72, 14, I18n.format("ae2csl.menu.frequency_manager_menu.confirm_password_button")));
        addButton(new AE2CSIconButton(BTN_BACK, guiLeft + 151, guiTop - 5, backIcon(), Arrays.asList(I18n.format("ae2csl.gui.back"))));

        // Left toolbar (matches source FrequencyBandManagerGUI.addToLeftToolbar order)
        int tbX = guiLeft - 22;
        int tbY = guiTop + 5;
        publicToggle = new AE2CSIconButton(BTN_PUBLIC, tbX, tbY, unlockedIcon(), toggleTooltip("ae2csl.menu.frequency_manager.button.change_public"));
        addButton(publicToggle);
        memcardToggle = new AE2CSIconButton(BTN_MEMCARD, tbX, tbY + 22, AE2CSIcon.ALLOW_MEMORY_CARD, toggleTooltip("ae2csl.menu.frequency_manager.button.change_allow_memory_card"));
        addButton(memcardToggle);
        addButton(new AE2CSIconButton(BTN_WHITELIST, tbX, tbY + 44, AE2CSIcon.WHITE_LIST_MODE, tooltip("ae2csl.menu.frequency_manager.button.open_white_list_manager")));
        deleteButton = new AE2CSIconButton(BTN_DELETE, tbX, tbY + 66, clearIcon(), tooltip("ae2csl.menu.frequency_manager.button.remove_band"));
        addButton(deleteButton);

        requestData(FrequencyBandActionPacket.REQUEST_BAND_DETAIL);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        passwordField.updateCursorCounter();
        if (removeArmed && --removeTicks <= 0) {
            removeArmed = false;
            if (deleteButton != null) {
                deleteButton.setArmed(false);
                deleteButton.setTooltip(tooltip("ae2csl.menu.frequency_manager.button.remove_band"));
            }
        }
        if (publicToggle != null) {
            publicToggle.setIcon(isPublic ? unlockedIcon() : lockedIcon());
            memcardToggle.setIcon(allowMemoryCard ? AE2CSIcon.ALLOW_MEMORY_CARD : AE2CSIcon.DENY_MEMORY_CARD);
        }
        if (tick % 10 == 0) {
            requestData(FrequencyBandActionPacket.REQUEST_BAND_DETAIL);
        }
    }

    public void acceptDetail(FrequencyBandDetailInfo detail, long usable, long used) {
        if (detail == null) return;
        bandName = detail.getName();
        isPublic = detail.isPublic();
        allowMemoryCard = detail.isAllowedMemoryCardCopy();
        errorState = detail.getErrorState();
        usableChannels = usable;
        usedChannels = used;

        entries.clear();
        if (detail.getSenderList() != null) {
            for (GlobalPos p : detail.getSenderList()) entries.add(new Entry(p, true));
        }
        if (detail.getReceiverList() != null) {
            for (GlobalPos p : detail.getReceiverList()) entries.add(new Entry(p, false));
        }
        int maxScroll = Math.max(0, entries.size() - visibleRows());
        scroll = Math.min(scroll, maxScroll);
    }

    private int visibleRows() { return Math.max(1, LIST_H / ROW_H); }

    private boolean inListArea(int absX, int absY) {
        int x = absX - guiLeft, y = absY - guiTop;
        return x >= LIST_X && x < LIST_X + LIST_W && y >= LIST_Y && y < LIST_Y + LIST_H;
    }

    private int rowAt(int absX, int absY) {
        if (!inListArea(absX, absY)) return -1;
        return (absY - guiTop - LIST_Y) / ROW_H;
    }

    private boolean inDeleteArea(int absX) {
        int x = absX - guiLeft;
        return x >= LIST_X + DELETE_AREA_X && x < LIST_X + LIST_W;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (inListArea(mx, my)) {
            int maxScroll = Math.max(0, entries.size() - visibleRows());
            scroll = Math.max(0, Math.min(maxScroll, scroll + (d > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        passwordField.mouseClicked(mx, my, button);
        if (button != 0) return; // left click only (matches source BroadcasterPanel.onClick)
        int rel = rowAt(mx, my);
        if (rel < 0) return;
        int idx = scroll + rel;
        if (idx < 0 || idx >= entries.size()) return;
        Entry e = entries.get(idx);
        // Source: clicking the delete "X" area (right 16px of the row) disconnects that
        // broadcaster; clicking anywhere else on the row teleports (tap) to it. We previously
        // mapped left/right click to disconnect/tap, so a plain left click disconnected the row.
        if (inDeleteArea(mx)) {
            AE2CrystalScienceLegacy.NETWORK.sendToServer(FrequencyBandActionPacket.withTarget(
                    tilePos, FrequencyBandActionPacket.DISCONNECT_BROADCASTER, e.pos.getDimension(), e.pos.getPos()));
        } else {
            AE2CrystalScienceLegacy.NETWORK.sendToServer(FrequencyBandActionPacket.withTarget(
                    tilePos, FrequencyBandActionPacket.TAP_BROADCASTER, e.pos.getDimension(), e.pos.getPos()));
        }
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        switch (btn.id) {
            case BTN_SET_PW:
                AE2CrystalScienceLegacy.NETWORK.sendToServer(FrequencyBandActionPacket.withStrings(
                        tilePos, FrequencyBandActionPacket.CHANGE_PASSWORD, passwordField.getText(), ""));
                passwordField.setText("");
                break;
            case BTN_PUBLIC:
                AE2CrystalScienceLegacy.NETWORK.sendToServer(FrequencyBandActionPacket.withFlag(
                        tilePos, FrequencyBandActionPacket.CHANGE_PUBLIC, !isPublic));
                requestData(FrequencyBandActionPacket.REQUEST_BAND_DETAIL);
                break;
            case BTN_MEMCARD:
                AE2CrystalScienceLegacy.NETWORK.sendToServer(FrequencyBandActionPacket.withFlag(
                        tilePos, FrequencyBandActionPacket.CHANGE_MEMORY_CARD, !allowMemoryCard));
                requestData(FrequencyBandActionPacket.REQUEST_BAND_DETAIL);
                break;
            case BTN_WHITELIST:
                navigate(EnderBroadcasterActionPacket.OPEN_WHITELIST);
                break;
            case BTN_DELETE:
                if (removeArmed) {
                    removeArmed = false;
                    AE2CrystalScienceLegacy.NETWORK.sendToServer(new FrequencyBandActionPacket(tilePos, FrequencyBandActionPacket.REMOVE_BAND));
                } else {
                    removeArmed = true;
                    removeTicks = 60;
                    if (deleteButton != null) {
                        deleteButton.setArmed(true);
                        deleteButton.setTooltip(Arrays.asList(I18n.format("ae2csl.menu.frequency_manager_menu.click_again")));
                    }
                    mc.player.sendStatusMessage(new TextComponentTranslation("ae2csl.menu.frequency_manager_menu.click_again"), false);
                }
                break;
            case BTN_BACK:
                navigate(EnderBroadcasterActionPacket.OPEN_MAIN);
                break;
        }
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
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        passwordField.drawTextBox();
        drawTextFieldPlaceholder(passwordField, "ae2csl.menu.frequency_manager_menu.input_password_field");
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
        fontRenderer.drawString(I18n.format("ae2csl.menu.title.frequency_band_manager"), 8, 6, 4210752);

        fontRenderer.drawString(I18n.format("ae2csl.menu.frequency_manager_menu.band_name", bandName), 10, 170, 4210752);
        fontRenderer.drawString(I18n.format("ae2csl.menu.frequency_manager_menu.band_usage", usedChannels + "/" + usableChannels), 10, 180, 4210752);
        fontRenderer.drawString(I18n.format("ae2csl.menu.frequency_manager_menu.band_error." + errorState.name().toLowerCase()), 10, 190, 4210752);

        int visible = visibleRows();
        for (int rel = 0; rel < visible; rel++) {
            int idx = scroll + rel;
            if (idx >= entries.size()) break;
            Entry e = entries.get(idx);
            int y = LIST_Y + rel * ROW_H + 2;
            boolean hover = rel == rowAt(mx, my);
            String role = e.sender ? "[S]" : "[R]";
            String label = role + " (" + e.pos.getDimension() + ", " + e.pos.getPos().getX() + ", "
                    + e.pos.getPos().getY() + ", " + e.pos.getPos().getZ() + ")";
            fontRenderer.drawString(label, LIST_X + 2, y, hover ? 0xFFFF55 : 0xFFFFFF);
            // delete "x" indicator on the right of each row (click it to disconnect that broadcaster)
            fontRenderer.drawString("x", LIST_X + DELETE_AREA_X + 4, y, 0xFF5555);
        }
    }

    private static List<String> tooltip(String key) {
        return Arrays.asList(I18n.format(key).replace("\\n", "\n").split("\n", -1));
    }

    private static List<String> toggleTooltip(String key) {
        return Arrays.asList(
                I18n.format(key + ".title"),
                I18n.format(key + ".desc"));
    }

    private static IButtonIcon backIcon() {
        return () -> Blitter.texture(BACK_TEX, 16, 16).src(0, 0, 16, 16);
    }

    private static IButtonIcon clearIcon() {
        return () -> Blitter.texture(CLEAR_TEX, 16, 16).src(0, 0, 16, 16);
    }

    private static IButtonIcon lockedIcon() {
        return () -> Blitter.texture(LOCKED_TEX, 16, 16).src(0, 0, 16, 16);
    }

    private static IButtonIcon unlockedIcon() {
        return () -> Blitter.texture(UNLOCKED_TEX, 16, 16).src(0, 0, 16, 16);
    }
}
