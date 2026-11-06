/*
 * AE2 Crystal Science - SideConfigActionPacket (1.12.2)
 * 客户端侧配界面操作：循环某面策略 / 切换自动输入 / 切换自动输出。
 * 服务端应用后回发 SideConfigSyncPacket 刷新界面。
 */
package ae2csl.network;

import ae2csl.AE2CrystalScienceLegacy;
import ae2csl.machine.ISideConfigHost;
import ae2csl.machine.SideConfigData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SideConfigActionPacket implements IMessage {

    public static final int ACTION_CYCLE_SIDE = 0;
    public static final int ACTION_TOGGLE_AUTO_IMPORT = 1;
    public static final int ACTION_TOGGLE_AUTO_EXPORT = 2;
    public static final int ACTION_CLEAR_SIDES = 3;

    private BlockPos pos;
    private int action;
    private int sideOrdinal;

    public SideConfigActionPacket() {}

    private SideConfigActionPacket(BlockPos pos, int action, int sideOrdinal) {
        this.pos = pos; this.action = action; this.sideOrdinal = sideOrdinal;
    }

    public static SideConfigActionPacket cycleSide(BlockPos pos, EnumFacing side) {
        return new SideConfigActionPacket(pos, ACTION_CYCLE_SIDE, side.ordinal());
    }
    public static SideConfigActionPacket toggleAutoImport(BlockPos pos) {
        return new SideConfigActionPacket(pos, ACTION_TOGGLE_AUTO_IMPORT, 0);
    }
    public static SideConfigActionPacket toggleAutoExport(BlockPos pos) {
        return new SideConfigActionPacket(pos, ACTION_TOGGLE_AUTO_EXPORT, 0);
    }
    public static SideConfigActionPacket clearSides(BlockPos pos) {
        return new SideConfigActionPacket(pos, ACTION_CLEAR_SIDES, 0);
    }

    @Override public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        action = buf.readByte();
        sideOrdinal = buf.readByte();
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX()); buf.writeInt(pos.getY()); buf.writeInt(pos.getZ());
        buf.writeByte(action);
        buf.writeByte(sideOrdinal);
    }

    public static class Handler implements IMessageHandler<SideConfigActionPacket, IMessage> {
        @Override
        public IMessage onMessage(SideConfigActionPacket msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            ((WorldServer) player.world).addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(msg.pos);
                if (!(te instanceof ISideConfigHost)) return;
                SideConfigData cfg = ((ISideConfigHost) te).getSideConfig();
                switch (msg.action) {
                    case ACTION_CYCLE_SIDE:
                        if (msg.sideOrdinal >= 0 && msg.sideOrdinal < EnumFacing.VALUES.length) {
                            cfg.cycle(EnumFacing.VALUES[msg.sideOrdinal]);
                        }
                        break;
                    case ACTION_TOGGLE_AUTO_IMPORT:
                        cfg.setAutoImport(!cfg.isAutoImport());
                        break;
                    case ACTION_TOGGLE_AUTO_EXPORT:
                        cfg.setAutoExport(!cfg.isAutoExport());
                        break;
                    case ACTION_CLEAR_SIDES:
                        // 对齐源码 onClearSideDirPolicy：所有面置为禁用
                        for (EnumFacing f : EnumFacing.VALUES) cfg.set(f, ae2csl.machine.SidePolicy.NONE);
                        break;
                    default:
                        return;
                }
                AE2CrystalScienceLegacy.NETWORK.sendTo(
                    new SideConfigSyncPacket(msg.pos, cfg.encode()), player);
            });
            return null;
        }
    }
}
