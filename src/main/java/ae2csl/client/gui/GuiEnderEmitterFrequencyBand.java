/*
 * AE2 Crystal Science - GuiEnderEmitterFrequencyBand
 * Lists all frequency bands the player can see; click one to open the emitter link menu.
 */
package ae2csl.client.gui;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.api.linker.broadcast.networking.BroadcastBandsField;
import ae2csl.client.gui.widgets.AE2CSTextField;
import ae2csl.container.ContainerEnderEmitterFrequencyBand;
import ae2csl.network.EnderEmitterActionPacket;
import ae2csl.network.FrequencyBandActionPacket;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuiEnderEmitterFrequencyBand extends GuiBroadcastSubMenu {

    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/frequency_band_menu.png");
    private static final int TEXTURE_W = 256, TEXTURE_H = 256;

    private static final int LIST_X = 9, LIST_Y = 39, LIST_W = 158, LIST_H = 170;
    private static final int ROW_H = 17;

    private AE2CSTextField searchField;
    private List<BroadcastBandsField.Entry> bands = new ArrayList<>();
    private final List<BroadcastBandsField.Entry> filtered = new ArrayList<>();
    private int scroll;

    public GuiEnderEmitterFrequencyBand(InventoryPlayer pi, TileEntity t) {
        super(new ContainerEnderEmitterFrequencyBand(pi, t), t);
        this.xSize = 195;
        this.ySize = 219;
    }

    @Override
    public void initGui() {
        super.initGui();
        searchField = new AE2CSTextField(0, fontRenderer, guiLeft + 8, guiTop + 18, 89, 12);
        searchField.setMaxStringLength(64);
        requestData(FrequencyBandActionPacket.REQUEST_BANDS_LIST);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        searchField.updateCursorCounter();
        if (tick % 20 == 0) {
            requestData(FrequencyBandActionPacket.REQUEST_BANDS_LIST);
        }
    }

    public void acceptBands(BroadcastBandsField field) {
        this.bands = field != null && field.getBands() != null
                ? new ArrayList<>(field.getBands())
                : new ArrayList<>();
        rebuildFilter();
    }

    private void rebuildFilter() {
        filtered.clear();
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        for (BroadcastBandsField.Entry e : bands) {
            if (q.isEmpty() || (e.getName() != null && e.getName().toLowerCase(Locale.ROOT).contains(q))) {
                filtered.add(e);
            }
        }
        int maxScroll = Math.max(0, filtered.size() - visibleRows());
        scroll = Math.min(scroll, maxScroll);
    }

    private int visibleRows() {
        return Math.max(1, LIST_H / ROW_H);
    }

    private boolean inListArea(int absX, int absY) {
        int x = absX - guiLeft, y = absY - guiTop;
        return x >= LIST_X && x < LIST_X + LIST_W && y >= LIST_Y && y < LIST_Y + LIST_H;
    }

    private int rowAt(int absX, int absY) {
        if (!inListArea(absX, absY)) return -1;
        return (absY - guiTop - LIST_Y) / ROW_H;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (inListArea(mx, my)) {
            int maxScroll = Math.max(0, filtered.size() - visibleRows());
            scroll = Math.max(0, Math.min(maxScroll, scroll + (d > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        searchField.mouseClicked(mx, my, button);
        // 列表区域与玩家物品栏重叠：优先处理频段选中，不让槽位点击拦截
        if (button == 0 && inListArea(mx, my)) {
            int rel = rowAt(mx, my);
            if (rel >= 0) {
                int idx = scroll + rel;
                if (idx >= 0 && idx < filtered.size()) {
                    BroadcastBandsField.Entry entry = filtered.get(idx);
                    if (entry.getName() != null) {
                        GuiEnderEmitterFrequencyBandLink.pendingBand = entry.getName();
                        AE2CrystalScienceLegacy.NETWORK.sendToServer(
                                new EnderEmitterActionPacket(tilePos, EnderEmitterActionPacket.OPEN_LINK_MENU, entry.getName()));
                    }
                }
            }
            return;
        }
        super.mouseClicked(mx, my, button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField.isFocused()) {
            searchField.textboxKeyTyped(typedChar, keyCode);
            rebuildFilter();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        searchField.drawTextBox();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mx, int my) {
        mc.getTextureManager().bindTexture(BG);
        drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize, TEXTURE_W, TEXTURE_H);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRenderer.drawString(I18n.format("ae2csl.menu.title.frequency_bands"), 8, 6, 4210752);

        int visible = visibleRows();
        for (int rel = 0; rel < visible; rel++) {
            int idx = scroll + rel;
            if (idx >= filtered.size()) break;
            BroadcastBandsField.Entry e = filtered.get(idx);
            int y = LIST_Y + rel * ROW_H + 2;
            boolean hover = rel == rowAt(mx, my);
            String label = e.getName();
            if (e.isEncrypted()) label += "  " + I18n.format("ae2csl.menu.frequency_band.encrypted");
            if (!e.isPublic()) label += "  " + I18n.format("ae2csl.menu.frequency_band.private");
            fontRenderer.drawString(label, LIST_X + 2, y, hover ? 0xFFFF55 : 0xFFFFFF);
        }
    }
}
