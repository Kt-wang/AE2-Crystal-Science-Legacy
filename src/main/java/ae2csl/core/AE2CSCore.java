/*
 * AE2 Crystal Science - AE2CSCore (1.12.2)
 * Coremod + early mixin loader. Registers mixins.ae2csl.json so the port's
 * mixins (GridNodeMixin etc.) actually get loaded. Mirrors AE2-Supergiant's
 * ae2.core.mixins.AE2Core.
 */
package ae2csl.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name("AE2CSCore")
@IFMLLoadingPlugin.SortingIndex(-1)
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class AE2CSCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Nullable
    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Nullable
    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Nullable
    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public List<String> getMixinConfigs() {
        return java.util.Collections.singletonList("mixins.ae2csl.json");
    }
}
