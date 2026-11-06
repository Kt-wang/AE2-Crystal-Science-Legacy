/*
 * AE2 Crystal Science - FrequencyBandSyncPacket (1.12.2)
 * Server -> Client data for the Ender Broadcaster sub-GUIs (pull model).
 */
package ae2csl.network;

import ae2csl.api.linker.broadcast.networking.BroadcastBandsField;
import ae2csl.api.linker.broadcast.networking.FrequencyBandDetailInfo;
import ae2csl.api.networking.ServerPlayerInfo;
import ae2csl.client.gui.GuiBandWhiteListManager;
import ae2csl.client.gui.GuiEnderEmitterFrequencyBand;
import ae2csl.client.gui.GuiFrequencyBand;
import ae2csl.client.gui.GuiFrequencyBandManager;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class FrequencyBandSyncPacket implements IMessage {

    public static final byte BANDS_LIST  = 0;
    public static final byte BAND_DETAIL = 1;
    public static final byte WHITELIST   = 2;

    private byte type;
    private BroadcastBandsField bandsField;
    private FrequencyBandDetailInfo detail;
    private long usableChannels;
    private long usedChannels;
    private ServerPlayerInfo whitelist;
    private ServerPlayerInfo otherPlayers;

    public FrequencyBandSyncPacket() {}

    private FrequencyBandSyncPacket(byte type) {
        this.type = type;
    }

    public static FrequencyBandSyncPacket bands(BroadcastBandsField field) {
        FrequencyBandSyncPacket p = new FrequencyBandSyncPacket(BANDS_LIST);
        p.bandsField = field;
        return p;
    }

    public static FrequencyBandSyncPacket detail(FrequencyBandDetailInfo detail, long usable, long used) {
        FrequencyBandSyncPacket p = new FrequencyBandSyncPacket(BAND_DETAIL);
        p.detail = detail;
        p.usableChannels = usable;
        p.usedChannels = used;
        return p;
    }

    public static FrequencyBandSyncPacket whitelist(ServerPlayerInfo wl, ServerPlayerInfo other) {
        FrequencyBandSyncPacket p = new FrequencyBandSyncPacket(WHITELIST);
        p.whitelist = wl;
        p.otherPlayers = other;
        return p;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        switch (type) {
            case BANDS_LIST:
                bandsField = new BroadcastBandsField(new net.minecraft.network.PacketBuffer(buf));
                break;
            case BAND_DETAIL:
                detail = new FrequencyBandDetailInfo(new net.minecraft.network.PacketBuffer(buf));
                usableChannels = buf.readLong();
                usedChannels = buf.readLong();
                break;
            case WHITELIST:
                whitelist = new ServerPlayerInfo(new net.minecraft.network.PacketBuffer(buf));
                otherPlayers = new ServerPlayerInfo(new net.minecraft.network.PacketBuffer(buf));
                break;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        net.minecraft.network.PacketBuffer pb = new net.minecraft.network.PacketBuffer(buf);
        switch (type) {
            case BANDS_LIST:
                if (bandsField != null) bandsField.writeToPacket(pb);
                break;
            case BAND_DETAIL:
                if (detail != null) detail.writeToPacket(pb);
                buf.writeLong(usableChannels);
                buf.writeLong(usedChannels);
                break;
            case WHITELIST:
                if (whitelist != null) whitelist.writeToPacket(pb);
                else new ServerPlayerInfo(new java.util.LinkedHashMap<>()).writeToPacket(pb);
                if (otherPlayers != null) otherPlayers.writeToPacket(pb);
                else new ServerPlayerInfo(new java.util.LinkedHashMap<>()).writeToPacket(pb);
                break;
        }
    }

    public static class Handler implements IMessageHandler<FrequencyBandSyncPacket, IMessage> {
        @Override
        public IMessage onMessage(FrequencyBandSyncPacket msg, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                net.minecraft.client.gui.GuiScreen s = Minecraft.getMinecraft().currentScreen;
                switch (msg.type) {
                    case BANDS_LIST:
                        if (s instanceof GuiFrequencyBand) {
                            ((GuiFrequencyBand) s).acceptBands(msg.bandsField);
                        } else if (s instanceof GuiEnderEmitterFrequencyBand) {
                            ((GuiEnderEmitterFrequencyBand) s).acceptBands(msg.bandsField);
                        }
                        break;
                    case BAND_DETAIL:
                        if (s instanceof GuiFrequencyBandManager) {
                            ((GuiFrequencyBandManager) s).acceptDetail(msg.detail, msg.usableChannels, msg.usedChannels);
                        }
                        break;
                    case WHITELIST:
                        if (s instanceof GuiBandWhiteListManager) {
                            ((GuiBandWhiteListManager) s).acceptWhitelist(msg.whitelist, msg.otherPlayers);
                        }
                        break;
                }
            });
            return null;
        }
    }
}
