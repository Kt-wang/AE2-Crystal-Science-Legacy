/*
 * AE2 Crystal Science - GuiResonatingDriveSettings (1.12.2)
 * 谐振ME驱动器设置子界面：齿轮进入，包含「自动拉取存储元件」开关行（开关图标 + 文字），
 * 右上角返回按钮回到驱动器主界面。样式为普通 GuiContainer（参考 GuiEnderBroadcaster）。
 */
package ae2csl.client.gui;

import ae2.client.gui.style.Blitter;
import ae2csl.Tags;
import ae2csl.client.gui.icon.AE2CSIcon;
import ae2csl.client.gui.icon.IButtonIcon;
import ae2csl.client.gui.widgets.AE2CSIconButton;
import ae2csl.container.ContainerResonatingDriveSettings;
import ae2csl.tile.storage.TileResonatingDrive;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GuiResonatingDriveSettings extends GuiContainer {

    private static final ResourceLocation BG =
        new ResourceLocation(Tags.MOD_ID, "textures/gui/resonating_drive_settings.png");
    private static final ResourceLocation BACK_TEX =
        new ResourceLocation("ae2", "textures/guis/icons/back.png");

    private static final int BTN_BACK = 0;
    private static final int BTN_AUTO_PULL = 1;

    private final ContainerResonatingDriveSettings menu;
    private AE2CSIconButton autoPullButton;

    public GuiResonatingDriveSettings(InventoryPlayer playerInv, TileResonatingDrive tile) {
        super(new ContainerResonatingDriveSettings(playerInv, tile));
        this.menu = (ContainerResonatingDriveSettings) this.inventorySlots;
        this.xSize = 176;
        this.ySize = 80;
    }

    @Override
    public void initGui() {
        super.initGui();
        addButton(new AE2CSIconButton(BTN_BACK, guiLeft + 152, guiTop - 5, backIcon(),
            Arrays.asList(I18n.format("ae2csl.gui.back"))));
        this.autoPullButton = new AE2CSIconButton(BTN_AUTO_PULL, guiLeft + 8, guiTop + 26,
            AE2CSIcon.PULL_MODE_OFF, autoPullTooltip());
        addButton(this.autoPullButton);
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        if (btn.id == BTN_BACK) {
            this.menu.goBack();
        } else if (btn.id == BTN_AUTO_PULL) {
            this.menu.toggleAutoPull();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // 与服务端同步的状态保持一致（@GuiSync 每 tick 广播）
        this.autoPullButton.setIcon(
            this.menu.autoPull ? AE2CSIcon.PULL_MODE_ON : AE2CSIcon.PULL_MODE_OFF);
        this.autoPullButton.setTooltip(autoPullTooltip());
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
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.mc.getTextureManager().bindTexture(BG);
        drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("gui.ae2csl.resonating_drive_settings"), 8, 6, 0x404040);
        // 开关行文字（开关图标右侧，对齐 img_1.png 参考样式）
        this.fontRenderer.drawString(I18n.format("ae2csl.gui.resonating_drive.auto_pull"),
            28, 30, 0x000000);
    }

    private List<String> autoPullTooltip() {
        return Arrays.asList(I18n.format(this.menu.autoPull
            ? "ae2csl.gui.resonating_drive.auto_pull.on"
            : "ae2csl.gui.resonating_drive.auto_pull.off"));
    }

    private static IButtonIcon backIcon() {
        return () -> Blitter.texture(BACK_TEX, 16, 16).src(0, 0, 16, 16);
    }
}
