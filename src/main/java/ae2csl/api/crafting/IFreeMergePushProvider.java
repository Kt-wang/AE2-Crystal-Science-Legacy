/*
 * AE2 Crystal Science - IFreeMergePushProvider (1.12.2)
 * 标记接口：实现该接口的合成供应器走「免并行预算」合并推送。
 * 效果（由 ae2csl.mixin.CraftingCpuLogicMixin 实现）：
 *  - 合并推送倍率不受合成 CPU 并行处理单元预算限制（仍受任务剩余/材料/能耗约束）；
 *  - 一次合并推送只消耗 1 次 CPU 操作预算。
 * 用途：自装配样板供应器的超频卡即供应器侧并行能力（轮容量 8+128n），
 * 不应再要求 CPU 侧堆并行处理单元。
 */
package ae2csl.api.crafting;

public interface IFreeMergePushProvider {
}
