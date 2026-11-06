/*
 * AE2 Crystal Science - ContainerSideConfig
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;

public class ContainerSideConfig extends AEBaseContainer {

    public ContainerSideConfig(InventoryPlayer playerInv, TileEntity tile) {
        super(playerInv, tile);
        // 对齐源码：侧配界面不含任何槽位（96x101 小面板，无玩家背包）

        // 打开界面时把当前侧配同步给客户端（后续变更由 SideConfigActionPacket 的回包刷新）
        if (tile instanceof ae2csl.machine.ISideConfigHost
            && playerInv.player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            ae2csl.AE2CrystalScienceLegacy.NETWORK.sendTo(
                new ae2csl.network.SideConfigSyncPacket(tile.getPos(),
                    ((ae2csl.machine.ISideConfigHost) tile).getSideConfig().encode()),
                (net.minecraft.entity.player.EntityPlayerMP) playerInv.player);
        }
    }

    @Override
    public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) { return true; }
}
