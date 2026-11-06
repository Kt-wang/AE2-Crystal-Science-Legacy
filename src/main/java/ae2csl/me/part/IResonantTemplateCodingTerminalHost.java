/*
 * AE2 Crystal Science - IResonantTemplateCodingTerminalHost Interface (1.12.2)
 */
package ae2csl.me.part;

public interface IResonantTemplateCodingTerminalHost {

    boolean isPullRecipeInputsToRealGrid();
    void setPullRecipeInputsToRealGrid(boolean value);

    boolean isPulledAnvilMode();
    void setPulledAnvilMode(boolean value);

    boolean isEncodeResonatingPattern();
    void setEncodeResonatingPattern(boolean value);
}
