/*
 * AE2 Crystal Science - ContainerEnderBroadcaster
 * Main area 176×215 with a vertical toolbar on the left.
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2csl.tile.ender.TileEnderBroadcaster;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public class ContainerEnderBroadcaster extends AEBaseContainer {
    private final TileEnderBroadcaster tile;

    public ContainerEnderBroadcaster(InventoryPlayer pi, TileEnderBroadcaster t) {
        super(pi, t);
        this.tile = t;

        // Player inventory at bottom: y=133 for main inv, y=191 for hotbar
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(pi, c + r * 9 + 9, 8 + c * 18, 131 + r * 18), SlotSemantics.PLAYER_INVENTORY);
        for (int c = 0; c < 9; c++)
            addSlot(new Slot(pi, c, 8 + c * 18, 189), SlotSemantics.PLAYER_HOTBAR);
    }

    public TileEnderBroadcaster getTile() { return tile; }

    @Override
    public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) { return true; }
}
