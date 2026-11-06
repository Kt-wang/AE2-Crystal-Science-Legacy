/*
 * AE2 Crystal Science - EnderInterfacePart (1.12.2)
 * 对照 1.21.1 源码 common/me/part/EnderInterfacePart.java
 * 末影接口的线缆部件形态（吸收掉落物版接口）。
 * 注：部件形态的范围线框动态渲染 1.12.2 暂无对应基建（CableBus TESR），暂缺。
 */
package ae2csl.me.part;

import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.core.AppEng;
import ae2.items.parts.PartModels;
import ae2.parts.PartModel;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.client.gui.GuiHandler;
import ae2csl.me.logic.EnderInterfaceHost;
import ae2csl.me.logic.EnderInterfaceLogic;
import ae2csl.me.logic.IntegratedInterfaceLogic;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class EnderInterfacePart extends IntegratedInterfacePart implements EnderInterfaceHost {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(Tags.MOD_ID,
        "part/ender_interface/base");
    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_off"));
    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_on"));
    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE,
        AppEng.makeId("part/interface_has_channel"));

    public EnderInterfacePart(IPartItem<?> partItem) {
        super(partItem);
    }

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
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return MODELS_ON;
        } else {
            return MODELS_OFF;
        }
    }

    @Override
    public boolean onUseWithoutItem(EntityPlayer player, Vec3d pos) {
        if (!player.world.isRemote) {
            BlockPos tilePos = this.getTileEntity().getPos();
            player.openGui(AE2CrystalScienceLegacy.INSTANCE,
                GuiHandler.GUI_ENDER_INTERFACE_PART + this.getSide().ordinal(),
                player.world, tilePos.getX(), tilePos.getY(), tilePos.getZ());
        }
        return true;
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, EntityPlayer player, EnumHand hand, Vec3d pos) {
        if (super.onUseItemOn(heldItem, player, hand, pos)) {
            return true;
        }
        return this.onUseWithoutItem(player, pos);
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ae2.container.ISubGui subGui) {
        BlockPos tilePos = this.getTileEntity().getPos();
        player.openGui(AE2CrystalScienceLegacy.INSTANCE,
            GuiHandler.GUI_ENDER_INTERFACE_PART + this.getSide().ordinal(),
            player.world, tilePos.getX(), tilePos.getY(), tilePos.getZ());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return this.getPartItem().asItemStack();
    }

    @Override
    public void markForLogicClientUpdate() {
        if (this.getTileEntity().getWorld() != null && !this.getTileEntity().getWorld().isRemote) {
            this.getHost().markForUpdate();
        }
    }

    // ---- 范围显示的客户端同步（对照源码 writeToStream/readFromStream） ----

    @Override
    public void writeToStream(PacketBuffer data) {
        super.writeToStream(data);
        data.writeBoolean(this.getEnderInterfaceLogic().isRenderRangeInClient());
        data.writeInt(this.getEnderInterfaceLogic().getRange());
    }

    @Override
    public boolean readFromStream(PacketBuffer data) {
        super.readFromStream(data);
        this.getEnderInterfaceLogic().setRenderRangeInClient(data.readBoolean());
        this.getEnderInterfaceLogic().setRange(data.readInt());
        return true;
    }
}
