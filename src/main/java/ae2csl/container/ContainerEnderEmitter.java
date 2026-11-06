/*
 * AE2 Crystal Science - ContainerEnderEmitter
 * Player inventory slots only; the emitter has no upgrade slots.
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2csl.tile.ender.TileEnderEmitter;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public class ContainerEnderEmitter extends AEBaseContainer {
    private final TileEnderEmitter tile;

    public ContainerEnderEmitter(InventoryPlayer pi, TileEnderEmitter t) {
        super(pi, t);
        this.tile = t;

        // Player inventory at bottom: texture slot borders at y=100/118/136 => items at y=84; hotbar y=141
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(pi, c + r * 9 + 9, 8 + c * 18, 84 + r * 18), SlotSemantics.PLAYER_INVENTORY);
        for (int c = 0; c < 9; c++)
            addSlot(new Slot(pi, c, 8 + c * 18, 142), SlotSemantics.PLAYER_HOTBAR);
    }

    public TileEnderEmitter getTile() { return tile; }

    @Override
    public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) { return true; }
}
