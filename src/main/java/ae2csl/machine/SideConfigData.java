/*
 * AE2 Crystal Science - SideConfigData (1.12.2)
 * 侧面配置数据（对齐 1.21.1 源码 SideConfigComponent 的数据部分）。
 * 每面一个 SidePolicy（默认 ALL）；自动输入/自动输出默认关闭。
 * 同步/NBT 用位掩码：6 面 × 2 bit 策略 + bit12=autoImport + bit13=autoExport。
 */
package ae2csl.machine;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import java.util.EnumMap;

public class SideConfigData {

    private final EnumMap<EnumFacing, SidePolicy> policies = new EnumMap<>(EnumFacing.class);
    private boolean autoImport;
    private boolean autoExport;
    private final Runnable dirtyCallback;

    public SideConfigData(Runnable dirtyCallback) {
        this.dirtyCallback = dirtyCallback;
        for (EnumFacing f : EnumFacing.VALUES) {
            policies.put(f, SidePolicy.ALL);
        }
    }

    public SidePolicy get(EnumFacing facing) {
        SidePolicy p = policies.get(facing);
        return p == null ? SidePolicy.ALL : p;
    }

    public void set(EnumFacing facing, SidePolicy policy) {
        if (get(facing) == policy) return;
        policies.put(facing, policy);
        markDirty();
    }

    public void cycle(EnumFacing facing) {
        set(facing, get(facing).next());
    }

    public boolean isAutoImport() { return autoImport; }
    public boolean isAutoExport() { return autoExport; }

    public void setAutoImport(boolean v) {
        if (autoImport != v) { autoImport = v; markDirty(); }
    }

    public void setAutoExport(boolean v) {
        if (autoExport != v) { autoExport = v; markDirty(); }
    }

    private void markDirty() {
        if (dirtyCallback != null) dirtyCallback.run();
    }

    // ---- 位掩码编解码（NBT + 网络同步共用） ----
    public int encode() {
        int mask = 0;
        for (EnumFacing f : EnumFacing.VALUES) {
            mask |= get(f).ordinal() << (f.ordinal() * 2);
        }
        if (autoImport) mask |= 1 << 12;
        if (autoExport) mask |= 1 << 13;
        return mask;
    }

    /** 客户端同步用：直接覆写，不触发 dirty */
    public void decode(int mask) {
        for (EnumFacing f : EnumFacing.VALUES) {
            policies.put(f, SidePolicy.values()[(mask >> (f.ordinal() * 2)) & 3]);
        }
        autoImport = (mask & (1 << 12)) != 0;
        autoExport = (mask & (1 << 13)) != 0;
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("sideConfig", encode());
    }

    public void readFromNBT(NBTTagCompound tag) {
        if (tag.hasKey("sideConfig")) decode(tag.getInteger("sideConfig"));
    }
}
