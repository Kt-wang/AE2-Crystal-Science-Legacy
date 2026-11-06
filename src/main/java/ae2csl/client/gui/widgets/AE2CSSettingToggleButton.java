/*
 * AE2 Crystal Science - AE2CSSettingToggleButton (1.12.2)
 * 对照 1.21.1 源码 AE2CSSettingToggleButton/AECSServerSettingToggleButton。
 * fork 的 SettingToggleButton 外观注册是包私有，自定义 Setting 无法注册外观，
 * 因此用 BlitterIconButton 自行实现：外观(图标 Blitter + tooltip)由外部按枚举值提供，
 * 点击循环取值并通过 ConfigValueServerPacket 发到服务端。
 */
package ae2csl.client.gui.widgets;

import ae2.api.config.Setting;
import ae2.client.gui.AEBaseGui;
import ae2.client.gui.style.Blitter;
import ae2.core.network.InitNetwork;
import ae2.core.network.serverbound.ConfigValueServerPacket;
import ae2.util.EnumCycler;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

public class AE2CSSettingToggleButton<T extends Enum<T>> extends BlitterIconButton {

    private final Setting<T> buttonSetting;
    private final EnumSet<T> validValues;
    private final Function<T, Blitter> iconMapper;
    private final Function<T, List<ITextComponent>> tooltipMapper;
    private T currentValue;

    public AE2CSSettingToggleButton(Setting<T> setting, T val, Function<T, Blitter> iconMapper,
                                   Function<T, List<ITextComponent>> tooltipMapper) {
        super(() -> {
        }, () -> null);
        this.buttonSetting = setting;
        this.validValues = EnumSet.copyOf(setting.getValues());
        this.iconMapper = iconMapper;
        this.tooltipMapper = tooltipMapper;
        this.currentValue = val;
    }

    public Setting<T> getSetting() {
        return this.buttonSetting;
    }

    public T getCurrentValue() {
        return this.currentValue;
    }

    public void set(T e) {
        this.currentValue = e;
    }

    @Nullable
    @Override
    protected Blitter getBlitterIcon() {
        return iconMapper.apply(this.currentValue);
    }

    @Override
    public List<ITextComponent> getTooltipMessage() {
        List<ITextComponent> lines = tooltipMapper.apply(this.currentValue);
        return lines != null ? lines : Collections.emptyList();
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        boolean handled = super.mousePressed(mc, mouseX, mouseY);
        if (handled) {
            cycleValue();
        }
        return handled;
    }

    private void cycleValue() {
        boolean backwards = Minecraft.getMinecraft().currentScreen instanceof AEBaseGui<?> gui
            && gui.isHandlingRightClick();
        T next = EnumCycler.rotateEnum(this.currentValue, backwards, this.validValues);
        this.currentValue = next;
        if (Minecraft.getMinecraft().currentScreen instanceof AEBaseGui<?> gui) {
            InitNetwork.sendToServer(
                new ConfigValueServerPacket(gui.getContainer().windowId, this.buttonSetting, next));
        }
    }
}
