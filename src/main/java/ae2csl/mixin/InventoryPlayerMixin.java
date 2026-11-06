/*
 * AE2 Crystal Science - InventoryPlayerMixin (1.12.2, client-only)
 *
 * 原版滚轮会切换快捷栏物品。谐振样板 / 连接器 / 存储卡用「潜行 + 滚轮」切换槽位，
 * 这里在 changeCurrentItem 入口直接取消，做到完全不换物品（无闪动）。
 *
 * 1.12.2 的 vanilla 目标在不同环境名字不同：
 *  - dev 客户端（MCP）：changeCurrentItem
 *  - 生产环境（SRG）：func_70453_c
 * 因此 remap=false + 同时列出两个名字 + require=0：
 * 匹配到哪个就注入哪个，都没匹配到也只是不锁定（不会崩）。
 * 本 mixin 只在 client 列表启用，服务端不会加载（服务端 dev 是 SRG 名，避免类加载问题）。
 */
package ae2csl.mixin;

import ae2csl.item.IResonatingTargetModeItem;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryPlayer.class)
public abstract class InventoryPlayerMixin {

    @Inject(method = {"changeCurrentItem", "func_70453_c"},
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void ae2cs$lockHotbarOnSneakScroll(int direction, CallbackInfo ci) {
        InventoryPlayer self = (InventoryPlayer) (Object) this;
        EntityPlayer player = self.player;
        if (player == null || !player.world.isRemote || !player.isSneaking()) {
            return;
        }

        ItemStack held = player.getHeldItemMainhand();
        if (!held.isEmpty() && held.getItem() instanceof IResonatingTargetModeItem) {
            ci.cancel();
        }
    }
}
