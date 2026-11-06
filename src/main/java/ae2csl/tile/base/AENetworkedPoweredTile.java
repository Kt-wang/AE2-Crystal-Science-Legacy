/*
 * AE2 Crystal Science - Self-Powered Networked TileEntity
 * Machines that consume AE power from the grid.
 */
package ae2csl.tile.base;

import ae2.api.config.Actionable;
import ae2.api.config.PowerMultiplier;
import ae2.api.networking.energy.IAEPowerStorage;
import ae2.api.config.AccessRestriction;
import ae2.me.ManagedGridNode;


public abstract class AENetworkedPoweredTile extends AENetworkedTileBase implements IAEPowerStorage {

    private double currentPower;
    private double maxPower;
    private boolean serviceRegistered = false;

    protected AENetworkedPoweredTile(double maxPower) {
        this.maxPower = maxPower;
        this.currentPower = 0;
    }

    @Override
    public ManagedGridNode getMainNode() {
        ManagedGridNode node = super.getMainNode();
        if (node != null && !serviceRegistered) {
            node.addService(IAEPowerStorage.class, this);
            serviceRegistered = true;
        }
        return node;
    }

    @Override
    protected void onMainNodeDestroyed() {
        // The node is recreated on the next onLoad(), so the power service must be re-registered.
        serviceRegistered = false;
    }

    // ---- IAEPowerStorage ----
    @Override public double getAECurrentPower() { return currentPower; }
    @Override public double getAEMaxPower() { return maxPower; }
    @Override public boolean isAEPublicPowerStorage() { return true; }
    @Override public AccessRestriction getPowerFlow() { return AccessRestriction.READ_WRITE; }

    @Override
    public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) {
        double extracted = Math.min(amount, currentPower);
        if (mode == Actionable.MODULATE) {
            currentPower -= extracted;
        }
        return extracted;
    }

    @Override
    public double injectAEPower(double amount, Actionable mode) {
        double space = maxPower - currentPower;
        double added = Math.min(amount, space);
        if (mode == Actionable.MODULATE) {
            currentPower += added;
        }
        return added;
    }

    // Accessors
    public double getCurrentPower() { return currentPower; }
    public double getMaxPower() { return maxPower; }
    protected void setCurrentPower(double p) { this.currentPower = Math.min(p, maxPower); }
    protected void setMaxPower(double p) { this.maxPower = p; }

    /**
     * Try to draw power, first from internal buffer, then from the AE grid's energy service.
     * This mirrors how AE2's own TileInscriber.selectEnergySource() works.
     *
     * @param amount The amount of AE power needed
     * @return The amount of power actually consumed (maybe less than requested)
     */
    protected double tryDrawPower(double amount) {
        // 1) Use internal buffer first
        if (currentPower >= amount) {
            currentPower -= amount;
            return amount;
        }

        // 2) Try to pull directly from the AE grid
        ManagedGridNode node = getMainNode();
        if (node != null) {
            var grid = node.getGrid();
            if (grid != null) {
                var energy = grid.getEnergyService();
                if (energy != null) {
                    double fromGrid = energy.extractAEPower(amount, Actionable.MODULATE, PowerMultiplier.ONE);
                    if (fromGrid >= amount) {
                        return amount;
                    }
                    // Partial from grid, remainder from internal
                    if (fromGrid > 0 || currentPower > 0) {
                        double total = fromGrid + currentPower;
                        double consumed = Math.min(total, amount);
                        currentPower = Math.max(0, total - consumed);
                        return consumed;
                    }
                }
            }
        }

        return 0;
    }

    /**
     * Pull power from the AE grid into this machine's internal buffer.
     * The internal buffer is what the GUI energy bar displays, so machines
     * must charge it from the grid before consuming from it.
     *
     * @param amount The maximum amount of power to charge this tick
     * @return The amount of power actually charged into the buffer
     */
    protected double chargeFromGrid(double amount) {
        double space = maxPower - currentPower;
        if (space <= 0) return 0;
        double toCharge = Math.min(amount, space);

        ManagedGridNode node = getMainNode();
        if (node != null) {
            var grid = node.getGrid();
            if (grid != null) {
                var energy = grid.getEnergyService();
                if (energy != null) {
                    double charged = energy.extractAEPower(toCharge, Actionable.MODULATE, PowerMultiplier.ONE);
                    currentPower += charged;
                    return charged;
                }
            }
        }
        return 0;
    }

    /**
     * Push power from internal buffer to the AE grid.
     * Used by generators (e.g. Crystal Vibration Chamber) to actively output power.
     *
     * @param amount The amount of AE power to push
     * @return The amount of power actually pushed to the grid (may be less than requested)
     */
    protected double tryPushPower(double amount) {
        double available = Math.min(amount, currentPower);
        if (available <= 0) return 0;

        ManagedGridNode node = getMainNode();
        if (node != null) {
            var grid = node.getGrid();
            if (grid != null) {
                var energy = grid.getEnergyService();
                if (energy != null) {
                    double leftover = energy.injectPower(available, Actionable.MODULATE);
                    double pushed = available - leftover;
                    if (pushed > 0) {
                        currentPower -= pushed;
                        return pushed;
                    }
                }
            }
        }
        return 0;
    }
}
