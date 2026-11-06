/*
 * AE2 Crystal Science - SidePolicy (1.12.2)
 */
package ae2csl.machine.component;

public enum SidePolicy {

    INSERT(true, false),
    EXTRACT(false, true),
    NONE(false, false),
    ALL(true, true);

    private final boolean allowInsert;
    private final boolean allowExtract;

    SidePolicy(boolean allowInsert, boolean allowExtract) {
        this.allowInsert = allowInsert;
        this.allowExtract = allowExtract;
    }

    public boolean allowExtract() { return allowExtract; }
    public boolean allowInsert() { return allowInsert; }
}
