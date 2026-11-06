/*
 * AE2 Crystal Science - GuiBandWhiteListManager
 * Manage a band's player whitelist (left = whitelisted, right = other online players).
 */
package ae2csl.client.gui;

import ae2.client.gui.style.Blitter;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.api.networking.ServerPlayerInfo;
import ae2csl.client.gui.icon.IButtonIcon;
import ae2csl.client.gui.widgets.AE2CSIconButton;
import ae2csl.container.ContainerBandWhiteListManager;
import ae2csl.network.EnderBroadcasterActionPacket;
import ae2csl.network.FrequencyBandActionPacket;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiBandWhiteListManager extends GuiBroadcastSubMenu {

    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/band_white_list_manager_menu.png");
    private static final int TEXTURE_W = 512, TEXTURE_H = 512;
    private static final ResourceLocation BACK_TEX = new ResourceLocation("ae2", "textures/guis/icons/back.png");

    private static final int WL_X = 9, WL_Y = 22, WL_W = 158, WL_H = 187;
    private static final int OTHER_X = 193, OTHER_Y = 22, OTHER_W = 158, OTHER_H = 187;
    private static final int ROW_H = 17;

    private static final int BTN_BACK = 0;

    private final List<Map.Entry<UUID, String>> whitelist = new ArrayList<>();
    private final List<Map.Entry<UUID, String>> other = new ArrayList<>();
    private int wlScroll;
    private int otherScroll;

    public GuiBandWhiteListManager(InventoryPlayer pi, TileEntity t) {
        super(new ContainerBandWhiteListManager(pi, t), t);
        this.xSize = 363;
        this.ySize = 219;
    }

    @Override
    public void initGui() {
        super.initGui();
        addButton(new AE2CSIconButton(BTN_BACK, guiLeft + 319, guiTop - 5, backIcon(), Arrays.asList(I18n.format("ae2csl.gui.back"))));
        requestData(FrequencyBandActionPacket.REQUEST_WHITELIST);
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        if (btn.id == BTN_BACK) navigate(EnderBroadcasterActionPacket.OPEN_BAND_MANAGER);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
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
    public void updateScreen() {
        super.updateScreen();
        if (tick % 20 == 0) {
            requestData(FrequencyBandActionPacket.REQUEST_WHITELIST);
        }
    }

    public void acceptWhitelist(ServerPlayerInfo wl, ServerPlayerInfo otherPlayers) {
        whitelist.clear();
        if (wl != null && wl.getPlayerInfo() != null) {
            whitelist.addAll(wl.getPlayerInfo().entrySet());
        }
        other.clear();
        if (otherPlayers != null && otherPlayers.getPlayerInfo() != null) {
            other.addAll(otherPlayers.getPlayerInfo().entrySet());
        }
        clampScrolls();
    }

    private int visibleRows(int height) { return Math.max(1, height / ROW_H); }

    private void clampScrolls() {
        int wlMax = Math.max(0, whitelist.size() - visibleRows(WL_H));
        wlScroll = Math.min(wlScroll, wlMax);
        int otherMax = Math.max(0, other.size() - visibleRows(OTHER_H));
        otherScroll = Math.min(otherScroll, otherMax);
    }

    private boolean inArea(int absX, int absY, int ax, int ay, int aw, int ah) {
        int x = absX - guiLeft, y = absY - guiTop;
        return x >= ax && x < ax + aw && y >= ay && y < ay + ah;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (inArea(mx, my, WL_X, WL_Y, WL_W, WL_H)) {
            int max = Math.max(0, whitelist.size() - visibleRows(WL_H));
            wlScroll = Math.max(0, Math.min(max, wlScroll + (d > 0 ? -1 : 1)));
        } else if (inArea(mx, my, OTHER_X, OTHER_Y, OTHER_W, OTHER_H)) {
            int max = Math.max(0, other.size() - visibleRows(OTHER_H));
            otherScroll = Math.max(0, Math.min(max, otherScroll + (d > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        super.mouseClicked(mx, my, button);
        if (button != 0) return;

        if (inArea(mx, my, WL_X, WL_Y, WL_W, WL_H)) {
            int rel = (my - guiTop - WL_Y) / ROW_H;
            int idx = wlScroll + rel;
            if (idx >= 0 && idx < whitelist.size()) {
                toggle(whitelist.get(idx).getKey());
            }
        } else if (inArea(mx, my, OTHER_X, OTHER_Y, OTHER_W, OTHER_H)) {
            int rel = (my - guiTop - OTHER_Y) / ROW_H;
            int idx = otherScroll + rel;
            if (idx >= 0 && idx < other.size()) {
                toggle(other.get(idx).getKey());
            }
        }
    }

    private void toggle(UUID uuid) {
        AE2CrystalScienceLegacy.NETWORK.sendToServer(FrequencyBandActionPacket.withUuid(
                tilePos, FrequencyBandActionPacket.WHITELIST_TOGGLE, uuid));
        requestData(FrequencyBandActionPacket.REQUEST_WHITELIST);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mx, int my) {
        mc.getTextureManager().bindTexture(BG);
        drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize, TEXTURE_W, TEXTURE_H);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRenderer.drawString(I18n.format("ae2csl.menu.title.band_white_list_manager"), 8, 6, 4210752);

        int relX = mx, relY = my;
        int visible = visibleRows(WL_H);
        for (int rel = 0; rel < visible; rel++) {
            int idx = wlScroll + rel;
            if (idx >= whitelist.size()) break;
            Map.Entry<UUID, String> e = whitelist.get(idx);
            boolean hover = inArea(relX, relY, WL_X, WL_Y, WL_W, WL_H) && ((relY - guiTop - WL_Y) / ROW_H) == rel;
            fontRenderer.drawString(e.getValue(), WL_X + 2, WL_Y + rel * ROW_H + 2, hover ? 0xFF5555 : 0xFFFFFF);
        }

        int visibleO = visibleRows(OTHER_H);
        for (int rel = 0; rel < visibleO; rel++) {
            int idx = otherScroll + rel;
            if (idx >= other.size()) break;
            Map.Entry<UUID, String> e = other.get(idx);
            boolean hover = inArea(relX, relY, OTHER_X, OTHER_Y, OTHER_W, OTHER_H) && ((relY - guiTop - OTHER_Y) / ROW_H) == rel;
            fontRenderer.drawString(e.getValue(), OTHER_X + 2, OTHER_Y + rel * ROW_H + 2, hover ? 0x55FF55 : 0xFFFFFF);
        }
    }

    private static IButtonIcon backIcon() {
        return () -> Blitter.texture(BACK_TEX, 16, 16).src(0, 0, 16, 16);
    }
}
