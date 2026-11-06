/*
 * AE2 Crystal Science
 * Crystal Seed Item - Grows into pure crystals when in water or in a Crystal Growth Chamber
 */
package ae2csl.item.crystal;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;

public class CrystalSeedItem extends Item {

    private static final String NBT_GROW_TICKS = "GrowTicks";
    private static final String NBT_OVERGROW_TICK = "OvergrowTick";

    private final Item growTo;
    private final String growToRegistryName;  // Lazy lookup if growTo is null
    private final int overgrowTick;

    public CrystalSeedItem(String name, Item growTo, int overgrowTick) {
        this.growTo = growTo;
        this.growToRegistryName = null;
        this.overgrowTick = overgrowTick;
    }

    public CrystalSeedItem(String name, Item growTo) {
        this(name, growTo, 600);
    }

    /**
     * Construct a seed that lazily resolves its grow-to item by registry name.
     * Use this when the target item may not be registered yet (e.g. AE2 items).
     */
    public CrystalSeedItem(String registryName, int overgrowTick) {
        this.growTo = null;
        this.growToRegistryName = registryName;
        this.overgrowTick = overgrowTick;
    }

    /**
     * Default constructor — resolves AE2 certus_quartz_crystal at runtime.
     */
    public CrystalSeedItem() {
        this.growTo = null;
        this.growToRegistryName = "ae2:certus_quartz_crystal";
        this.overgrowTick = 600;
    }

    public int getGrowTicks(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(NBT_GROW_TICKS)) {
            return stack.getTagCompound().getInteger(NBT_GROW_TICKS);
        }
        return 0;
    }

    public void setGrowTicks(ItemStack stack, int ticks) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger(NBT_GROW_TICKS, ticks);
    }

    public int getOvergrowTick(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(NBT_OVERGROW_TICK)) {
            return stack.getTagCompound().getInteger(NBT_OVERGROW_TICK);
        }
        return overgrowTick;
    }

    public Item getGrowTo() {
        if (growTo != null) return growTo;
        // Lazy resolve from registry name (for default constructor)
        if (growToRegistryName != null) {
            Item resolved = Item.getByNameOrId(growToRegistryName);
            return resolved != null ? resolved : net.minecraft.init.Items.DIAMOND;
        }
        return net.minecraft.init.Items.DIAMOND;
    }

    public float getGrowProgress(ItemStack stack) {
        return Math.min(1.0f, (float) getGrowTicks(stack) / getOvergrowTick(stack));
    }

    /**
     * Grow the crystal seed by the given ticks and return the resulting stack
     */
    public static ItemStack grow(ItemStack stack, int ticks) {
        if (!(stack.getItem() instanceof CrystalSeedItem)) {
            return stack;
        }

        CrystalSeedItem seedItem = (CrystalSeedItem) stack.getItem();
        int currentTicks = seedItem.getGrowTicks(stack);
        int newTicks = currentTicks + ticks;
        int overgrowTick = seedItem.getOvergrowTick(stack);

        if (newTicks >= overgrowTick) {
            ItemStack newStack = new ItemStack(seedItem.getGrowTo());
            newStack.setCount(stack.getCount());
            return newStack;
        }

        ItemStack result = stack.copy();
        seedItem.setGrowTicks(result, newTicks);
        return result;
    }

    /**
     * Process water growth - called by Forge every tick while the seed is a dropped item entity.
     */
    @Override
    public boolean onEntityItemUpdate(EntityItem entityItem) {
        if (!entityItem.getEntityWorld().isRemote && entityItem.isInWater()) {
            ItemStack stack = entityItem.getItem();
            if (!stack.isEmpty()) {
                ItemStack grownStack = grow(stack, 1);
                entityItem.setItem(grownStack);
            }
        }
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, java.util.List<String> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTagCompound()) {
            int ticks = getGrowTicks(stack);
            int max = getOvergrowTick(stack);
            int pct = (int)((float)ticks / max * 100);
            tooltip.add(net.minecraft.client.resources.I18n.format("message.ae2csl.tooltip.seed_growth", pct));
        }
    }
}
