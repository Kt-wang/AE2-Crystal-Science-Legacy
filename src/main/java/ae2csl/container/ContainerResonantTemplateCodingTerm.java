/*
 * AE2 Crystal Science - ContainerResonantTemplateCodingTerm (1.12.2)
 *
 * 直接复用 AE2 的样板编码终端容器（编码网格、样板槽、模式切换等），
 * 本模组只额外暴露自己的开关接口。
 */
package ae2csl.container;

import ae2.container.me.patternencode.ContainerPatternEncodingTerm;
import ae2csl.me.part.IResonantTemplateCodingTerminalHost;

import net.minecraft.entity.player.InventoryPlayer;

public class ContainerResonantTemplateCodingTerm extends ContainerPatternEncodingTerm {

    private final IResonantTemplateCodingTerminalHost host;

    public ContainerResonantTemplateCodingTerm(InventoryPlayer ip,
                                               ae2.helpers.IPatternTerminalGuiHost guiHost,
                                               IResonantTemplateCodingTerminalHost host) {
        super(ip, guiHost);
        this.host = host;
    }

    public IResonantTemplateCodingTerminalHost getResonantHost() {
        return host;
    }
}
