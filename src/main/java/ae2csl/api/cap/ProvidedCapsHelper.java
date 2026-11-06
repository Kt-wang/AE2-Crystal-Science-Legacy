/*
 * AE2 Crystal Science - API ProvidedCapsHelper
 * Ported from 1.21.1 source
 *
 * Discovers capability classes declared via @ProvideCaps annotation
 * on a class and its superclass/interface hierarchy.
 */
package ae2csl.api.cap;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ProvidedCapsHelper {

    public static Set<Class<?>> getProvidedClasses(Class<?> cls) {
        Set<Class<?>> out = new LinkedHashSet<>();
        collect(cls, out, new HashSet<>());
        return out;
    }

    private static void collect(Class<?> type, Set<Class<?>> out, Set<Class<?>> visited) {
        if (type == null || type == Object.class || !visited.add(type)) {
            return;
        }

        // Only get annotations declared on this type
        for (ProvideCaps a : type.getDeclaredAnnotationsByType(ProvideCaps.class)) {
            out.add(a.value());
        }

        // Superclass chain
        collect(type.getSuperclass(), out, visited);

        // Interface chain (including super-interfaces)1
        for (Class<?> itf : type.getInterfaces()) {
            collect(itf, out, visited);
        }
    }

    private ProvidedCapsHelper() {}
}
