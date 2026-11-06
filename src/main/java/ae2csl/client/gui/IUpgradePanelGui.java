/*
 * AE2 Crystal Science - IUpgradePanelGui (1.12.2)
 * Implemented by GUIs that render an AE2CSUpgradePanel, exposes its
 * screen-space bounds (used for JEI exclusion areas).
 */
package ae2csl.client.gui;

import java.awt.Rectangle;

public interface IUpgradePanelGui {

    /** @return screen-space bounds of the upgrade panel. */
    Rectangle getUpgradePanelArea();
}
