/*
 * AE2 Crystal Science - Generic Stack Inv Component
 * Wraps a vanilla IInventory with a port identifier.
 * Ported from common/machine/component/GenericStackInvComponent.java
 */
package ae2csl.machine.component;

import net.minecraft.inventory.IInventory;
import java.util.*;

public class GenericStackInvComponent {

    private final Map<InvPort, IInventory> ports = new LinkedHashMap<>();

    /** Register an inventory under a specific port */
    public void addPort(InvPort port, IInventory inventory) {
        ports.put(port, inventory);
    }

    /** Get inventory for a specific port */
    public IInventory port(InvPort port) {
        return ports.get(port);
    }

    /** Get all ports */
    public Collection<InvPort> getPorts() {
        return ports.keySet();
    }

    public IInventory getInventory() {
        return port(InvPort.WORK);
    }

    public InvPort getPort() {
        return ports.keySet().iterator().next();
    }
}
