/*
 * AE2 Crystal Science - ContainerFrequencyBandLink
 * Modal sub-menu for the Ender Broadcaster; no inventory slots.
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerFrequencyBandLink extends AEBaseContainer {
    private final TileEntity tile;

    public ContainerFrequencyBandLink(InventoryPlayer playerInv, TileEntity tile) {
        super(playerInv, tile);
        this.tile = tile;
    }

    public TileEntity getTile() { return tile; }

    @Override public boolean canInteractWith(EntityPlayer p) { return true; }
}
