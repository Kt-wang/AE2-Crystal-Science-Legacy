/*
 * AE2 Crystal Science - EnderInterfaceHost (1.12.2)
 * 对照 1.21.1 源码 common/me/logic/EnderInterfaceHost.java
 */
package ae2csl.me.logic;

import ae2csl.api.render.ICustomRenderBounding;

public interface EnderInterfaceHost extends IntegratedInterfaceHost, ICustomRenderBounding {

    EnderInterfaceLogic getEnderInterfaceLogic();

    void markForLogicClientUpdate();

    /**
     * 末影接口不是样板容器（源码不实现 PatternContainer），不在样板访问终端显示
     */
    @Override
    default boolean isVisibleInTerminal() {
        return false;
    }

    @Override
    default boolean enableCustomRenderBounding() {
        return getEnderInterfaceLogic().isRenderRangeInClient();
    }

    @Override
    default int getRange() {
        return getEnderInterfaceLogic().getRange();
    }

    @Override
    boolean isExtended();
}
