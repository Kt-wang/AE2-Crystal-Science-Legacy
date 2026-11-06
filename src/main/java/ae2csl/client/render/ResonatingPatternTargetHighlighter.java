/*
 * AE2 Crystal Science - ResonatingPatternTargetHighlighter (1.12.2)
 * 对照源码 client/render/ResonatingPatternTargetHighlighter.java
 * 手持谐振样板时渲染目标面：选中=绿色半透明，未选中=蓝色半透明。
 * 1.12.2 用 RenderWorldLastEvent + 手工 GL 四边形（无 RenderLevelStageEvent/VertexConsumer 基建）。
 */
package ae2csl.client.render;

import ae2csl.me.crafting.EncodedResonatingPattern;
import ae2csl.me.crafting.ResonatingPatternDetails;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import java.util.Optional;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = "ae2csl", value = Side.CLIENT)
public final class ResonatingPatternTargetHighlighter {

    // 选中：绿色 + 半透明
    private static final float SEL_R = 0f, SEL_G = 1f, SEL_B = 0f;
    // 未选中：蓝色 + 半透明
    private static final float UNS_R = 0f, UNS_G = 0.31f, UNS_B = 1f;
    private static final float A = 0.35f;
    // 防Z-fighting
    private static final double EPS = 0.002;

    private ResonatingPatternTargetHighlighter() {
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) {
            return;
        }

        ItemStack stack = getHeldResonatingPattern(player);
        if (stack.isEmpty()) {
            return;
        }

        EncodedResonatingPattern encoded = readEncoded(stack);
        if (encoded == null) {
            return;
        }

        int size = encoded.getSparseInputs().size();
        if (size <= 0) {
            return;
        }

        // 选中槽位索引（存于物品 NBT）
        int sel = 0;
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("resonating_selected_input")) {
            sel = ResonatingPatternDetails.clampSelected(
                stack.getTagCompound().getInteger("resonating_selected_input"), size);
        }

        World level = player.world;
        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-camX, -camY, -camZ);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < size; i++) {
            Optional<EncodedResonatingPattern.Target> opt = encoded.targetOfSparseInput(i);
            if (opt.isEmpty()) {
                continue;
            }

            EncodedResonatingPattern.Target t = opt.get();

            // 跨维度不渲染
            if (level.provider.getDimension() != t.getPos().getDimension()) {
                continue;
            }

            BlockPos pos = t.getPos().getPos();
            if (!level.isBlockLoaded(pos)) {
                continue;
            }

            boolean selected = (i == sel);
            if (selected) {
                drawFaceQuad(buffer, pos, t.getFace(), SEL_R, SEL_G, SEL_B);
            } else {
                drawFaceQuad(buffer, pos, t.getFace(), UNS_R, UNS_G, UNS_B);
            }
        }

        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static ItemStack getHeldResonatingPattern(EntityPlayer player) {
        ItemStack main = player.getHeldItemMainhand();
        if (readEncoded(main) != null) {
            return main;
        }
        ItemStack off = player.getHeldItemOffhand();
        if (readEncoded(off) != null) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    private static EncodedResonatingPattern readEncoded(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return null;
        }
        net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey(ResonatingPatternDetails.TAG_ENCODED, 10)) {
            return null;
        }
        try {
            return EncodedResonatingPattern.readFromNBT(tag.getCompoundTag(ResonatingPatternDetails.TAG_ENCODED));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 在目标面外侧画一个四边形（EPS 外扩防 Z-fighting）
     */
    private static void drawFaceQuad(BufferBuilder buffer, BlockPos pos, EnumFacing face,
                                     float r, float g, float b) {
        double x1 = pos.getX() - EPS, y1 = pos.getY() - EPS, z1 = pos.getZ() - EPS;
        double x2 = pos.getX() + 1 + EPS, y2 = pos.getY() + 1 + EPS, z2 = pos.getZ() + 1 + EPS;

        switch (face) {
            case UP -> quad(buffer, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, r, g, b);
            case DOWN -> quad(buffer, x1, y1, z1, x1, y1, z2, x2, y1, z2, x2, y1, z1, r, g, b);
            case NORTH -> quad(buffer, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, r, g, b);
            case SOUTH -> quad(buffer, x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2, r, g, b);
            case WEST -> quad(buffer, x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2, r, g, b);
            case EAST -> quad(buffer, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, r, g, b);
        }
    }

    private static void quad(BufferBuilder buffer,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4,
                             float r, float g, float b) {
        buffer.pos(x1, y1, z1).color(r, g, b, A).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, A).endVertex();
        buffer.pos(x3, y3, z3).color(r, g, b, A).endVertex();
        buffer.pos(x4, y4, z4).color(r, g, b, A).endVertex();
    }
}
