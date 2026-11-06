/*
 * AE2 Crystal Science - API PatternHelper
 * Ported from 1.21.1 source
 *
 * Helper for condensing pattern stacks (merging duplicate types).
 */
package ae2csl.api.util;

import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class PatternHelper {

    /**
     * Given a potentially sparse stack array with possible duplicates,
     * produce a new array with no null elements and each type appearing once,
     * preserving original order.
     * <p>
     * This code is copied from AE2.
     */
    public static List<GenericStack> condenseStacks(List<GenericStack> sparseInput) {
        LinkedHashMap<AEKey, Long> map = new LinkedHashMap<>();

        for (GenericStack input : sparseInput) {
            if (input != null) {
                map.merge(input.what(), input.amount(), Long::sum);
            }
        }

        if (map.isEmpty()) {
            throw new IllegalStateException("No pattern here!");
        }

        List<GenericStack> out = new ArrayList<>(map.size());
        for (var entry : map.entrySet()) {
            out.add(new GenericStack(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    private PatternHelper() {}
}
