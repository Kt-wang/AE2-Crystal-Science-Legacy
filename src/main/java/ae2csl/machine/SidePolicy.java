/*
 * AE2 Crystal Science - SidePolicy (1.12.2)
 * 侧面策略（对齐 1.21.1 源码 SidePolicy）：
 * 控制自动化（漏斗/管道）能力暴露与主动搬运方向。
 */
package ae2csl.machine;

public enum SidePolicy {

    ALL(true, true),
    INSERT(true, false),
    EXTRACT(false, true),
    NONE(false, false);

    private final boolean allowInsert;
    private final boolean allowExtract;

    SidePolicy(boolean allowInsert, boolean allowExtract) {
        this.allowInsert = allowInsert;
        this.allowExtract = allowExtract;
    }

    public boolean allowInsert() { return allowInsert; }
    public boolean allowExtract() { return allowExtract; }

    /** GUI 点击时的循环顺序（对齐源码 ALL→INSERT→EXTRACT→NONE） */
    public SidePolicy next() {
        return values()[(ordinal() + 1) & 3];
    }
}
