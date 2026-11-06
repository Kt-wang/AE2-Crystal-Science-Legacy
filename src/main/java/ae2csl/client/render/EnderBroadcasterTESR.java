/*
 * AE2 Crystal Science - EnderBroadcasterTESR (1.12.2)
 * Port of 1.21.1 EnderBroadcasterRender.
 *
 * Renders the animated crystal core on top of the broadcaster's static frame
 * (the frame is the normal block model). The core picks one of three models
 * (off / sender / receiver) based on the tile's client state, and applies a
 * scale + rotation animation seeded deterministically from the band name.
 */
package ae2csl.client.render;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.tile.ender.TileEnderBroadcaster;

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
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;

import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EnderBroadcasterTESR extends TileEntitySpecialRenderer<TileEnderBroadcaster> {

    private static final ResourceLocation OFF_CORE =
            new ResourceLocation(Tags.MOD_ID, "block/me_ender_broadcaster/off_core");
    private static final ResourceLocation SENDER_CORE =
            new ResourceLocation(Tags.MOD_ID, "block/me_ender_broadcaster/sender_core");
    private static final ResourceLocation RECEIVER_CORE =
            new ResourceLocation(Tags.MOD_ID, "block/me_ender_broadcaster/receiver_core");

    // 缓存动画参数，避免每帧重复生成（与 1.21.1 一致）
    private static final Map<String, AnimParams> PARAM_CACHE = new ConcurrentHashMap<>();

    private IBakedModel offCore;
    private IBakedModel senderCore;
    private IBakedModel receiverCore;

    private IBakedModel getOffCore() {
        if (this.offCore == null) this.offCore = bake(OFF_CORE);
        return this.offCore;
    }

    private IBakedModel getSenderCore() {
        if (this.senderCore == null) this.senderCore = bake(SENDER_CORE);
        return this.senderCore;
    }

    private IBakedModel getReceiverCore() {
        if (this.receiverCore == null) this.receiverCore = bake(RECEIVER_CORE);
        return this.receiverCore;
    }

    private static IBakedModel bake(ResourceLocation modelId) {
        try {
            return ModelLoaderRegistry.getModel(modelId)
                    .bake(TRSRTransformation.identity(), DefaultVertexFormats.BLOCK,
                            texture -> Minecraft.getMinecraft().getTextureMapBlocks()
                                    .getAtlasSprite(texture.toString()));
        } catch (Exception e) {
            AE2CrystalScienceLegacy.LOGGER.error("Failed to bake ender broadcaster core model {}", modelId, e);
            return null;
        }
    }

    @Override
    public void render(TileEnderBroadcaster tile, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        if (tile.getWorld() == null) return;

        IBakedModel core;
        if (tile.isActiveForClient()) {
            core = tile.isAsSenderForClient() ? getSenderCore() : getReceiverCore();
        } else {
            core = getOffCore();
        }
        if (core == null) return;

        String name = tile.getBandName();
        if (name == null) name = "";
        AnimParams p = PARAM_CACHE.computeIfAbsent(name, AnimParams::fromName);

        float time = tile.getWorld().getTotalWorldTime() + partialTicks;

        // 周期内归一化时间
        float t = (time % p.cycleTicks) / (float) p.cycleTicks;

        // 三段：A(缩小+旋转到 qA) -> B(放大回 1 + 旋转到 qAB) -> C(回到 identity)
        float aEnd = p.phaseA;
        float bEnd = p.phaseA + p.phaseB;

        float scale;
        Quat q;

        if (t < aEnd) {
            float u = t / aEnd;
            float e = easeInOut(u);
            scale = lerp(1.0f, p.minScale, e); // 只缩小
            q = Quat.slerp(Quat.IDENTITY, p.qA, e);
        } else if (t < bEnd) {
            float u = (t - aEnd) / p.phaseB;
            float e = easeInOut(u);
            scale = lerp(p.minScale, 1.0f, e); // 放大回原大小
            q = Quat.slerp(p.qA, p.qAB, e);
        } else {
            float u = (t - bEnd) / p.phaseC;
            float e = easeInOut(u);
            scale = 1.0f; // 保持原大小
            q = Quat.slerp(p.qAB, Quat.IDENTITY, e); // 回到原始姿态
        }

        IBlockState blockState = tile.getWorld().getBlockState(tile.getPos());
        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        if (Minecraft.isAmbientOcclusionEnabled()) {
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
        } else {
            GlStateManager.shadeModel(GL11.GL_FLAT);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        // 围绕方块中心应用：缩放 + 旋转
        GlStateManager.translate(0.5f, 0.5f, 0.5f);
        GlStateManager.scale(scale, scale, scale);
        applyRotation(q);
        GlStateManager.translate(-0.5f, -0.5f, -0.5f);

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        buffer.setTranslation(-tile.getPos().getX(), -tile.getPos().getY(), -tile.getPos().getZ());
        dispatcher.getBlockModelRenderer().renderModel(tile.getWorld(), core, blockState, tile.getPos(), buffer, false);
        buffer.setTranslation(0, 0, 0);
        tessellator.draw();

        GlStateManager.popMatrix();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        RenderHelper.enableStandardItemLighting();
    }

    private static void applyRotation(Quat q) {
        // 归一化后转为轴角，交给 GlStateManager.rotate
        float len = (float) Math.sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w);
        if (len < 1e-8f) return;
        float x = q.x / len, y = q.y / len, z = q.z / len, w = q.w / len;
        w = Math.max(-1.0f, Math.min(1.0f, w));
        float angle = 2.0f * (float) Math.acos(w);
        float s = (float) Math.sqrt(1.0f - w * w);
        if (s < 1e-4f) {
            GlStateManager.rotate((float) Math.toDegrees(angle), 1.0f, 0.0f, 0.0f);
        } else {
            GlStateManager.rotate((float) Math.toDegrees(angle), x / s, y / s, z / s);
        }
    }

    private static float lerp(float from, float to, float time) {
        return from + (to - from) * time;
    }

    // 平滑插值
    private static float easeInOut(float t) {
        return t * t * (3f - 2f * t);
    }

    // 12 个固定轴，让不同 name 变化更明显
    private static float[] pickAxis(int idx) {
        idx = Math.floorMod(idx, 12);
        switch (idx) {
            case 0: return new float[]{1, 0, 0};
            case 1: return new float[]{0, 1, 0};
            case 2: return new float[]{0, 0, 1};
            case 3: return normalize(1, 1, 0);
            case 4: return normalize(1, 0, 1);
            case 5: return normalize(0, 1, 1);
            case 6: return normalize(1, -1, 0);
            case 7: return normalize(1, 0, -1);
            case 8: return normalize(0, 1, -1);
            case 9: return normalize(1, 1, 1);
            case 10: return normalize(1, 1, -1);
            default: return normalize(1, -1, 1);
        }
    }

    private static float[] normalize(float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        return new float[]{x / len, y / len, z / len};
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static long fnv1a64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    // 最小四元数，复刻 JOML Quaternionf 的 rotateAxis / mul / slerp 语义
    private static final class Quat {
        static final Quat IDENTITY = new Quat(0, 0, 0, 1);

        final float x, y, z, w;

        Quat(float x, float y, float z, float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        static Quat fromAxisAngle(float ax, float ay, float az, float angleDeg) {
            float hangle = (float) Math.toRadians(angleDeg) * 0.5f;
            float sinAngle = (float) Math.sin(hangle);
            float vLength = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            return new Quat(
                    ax / vLength * sinAngle,
                    ay / vLength * sinAngle,
                    az / vLength * sinAngle,
                    (float) Math.cos(hangle));
        }

        Quat mul(Quat q) {
            return new Quat(
                    w * q.x + x * q.w + y * q.z - z * q.y,
                    w * q.y - x * q.z + y * q.w + z * q.x,
                    w * q.z + x * q.y - y * q.x + z * q.w,
                    w * q.w - x * q.x - y * q.y - z * q.z);
        }

        // 等价于 JOML Quaternionf.slerp(target, alpha)
        static Quat slerp(Quat from, Quat to, float alpha) {
            float cosom = from.x * to.x + from.y * to.y + from.z * to.z + from.w * to.w;
            float absCosom = Math.abs(cosom);
            float scale0, scale1;
            if (1.0f - absCosom > 1e-6f) {
                float sinSqr = 1.0f - absCosom * absCosom;
                float sinom = 1.0f / (float) Math.sqrt(sinSqr);
                float omega = (float) Math.atan2(sinSqr * sinom, absCosom);
                scale0 = (float) Math.sin((1.0f - alpha) * omega) * sinom;
                scale1 = (float) Math.sin(alpha * omega) * sinom;
            } else {
                scale0 = 1.0f - alpha;
                scale1 = alpha;
            }
            scale1 = cosom >= 0.0f ? scale1 : -scale1;
            return new Quat(
                    scale0 * from.x + scale1 * to.x,
                    scale0 * from.y + scale1 * to.y,
                    scale0 * from.z + scale1 * to.z,
                    scale0 * from.w + scale1 * to.w);
        }
    }

    private static final class AnimParams {
        final int cycleTicks;
        final float phaseA, phaseB, phaseC;
        final float minScale;
        final Quat qA, qAB;

        AnimParams(int cycleTicks, float phaseA, float phaseB, float phaseC, float minScale, Quat qA, Quat qAB) {
            this.cycleTicks = cycleTicks;
            this.phaseA = phaseA;
            this.phaseB = phaseB;
            this.phaseC = phaseC;
            this.minScale = minScale;
            this.qA = qA;
            this.qAB = qAB;
        }

        static AnimParams fromName(String name) {
            long seed = fnv1a64(name);
            SplitMix64 rng = new SplitMix64(seed);

            // 周期：80~200 ticks
            int cycle = 80 + rng.nextIntBounded(121);

            // 三段占比：A 0.25~0.45, B 0.25~0.45, C 剩余且 >=0.15
            float a = 0.25f + rng.nextFloat() * 0.20f;
            float b = 0.25f + rng.nextFloat() * 0.20f;
            float c = 1.0f - a - b;
            if (c < 0.15f) {
                float deficit = 0.15f - c;
                a -= deficit * 0.5f;
                b -= deficit * 0.5f;
                c = 0.15f;
            }

            // 最小缩放：0.55~0.90
            float minScale = 0.55f + rng.nextFloat() * 0.35f;

            // 两段旋转轴：从若干方向里选，尽量不重复
            float[] axisA = pickAxis(rng.nextIntBounded(12));
            float[] axisB = pickAxis(rng.nextIntBounded(12));
            if (dot(axisA, axisB) > 0.85f) {
                axisB = pickAxis((rng.nextIntBounded(12) + 5) % 12);
            }

            // 角度：A 90~270, B 120~360
            float angA = 90f + rng.nextFloat() * 180f;
            float angB = 120f + rng.nextFloat() * 240f;
            if (rng.nextBoolean()) angA = -angA;
            if (rng.nextBoolean()) angB = -angB;

            Quat qA = Quat.fromAxisAngle(axisA[0], axisA[1], axisA[2], angA);
            Quat qB = Quat.fromAxisAngle(axisB[0], axisB[1], axisB[2], angB);
            Quat qAB = qA.mul(qB);

            return new AnimParams(cycle, a, b, c, minScale, qA, qAB);
        }
    }

    private static final class SplitMix64 {
        private long x;

        SplitMix64(long seed) {
            this.x = seed;
        }

        long nextLong() {
            long z = (x += 0x9E3779B97F4A7C15L);
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }

        float nextFloat() {
            return (nextLong() >>> 40) / (float) (1 << 24);
        }

        boolean nextBoolean() {
            return (nextLong() & 1L) != 0;
        }

        int nextIntBounded(int bound) {
            long r = nextLong();
            long m = (r >>> 1) % bound;
            return (int) m;
        }
    }
}
