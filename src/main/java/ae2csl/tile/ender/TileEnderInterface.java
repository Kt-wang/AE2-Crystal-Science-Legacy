/*
 * AE2 Crystal Science - TileEnderInterface (1.12.2)
 * 对照 1.21.1 源码 common/block/entity/EnderInterfaceBlockEntity.java
 * 常规 ME 接口能力 + 掉落物吸收。复用 TileIntegratedInterface 的宿主实现。
 */
package ae2csl.tile.ender;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.client.gui.GuiHandler;
import ae2csl.init.AE2CSBlocks;
import ae2csl.me.logic.EnderInterfaceHost;
import ae2csl.me.logic.EnderInterfaceLogic;
import ae2csl.me.logic.IntegratedInterfaceLogic;
import ae2csl.tile.integrated.TileIntegratedInterface;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class TileEnderInterface extends TileIntegratedInterface implements EnderInterfaceHost {

    @Override
    protected IntegratedInterfaceLogic createLogic() {
        int absorbConfigSlots = isExtended() ? 36 : 18;
        return new EnderInterfaceLogic(this.getMainNode(), this, 9, absorbConfigSlots);
    }

    @Override
    public EnderInterfaceLogic getEnderInterfaceLogic() {
        return (EnderInterfaceLogic) getLogic();
    }

    @Override
    public void markForLogicClientUpdate() {
        if (world != null && !world.isRemote) {
            this.markForClientUpdate();
        }
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(AE2CSBlocks.ENDER_INTERFACE_BLOCK);
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(AE2CSBlocks.ENDER_INTERFACE_BLOCK);
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ae2.container.ISubGui subGui) {
        player.openGui(AE2CrystalScienceLegacy.INSTANCE, GuiHandler.GUI_ENDER_INTERFACE, world,
            pos.getX(), pos.getY(), pos.getZ());
    }

    // ---- 范围显示的客户端同步（对照源码 writeToStream/readFromStream） ----

    @Override
    protected void writeToStream(ByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.getEnderInterfaceLogic().isRenderRangeInClient());
        data.writeInt(this.getEnderInterfaceLogic().getRange());
    }

    @Override
    protected boolean readFromStream(ByteBuf data) {
        boolean changed = super.readFromStream(data);
        this.getEnderInterfaceLogic().setRenderRangeInClient(data.readBoolean());
        this.getEnderInterfaceLogic().setRange(data.readInt());
        return true;
    }
}
