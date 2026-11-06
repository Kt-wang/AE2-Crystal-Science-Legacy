/*
 * AE2 Crystal Science - ContainerFrequencyBand
 * Modal sub-menu for the Ender Broadcaster; no inventory slots (list-only UI).
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerFrequencyBand extends AEBaseContainer {
    private final TileEntity tile;

    public ContainerFrequencyBand(InventoryPlayer playerInv, TileEntity tile) {
        super(playerInv, tile);
        this.tile = tile;
    }

    public TileEntity getTile() { return tile; }

    @Override public boolean canInteractWith(EntityPlayer p) { return true; }
}
