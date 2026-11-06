/*
 * AE2 Crystal Science - API CustomReturnableSubMenu Interface
 * Ported from 1.21.1 source
 *
 * Used to specify which menu a SubMenu should return to.
 * Must be used together with CustomReturnableSubMenuHost.
 *
 * NOTE: AE2-Supergiant (1.12.2) does not have ISubMenu/ISubMenuHost/MenuOpener.
 * This interface serves as a placeholder for future submenu implementation.
 */
package ae2csl.api.submenu;

public interface CustomReturnableSubMenu {

    /**
     * Get the menu type this submenu should return to.
     * In 1.12.2 this returns a GuiHandler ID.
     */
    int getReturnToMenuId();
}
