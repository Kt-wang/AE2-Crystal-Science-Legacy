/*
 * AE2 Crystal Science - CableBusBlockEntityMixin (1.12.2)
 * When a Part is added/replaced/removed on a CableBus, notify nearby EnderEmitters.
 */
package ae2csl.mixin;

import ae2.api.parts.IPart;
import ae2.api.parts.IPartItem;
import ae2.tile.networking.TileCableBus;
import ae2csl.tile.ender.TileEnderEmitter;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(value = TileCableBus.class, remap = false)
public class CableBusBlockEntityMixin {

    @Inject(method = "addPart", at = @At("RETURN"), remap = false)
    public void ae2cs$addPart(IPartItem<? extends IPart> partItem, EnumFacing side, @Nullable net.minecraft.entity.player.EntityPlayer player,
                              CallbackInfoReturnable<? extends IPart> cir) {
        if (cir.getReturnValue() != null) {
            TileEntity self = (TileEntity) (Object) this;
            World world = self.getWorld();
            if (world != null && !world.isRemote) {
                TileEnderEmitter.addPosToRecentEmitter(world, self.getPos());
            }
        }
    }

    @Inject(method = "replacePart", at = @At("RETURN"), remap = false)
    public void ae2cs$replacePart(IPartItem<? extends IPart> partItem, @Nullable EnumFacing side,
                                   net.minecraft.entity.player.EntityPlayer owner, EnumHand hand,
                                   CallbackInfoReturnable<? extends IPart> cir) {
        if (cir.getReturnValue() != null) {
            TileEntity self = (TileEntity) (Object) this;
            World world = self.getWorld();
            if (world != null && !world.isRemote) {
                TileEnderEmitter.addPosToRecentEmitter(world, self.getPos());
            }
        }
    }

    @Inject(method = "removePart", at = @At("RETURN"), remap = false)
    public void ae2cs$removePart(IPart part, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() != null && cir.getReturnValue()) {
            TileEntity self = (TileEntity) (Object) this;
            World world = self.getWorld();
            if (world != null && !world.isRemote) {
                TileEnderEmitter.addPosToRecentEmitter(world, self.getPos());
            }
        }
    }
}
