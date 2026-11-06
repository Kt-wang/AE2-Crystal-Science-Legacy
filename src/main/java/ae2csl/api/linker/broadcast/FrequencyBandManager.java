/*
 * AE2 Crystal Science - API FrequencyBandManager
 * Ported from 1.21.1 source
 *
 * Manages all frequency bands on the server (persistence + runtime recompute scheduling).
 * Uses WorldSavedData for persistence in 1.12.2.
 */
package ae2csl.api.linker.broadcast;

import ae2csl.api.ids.AE2CSConstants;
import ae2csl.api.linker.broadcast.networking.BroadcastBandsField;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import javax.annotation.Nullable;
import java.util.*;

@Mod.EventBusSubscriber(modid = AE2CSConstants.MODID)
public class FrequencyBandManager extends WorldSavedData {

    private static final String DATA_NAME = "ae2csl_frequency_band_manager";

    /** name -> band instance */
    private final Map<String, BroadcastFrequencyBand> frequencyBands = new LinkedHashMap<>();

    /** Runtime: bands marked for recompute (value = gameTime when marked) */
    private final transient Object2LongOpenHashMap<String> dirtyRuntimeAt = new Object2LongOpenHashMap<>();

    public FrequencyBandManager() {
        super(DATA_NAME);
        dirtyRuntimeAt.defaultReturnValue(Long.MIN_VALUE);
    }

    public FrequencyBandManager(String name) {
        super(name);
        dirtyRuntimeAt.defaultReturnValue(Long.MIN_VALUE);
    }

    // ---- Static accessors ----

    @Nullable
    public static BroadcastFrequencyBand getBand(String bandName) {
        FrequencyBandManager manager = resolveManager();
        if (manager == null) return null;
        return manager.frequencyBands.get(bandName);
    }

    public static boolean isBandPresent(String bandName) {
        FrequencyBandManager manager = resolveManager();
        if (manager == null) return false;
        return manager.frequencyBands.containsKey(bandName);
    }

    @Nullable
    public static BroadcastBandsField getBandsInfo() {
        FrequencyBandManager manager = resolveManager();
        if (manager == null) return null;

        List<BroadcastBandsField.Entry> out = new ArrayList<>(manager.frequencyBands.size());

        for (BroadcastFrequencyBand band : manager.frequencyBands.values()) {
            String name = band.getName();
            boolean isPublic = band.isPublic();
            boolean isEncrypted = !band.getPassword().isEmpty();

            byte flags = BroadcastBandsField.Entry.pack(isPublic, isEncrypted);
            out.add(new BroadcastBandsField.Entry(name, flags));
        }

        return new BroadcastBandsField(Collections.unmodifiableList(out));
    }

    @Nullable
    public static BroadcastBandsField getBandsInfoByPlayer(EntityPlayer player) {
        FrequencyBandManager manager = resolveManager();
        if (manager == null) return null;

        UUID playerUUID = player.getUniqueID();
        List<BroadcastBandsField.Entry> out = new ArrayList<>(manager.frequencyBands.size());

        for (BroadcastFrequencyBand band : manager.frequencyBands.values()) {
            String name = band.getName();

            boolean isPublic = band.isPublic();
            if (!isPublic && !band.validWhiteList(playerUUID)) continue;

            boolean isEncrypted = !band.getPassword().isEmpty();

            byte flags = BroadcastBandsField.Entry.pack(isPublic, isEncrypted);
            out.add(new BroadcastBandsField.Entry(name, flags));
        }

        return new BroadcastBandsField(Collections.unmodifiableList(out));
    }

    @Nullable
    public static BroadcastFrequencyBand tryCreateBand(String bandName, String password, UUID ownerId, boolean isPublic, boolean allowedMemoryCardCopy) {
        FrequencyBandManager manager = resolveManager();
        if (manager == null) return null;

        BroadcastFrequencyBand band = manager.frequencyBands.get(bandName);
        if (band == null) {
            band = new BroadcastFrequencyBand(bandName, password, ownerId, isPublic, allowedMemoryCardCopy);
            manager.frequencyBands.put(bandName, band);
            manager.markDirty();
        }
        return band;
    }

    public static boolean deleteBand(String bandName) {
        FrequencyBandManager manager = resolveManager();
        if (manager == null) return false;

        BroadcastFrequencyBand band = manager.frequencyBands.get(bandName);
        if (band == null) return false;

        try {
            band.onRemoved();
        } catch (Throwable ignored) {}

        manager.frequencyBands.remove(bandName);
        manager.dirtyRuntimeAt.removeLong(bandName);
        manager.markDirty();
        return true;
    }

    public static void markSavedDataDirty() {
        FrequencyBandManager manager = resolveManager();
        if (manager != null) manager.markDirty();
    }

    public static void markRuntimeDirty(MinecraftServer server, String bandName) {
        FrequencyBandManager manager = resolveManager();
        if (manager == null) return;

        long now = server.getWorld(0).getTotalWorldTime();

        if (manager.dirtyRuntimeAt.getLong(bandName) == Long.MIN_VALUE) {
            manager.dirtyRuntimeAt.put(bandName, now);
        }
    }

    /**
     * Tick handler: recompute dirty bands one tick after marking.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        FrequencyBandManager manager = resolveManager();
        if (manager == null) return;

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        long now = server.getWorld(0).getTotalWorldTime();

        var it = manager.dirtyRuntimeAt.object2LongEntrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            String bandName = entry.getKey();
            long markedAt = entry.getLongValue();

            // Recompute next tick to avoid colliding with AE2 network pathfinding
            if (now > markedAt) {
                BroadcastFrequencyBand band = manager.frequencyBands.get(bandName);
                if (band != null) {
                    band.recomputeRuntime();
                }
                it.remove();
            }
        }
    }

    // ---- WorldSavedData ----

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        NBTTagList bandsTag = tag.getTagList("bands", 10);
        for (int i = 0; i < bandsTag.tagCount(); i++) {
            NBTTagCompound compoundBandTag = bandsTag.getCompoundTagAt(i);

            try {
                BroadcastFrequencyBand band = new BroadcastFrequencyBand("", "", UUID.randomUUID(), false, false);
                band.deserializeNBT(compoundBandTag);
                this.frequencyBands.put(band.getName(), band);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        NBTTagList bandTags = new NBTTagList();
        for (BroadcastFrequencyBand band : frequencyBands.values()) {
            bandTags.appendTag(band.serializeNBT());
        }
        tag.setTag("bands", bandTags);
        return tag;
    }

    // ---- Internal ----

    @Nullable
    private static FrequencyBandManager resolveManager() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return null;

        WorldSavedData data = server.getWorld(0).getMapStorage()
                .getOrLoadData(FrequencyBandManager.class, DATA_NAME);
        if (data == null) {
            data = new FrequencyBandManager();
            server.getWorld(0).getMapStorage().setData(DATA_NAME, data);
        }
        return (FrequencyBandManager) data;
    }
}
