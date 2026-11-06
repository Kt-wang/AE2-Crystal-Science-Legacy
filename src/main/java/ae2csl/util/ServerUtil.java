package ae2csl.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;

public final class ServerUtil {
    private ServerUtil() {}

    public static String getPlayerNameByUUID(UUID uuid, MinecraftServer server) {
        EntityPlayerMP online = server.getPlayerList().getPlayerByUUID(uuid);
        if (online != null) return online.getGameProfile().getName();
        // Offline: 1.12.2 doesn't have a simple offline name cache
        return "Unknown";
    }
}
