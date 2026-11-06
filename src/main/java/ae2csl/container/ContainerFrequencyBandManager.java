/*
 * AE2 Crystal Science - ContainerFrequencyBandManager
 * Modal sub-menu for the Ender Broadcaster; no inventory slots.
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerFrequencyBandManager extends AEBaseContainer {
    private final TileEntity tile;

    public ContainerFrequencyBandManager(InventoryPlayer playerInv, TileEntity tile) {
        super(playerInv, tile);
        this.tile = tile;
    }

    public TileEntity getTile() { return tile; }

    @Override public boolean canInteractWith(EntityPlayer p) { return true; }
}
