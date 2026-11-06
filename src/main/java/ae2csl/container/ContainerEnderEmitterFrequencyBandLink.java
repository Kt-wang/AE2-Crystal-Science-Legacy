/*
 * AE2 Crystal Science - ContainerEnderEmitterFrequencyBandLink
 * Modal sub-menu for the Ender Emitter; no inventory slots.
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerEnderEmitterFrequencyBandLink extends AEBaseContainer {
    public ContainerEnderEmitterFrequencyBandLink(InventoryPlayer pi, TileEntity t) {
        super(pi, t);
    }

    @Override public boolean canInteractWith(EntityPlayer p) { return true; }
}
