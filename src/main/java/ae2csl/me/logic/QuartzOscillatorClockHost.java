/*
 * AE2 Crystal Science - QuartzOscillatorClockHost Interface
 * Ported from 1.21.1 source
 */
package ae2csl.me.logic;

import ae2.api.upgrades.IUpgradeableObject;
import net.minecraft.util.EnumFacing;

import java.util.EnumSet;

public interface QuartzOscillatorClockHost extends IUpgradeableObject {

    QuartzOscillatorClockLogic getLogic();

    net.minecraft.tileentity.TileEntity getTileEntity();

    EnumSet<EnumFacing> getTargets();

    void saveChanges();

    void setPulseActive(boolean active);

    boolean isPulseActive();
}
