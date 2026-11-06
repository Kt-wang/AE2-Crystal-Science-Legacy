/*
 * AE2 Crystal Science - ContainerSetStockAmount (1.12.2)
 * 移植自 fork 的 ContainerSetStockAmount，host 类型换成 IntegratedInterfaceHost。
 * 打开路径: ContainerIntegratedInterface.openSetAmountMenu → 暂存 pending → openGui(GUI_II_SET_STOCK_AMOUNT)
 * （fork 的 SwitchGuisPacket 子GUI系统把 SET_STOCK_AMOUNT 硬编码为 InterfaceLogicHost，无法复用）
 */
package ae2csl.container;

import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.container.AEBaseContainer;
import ae2.container.ISubGui;
import ae2.container.SlotSemantics;
import ae2.container.guisync.GuiSync;
import ae2.container.slot.InaccessibleSlot;
import ae2.helpers.externalstorage.GenericStackInv;
import ae2.util.inv.AppEngInternalInventory;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.me.logic.IntegratedInterfaceHost;

import com.google.common.primitives.Ints;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ContainerSetStockAmount extends AEBaseContainer implements ISubGui {

    public static final String ACTION_SET_STOCK_AMOUNT = "setStockAmount";

    /** 服务端暂存：玩家即将打开的设置数量界面的初始数据 */
    private static final Map<UUID, PendingOpen> PENDING = new ConcurrentHashMap<>();

    private record PendingOpen(IntegratedInterfaceHost host, int slot, AEKey whatToStock, int initialAmount) {
    }

    private final InaccessibleSlot stockedItem;
    private final IntegratedInterfaceHost host;

    @Nullable
    private AEKey whatToStock;
    @GuiSync(1)
    private int initialAmount = -1;
    @GuiSync(2)
    private int maxAmount = -1;
    @GuiSync(3)
    private int slot = -1;

    public ContainerSetStockAmount(InventoryPlayer ip, IntegratedInterfaceHost host) {
        super(ip, host);
        registerClientAction(ACTION_SET_STOCK_AMOUNT, Integer.class, this::confirm);
        this.host = host;
        this.stockedItem = new InaccessibleSlot(new AppEngInternalInventory(1), 0);
        this.addSlot(this.stockedItem, SlotSemantics.MACHINE_OUTPUT);
    }

    /**
     * 服务端：暂存初始数据并打开子GUI
     */
    public static void open(EntityPlayer player, IntegratedInterfaceHost host, int slot, AEKey whatToStock,
                            int initialAmount) {
        TileEntity tile = host.getTileEntity();
        PENDING.put(player.getUniqueID(), new PendingOpen(host, slot, whatToStock, initialAmount));
        int guiId;
        if (host instanceof ae2csl.me.part.IntegratedInterfacePart part) {
            guiId = GuiHandler.GUI_II_SET_STOCK_AMOUNT_PART + part.getSide().ordinal();
        } else {
            guiId = GuiHandler.GUI_II_SET_STOCK_AMOUNT;
        }
        player.openGui(AE2CrystalScienceLegacy.INSTANCE, guiId, tile.getWorld(),
            tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
    }

    /**
     * 服务端 GuiHandler 调用：取出暂存数据并初始化容器
     */
    public static ContainerSetStockAmount createFromPending(InventoryPlayer ip, IntegratedInterfaceHost host) {
        ContainerSetStockAmount container = new ContainerSetStockAmount(ip, host);
        PendingOpen pending = PENDING.remove(ip.player.getUniqueID());
        if (pending != null && pending.host() == host) {
            container.setWhatToStock(pending.slot(), pending.whatToStock(), pending.initialAmount());
        }
        return container;
    }

    private void setWhatToStock(int slot, AEKey whatToStock, int initialAmount) {
        this.slot = slot;
        this.whatToStock = Objects.requireNonNull(whatToStock, "whatToStock");
        this.initialAmount = initialAmount;
        this.maxAmount = Ints.saturatedCast(host.getConfigInv().getMaxAmount(whatToStock));
        this.stockedItem.putStack(whatToStock.wrapForDisplayOrFilter());
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public void confirm(int amount) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_STOCK_AMOUNT, amount);
            return;
        }

        GenericStackInv config = host.getConfigInv();
        if (this.whatToStock == null || this.slot < 0 || this.slot >= config.size()) {
            host.returnToMainContainer(getPlayer(), this);
            return;
        }

        if (!Objects.equals(config.getKey(this.slot), whatToStock)) {
            host.returnToMainContainer(getPlayer(), this);
            return;
        }

        amount = (int) Math.min(amount, config.getMaxAmount(whatToStock));
        if (amount <= 0) {
            config.setStack(slot, null);
        } else {
            config.setStack(slot, new GenericStack(whatToStock, amount));
        }
        host.returnToMainContainer(getPlayer(), this);
    }

    public int getInitialAmount() {
        return initialAmount;
    }

    @Nullable
    public AEKey getWhatToStock() {
        GenericStack stack = GenericStack.fromItemStack(stockedItem.getStack());
        return stack != null ? stack.what() : null;
    }

    @Override
    public IntegratedInterfaceHost getHost() {
        return this.host;
    }
}
