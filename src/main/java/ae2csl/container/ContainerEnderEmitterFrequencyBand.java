/*
 * AE2 Crystal Science - ContainerEnderEmitterFrequencyBand
 * Modal sub-menu for the Ender Emitter; no inventory slots (list-only UI).
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerEnderEmitterFrequencyBand extends AEBaseContainer {
    public ContainerEnderEmitterFrequencyBand(InventoryPlayer playerInv, TileEntity tile) {
        super(playerInv, tile);
    }

    @Override public boolean canInteractWith(EntityPlayer p) { return true; }
}
