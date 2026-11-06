/*
 * AE2 Crystal Science
 */

package ae2csl.core;

import net.minecraft.util.ResourceLocation;

/**
 * Base interface for common mod functionality.
 * This is implemented by both AppEngServer and AppEngClient.
 */
public interface AppEng {

    String MOD_ID = "ae2csl";
    String MOD_NAME = "AE2 Crystal Science: Legacy";

    static ResourceLocation makeId(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    static ResourceLocation makeId(String name, String path) {
        return new ResourceLocation(name, path);
    }
}