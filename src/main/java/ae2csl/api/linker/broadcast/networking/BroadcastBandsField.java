/*
 * AE2 Crystal Science - API BroadcastBandsField
 * Ported from 1.21.1 source (originally a record)
 *
 * Sends simplified band information from server to client.
 */
package ae2csl.api.linker.broadcast.networking;

import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BroadcastBandsField {

    private final List<Entry> bands;

    public BroadcastBandsField(List<Entry> bands) {
        this.bands = bands != null ? bands : Collections.emptyList();
    }

    public BroadcastBandsField(PacketBuffer buf) {
        this(read(buf));
    }

    public List<Entry> getBands() {
        return bands;
    }

    public void writeToPacket(PacketBuffer buf) {
        buf.writeVarInt(bands.size());
        for (Entry entry : bands) {
            buf.writeString(entry.getName());
            buf.writeByte(entry.getFlags());
        }
    }

    private static List<Entry> read(PacketBuffer buf) {
        int n = buf.readVarInt();
        List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String name = buf.readString(64);
            byte flags = buf.readByte();
            list.add(new Entry(name, flags));
        }
        return Collections.unmodifiableList(list);
    }

    public static final class Entry {
        private final String name;
        private final byte flags;

        public Entry(String name, byte flags) {
            this.name = name;
            this.flags = flags;
        }

        public String getName() { return name; }
        public byte getFlags() { return flags; }

        public boolean isPublic() {
            return (flags & 0x01) != 0;
        }

        public boolean isEncrypted() {
            return (flags & 0x02) != 0;
        }

        public static byte pack(boolean isPublic, boolean isEncrypted) {
            return (byte) ((isPublic ? 0x01 : 0) | (isEncrypted ? 0x02 : 0));
        }
    }
}
