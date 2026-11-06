/*
 * AE2 Crystal Science - API FrequencyBandDetailInfo
 * Ported from 1.21.1 source (originally a record)
 *
 * Detailed information about a single frequency band, sent from server to client.
 */
package ae2csl.api.linker.broadcast.networking;

import ae2csl.api.linker.broadcast.BroadcastFrequencyBand;
import ae2csl.api.util.GlobalPos;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FrequencyBandDetailInfo {

    private final String name;
    private final boolean isEncrypted;
    private final boolean isPublic;
    private final boolean allowedMemoryCardCopy;
    private final BroadcastFrequencyBand.BandError errorState;
    private final List<String> whiteList;
    private final List<GlobalPos> senderList;
    private final List<GlobalPos> receiverList;

    public FrequencyBandDetailInfo(String name, boolean isEncrypted, boolean isPublic,
                                   boolean allowedMemoryCardCopy,
                                   BroadcastFrequencyBand.BandError errorState,
                                   List<String> whiteList,
                                   List<GlobalPos> senderList,
                                   List<GlobalPos> receiverList) {
        this.name = name;
        this.isEncrypted = isEncrypted;
        this.isPublic = isPublic;
        this.allowedMemoryCardCopy = allowedMemoryCardCopy;
        this.errorState = errorState;
        this.whiteList = whiteList != null ? whiteList : Collections.emptyList();
        this.senderList = senderList != null ? senderList : Collections.emptyList();
        this.receiverList = receiverList != null ? receiverList : Collections.emptyList();
    }

    public FrequencyBandDetailInfo(PacketBuffer buf) {
        this.name = buf.readString(64);
        this.isEncrypted = buf.readBoolean();
        this.isPublic = buf.readBoolean();
        this.allowedMemoryCardCopy = buf.readBoolean();
        this.errorState = buf.readEnumValue(BroadcastFrequencyBand.BandError.class);
        this.whiteList = readStringList(buf);
        this.senderList = readGlobalPosList(buf);
        this.receiverList = readGlobalPosList(buf);
    }

    public String getName() { return name; }
    public boolean isEncrypted() { return isEncrypted; }
    public boolean isPublic() { return isPublic; }
    public boolean isAllowedMemoryCardCopy() { return allowedMemoryCardCopy; }
    public BroadcastFrequencyBand.BandError getErrorState() { return errorState; }
    public List<String> getWhiteList() { return whiteList; }
    public List<GlobalPos> getSenderList() { return senderList; }
    public List<GlobalPos> getReceiverList() { return receiverList; }

    public void writeToPacket(PacketBuffer buf) {
        buf.writeString(name);
        buf.writeBoolean(isEncrypted);
        buf.writeBoolean(isPublic);
        buf.writeBoolean(allowedMemoryCardCopy);
        buf.writeEnumValue(errorState);

        buf.writeVarInt(whiteList.size());
        for (String entry : whiteList) {
            buf.writeString(entry);
        }

        buf.writeVarInt(senderList.size());
        for (GlobalPos sender : senderList) {
            writeGlobalPos(buf, sender);
        }

        buf.writeVarInt(receiverList.size());
        for (GlobalPos receiver : receiverList) {
            writeGlobalPos(buf, receiver);
        }
    }

    private static List<String> readStringList(PacketBuffer buf) {
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readString(64));
        }
        return Collections.unmodifiableList(list);
    }

    private static List<GlobalPos> readGlobalPosList(PacketBuffer buf) {
        int size = buf.readVarInt();
        List<GlobalPos> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int dim = buf.readVarInt();
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            list.add(new GlobalPos(dim, new net.minecraft.util.math.BlockPos(x, y, z)));
        }
        return Collections.unmodifiableList(list);
    }

    private static void writeGlobalPos(PacketBuffer buf, GlobalPos pos) {
        buf.writeVarInt(pos.getDimension());
        buf.writeInt(pos.getPos().getX());
        buf.writeInt(pos.getPos().getY());
        buf.writeInt(pos.getPos().getZ());
    }
}
