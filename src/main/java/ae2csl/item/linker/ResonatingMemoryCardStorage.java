/*
 * AE2 Crystal Science - ResonatingMemoryCardStorage (1.12.2)
 * Ported from 1.21.1 source (record → class, DataComponentMap → NBTTagCompound)
 */
package ae2csl.item.linker;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ResonatingMemoryCardStorage {

    public static final int SLOT_COUNT = 9;

    private final int selectedSlot;
    private final List<Slot> slots;

    public ResonatingMemoryCardStorage(int selectedSlot, List<Slot> slots) {
        this.selectedSlot = clampSlot(selectedSlot);
        this.slots = normalizeSlots(slots);
    }

    public static final ResonatingMemoryCardStorage EMPTY = new ResonatingMemoryCardStorage(0, Collections.emptyList());

    public int getSelectedSlot() { return selectedSlot; }
    public List<Slot> getSlots() { return slots; }

    public Slot getSelected() { return slots.get(selectedSlot); }

    public ResonatingMemoryCardStorage withSelectedSlot(int slot) {
        return new ResonatingMemoryCardStorage(slot, slots);
    }

    public ResonatingMemoryCardStorage withSelectedSlotData(String settingsName, NBTTagCompound data) {
        List<Slot> copy = new ArrayList<>(slots);
        copy.set(selectedSlot, new Slot(settingsName, data));
        return new ResonatingMemoryCardStorage(selectedSlot, copy);
    }

    public ResonatingMemoryCardStorage clearSelectedSlot() {
        List<Slot> copy = new ArrayList<>(slots);
        copy.set(selectedSlot, Slot.EMPTY);
        return new ResonatingMemoryCardStorage(selectedSlot, copy);
    }

    public static int clampSlot(int slot) {
        return Math.max(0, Math.min(SLOT_COUNT - 1, slot));
    }

    private static List<Slot> normalizeSlots(List<Slot> input) {
        List<Slot> out = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            out.add(i < input.size() ? input.get(i) : Slot.EMPTY);
        }
        return Collections.unmodifiableList(out);
    }

    // ---- NBT ----
    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("selectedSlot", selectedSlot);
        NBTTagList list = new NBTTagList();
        for (Slot slot : slots) {
            NBTTagCompound st = new NBTTagCompound();
            st.setString("name", slot.getSettingsName());
            if (slot.getData() != null) st.setTag("data", slot.getData());
            list.appendTag(st);
        }
        tag.setTag("slots", list);
        return tag;
    }

    public static ResonatingMemoryCardStorage readFromNBT(NBTTagCompound tag) {
        int sel = tag.getInteger("selectedSlot");
        NBTTagList list = tag.getTagList("slots", 10);
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound st = list.getCompoundTagAt(i);
            slots.add(new Slot(st.getString("name"), st.hasKey("data") ? st.getCompoundTag("data") : null));
        }
        return new ResonatingMemoryCardStorage(sel, slots);
    }

    // ---- Slot ----
    public static final class Slot {
        public static final Slot EMPTY = new Slot("", new NBTTagCompound());

        private final String settingsName;
        private final NBTTagCompound data;

        public Slot(String settingsName, NBTTagCompound data) {
            this.settingsName = (settingsName == null || settingsName.isEmpty()) ? "" : settingsName;
            this.data = (data == null) ? new NBTTagCompound() : data;
        }

        public String getSettingsName() { return settingsName; }
        public NBTTagCompound getData() { return data; }
        public boolean isEmpty() { return data.isEmpty(); }
    }
}
