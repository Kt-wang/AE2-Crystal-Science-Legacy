/*
 * AE2 Crystal Science - AEBaseTileBlockMixin (1.12.2)
 *
 * fork 的记忆卡交互把设置直接写进物品 NBT 的固定槽（单槽）。谐振记忆卡有 9 个槽，
 * 这里拦截 useMemoryCard：持有谐振记忆卡时按「当前选中槽」保存/应用，
 * 其余记忆卡行为保持不变。
 */
package ae2csl.mixin;

import ae2.block.AEBaseTileBlock;
import ae2csl.item.linker.ResonatingMemoryCardHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AEBaseTileBlock.class, remap = false)
public abstract class AEBaseTileBlockMixin {

    @Inject(method = "useMemoryCard", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2cs$useResonatingMemoryCard(World world, BlockPos pos, ItemStack cardStack,
                                               EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (ResonatingMemoryCardHelper.tryUseOnBlock(world, pos, cardStack, player)) {
            cir.setReturnValue(true);
        }
    }
}
