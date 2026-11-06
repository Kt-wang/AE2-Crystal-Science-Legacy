/*
 * AE2 Crystal Science
 * Pure Crystal Item - Used as fuel for Crystal Growth Chamber
 */
package ae2csl.item.crystal;

import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;

public class PureCrystalItem extends Item {

    private static final String NBT_BURN_TIME = "BurnTime";
    private static final String NBT_ENERGY_PER_TICK = "EnergyPerTick";

    private final double energyPerTick;
    private final int burnTime;

    public PureCrystalItem(String name, double energyPerTick, int burnTime) {
        this.energyPerTick = energyPerTick;
        this.burnTime = burnTime;
    }

    public PureCrystalItem(String name, double energyPerTick) {
        this(name, energyPerTick, 600);
    }

    public PureCrystalItem() {
        this("pure_crystal", 100.0, 600);
    }

    public int getBurnTime(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(NBT_BURN_TIME)) {
            return stack.getTagCompound().getInteger(NBT_BURN_TIME);
        }
        return burnTime;
    }

    public void setBurnTime(ItemStack stack, int time) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger(NBT_BURN_TIME, time);
    }

    public double getEnergyPerTick(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(NBT_ENERGY_PER_TICK)) {
            return stack.getTagCompound().getDouble(NBT_ENERGY_PER_TICK);
        }
        return energyPerTick;
    }

    public void setEnergyPerTick(ItemStack stack, double energy) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setDouble(NBT_ENERGY_PER_TICK, energy);
    }

    public double getTotalEnergy(ItemStack stack) {
        return getEnergyPerTick(stack) * getBurnTime(stack);
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, java.util.List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(net.minecraft.client.resources.I18n.format("message.ae2csl.tooltip.burn_time", getBurnTime(stack)));
        tooltip.add(net.minecraft.client.resources.I18n.format("message.ae2csl.tooltip.energy", String.format("%.0f", getEnergyPerTick(stack))));
    }
}
