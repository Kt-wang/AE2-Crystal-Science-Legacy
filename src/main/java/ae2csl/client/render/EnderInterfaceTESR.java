/*
 * AE2 Crystal Science - EnderInterfaceTESR (1.12.2)
 * 对照 1.21.1 源码 common/block/render/EnderInterfaceRender.java
 * 开启"显示工作范围"时绘制切比雪夫范围线框。
 */
package ae2csl.client.render;

import ae2csl.tile.ender.TileEnderInterface;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import org.lwjgl.opengl.GL11;

public class EnderInterfaceTESR extends TileEntitySpecialRenderer<TileEnderInterface> {

    private static final float R = 0.2f, G = 0.9f, B = 0.9f, A = 0.8f;

    @Override
    public boolean isGlobalRenderer(TileEnderInterface te) {
        // 开启范围显示时允许离屏渲染（对齐源码 shouldRenderOffScreen）
        return te.getEnderInterfaceLogic().isRenderRangeInClient();
    }

    @Override
    public void render(TileEnderInterface te, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        var logic = te.getEnderInterfaceLogic();
        if (!logic.isRenderRangeInClient()) {
            return;
        }

        int r = logic.getRange();
        if (r <= 0) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 源码 AABB: (-r,-r,-r) → (r+1, r+1, r+1)
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        drawLineBox(buffer, -r, -r, -r, r + 1.0, r + 1.0, r + 1.0, R, G, B, A);
        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void drawLineBox(BufferBuilder buffer, double x1, double y1, double z1,
                                    double x2, double y2, double z2,
                                    float r, float g, float b, float a) {
        double[][] c = {
            {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}, {x1, y1, z2},
            {x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}
        };
        int[][] edges = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (int[] e : edges) {
            buffer.pos(c[e[0]][0], c[e[0]][1], c[e[0]][2]).color(r, g, b, a).endVertex();
            buffer.pos(c[e[1]][0], c[e[1]][1], c[e[1]][2]).color(r, g, b, a).endVertex();
        }
    }
}
