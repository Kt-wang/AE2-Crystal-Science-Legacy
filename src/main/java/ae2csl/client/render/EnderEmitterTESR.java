/*
 * AE2 Crystal Science - EnderEmitterTESR (1.12.2)
 * Port of 1.21.1 EnderEmitterRenderer.
 *
 * Renders the rotating antenna (on_top / off_top) on top of the emitter's base,
 * and — when link status is enabled — the pending (light red) / linked (purple)
 * beams plus the two Chebyshev range wireframes (auto range teal, max range amber).
 */
package ae2csl.client.render;

import ae2.api.orientation.IOrientableBlock;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.tile.ender.TileEnderEmitter;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;

import org.lwjgl.opengl.GL11;

import java.util.List;

public class EnderEmitterTESR extends TileEntitySpecialRenderer<TileEnderEmitter> {

    private static final ResourceLocation ON_TOP =
            new ResourceLocation(Tags.MOD_ID, "block/me_ender_emitter/on_top");
    private static final ResourceLocation OFF_TOP =
            new ResourceLocation(Tags.MOD_ID, "block/me_ender_emitter/off_top");

    private static final int MAX_BEAMS_PER_SET = 512;

    private static final float PENDING_WIDTH = 0.045f;
    private static final float LINKED_WIDTH = 0.060f;

    // 自动连接范围（偏青绿）
    private static final float AUTO_R = 0.20f, AUTO_G = 0.95f, AUTO_B = 0.75f;
    // 最大连接范围（偏黄橙）
    private static final float MAX_R = 1.00f, MAX_G = 0.85f, MAX_B = 0.15f;

    private IBakedModel onTop;
    private IBakedModel offTop;

    private IBakedModel getOnTop() {
        if (this.onTop == null) this.onTop = bake(ON_TOP);
        return this.onTop;
    }

    private IBakedModel getOffTop() {
        if (this.offTop == null) this.offTop = bake(OFF_TOP);
        return this.offTop;
    }

    private static IBakedModel bake(ResourceLocation modelId) {
        try {
            return ModelLoaderRegistry.getModel(modelId)
                    .bake(TRSRTransformation.identity(), DefaultVertexFormats.BLOCK,
                            texture -> Minecraft.getMinecraft().getTextureMapBlocks()
                                    .getAtlasSprite(texture.toString()));
        } catch (Exception e) {
            AE2CrystalScienceLegacy.LOGGER.error("Failed to bake ender emitter top model {}", modelId, e);
            return null;
        }
    }

    @Override
    public void render(TileEnderEmitter tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        World world = tile.getWorld();
        if (world == null) return;

        renderTopModel(tile, world, x, y, z, partialTicks);

        if (tile.isShowLinkStatus()) {
            renderLinkStatus(tile, x, y, z, partialTicks);
        }
    }

    // ---- 顶部旋转天线模型 ----
    private void renderTopModel(TileEnderEmitter tile, World world, double x, double y, double z, float partialTicks) {
        IBakedModel model = tile.isActive() ? getOnTop() : getOffTop();
        if (model == null) return;

        IBlockState blockState = world.getBlockState(tile.getPos());
        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.disableCull();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        // 朝向旋转（把默认 NORTH 对齐到方块 facing）
        EnumFacing facing = EnumFacing.NORTH;
        if (blockState.getBlock() instanceof IOrientableBlock) {
            facing = ((IOrientableBlock) blockState.getBlock()).getOrientationStrategy().getFacing(blockState);
        }
        GlStateManager.translate(0.5f, 0.5f, 0.5f);
        applyFacingRotation(facing);
        GlStateManager.translate(-0.5f, -0.5f, -0.5f);

        if (tile.isActive()) {
            float angleDeg = ((world.getTotalWorldTime() + partialTicks) * 6.0f) % 360.0f;
            GlStateManager.translate(0.5f, 0.5f, 0.5f);
            GlStateManager.rotate(angleDeg, 0.0f, 1.0f, 0.0f);
            GlStateManager.translate(-0.5f, -0.5f, -0.5f);
        }

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        buffer.setTranslation(-tile.getPos().getX(), -tile.getPos().getY(), -tile.getPos().getZ());
        dispatcher.getBlockModelRenderer().renderModel(world, model, blockState, tile.getPos(), buffer, false);
        buffer.setTranslation(0, 0, 0);
        tessellator.draw();

        GlStateManager.popMatrix();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        RenderHelper.enableStandardItemLighting();
    }

    // ---- 光束与范围线框 ----
    private void renderLinkStatus(TileEnderEmitter tile, double x, double y, double z, float partialTicks) {
        World world = tile.getWorld();
        if (world == null) return;

        BlockPos bePos = tile.getPos();

        // 相机世界坐标
        double camX = Minecraft.getMinecraft().getRenderManager().viewerPosX;
        double camY = Minecraft.getMinecraft().getRenderManager().viewerPosY;
        double camZ = Minecraft.getMinecraft().getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 范围线框（局部坐标）
        int autoR = clampRange(tile.getLinkDistance());
        int maxR = clampRange(tile.getMaxLinkDistanceForClient());
        if (maxR < autoR) maxR = autoR;

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        if (maxR > 0) {
            drawLineBox(buffer, 0, 0, 0, 1, 1, 1, maxR, MAX_R, MAX_G, MAX_B, 0.55f);
        }
        if (autoR > 0) {
            drawLineBox(buffer, 0, 0, 0, 1, 1, 1, autoR, AUTO_R, AUTO_G, AUTO_B, 0.55f);
        }
        tessellator.draw();

        // 光束（局部坐标，起点本方块中心）
        double startX = 0.5, startY = 0.5, startZ = 0.5;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        List<BlockPos> pending = tile.getPendingRenderPositionsSnapshot();
        int c1 = 0;
        for (BlockPos target : pending) {
            if (++c1 > MAX_BEAMS_PER_SET) break;
            if (!world.isBlockLoaded(target)) continue;
            double ex = target.getX() - bePos.getX() + 0.5;
            double ey = target.getY() - bePos.getY() + 0.5;
            double ez = target.getZ() - bePos.getZ() + 0.5;
            drawBeamCross(buffer, startX, startY, startZ, ex, ey, ez, PENDING_WIDTH,
                    1.00f, 0.35f, 0.35f, 0.35f, camX - bePos.getX(), camY - bePos.getY(), camZ - bePos.getZ());
        }

        List<BlockPos> linked = tile.getLinkedRenderPositionsSnapshot();
        int c2 = 0;
        for (BlockPos target : linked) {
            if (++c2 > MAX_BEAMS_PER_SET) break;
            if (!world.isBlockLoaded(target)) continue;
            double ex = target.getX() - bePos.getX() + 0.5;
            double ey = target.getY() - bePos.getY() + 0.5;
            double ez = target.getZ() - bePos.getZ() + 0.5;
            drawBeamCross(buffer, startX, startY, startZ, ex, ey, ez, LINKED_WIDTH,
                    0.55f, 0.05f, 1.00f, 0.80f, camX - bePos.getX(), camY - bePos.getY(), camZ - bePos.getZ());
        }

        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void applyFacingRotation(EnumFacing facing) {
        switch (facing) {
            case NORTH: break;
            case SOUTH: GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f); break;
            case EAST:  GlStateManager.rotate(-90.0f, 0.0f, 1.0f, 0.0f); break;
            case WEST:  GlStateManager.rotate(90.0f, 0.0f, 1.0f, 0.0f); break;
            default: break;
        }
    }

    private static int clampRange(int v) {
        return Math.max(0, Math.min(v, 512));
    }

    // ---- 线框立方体（以 base 方块为 [0..1]，向四周膨胀 radius）----
    private static void drawLineBox(BufferBuilder buffer, double bx1, double by1, double bz1,
                                    double bx2, double by2, double bz2, double radius,
                                    float r, float g, float b, float a) {
        double x1 = bx1 - radius, y1 = by1 - radius, z1 = bz1 - radius;
        double x2 = bx2 + radius, y2 = by2 + radius, z2 = bz2 + radius;

        // 8 个角
        double[][] c = {
                {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}, {x1, y1, z2},
                {x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}
        };
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0}, // 下
                {4, 5}, {5, 6}, {6, 7}, {7, 4}, // 上
                {0, 4}, {1, 5}, {2, 6}, {3, 7}  // 竖直
        };
        for (int[] e : edges) {
            buffer.pos(c[e[0]][0], c[e[0]][1], c[e[0]][2]).color(r, g, b, a).endVertex();
            buffer.pos(c[e[1]][0], c[e[1]][1], c[e[1]][2]).color(r, g, b, a).endVertex();
        }
    }

    // ---- 双交叉面光束（面向相机）----
    private static void drawBeamCross(BufferBuilder buffer,
                                      double sx, double sy, double sz,
                                      double ex, double ey, double ez,
                                      float width, float r, float g, float b, float a,
                                      double camX, double camY, double camZ) {
        double dx = ex - sx, dy = ey - sy, dz = ez - sz;
        double lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq < 1.0e-6) return;
        double len = Math.sqrt(lenSq);
        double dirX = dx / len, dirY = dy / len, dirZ = dz / len;

        double mx = (sx + ex) * 0.5, my = (sy + ey) * 0.5, mz = (sz + ez) * 0.5;
        double tcX = camX - mx, tcY = camY - my, tcZ = camZ - mz;
        double tLenSq = tcX * tcX + tcY * tcY + tcZ * tcZ;
        if (tLenSq < 1.0e-6) { tcX = 0; tcY = 1; tcZ = 0; } else {
            double tLen = Math.sqrt(tLenSq);
            tcX /= tLen; tcY /= tLen; tcZ /= tLen;
        }

        // side1 = dir × toCam
        double s1x = dirY * tcZ - dirZ * tcY;
        double s1y = dirZ * tcX - dirX * tcZ;
        double s1z = dirX * tcY - dirY * tcX;
        double s1LenSq = s1x * s1x + s1y * s1y + s1z * s1z;
        if (s1LenSq < 1.0e-6) {
            double fx = 0, fy = 1, fz = 0;
            if (Math.abs(dirY) < 0.99) { fx = 0; fy = 1; fz = 0; } else { fx = 1; fy = 0; fz = 0; }
            s1x = dirY * fz - dirZ * fy;
            s1y = dirZ * fx - dirX * fz;
            s1z = dirX * fy - dirY * fx;
            s1LenSq = s1x * s1x + s1y * s1y + s1z * s1z;
        }
        double s1Len = Math.sqrt(s1LenSq);
        double halfW = width * 0.5;
        double s1nx = s1x / s1Len * halfW, s1ny = s1y / s1Len * halfW, s1nz = s1z / s1Len * halfW;

        // side2 = dir × side1
        double s2x = dirY * s1nz - dirZ * s1ny;
        double s2y = dirZ * s1nx - dirX * s1nz;
        double s2z = dirX * s1ny - dirY * s1nx;
        double s2LenSq = s2x * s2x + s2y * s2y + s2z * s2z;
        double s2nx, s2ny, s2nz;
        if (s2LenSq > 1.0e-8) {
            double s2Len = Math.sqrt(s2LenSq);
            s2nx = s2x / s2Len * halfW; s2ny = s2y / s2Len * halfW; s2nz = s2z / s2Len * halfW;
        } else {
            s2nx = s2ny = s2nz = 0;
        }

        float aStart = a * 0.85f;
        float aEnd = a * 0.20f;

        drawBeamQuad(buffer, sx, sy, sz, ex, ey, ez, s1nx, s1ny, s1nz, r, g, b, aStart, aEnd);
        if (s2nx != 0 || s2ny != 0 || s2nz != 0) {
            drawBeamQuad(buffer, sx, sy, sz, ex, ey, ez, s2nx, s2ny, s2nz, r, g, b, aStart, aEnd);
        }
    }

    private static void drawBeamQuad(BufferBuilder buffer,
                                     double sx, double sy, double sz,
                                     double ex, double ey, double ez,
                                     double ox, double oy, double oz,
                                     float r, float g, float b, float aStart, float aEnd) {
        double s1x = sx + ox, s1y = sy + oy, s1z = sz + oz;
        double s2x = sx - ox, s2y = sy - oy, s2z = sz - oz;
        double e1x = ex + ox, e1y = ey + oy, e1z = ez + oz;
        double e2x = ex - ox, e2y = ey - oy, e2z = ez - oz;

        buffer.pos(s1x, s1y, s1z).color(r, g, b, aStart).endVertex();
        buffer.pos(s2x, s2y, s2z).color(r, g, b, aStart).endVertex();
        buffer.pos(e2x, e2y, e2z).color(r, g, b, aEnd).endVertex();

        buffer.pos(s1x, s1y, s1z).color(r, g, b, aStart).endVertex();
        buffer.pos(e2x, e2y, e2z).color(r, g, b, aEnd).endVertex();
        buffer.pos(e1x, e1y, e1z).color(r, g, b, aEnd).endVertex();
    }
}
