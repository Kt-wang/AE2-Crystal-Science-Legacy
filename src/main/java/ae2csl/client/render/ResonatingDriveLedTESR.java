/*
 * AE2 Crystal Science - ResonatingDriveLedTESR (1.12.2)
 * 谐振ME驱动器舱位 LED 渲染：10 舱位 ↔ 页，按填充百分比绿→红渐变。
 * LED 几何沿用 AE2 CellLedRenderer 的小立方体（见 DriveLedTESR）；
 * 颜色不按 CellState 离散档，而是按 1..15 档位做 HSB 渐变。
 */
package ae2csl.client.render;

import ae2.api.orientation.BlockOrientation;
import ae2.client.render.BlockEntityRenderHelper;
import ae2.client.render.model.DriveBakedModel;
import ae2csl.tile.storage.TileResonatingDrive;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import org.lwjgl.opengl.GL11;

import javax.vecmath.Vector3f;
import java.awt.Color;

public class ResonatingDriveLedTESR extends TileEntitySpecialRenderer<TileResonatingDrive> {

    // LED 小立方体几何（同 AE2 CellLedRenderer）
    private static final float L = 5 / 16.f;
    private static final float R = 4 / 16.f;
    private static final float T = 1 / 16.f;
    private static final float B = -0.001f / 16.f;
    private static final float FR = -0.001f / 16.f;
    private static final float BA = 0.999f / 16.f;

    private static final float[] LED_QUADS = {
        R, T, FR, L, T, FR, L, B, FR, R, B, FR,
        L, T, FR, L, T, BA, L, B, BA, L, B, FR,
        R, T, BA, R, T, FR, R, B, FR, R, B, BA,
        R, T, BA, L, T, BA, L, T, FR, R, T, FR,
        R, B, FR, L, B, FR, L, B, BA, R, B, BA};

    @Override
    public void render(TileResonatingDrive te, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        if (!te.isPowered()) {
            return; // 离线熄灯
        }
        boolean anyLit = false;
        for (int i = 0; i < 10; i++) {
            if (te.getLedLevel(i) > 0) {
                anyLit = true;
                break;
            }
        }
        if (!anyLit) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.translate(0.5F, 0.5F, 0.5F);

        BlockOrientation orientation = BlockOrientation.get(te);
        BlockEntityRenderHelper.applyOrientation(orientation);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        Vector3f origin = new Vector3f();
        for (int led = 0; led < 10; led++) {
            int level = te.getLedLevel(led);
            if (level <= 0) {
                continue;
            }
            DriveBakedModel.getSlotOrigin(led / 2, led % 2, origin);
            renderLed(buffer, origin, colorForLevel(level));
        }
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /** 档位 1..15：绿(0.33) → 黄 → 红(0.0) HSB 渐变 */
    private static Vector3f colorForLevel(int level) {
        float t = (level - 1) / 14.0f;
        float hue = 0.33f * (1.0f - t);
        int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f);
        return new Vector3f(
            ((rgb >> 16) & 0xFF) / 255.0f,
            ((rgb >> 8) & 0xFF) / 255.0f,
            (rgb & 0xFF) / 255.0f);
    }

    private static void renderLed(BufferBuilder buffer, Vector3f origin, Vector3f color) {
        for (int i = 0; i < LED_QUADS.length; i += 3) {
            buffer.pos(origin.x + LED_QUADS[i], origin.y + LED_QUADS[i + 1], origin.z + LED_QUADS[i + 2])
                .color(color.x, color.y, color.z, 1.0f)
                .endVertex();
        }
    }
}
