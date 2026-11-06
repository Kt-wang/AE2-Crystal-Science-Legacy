/*
 * AE2 Crystal Science - GuiSideConfig (1.12.2)
 * 侧面配置界面（布局对齐 1.21.1 源码 SideConfigGUI / side_config_menu.json）：
 * 96x101 小面板（无玩家背包），6 个方向策略按钮按立方体十字布局，按钮中央渲染
 * 该方向邻居方块的物品图标；左侧工具栏为自动输入/自动输出/清空图标按钮，
 * 返回按钮在面板右上角外侧（源码 back_button.json: right-24, top-5）。
 * 移植版机器无朝向属性，方向用绝对方向（上/下/北/南/西/东）。
 */
package ae2csl.client.gui;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.container.ContainerSideConfig;
import ae2csl.machine.ISideConfigHost;
import ae2csl.machine.SideConfigData;
import ae2csl.machine.SidePolicy;
import ae2csl.network.OpenGuiPacket;
import ae2csl.network.SideConfigActionPacket;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GuiSideConfig extends GuiContainer {

    private static final ResourceLocation BG = new ResourceLocation(Tags.MOD_ID, "textures/gui/side_config_menu.png");

    // 方向按钮 id = 0..5（坐标取自源码 side_config_menu.json 的十字布局）
    private static final int BTN_AUTO_IMPORT = 6;
    private static final int BTN_AUTO_EXPORT = 7;
    private static final int BTN_CLEAR = 8;
    private static final int BTN_BACK = 9;

    private final TileEntity tile;
    private ae2csl.client.gui.widgets.AutoModeToggleButton autoImportBtn;
    private ae2csl.client.gui.widgets.AutoModeToggleButton autoExportBtn;

    public GuiSideConfig(InventoryPlayer playerInv, TileEntity tile) {
        super(new ContainerSideConfig(playerInv, tile));
        this.tile = tile;
        this.xSize = 96;
        this.ySize = 101;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();

        // 六面十字布局（源码 side_config_menu.json）：
        //            [上](39,20)
        // [西](18,42) [北](39,42) [东](60,42)
        // [南](18,64) [下](39,64)
        addSideButton(0, 39, 20, EnumFacing.UP);
        addSideButton(1, 18, 42, EnumFacing.WEST);
        addSideButton(2, 39, 42, EnumFacing.NORTH);
        addSideButton(3, 60, 42, EnumFacing.EAST);
        addSideButton(4, 18, 64, EnumFacing.SOUTH);
        addSideButton(5, 39, 64, EnumFacing.DOWN);

        // 左侧工具栏（源码顺序）：自动输入 / 自动输出 / 清空
        // 自动开关按钮：启用=亮色背景、禁用=暗色背景（对齐源码 AECSAutoModeToggleButton）
        // 位置：紧贴面板左缘的贴条工具栏（AE2CSToolbar，复刻 VerticalButtonBar）
        autoImportBtn = new ae2csl.client.gui.widgets.AutoModeToggleButton(BTN_AUTO_IMPORT,
            AE2CSToolbar.buttonX(guiLeft), AE2CSToolbar.buttonY(guiTop, 0),
            ae2csl.client.gui.icon.AE2CSIcon.RECEIVER_STATE);
        autoExportBtn = new ae2csl.client.gui.widgets.AutoModeToggleButton(BTN_AUTO_EXPORT,
            AE2CSToolbar.buttonX(guiLeft), AE2CSToolbar.buttonY(guiTop, 1),
            ae2csl.client.gui.icon.AE2CSIcon.SENDER_STATE);
        this.buttonList.add(autoImportBtn);
        this.buttonList.add(autoExportBtn);
        this.buttonList.add(new ae2csl.client.gui.widgets.AE2CSIconButton(BTN_CLEAR,
            AE2CSToolbar.iconButtonX(guiLeft), AE2CSToolbar.buttonY(guiTop, 2),
            ae2csl.client.gui.icon.AdaptedAE2Icon.CLEAR,
            Collections.singletonList(I18n.format("gui.ae2csl.side_config.clear"))));

        // 返回：面板右上角外侧（源码 back_button.json: right 24, top -5）
        this.buttonList.add(new ae2csl.client.gui.widgets.AE2CSIconButton(BTN_BACK, guiLeft + xSize - 24, guiTop - 5,
            ae2csl.client.gui.icon.AdaptedAE2Icon.BACK,
            Collections.singletonList(I18n.format("gui.ae2csl.side_config.back"))));
        refreshState();
    }

    private void addSideButton(int id, int x, int y, EnumFacing side) {
        this.buttonList.add(new ae2csl.client.gui.widgets.SidePolicyButton(id, guiLeft + x, guiTop + y, side));
    }

    private ISideConfigHost host() {
        return tile instanceof ISideConfigHost ? (ISideConfigHost) tile : null;
    }

    /** 每帧从客户端副本刷新按钮状态与邻居方块图标（同步包到达后自动生效） */
    private void refreshState() {
        ISideConfigHost host = host();
        if (host == null) return;
        SideConfigData cfg = host.getSideConfig();
        for (GuiButton b : this.buttonList) {
            if (b instanceof ae2csl.client.gui.widgets.SidePolicyButton) {
                ae2csl.client.gui.widgets.SidePolicyButton sb = (ae2csl.client.gui.widgets.SidePolicyButton) b;
                sb.setPolicy(cfg.get(sb.getSide()));
                sb.setOverlay(neighborStack(sb.getSide()));
            }
        }
        // 对齐源码：启用/禁用切换亮暗背景 + tooltip 状态行
        autoImportBtn.setEnabledState(cfg.isAutoImport());
        autoExportBtn.setEnabledState(cfg.isAutoExport());
        autoImportBtn.setTooltip(Arrays.asList(
            I18n.format("gui.ae2csl.side_config.auto_import"), onOff(cfg.isAutoImport())));
        autoExportBtn.setTooltip(Arrays.asList(
            I18n.format("gui.ae2csl.side_config.auto_export"), onOff(cfg.isAutoExport())));
    }

    /**
     * 该方向邻居方块的物品图标（对齐源码 getItemOverlay：邻居方块 asItem，空气则无）。
     * 用 getPickBlock（创造中键选取语义）拿展示用物品，避免 getMetaFromState 造出
     * 无物品模型的 stack 而渲染成丢失贴图棋盘格；模型仍缺失的兜底不渲染。
     */
    private ItemStack neighborStack(EnumFacing side) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || tile == null) return ItemStack.EMPTY;
        BlockPos np = tile.getPos().offset(side);
        IBlockState state = mc.world.getBlockState(np);
        ItemStack stack;
        try {
            stack = state.getBlock().getPickBlock(state,
                new net.minecraft.util.math.RayTraceResult(
                    net.minecraft.util.math.RayTraceResult.Type.BLOCK,
                    new net.minecraft.util.math.Vec3d(np.getX() + 0.5, np.getY() + 0.5, np.getZ() + 0.5),
                    side.getOpposite(), np),
                mc.world, np, mc.player);
        } catch (Exception e) {
            Item item = Item.getItemFromBlock(state.getBlock());
            stack = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        }
        if (stack.isEmpty()) return ItemStack.EMPTY;
        // 模型缺失的方块不渲染图标（防止紫黑棋盘格）
        net.minecraft.client.renderer.block.model.IBakedModel model =
            mc.getRenderItem().getItemModelMesher().getItemModel(stack);
        if (model == mc.getRenderItem().getItemModelMesher().getModelManager().getMissingModel()) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    private static String onOff(boolean v) {
        return I18n.format(v ? "gui.ae2csl.side_config.on" : "gui.ae2csl.side_config.off");
    }

    private static String policyName(SidePolicy p) {
        switch (p) {
            case INSERT: return I18n.format("gui.ae2csl.side_config.policy.insert");
            case EXTRACT: return I18n.format("gui.ae2csl.side_config.policy.extract");
            case NONE: return I18n.format("gui.ae2csl.side_config.policy.none");
            default: return I18n.format("gui.ae2csl.side_config.policy.all");
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        ISideConfigHost host = host();
        if (host == null) return;
        int id = button.id;
        if (id >= 0 && id < 6) {
            EnumFacing side = ((ae2csl.client.gui.widgets.SidePolicyButton) button).getSide();
            AE2CrystalScienceLegacy.NETWORK.sendToServer(SideConfigActionPacket.cycleSide(tile.getPos(), side));
        } else if (id == BTN_AUTO_IMPORT) {
            AE2CrystalScienceLegacy.NETWORK.sendToServer(SideConfigActionPacket.toggleAutoImport(tile.getPos()));
        } else if (id == BTN_AUTO_EXPORT) {
            AE2CrystalScienceLegacy.NETWORK.sendToServer(SideConfigActionPacket.toggleAutoExport(tile.getPos()));
        } else if (id == BTN_CLEAR) {
            AE2CrystalScienceLegacy.NETWORK.sendToServer(SideConfigActionPacket.clearSides(tile.getPos()));
        } else if (id == BTN_BACK) {
            AE2CrystalScienceLegacy.NETWORK.sendToServer(new OpenGuiPacket(tile.getPos(), host.getMachineGuiId()));
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        refreshState();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(BG);
        // 源码背景区域 (0,0,96,101)
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, 96, 101);
        // 左侧工具栏贴条背景（3 个按钮高）
        AE2CSToolbar.draw(guiLeft, guiTop, 3);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 源码标题键 ae2csl.menu.title.side_config_menu（面配置）
        fontRenderer.drawString(I18n.format("ae2csl.menu.title.side_config_menu"), 8, 6, 4210752);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (GuiButton btn : this.buttonList) {
            if (!btn.visible || !btn.isMouseOver()) continue;
            if (btn instanceof ae2csl.client.gui.widgets.SidePolicyButton) {
                // 方向按钮 tooltip："北: 全部"（对齐源码 ae2cs.button.side_config.tooltip）
                ae2csl.client.gui.widgets.SidePolicyButton sb = (ae2csl.client.gui.widgets.SidePolicyButton) btn;
                String dir = I18n.format("gui.ae2csl.side_config.dir." + sb.getSide().getName());
                drawHoveringText(Collections.singletonList(dir + ": " + policyName(sb.getPolicy())), mouseX, mouseY);
                break;
            }
            if (btn instanceof ae2csl.client.gui.widgets.AutoModeToggleButton) {
                List<String> tt = ((ae2csl.client.gui.widgets.AutoModeToggleButton) btn).getTooltip();
                if (tt != null && !tt.isEmpty()) {
                    drawHoveringText(tt, mouseX, mouseY);
                    break;
                }
            }
            if (btn instanceof ae2csl.client.gui.widgets.AE2CSIconButton) {
                List<String> tt = ((ae2csl.client.gui.widgets.AE2CSIconButton) btn).getTooltip();
                if (tt != null && !tt.isEmpty()) {
                    drawHoveringText(tt, mouseX, mouseY);
                    break;
                }
            }
        }
    }
}
