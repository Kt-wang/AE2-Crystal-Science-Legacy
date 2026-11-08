/*
 * AE2 Crystal Science - ResonantPatternEncodingCapacity (1.12.2)
 *
 * 对照 1.21.1 源码：把 AE2 样板编码逻辑的编码库存扩容到
 * 144 输入 / 36 输出（AE2 默认 81 / 27）。
 * 1.12.2 fork 的字段是 private final，只能用反射替换。
 */
package ae2csl.me;

import ae2.parts.encoding.PatternEncodingLogic;
import ae2.util.ConfigInventory;
import ae2csl.AE2CrystalScienceLegacy;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

public final class ResonantPatternEncodingCapacity {

    public static final int PROCESSING_INPUT_SLOTS = 144;
    public static final int PROCESSING_OUTPUT_SLOTS = 36;

    private static final String FIELD_INPUTS = "encodedInputInv";
    private static final String FIELD_OUTPUTS = "encodedOutputInv";

    private static Field inputsField;
    private static Field outputsField;

    private ResonantPatternEncodingCapacity() {}

    /** 扩容指定逻辑的编码库存（幂等） */
    public static boolean expand(PatternEncodingLogic logic) {
        if (logic == null) {
            return false;
        }
        if (logic.getEncodedInputInv() != null
            && logic.getEncodedInputInv().size() == PROCESSING_INPUT_SLOTS
            && logic.getEncodedOutputInv() != null
            && logic.getEncodedOutputInv().size() == PROCESSING_OUTPUT_SLOTS) {
            return true;
        }
        try {
            Field inputs = inputs();
            Field outputs = outputs();
            if (inputs == null || outputs == null) {
                return false;
            }
            ConfigInventory newInputs = ConfigInventory.configStacks(PROCESSING_INPUT_SLOTS)
                .changeListener(logic::saveChanges)
                .allowOverstacking(true)
                .build();
            ConfigInventory newOutputs = ConfigInventory.configStacks(PROCESSING_OUTPUT_SLOTS)
                .changeListener(logic::saveChanges)
                .allowOverstacking(true)
                .build();
            inputs.set(logic, newInputs);
            outputs.set(logic, newOutputs);
            return true;
        } catch (ReflectiveOperationException e) {
            AE2CrystalScienceLegacy.LOGGER.warn("Failed to expand resonant pattern encoding capacity: {}", e.toString());
            return false;
        }
    }

    @Nullable
    private static Field inputs() {
        if (inputsField == null) {
            inputsField = find(FIELD_INPUTS);
        }
        return inputsField;
    }

    @Nullable
    private static Field outputs() {
        if (outputsField == null) {
            outputsField = find(FIELD_OUTPUTS);
        }
        return outputsField;
    }

    @Nullable
    private static Field find(String name) {
        try {
            Field field = PatternEncodingLogic.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            AE2CrystalScienceLegacy.LOGGER.warn("PatternEncodingLogic has no field {}: {}", name, e.toString());
            return null;
        }
    }
}
