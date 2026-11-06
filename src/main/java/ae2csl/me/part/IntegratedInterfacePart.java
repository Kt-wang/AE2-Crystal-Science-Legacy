/*
 * AE2 Crystal Science - IntegratedInterfacePart (1.12.2)
 * 对照 1.21.1 源码 common/me/part/IntegratedInterfacePart.java
 * 集成接口的线缆部件形态。GUI 打开在阶段C接入 port GuiHandler。
 */
package ae2csl.me.part;

import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.IManagedGridNode;
import ae2.api.parts.IPartCollisionHelper;
import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.api.stacks.AEItemKey;
import ae2.api.util.AECableType;
import ae2.core.AppEng;
import ae2.items.parts.PartModels;
import ae2.parts.AEBasePart;
import ae2.parts.PartModel;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.client.gui.GuiHandler;
import ae2csl.me.logic.IntegratedInterfaceHost;
import ae2csl.me.logic.IntegratedInterfaceLogic;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;

public class IntegratedInterfacePart extends AEBasePart implements IntegratedInterfaceHost {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(Tags.MOD_ID,
        "part/integrated_interface/base");
    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_off"));
    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_on"));
    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE,
        AppEng.makeId("part/interface_has_channel"));

    private static final IGridNodeListener<IntegratedInterfacePart> NODE_LISTENER = new NodeListener<>() {
        @Override
        public void onStateChanged(IntegratedInterfacePart nodeOwner, IGridNode node, State state) {
            super.onStateChanged(nodeOwner, node, state);
            nodeOwner.getLogic().onMainNodeStateChanged();
        }
    };

    private final IntegratedInterfaceLogic logic = this.createLogic();
    private int priority;

    public IntegratedInterfacePart(IPartItem<?> partItem) {
        super(partItem);
    }

    protected IntegratedInterfaceLogic createLogic() {
        int size = isExtended() ? 36 : 9;
        return new IntegratedInterfaceLogic(this.getMainNode(), this, size, size);
    }

    @Override
    protected IManagedGridNode createMainNode() {
        return ae2.api.networking.GridHelper.createManagedNode(this, NODE_LISTENER);
    }

    @Override
    public boolean isExtended() {
        return false; // EX 变体由子类覆盖（阶段D）
    }

    @Override
    public IPartModel getStaticModels() {
        if (isActive() && isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (isPowered()) {
            return MODELS_ON;
        } else {
            return MODELS_OFF;
        }
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(5, 5, 12, 11, 11, 14);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.logic.readFromNBT(data);
        this.priority = data.getInteger("priority");
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.logic.writeToNBT(data);
        data.setInteger("priority", this.priority);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        this.logic.addDrops(drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.logic.clearContent();
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 4;
    }

    @Override
    public boolean onUseWithoutItem(EntityPlayer player, Vec3d pos) {
        if (!player.world.isRemote) {
            BlockPos tilePos = this.getTileEntity().getPos();
            player.openGui(AE2CrystalScienceLegacy.INSTANCE,
                GuiHandler.GUI_INTEGRATED_INTERFACE_PART + this.getSide().ordinal(),
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

    /**
     * 子界面返回：重开部件形态主GUI
     */
    @Override
    public void returnToMainContainer(EntityPlayer player, ae2.container.ISubGui subGui) {
        BlockPos tilePos = this.getTileEntity().getPos();
        player.openGui(AE2CrystalScienceLegacy.INSTANCE,
            GuiHandler.GUI_INTEGRATED_INTERFACE_PART + this.getSide().ordinal(),
            player.world, tilePos.getX(), tilePos.getY(), tilePos.getZ());
    }

    // ---- IntegratedInterfaceHost ----

    @Override
    public IntegratedInterfaceLogic getLogic() {
        return this.logic;
    }

    @Override
    public EnumSet<EnumFacing> getTargets() {
        // 部件形态只向安装面推送
        return EnumSet.of(this.getSide());
    }

    @Override
    public void saveChanges() {
        this.getHost().markForSave();
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(getMainMenuIcon());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return this.getPartItem().asItemStack();
    }

    // ---- IPriorityHost ----

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int newValue) {
        this.priority = newValue;
        saveChanges();
    }

    // ---- 记忆卡 ----

    @Override
    public void exportSettings(ae2.util.SettingsFrom mode, NBTTagCompound output) {
        super.exportSettings(mode, output);
        if (mode == ae2.util.SettingsFrom.MEMORY_CARD) {
            this.logic.exportSettings(output);
            output.setInteger("priority", this.priority);
        }
    }

    @Override
    public void importSettings(ae2.util.SettingsFrom mode, NBTTagCompound input,
                               @javax.annotation.Nullable EntityPlayer player) {
        super.importSettings(mode, input, player);
        if (mode == ae2.util.SettingsFrom.MEMORY_CARD) {
            this.logic.importSettings(input, player);
            if (input.hasKey("priority", net.minecraftforge.common.util.Constants.NBT.TAG_INT)) {
                setPriority(input.getInteger("priority"));
            }
        }
    }

    @Override
    public void onNeighborChanged(net.minecraft.world.IBlockAccess level, net.minecraft.util.math.BlockPos pos,
                                  net.minecraft.util.math.BlockPos neighbor) {
        this.logic.updateRedstoneState();
    }
}
