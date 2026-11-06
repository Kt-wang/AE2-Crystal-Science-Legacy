package ae2csl.util;

import ae2.api.config.Setting;
import ae2.api.util.IConfigManager;
import ae2.helpers.patternprovider.PatternProviderLogicHost;
import ae2csl.me.logic.MirrorPatternProviderLogic;
import net.minecraft.nbt.NBTTagCompound;
import java.util.Map;
import java.util.Set;

public class MirrorConfigManager implements IConfigManager {
    private final MirrorPatternProviderLogic host;
    private final IConfigManager delegate;

    public MirrorConfigManager(MirrorPatternProviderLogic host, IConfigManager delegate) {
        this.host = host; this.delegate = delegate;
    }

    @Override public Set<Setting<?>> getSettings() { return delegate.getSettings(); }

    @Override public <T extends Enum<T>> T getSetting(Setting<T> setting) {
        PatternProviderLogicHost target = host.cachedTarget.get();
        if (target != null) return target.getConfigManager().getSetting(setting);
        return delegate.getSetting(setting);
    }

    @Override public <T extends Enum<T>> void putSetting(Setting<T> setting, T value) { delegate.putSetting(setting, value); }
    @Override public void writeToNBT(NBTTagCompound tag) { delegate.writeToNBT(tag); }
    @Override public void readFromNBT(NBTTagCompound tag) { delegate.readFromNBT(tag); }
    @Override public boolean importSettings(Map<String, String> settings) { return delegate.importSettings(settings); }
    @Override public Map<String, String> exportSettings() { return delegate.exportSettings(); }
}
