/*
 * AE2 Crystal Science - ISideConfigHost (1.12.2)
 * 拥有侧面配置的机器 Tile。
 */
package ae2csl.machine;

public interface ISideConfigHost {

    SideConfigData getSideConfig();

    /** 机器自身 GUI 在 GuiHandler 中的 ID（侧配界面"返回"按钮用；编译期常量内联，服务端不会加载 GuiHandler） */
    int getMachineGuiId();
}
