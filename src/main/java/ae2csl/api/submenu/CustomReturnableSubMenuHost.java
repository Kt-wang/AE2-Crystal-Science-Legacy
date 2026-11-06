/*
 * AE2 Crystal Science - API CustomReturnableSubMenuHost Interface
 * Ported from 1.21.1 source
 *
 * Host that supports returning to a specific menu from a submenu.
 * Must be used together with CustomReturnableSubMenu.
 *
 * NOTE: AE2-Supergiant (1.12.2) does not have ISubMenuHost.
 * This interface serves as a placeholder for future submenu implementation.
 */
package ae2csl.api.submenu;

import net.minecraft.entity.player.EntityPlayer;

public interface CustomReturnableSubMenuHost {

    /**
     * Return from a submenu to the main menu.
     * In 1.12.2 this will be wired when the full GUI system is ported.
     */
    default void returnToMainMenu(EntityPlayer player, CustomReturnableSubMenu subMenu) {
        // Placeholder - implementation depends on GuiHandler setup
        // Will use FMLNetworkHandler.openGui() with the return menu ID
        player.closeScreen();
    }
}
