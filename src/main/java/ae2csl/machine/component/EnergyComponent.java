/*
 * AE2 Crystal Science - Energy Component
 * Wraps a machine's local AE power buffer for component access.
 * Ported from common/machine/component/EnergyComponent.java
 */
package ae2csl.machine.component;

/**
 * Wraps power buffer access for machines using the AENetworkedPoweredTile.
 * The host TileEntity implements IAEPowerStorage; this component provides
 * a unified interface for other machine components to query power.
 */
public class EnergyComponent {

    private final IPowerProvider host;

    public EnergyComponent(IPowerProvider host) {
        this.host = host;
    }

    public double getCurrentPower() { return host.getStoredPower(); }
    public double getMaxPower() { return host.getMaxPower(); }
    public boolean hasPower(double amount) { return getCurrentPower() >= amount; }
    public double consumePower(double amount) { return host.consumePower(amount); }
    public double storePower(double amount) { return host.storePower(amount); }

    /** Interface for machines that provide power */
    public interface IPowerProvider {
        double getStoredPower();
        double getMaxPower();
        double consumePower(double amount);
        double storePower(double amount);
    }
}
