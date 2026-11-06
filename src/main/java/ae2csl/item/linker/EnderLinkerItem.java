/*
 * AE2 Crystal Science - EnderLinkerItem (1.12.2)
 * Ported from 1.21.1 EnderLinkerItem#onItemUseFirst.
 *
 * Shift + 右键末影发信器：把发信器坐标绑定到物品（源码用 ENDER_EMITTER_POS
 * 数据组件，1.12.2 用物品 NBT：维度 int + BlockPos long 代替 GlobalPos）。
 * 右键 ME 网络节点宿主方块（IInWorldGridNodeHost，对齐源码
 * IN_WORLD_GRID_NODE_HOST capability）：把目标手动链接到绑定的发信器；
 * 对已链接/待链接的目标再次右键则解除链接。跨维度使用无效。
 */
package ae2csl.item.linker;

import ae2.api.networking.IInWorldGridNodeHost;
import ae2csl.tile.ender.TileEnderEmitter;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;

import java.util.List;

public class EnderLinkerItem extends Item {

    private static final String TAG_EMITTER_DIM = "emitterDim";
    private static final String TAG_EMITTER_POS = "emitterPos";

    public EnderLinkerItem() {
        setMaxStackSize(1);
        setTranslationKey("ender_linker");
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                           float hitX, float hitY, float hitZ, EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) return EnumActionResult.PASS;
        if (player == null) return EnumActionResult.PASS;
        if (world.isRemote) {
            return player.isSneaking() ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
        }

        ItemStack stack = player.getHeldItem(hand);
        TileEntity te = world.getTileEntity(pos);

        if (player.isSneaking() && te instanceof TileEnderEmitter) {
            // 绑定发信器：维度 + 坐标写入物品 NBT
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            tag.setInteger(TAG_EMITTER_DIM, world.provider.getDimension());
            tag.setLong(TAG_EMITTER_POS, pos.toLong());
            player.sendStatusMessage(new TextComponentTranslation("ae2csl.msg.item.ender_linker.bing_to_emitter"), true);
            return EnumActionResult.SUCCESS;
        } else if (!player.isSneaking() && te instanceof IInWorldGridNodeHost) {
            // 右键目标方块：完成/解除手动链接（未绑定或跨维度时直接吞掉交互）
            BlockPos emitterPos = getBoundEmitterPos(stack, world);
            if (emitterPos == null) return EnumActionResult.SUCCESS;

            TileEntity emitterTe = world.getTileEntity(emitterPos);
            if (emitterTe instanceof TileEnderEmitter) {
                TileEnderEmitter emitter = (TileEnderEmitter) emitterTe;
                if (emitter.getLinkedRenderPositionsSnapshot().contains(pos)
                        || emitter.getPendingRenderPositionsSnapshot().contains(pos)) {
                    TileEnderEmitter.removePosFromEmitter(emitter, pos);
                } else if (!emitter.getPos().equals(pos)) {
                    // 如果能成功添加的话，我们额外做一次移除，以实现类似换绑的功能
                    if (TileEnderEmitter.addPosToEmitter(emitter, pos, true, false)) {
                        TileEnderEmitter.removePosFromRecentEmitter(world, pos);
                        TileEnderEmitter.addPosToEmitter(emitter, pos, true, false);
                        player.sendStatusMessage(
                                new TextComponentTranslation("ae2csl.msg.item.ender_linker.success_to_link"), true);
                    } else {
                        player.sendStatusMessage(
                                new TextComponentTranslation("ae2csl.msg.item.ender_linker.failed_to_link"), true);
                    }
                }
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    /** 读取绑定的发信器坐标；未绑定或跨维度时返回 null（对齐源码 GlobalPos 维度校验）。 */
    private static BlockPos getBoundEmitterPos(ItemStack stack, World world) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_EMITTER_POS)) return null;
        if (tag.getInteger(TAG_EMITTER_DIM) != world.provider.getDimension()) return null;
        return BlockPos.fromLong(tag.getLong(TAG_EMITTER_POS));
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§7" + net.minecraft.client.resources.I18n.format("ae2csl.item.ender_linker.tooltip"));
        // 显示绑定的发信器位置；未绑定时显示未绑定状态（键与镜面连接器共用）
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_EMITTER_POS)) {
            tooltip.add(net.minecraft.util.text.TextFormatting.GRAY
                    + net.minecraft.client.resources.I18n.format("ae2csl.tooltip.unbound"));
        } else {
            BlockPos emitterPos = BlockPos.fromLong(tag.getLong(TAG_EMITTER_POS));
            tooltip.add(net.minecraft.util.text.TextFormatting.GRAY
                    + net.minecraft.client.resources.I18n.format("ae2csl.tooltip.target",
                            emitterPos.getX(), emitterPos.getY(), emitterPos.getZ()));
        }
    }
}
