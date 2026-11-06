/*
 * AE2 Crystal Science - AdaptedAE2Icon (1.12.2)
 * 把 AE2 fork 内建图标（ae2.client.gui.Icon，如 CLEAR/BACK）适配为 IButtonIcon。
 * 对齐 1.21.1 源码的 AdaptedAE2Icon。
 */
package ae2csl.client.gui.icon;

import ae2.client.gui.Icon;
import ae2.client.gui.style.Blitter;

public class AdaptedAE2Icon implements IButtonIcon {

    public static final AdaptedAE2Icon CLEAR = new AdaptedAE2Icon(Icon.CLEAR);
    public static final AdaptedAE2Icon BACK = new AdaptedAE2Icon(Icon.BACK);

    private final Icon icon;

    public AdaptedAE2Icon(Icon icon) {
        this.icon = icon;
    }

    @Override
    public Blitter getBlitter() {
        return icon.getBlitter();
    }
}
