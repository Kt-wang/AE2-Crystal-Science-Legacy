/*
 * AE2 Crystal Science - IButtonIcon
 * Provides a Blitter that describes how to render a button icon.
 * Reduced 1.12.2 port of the 1.21.1 source's IButtonIcon.
 */
package ae2csl.client.gui.icon;

import ae2.client.gui.style.Blitter;

@FunctionalInterface
public interface IButtonIcon {
    Blitter getBlitter();
}
