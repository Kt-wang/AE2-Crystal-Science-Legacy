/*
 * AE2 Crystal Science - CraftingCpuLogicMixin (1.12.2)
 *
 * 为 IFreeMergePushProvider（自装配式样板供应器）提供「免并行预算」合并推送通道：
 * fork 原生合并推送的倍率会被 remainingPatternBudget（并行处理单元 + 1）钳制，
 * 本 mixin 对标记供应器：
 *  - 把 `Math.min(taskRemaining, remainingPatternBudget)` 的结果直接取 taskRemaining（跳过并行预算）；
 *  - 把返回值收敛为 1，使一次合并推送只记 1 次 CPU 操作预算（供应器轮容量才是瓶颈）。
 *
 * 为什么不用 HEAD-cancel 重写整套逻辑：fork 1.1.0 的 tryPushPatternToProvider 含
 * dispatchLimit / protectInputs / 临时样板库存视图等新机制，重写会漏掉它们。
 *
 * 为什么不用「同包 mixin」：把 mixin 配置的 package 设为 ae2.crafting.execution 会让
 * Mixin 把整个包视为受保护包，任何其他地方直接引用 CraftingCpuLogic 都会抛
 * IllegalClassLoadError（合成 CPU 放置即崩）。这里改用 MixinExtras @Local 捕获局部变量，
 * mixin 留在自己的包内。
 */
package ae2csl.mixin;

import ae2.api.networking.crafting.ICraftingProvider;
import ae2.crafting.execution.CraftingCpuLogic;
import ae2csl.api.crafting.IFreeMergePushProvider;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CraftingCpuLogic.class, remap = false)
public abstract class CraftingCpuLogicMixin {

    /**
     * maxMultiplier = (int) Math.min(progress.value, remainingPatternBudget)
     * → 对免预算供应器直接返回任务剩余量。
     */
    @WrapOperation(method = "tryPushPatternToProvider",
                   at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(JJ)J"),
                   remap = false)
    private long ae2cs$freeMergeBudget(long taskRemaining, long budget, Operation<Long> original,
                                       @Local ICraftingProvider provider,
                                       @Local(ordinal = 1) boolean mergePush) {
        if (mergePush && provider instanceof IFreeMergePushProvider) {
            return taskRemaining;
        }
        return original.call(taskRemaining, budget);
    }

    /**
     * 一次合并推送只记 1 次 CPU 操作预算。
     */
    @ModifyReturnValue(method = "tryPushPatternToProvider", at = @At("RETURN"), remap = false)
    private int ae2cs$countAsOnePush(int pushed, @Local ICraftingProvider provider,
                                     @Local(ordinal = 1) boolean mergePush) {
        if (mergePush && provider instanceof IFreeMergePushProvider && pushed > 0) {
            return 1;
        }
        return pushed;
    }
}
