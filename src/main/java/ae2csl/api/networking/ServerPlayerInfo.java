/*
 * AE2 Crystal Science - API ServerPlayerInfo
 * Ported from 1.21.1 source (originally a record)
 *
 * Network packet data for sending player UUID->name mappings to client.
 */
package ae2csl.api.networking;

import net.minecraft.network.PacketBuffer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ServerPlayerInfo {

    private final Map<UUID, String> playerInfo;

    public ServerPlayerInfo(Map<UUID, String> playerInfo) {
        this.playerInfo = playerInfo != null ? playerInfo : new LinkedHashMap<>();
    }

    public ServerPlayerInfo(PacketBuffer buf) {
        this(readMap(buf));
    }

    public Map<UUID, String> getPlayerInfo() {
        return playerInfo;
    }

    private static Map<UUID, String> readMap(PacketBuffer buf) {
        int size = buf.readVarInt();
        Map<UUID, String> map = new LinkedHashMap<>(Math.max(0, size));

        for (int i = 0; i < size; i++) {
            UUID id = buf.readUniqueId();
            String name = buf.readString(64);
            map.put(id, name);
        }

        return map;
    }

    public void writeToPacket(PacketBuffer buf) {
        Map<UUID, String> map = this.playerInfo;
        if (map == null) map = new LinkedHashMap<>();

        buf.writeVarInt(map.size());
        for (Map.Entry<UUID, String> e : map.entrySet()) {
            buf.writeUniqueId(e.getKey());
            buf.writeString(e.getValue() == null ? "" : e.getValue());
        }
    }
}
