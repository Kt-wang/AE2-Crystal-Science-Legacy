/*
 * AE2 Crystal Science - ResonantTemplateCodingTerminalPart (1.12.2)
 *
 * 对照 1.21.1：继承 AE2 的样板编码终端部件（1.12.2 为 ae2.parts.encoding.PatternEncodingTerminalPart），
 * 从而直接获得编码逻辑（编码网格、样板槽、模式切换、自动填充等）。
 * 额外保存三个本模组开关，并用本模组自己的 GUI：
 *  - 抽取配方输入到实际网格
 *  - 铁砧模式
 *  - 编码为谐振样板
 */
package ae2csl.me.part;

import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.core.AppEng;
import ae2.items.parts.PartModels;
import ae2.parts.PartModel;
import ae2.parts.encoding.PatternEncodingTerminalPart;
import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.Tags;
import ae2csl.client.gui.GuiHandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ResonantTemplateCodingTerminalPart extends PatternEncodingTerminalPart
    implements IResonantTemplateCodingTerminalHost {

    private static final String TAG_PULL_RECIPE_INPUTS = "PullRecipeInputsToRealGrid";
    private static final String TAG_PULLED_ANVIL_MODE = "PulledAnvilMode";
    private static final String TAG_ENCODE_RESONATING = "EncodeResonatingPattern";

    public static final ResourceLocation MODEL_BASE =
        new ResourceLocation(Tags.MOD_ID, "part/resonant_template_coding_terminal/base");
    @PartModels public static final PartModel MODELS_OFF = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_off"));
    @PartModels public static final PartModel MODELS_ON = new PartModel(MODEL_BASE, AppEng.makeId("part/interface_on"));
    @PartModels public static final PartModel MODELS_HAS_CHANNEL =
        new PartModel(MODEL_BASE, AppEng.makeId("part/interface_has_channel"));

    private boolean pullRecipeInputsToRealGrid;
    private boolean pulledAnvilMode;
    private boolean encodeResonatingPattern;

    public ResonantTemplateCodingTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override public IPartModel getStaticModels() {
        if (isActive() && isPowered()) {
            return MODELS_HAS_CHANNEL;
        }
        return isPowered() ? MODELS_ON : MODELS_OFF;
    }

    /** 打开本模组自己的编码终端 GUI（id = 基值 + 面序号） */
    @Override
    public boolean onUseWithoutItem(EntityPlayer player, Vec3d pos) {
        if (!player.world.isRemote) {
            BlockPos tilePos = getTileEntity().getPos();
            player.openGui(AE2CrystalScienceLegacy.INSTANCE,
                GuiHandler.GUI_RESONANT_TEMPLATE_CODING_TERM_PART + getSide().ordinal(),
                player.world, tilePos.getX(), tilePos.getY(), tilePos.getZ());
        }
        return true;
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, EntityPlayer player, net.minecraft.util.EnumHand hand, Vec3d pos) {
        if (super.onUseItemOn(heldItem, player, hand, pos)) {
            return true;
        }
        return onUseWithoutItem(player, pos);
    }

    // ---- NBT ----

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        pullRecipeInputsToRealGrid = data.getBoolean(TAG_PULL_RECIPE_INPUTS);
        pulledAnvilMode = data.getBoolean(TAG_PULLED_ANVIL_MODE);
        encodeResonatingPattern = data.getBoolean(TAG_ENCODE_RESONATING);
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean(TAG_PULL_RECIPE_INPUTS, pullRecipeInputsToRealGrid);
        data.setBoolean(TAG_PULLED_ANVIL_MODE, pulledAnvilMode);
        data.setBoolean(TAG_ENCODE_RESONATING, encodeResonatingPattern);
    }

    // ---- IResonantTemplateCodingTerminalHost ----

    @Override public boolean isPullRecipeInputsToRealGrid() { return pullRecipeInputsToRealGrid; }

    @Override
    public void setPullRecipeInputsToRealGrid(boolean value) {
        if (pullRecipeInputsToRealGrid == value) {
            return;
        }
        pullRecipeInputsToRealGrid = value;
        if (!value) {
            pulledAnvilMode = false;
        }
        saveChanges();
    }

    @Override public boolean isPulledAnvilMode() { return pulledAnvilMode; }

    @Override
    public void setPulledAnvilMode(boolean value) {
        value = value && pullRecipeInputsToRealGrid;
        if (pulledAnvilMode == value) {
            return;
        }
        pulledAnvilMode = value;
        saveChanges();
    }

    @Override public boolean isEncodeResonatingPattern() { return encodeResonatingPattern; }

    @Override
    public void setEncodeResonatingPattern(boolean value) {
        if (encodeResonatingPattern == value) {
            return;
        }
        encodeResonatingPattern = value;
        saveChanges();
    }

    public void saveChanges() {
        getHost().markForSave();
    }

    public ItemStack getMainContainerIcon() {
        return new ItemStack(getPartItem().asItem());
    }
}
