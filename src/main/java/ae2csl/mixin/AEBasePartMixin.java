/*
 * AE2 Crystal Science - AEBasePartMixin (1.12.2)
 *
 * 与 AEBaseTileBlockMixin 同理：持有谐振记忆卡时，按当前选中槽保存/应用部件设置。
 */
package ae2csl.mixin;

import ae2.parts.AEBasePart;
import ae2csl.item.linker.ResonatingMemoryCardHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AEBasePart.class, remap = false)
public abstract class AEBasePartMixin {

    @Inject(method = "useMemoryCard", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2cs$useResonatingMemoryCard(ItemStack cardStack, EntityPlayer player,
                                               CallbackInfoReturnable<Boolean> cir) {
        AEBasePart self = (AEBasePart) (Object) this;
        if (ResonatingMemoryCardHelper.tryUseOnPart(self, cardStack, player)) {
            cir.setReturnValue(true);
        }
    }
}
