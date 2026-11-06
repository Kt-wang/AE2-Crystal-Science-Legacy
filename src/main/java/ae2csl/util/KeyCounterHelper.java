package ae2csl.util;

import ae2.api.stacks.KeyCounter;

public final class KeyCounterHelper {
    private KeyCounterHelper() {}
    public static KeyCounter deepCopy(KeyCounter src) { KeyCounter copy = new KeyCounter(); copy.addAll(src); return copy; }
}
