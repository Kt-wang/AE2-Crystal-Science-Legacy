/*
 * AE2 Crystal Science - MeteoritePatternProviderPart (1.12.2)
 */
package ae2csl.me.part;

import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.api.stacks.AEItemKey;
import ae2.core.AppEng;
import ae2.helpers.patternprovider.PatternProviderLogic;
import ae2.items.parts.PartModels;
import ae2.parts.PartModel;
import ae2.parts.crafting.PatternProviderPart;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.me.logic.MeteoritePatternProviderHost;
import ae2csl.me.logic.MeteoritePatternProviderLogic;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class MeteoritePatternProviderPart extends PatternProviderPart implements MeteoritePatternProviderHost {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(Tags.MOD_ID, "part/meteorite_pattern_provider/base");

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_off"));
    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_on"));
    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_has_channel"));

    public MeteoritePatternProviderPart(IPartItem<?> partItem) {
        super(partItem);
    }

    /**
     * 部件没有 Tile.update()，逻辑的 tickServer（产物入仓 + 工作轮刷新）
     * 注册到 AEPlugin 的服务器 tick 统一驱动；方块形态由 Tile.update 驱动。
     */
    @Override
    public void addToWorld() {
        super.addToWorld();
        if (getLogic() instanceof MeteoritePatternProviderLogic logic) {
            ae2csl.me.AEPlugin.registerMeteoritePartLogic(logic);
        }
    }

    @Override
    public void removeFromWorld() {
        if (getLogic() instanceof MeteoritePatternProviderLogic logic) {
            ae2csl.me.AEPlugin.unregisterMeteoritePartLogic(logic);
        }
        super.removeFromWorld();
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) return MODELS_HAS_CHANNEL;
        else if (this.isPowered()) return MODELS_ON;
        else return MODELS_OFF;
    }

    @Override
    protected PatternProviderLogic createLogic() {
        return new MeteoritePatternProviderLogic(getMainNode(), this, 63);
    }

    /**
     * 走自己的 GUI（fork 的 ContainerPatternProvider 容器层会拒绝合成样板）
     */
    @Override
    public boolean onUseWithoutItem(net.minecraft.entity.player.EntityPlayer player, net.minecraft.util.math.Vec3d pos) {
        if (!player.world.isRemote) {
            net.minecraft.util.math.BlockPos tilePos = this.getTileEntity().getPos();
            player.openGui(AE2CrystalScienceLegacy.INSTANCE,
                ae2csl.client.gui.GuiHandler.GUI_METEORITE_PATTERN_PROVIDER_PART + this.getSide().ordinal(),
                player.world, tilePos.getX(), tilePos.getY(), tilePos.getZ());
        }
        return true;
    }

    @Override
    public boolean onUseItemOn(net.minecraft.item.ItemStack heldItem, net.minecraft.entity.player.EntityPlayer player,
                               net.minecraft.util.EnumHand hand, net.minecraft.util.math.Vec3d pos) {
        if (super.onUseItemOn(heldItem, player, hand, pos)) {
            return true;
        }
        return this.onUseWithoutItem(player, pos);
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(getPartItem().asItem());
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return new ItemStack(getPartItem().asItem());
    }
}
