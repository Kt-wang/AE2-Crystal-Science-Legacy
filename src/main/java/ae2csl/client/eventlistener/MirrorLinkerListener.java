package ae2csl.client.eventlistener;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.api.ids.AE2CSConstants;
import ae2csl.item.linker.MirrorLinkerItem;
import ae2csl.network.MirrorLinkerBatchApplyPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(modid = AE2CSConstants.MODID, value = Side.CLIENT)
public class MirrorLinkerListener {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && !Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.objectMouseOver == null) return;
        if (mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK) return;

        if (!(mc.player.getHeldItemMainhand().getItem() instanceof MirrorLinkerItem)
            && !(mc.player.getHeldItemOffhand().getItem() instanceof MirrorLinkerItem)) return;
        if (mc.player.isSneaking()) return;

        BlockPos pos = mc.objectMouseOver.getBlockPos();
        Vec3d hit = mc.objectMouseOver.hitVec;
        boolean mainHand = mc.player.getHeldItemMainhand().getItem() instanceof MirrorLinkerItem;

        AE2CrystalScienceLegacy.NETWORK.sendToServer(new MirrorLinkerBatchApplyPacket(pos, mainHand, hit));
    }
}
