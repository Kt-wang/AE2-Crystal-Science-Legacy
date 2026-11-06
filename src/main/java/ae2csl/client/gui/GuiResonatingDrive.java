/*
 * AE2 Crystal Science - GuiResonatingDrive (1.12.2)
 * 谐振ME驱动器 GUI：63 槽/页 × N 页，底部翻页按钮 + 页码。
 * 布局见 screens/resonating_drive_menu.json。
 */
package ae2csl.client.gui;

import ae2.client.gui.AEBaseGui;
import ae2.client.gui.Icon;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.IconButton;
import ae2csl.container.ContainerResonatingDrive;
import ae2csl.tile.storage.TileResonatingDrive;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentTranslation;

public class GuiResonatingDrive extends AEBaseGui<ContainerResonatingDrive> {

    private static final String STYLE_PATH = "/screens/resonating_drive_menu.json";

    /** 与 resonating_drive_menu.json 中翻页按钮的 top 对齐 */
    private static final int PAGE_LABEL_TOP = 151;
    /** 页码文字颜色（纯黑，浅灰面板上可读性最高） */
    private static final int TEXT_COLOR = 0x000000;

    private final IconButton nextPageButton;
    private final IconButton prevPageButton;
    private final IconButton settingsButton;

    public GuiResonatingDrive(InventoryPlayer playerInv, TileResonatingDrive tile) {
        this(new ContainerResonatingDrive(playerInv, tile), playerInv,
            GuiStyleManager.loadStyleDoc(STYLE_PATH));
    }

    private GuiResonatingDrive(ContainerResonatingDrive container, InventoryPlayer playerInv,
                               GuiStyle style) {
        super(container, playerInv, style);

        this.prevPageButton = new PageButton(Icon.ARROW_LEFT,
            () -> getContainer().onTogglePageButton(-1));
        widgets.add("prevPage", this.prevPageButton);

        this.nextPageButton = new PageButton(Icon.ARROW_RIGHT,
            () -> getContainer().onTogglePageButton(1));
        widgets.add("nextPage", this.nextPageButton);

        // 齿轮：打开设置子界面（左侧工具栏）
        this.settingsButton = new PageButton(Icon.COG, () -> getContainer().openSettings());
        this.settingsButton.setMessage(
            new TextComponentTranslation("ae2csl.gui.resonating_drive.settings"));
        addToLeftToolbar(this.settingsButton);

        // 优先级按钮（右上角，走 fork 的子GUI系统 SwitchGuisPacket）
        widgets.addOpenPriorityButton();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        ContainerResonatingDrive menu = getContainer();
        this.prevPageButton.setVisibility(menu.getPageCount() > 1 && menu.getCurrentPage() > 0);
        this.nextPageButton.setVisibility(
            menu.getPageCount() > 1 && menu.getCurrentPage() + 1 < menu.getPageCount());
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        // 注意：drawFG 里 GL 矩阵已平移过 guiLeft/guiTop（参考 fork 的 GuiStorageBus），
        // 这里必须用 GUI 相对坐标，不能再加 offsetX/offsetY
        ContainerResonatingDrive menu = getContainer();
        if (menu.getPageCount() <= 1) {
            return;
        }
        String pageText = (menu.getCurrentPage() + 1) + " / " + menu.getPageCount();
        int textWidth = this.fontRenderer.getStringWidth(pageText);
        this.fontRenderer.drawString(pageText,
            (this.xSize - textWidth) / 2, PAGE_LABEL_TOP, TEXT_COLOR);
    }

    private static class PageButton extends IconButton {

        private final Icon icon;

        PageButton(Icon icon, Runnable onPress) {
            super(onPress);
            this.icon = icon;
        }

        @Override
        protected Icon getIcon() {
            return this.icon;
        }
    }
}
