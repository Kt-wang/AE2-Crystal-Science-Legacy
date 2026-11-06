/*
 * AE2 Crystal Science - AEBaseTileMixin (1.12.2)
 *
 * 1.12.2 的 IPart 没有逐 tick 回调，部件的工作只能靠网格 IGridTickable。
 * fork 的 PatternProviderLogic 已经占用了主节点的 IGridTickable 服务（返回仓/发送队列），
 * 不能再替换。这里在 AEBaseTile.update() 末尾补一刀：只对 IPartHost（线缆总线）生效，
 * 服务端遍历六个面，把谐振样板供应器部件的逻辑 tickServer() 驱动起来
 * （待发队列 / 拉动模式 / 本地待发不再只在 pushPattern 时冲刷）。
 */
package ae2csl.mixin;

import ae2.api.parts.IPartHost;
import ae2.tile.AEBaseTile;
import ae2csl.me.part.ResonatingPatternProviderPart;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AEBaseTile.class, remap = false)
public abstract class AEBaseTileMixin {

    @Inject(method = "update", at = @At("TAIL"), remap = false)
    private void ae2cs$tickResonatingPartLogic(CallbackInfo ci) {
        if (!((Object) this instanceof IPartHost partHost)) {
            return;
        }

        TileEntity self = (TileEntity) (Object) this;
        World world = self.getWorld();
        if (world == null || world.isRemote) {
            return;
        }

        for (EnumFacing side : EnumFacing.VALUES) {
            if (partHost.getPart(side) instanceof ResonatingPatternProviderPart part) {
                part.getResonatingLogic().tickServer();
            }
        }
    }
}
