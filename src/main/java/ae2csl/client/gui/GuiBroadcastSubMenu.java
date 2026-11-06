/*
 * AE2 Crystal Science - GuiBroadcastSubMenu
 * Shared base for the Ender Broadcaster sub-menus (band list / create / link / manager / whitelist).
 * Provides the broadcaster pos, data-request helper and back-navigation helper.
 */
package ae2csl.client.gui;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.network.EnderBroadcasterActionPacket;
import ae2csl.network.FrequencyBandActionPacket;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public abstract class GuiBroadcastSubMenu extends GuiContainer {

    protected final TileEntity tile;
    protected final BlockPos tilePos;
    protected int tick;

    public GuiBroadcastSubMenu(Container container, TileEntity tile) {
        super(container);
        this.tile = tile;
        this.tilePos = tile != null ? tile.getPos() : BlockPos.ORIGIN;
    }

    protected void requestData(byte action) {
        AE2CrystalScienceLegacy.NETWORK.sendToServer(new FrequencyBandActionPacket(tilePos, action));
    }

    protected void navigate(byte action) {
        AE2CrystalScienceLegacy.NETWORK.sendToServer(new EnderBroadcasterActionPacket(tilePos, action));
    }

    /** Draw a grayed hint inside a text field when it is empty and not focused. */
    protected void drawTextFieldPlaceholder(GuiTextField field, String key) {
        if (field.getText().isEmpty() && !field.isFocused()) {
            // AE2CSTextField inlays its text at (x, y) (background drawing disabled), so the
            // placeholder must align to the field's top-left rather than the vanilla +4 inset.
            fontRenderer.drawString(I18n.format(key),
                    field.x, field.y, 0x707070);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        tick++;
    }
}
