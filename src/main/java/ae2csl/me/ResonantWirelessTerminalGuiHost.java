/*
 * AE2 Crystal Science - ResonantWirelessTerminalGuiHost (1.12.2)
 *
 * 对照 1.21.1 源码 WirelessResonantTerminalMenuHost：无线谐振终端的手持宿主。
 * 继承 AE2 的无线样板编码终端宿主（自带样板编码逻辑与能量/链接处理），
 * 额外提供本模组的谐振编码开关，并把编码库存扩容到 144/36。
 */
package ae2csl.me;

import ae2.container.ISubGui;
import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.helpers.WirelessPatternEncodingTerminalGuiHost;
import ae2.items.tools.powered.WirelessTerminalItem;
import ae2.items.tools.powered.WirelessTerminals;
import ae2csl.me.part.IResonantTemplateCodingTerminalHost;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.function.BiConsumer;

public class ResonantWirelessTerminalGuiHost extends WirelessPatternEncodingTerminalGuiHost
    implements IResonantTemplateCodingTerminalHost {

    private static final String TAG_ENCODE_RESONATING = "EncodeResonatingPattern";

    /** 默认开启：无线终端也只产出谐振样板 */
    private boolean encodeResonatingPattern = true;

    public ResonantWirelessTerminalGuiHost(WirelessTerminalItem stackItem, WirelessTerminalItem terminalItem,
                                           EntityPlayer player, ItemGuiHostLocator locator,
                                           BiConsumer<EntityPlayer, ISubGui> returnToMainGui) {
        super(stackItem, terminalItem, player, locator, returnToMainGui);
        // 容量扩容（144 输入 / 36 输出），必须在容器创建之前
        ResonantPatternEncodingCapacity.expand(getLogic());

        NBTTagCompound data = WirelessTerminals.getExistingTerminalData(getItemStack(), terminalItem);
        if (data != null && data.hasKey(TAG_ENCODE_RESONATING)) {
            this.encodeResonatingPattern = data.getBoolean(TAG_ENCODE_RESONATING);
        }
    }

    @Override
    public boolean isPullRecipeInputsToRealGrid() {
        return false;
    }

    @Override
    public void setPullRecipeInputsToRealGrid(boolean value) {
        // 无线终端暂不支持抽取输入
    }

    @Override
    public boolean isPulledAnvilMode() {
        return false;
    }

    @Override
    public void setPulledAnvilMode(boolean value) {
        // 无线终端暂不支持铁砧模式
    }

    @Override
    public boolean isEncodeResonatingPattern() {
        return encodeResonatingPattern;
    }

    @Override
    public void setEncodeResonatingPattern(boolean value) {
        this.encodeResonatingPattern = value;
        WirelessTerminals.getTerminalData(getItemStack(), getTerminalItem())
            .setBoolean(TAG_ENCODE_RESONATING, value);
    }
}
