/*
 * AE2 Crystal Science - Machine Component Container
 * Manages components attached to a machine. Ported from common/machine/MachineComponentContainer.java
 */
package ae2csl.machine;

import ae2csl.machine.component.InvPort;
import ae2csl.machine.component.GenericStackInvComponent;
import net.minecraft.inventory.IInventory;
import javax.annotation.Nullable;
import java.util.*;

public class MachineComponentContainer {

    private final Map<Class<?>, Object> services = new HashMap<>();
    private final List<GenericStackInvComponent> invComponents = new ArrayList<>();
    @Nullable
    private IMachineHost host;

    public void setHost(IMachineHost host) { this.host = host; }
    public IMachineHost host() { return host; }

    public <T> void addService(Class<T> type, T service) { services.put(type, service); }

    public <T> boolean hasService(Class<T> type) { return services.containsKey(type); }

    public <T> void exposeService(Class<T> type, T service) { addService(type, service); }

    public void addInventory(GenericStackInvComponent inv) {
        invComponents.add(inv);
        addService(GenericStackInvComponent.class, inv);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> type) { return (T) services.get(type); }

    public <T> T get(Class<T> type) { return getService(type); }

    public List<GenericStackInvComponent> getInventories() { return invComponents; }

    public IInventory getInventory(InvPort port) {
        for (GenericStackInvComponent inv : invComponents) {
            if (inv.getPort() == port) return inv.getInventory();
        }
        return null;
    }

    public IInventory getInternalInventory() {
        IInventory inv = getInventory(InvPort.WORK);
        if (inv == null) inv = getInventory(InvPort.INPUT);
        return inv;
    }
}
