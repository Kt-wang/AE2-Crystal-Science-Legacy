/*
 * AE2 Crystal Science - GridNodeMixin (1.12.2)
 * Intercepts GridNode.getMaxChannels() to allow CustomChannelProviderHost overrides.
 */
package ae2csl.mixin;

import ae2.me.GridNode;
import ae2csl.api.CustomChannelProviderHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GridNode.class, remap = false)
public abstract class GridNodeMixin {

    @Shadow(remap = false)
    public abstract Object getOwner();

    @Inject(method = "getMaxChannels", at = @At("HEAD"), cancellable = true, remap = false)
    public void ae2cs$getMaxChannels(CallbackInfoReturnable<Integer> cir) {
        if (getOwner() instanceof CustomChannelProviderHost) {
            CustomChannelProviderHost host = (CustomChannelProviderHost) getOwner();
            if (host.isEnabledCustomChannel()) {
                cir.setReturnValue(host.getMaxChannels());
            }
        }
    }
}
