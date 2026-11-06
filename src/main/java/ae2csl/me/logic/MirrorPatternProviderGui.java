/*
 * AE2 Crystal Science - MirrorPatternProviderGui (1.12.2)
 * 对照源码 MirrorPatternProviderBlockEntity.openMenu / MirrorPatternProviderPart.openMenu：
 * 镜像样板供应器没有自己的 GUI，右键时重定向到被镜像目标的 GUI；
 * 未绑定/目标无效时聊天提示 target_missing。
 */
package ae2csl.me.logic;

import ae2.container.GuiIds;
import ae2.core.gui.GuiOpener;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2.parts.crafting.PatternProviderPart;
import ae2.tile.crafting.TilePatternProvider;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.tile.mirror.TileMeteoritePatternProvider;
import ae2csl.tile.mirror.TileMirrorPatternProvider;
import ae2csl.tile.resonating.TileResonatingPatternProvider;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

public final class MirrorPatternProviderGui {

    private MirrorPatternProviderGui() {
    }

    /**
     * 打开镜像目标自己的 GUI（服务端调用）
     */
    public static void openTargetGui(EntityPlayer player, MirrorPatternProviderLogic logic) {
        PatternProviderLogicHost targetHost = logic.resolveMirrorTargetHost();
        if (targetHost == null) {
            player.sendStatusMessage(
                new TextComponentTranslation("ae2csl.msg.mirror_pattern_provider.target_missing"), true);
            return;
        }

        TileEntity targetTile = targetHost.getTileEntity();
        BlockPos pos = targetTile.getPos();

        if (targetHost instanceof TileResonatingPatternProvider) {
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_RESONATING_PATTERN_PROVIDER,
                targetTile.getWorld(), pos.getX(), pos.getY(), pos.getZ());
        } else if (targetHost instanceof TileMeteoritePatternProvider) {
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_METEORITE_PATTERN_PROVIDER,
                targetTile.getWorld(), pos.getX(), pos.getY(), pos.getZ());
        } else if (targetHost instanceof ae2csl.tile.mirror.TileSimplePatternProvider) {
            // 初级样板供应器（方块形态）
            player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_SIMPLE_PATTERN_PROVIDER,
                targetTile.getWorld(), pos.getX(), pos.getY(), pos.getZ());
        } else if (targetHost instanceof ae2csl.me.part.MeteoritePatternProviderPart meteoritePart) {
            player.openGui(AE2CrystalScienceLegacy.INSTANCE,
                GuiHandler.GUI_METEORITE_PATTERN_PROVIDER_PART + meteoritePart.getSide().ordinal(),
                targetTile.getWorld(), pos.getX(), pos.getY(), pos.getZ());
        } else if (targetHost instanceof TileMirrorPatternProvider mirrorTile) {
            // 目标是未链接的镜像供应器（已链接的会被 resolveMirrorTargetHost 拒绝）——递归重定向
            openTargetGui(player, mirrorTile.getMirroringLogic());
        } else if (targetHost instanceof ae2csl.me.part.MirrorPatternProviderPart mirrorPart) {
            openTargetGui(player, mirrorPart.getMirroringLogic());
        } else if (targetHost instanceof TilePatternProvider) {
            // AE2 原版样板供应器（含扩展AE等兼容 fork TilePatternProvider 的方块）
            GuiOpener.openGui(player, GuiIds.GuiKey.PATTERN_PROVIDER, targetTile);
        } else if (targetHost instanceof PatternProviderPart part) {
            // 部件形态（fork 的 AEGuiHandler 对 PatternProviderPart 子类通用）
            GuiOpener.openPartGui(player, GuiIds.GuiKey.PATTERN_PROVIDER, part);
        } else {
            // 无法识别 GUI 打开方式的目标类型
            player.sendStatusMessage(
                new TextComponentTranslation("ae2csl.msg.mirror_pattern_provider.target_missing"), true);
        }
    }
}
