/*
 * AE2 Crystal Science - API MemoryCardBandInfo
 * Ported from 1.21.1 source (originally a record)
 *
 * Used for copying band info to a memory card.
 */
package ae2csl.api.linker.broadcast;

import net.minecraft.nbt.NBTTagCompound;

public final class MemoryCardBandInfo {

    private final String bandName;
    private final boolean asSender;

    public MemoryCardBandInfo(String bandName, boolean asSender) {
        this.bandName = bandName;
        this.asSender = asSender;
    }

    public String getBandName() { return bandName; }
    public boolean isAsSender() { return asSender; }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("band_name", bandName);
        tag.setBoolean("as_sender", asSender);
        return tag;
    }

    public static MemoryCardBandInfo readFromNBT(NBTTagCompound tag) {
        String bandName = tag.getString("band_name");
        boolean asSender = tag.getBoolean("as_sender");
        return new MemoryCardBandInfo(bandName, asSender);
    }
}
