/*
 * AE2 Crystal Science - IUpgradeSlotsHost (1.12.2)
 *
 * 本模组「自管槽位」的机器（非 AE2 UpgradeInventory）用一个固定的升级槽区间，
 * 记忆卡等工具需要知道这个区间才能复制升级卡。
 */
package ae2csl.machine;

public interface IUpgradeSlotsHost {

    /** 升级槽起始索引（到 getSizeInventory() 为止）；&lt;0 表示没有升级槽 */
    int getUpgradeSlotStart();
}
