/*
 * AE2 Crystal Science - GrowthAcceleratorBlockEntityMixin (1.12.2)
 * Accelerates CrystalSeedItem growth when seeds are in water near a GrowthAccelerator.
 * Mirrors the 1.21.1 source: inject at TAIL of TileGrowthAccelerator.onTick(int).
 */
package ae2csl.mixin;

import ae2.tile.misc.TileGrowthAccelerator;
import ae2csl.item.crystal.CrystalSeedItem;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = TileGrowthAccelerator.class, remap = false)
public abstract class GrowthAcceleratorBlockEntityMixin extends TileEntity {

    @Inject(method = "onTick", at = @At("TAIL"), remap = false)
    private void ae2cs$onTick(int ticksSinceLastCall, CallbackInfo ci) {
        if (this.getWorld() == null || this.getWorld().isRemote) return;

        BlockPos pos = this.getPos();
        double range = 2.0;
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(range);
        List<EntityItem> items = this.getWorld().getEntitiesWithinAABB(
                EntityItem.class, box,
                e -> (!e.getItem().isEmpty() && e.getItem().getItem() instanceof CrystalSeedItem && e.isInWater()));

        for (EntityItem item : items) {
            ItemStack grown = CrystalSeedItem.grow(item.getItem(), ticksSinceLastCall);
            item.setItem(grown);
        }
    }
}
