/*
 * AE2 Crystal Science - Machine Component Interface
 * Ported from 1.21.1: common/machine/component/IMachineComponent.java
 * Ports identify which I/O side a component slot belongs to.
 */
package ae2csl.machine.component;

public enum InvPort {
    /** Main working input */
    WORK,
    /** Primary input */
    INPUT,
    /** Primary output */
    OUTPUT,
    /** Upgrades */
    UPGRADES,
    /** Configuration */
    CONFIG;

    public boolean canOutput() {
        return this == OUTPUT || this == WORK;
    }

    public boolean canInput() {
        return this == INPUT || this == WORK || this == UPGRADES || this == CONFIG;
    }
}
