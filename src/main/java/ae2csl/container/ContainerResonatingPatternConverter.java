/*
 * AE2 Crystal Science - ContainerResonatingPatternConverter (1.12.2)
 * 对照源码 ResonatingPatternConverterMenu：18 槽（左9输入/右9输出），存于手持物品 NBT。
 */
package ae2csl.container;

import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2.container.slot.AppEngSlot;
import ae2.util.inv.AppEngInternalInventory;
import ae2csl.me.crafting.ResonatingPatternDetails;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ContainerResonatingPatternConverter extends AEBaseContainer {

    private static final String ACTION_CONVERT = "converter_pattern";
    private static final int LEFT_BOUNDARY = 9;
    private static final String TAG_INV = "converter_inv";

    private final AppEngInternalInventory inventory;

    public ContainerResonatingPatternConverter(InventoryPlayer ip) {
        super(ip, makeHost(ip));
        this.inventory = createInventory(ip.player);

        for (int i = 0; i < inventory.size(); i++) {
            if (i < LEFT_BOUNDARY) {
                this.addSlot(new AppEngSlot(inventory, i), SlotSemantics.MACHINE_INPUT);
            } else {
                this.addSlot(new AppEngSlot(inventory, i), SlotSemantics.MACHINE_OUTPUT);
            }
        }

        // 玩家背包槽（AEBaseContainer 不会自动添加；漏掉会导致 GUI 里无法取放任何物品）
        this.addPlayerInventorySlots(8, 84);

        registerClientAction(ACTION_CONVERT, this::onConverterPattern);
    }

    private AppEngInternalInventory createInventory(EntityPlayer player) {
        ItemStack held = player.getHeldItemMainhand();
        AppEngInternalInventory inv = new AppEngInternalInventory(new ae2.util.inv.InternalInventoryHost() {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv) {
                if (!player.world.isRemote) {
                    NBTTagCompound tag = held.getTagCompound() != null ? held.getTagCompound()
                        : new NBTTagCompound();
                    NBTTagCompound sub = new NBTTagCompound();
                    inv.writeToNBT(sub, TAG_INV);
                    tag.setTag(TAG_INV, sub);
                    held.setTagCompound(tag);
                }
            }

            @Override
            public boolean isClientSide() {
                return player.world.isRemote;
            }
        }, 18);

        // 读取存档
        if (held.hasTagCompound() && held.getTagCompound().hasKey(TAG_INV, 10)) {
            inv.readFromNBT(held.getTagCompound().getCompoundTag(TAG_INV), TAG_INV);
        }

        for (int i = 0; i < inv.size(); i++) {
            inv.setMaxStackSize(i, 1);
        }

        inv.setFilter(new ae2.util.inv.filter.IAEItemFilter() {
            @Override
            public boolean allowInsert(ae2.api.inventories.InternalInventory inv, int slot, ItemStack stack) {
                String id = stack.getItem().getRegistryName() != null
                    ? stack.getItem().getRegistryName().toString() : "";
                return id.equals("ae2csl:resonating_pattern") || id.equals("ae2:processing_pattern");
            }
        });

        return inv;
    }

    private static ae2.api.implementations.guiobjects.ItemGuiHost<?> makeHost(InventoryPlayer ip) {
        // 手持物品 GUI 的宿主（AEBaseContainer 要求宿主是 TileEntity/IPart/ItemGuiHost 之一）
        return new ae2.api.implementations.guiobjects.ItemGuiHost<>(
            ip.player.getHeldItemMainhand().getItem(), ip.player,
            ae2.core.gui.locator.GuiHostLocators.forHand(ip.player, net.minecraft.util.EnumHand.MAIN_HAND));
    }

    public AppEngInternalInventory getConverterInventory() {
        return inventory;
    }

    /**
     * 转换按钮：左侧处理样板 → 谐振样板移到右侧；谐振样板直接移到右侧
     */
    public void onConverterPattern() {
        if (isClientSide()) {
            sendClientAction(ACTION_CONVERT);
            return;
        }

        for (int i = 0; i < LEFT_BOUNDARY && i < inventory.size(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            String id = stack.getItem().getRegistryName() != null
                ? stack.getItem().getRegistryName().toString() : "";

            if (id.equals("ae2csl:resonating_pattern")) {
                // 谐振样板直接转移至右侧
                ItemStack remaining = insertToRange(inventory, stack, LEFT_BOUNDARY, inventory.size(), false);
                if (remaining.isEmpty()) {
                    inventory.setItemDirect(i, ItemStack.EMPTY);
                }
                continue;
            }

            if (id.equals("ae2:processing_pattern")) {
                ItemStack resonating = ResonatingPatternDetails.encode(stack, getPlayer().world);
                if (resonating.isEmpty()) {
                    continue;
                }
                ItemStack remaining = insertToRange(inventory, resonating, LEFT_BOUNDARY, inventory.size(), false);
                if (remaining.isEmpty()) {
                    inventory.setItemDirect(i, ItemStack.EMPTY);
                }
            }
        }
    }

    /**
     * 尝试把物品添加到指定的槽位范围，返回余量
     */
    protected ItemStack insertToRange(AppEngInternalInventory inv, ItemStack stack, int startIndex, int endIndex,
                                      boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int i = startIndex; i < endIndex; i++) {
            if (remaining.isEmpty()) {
                break;
            }
            remaining = inv.insertItem(i, remaining, simulate);
        }
        return remaining;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
