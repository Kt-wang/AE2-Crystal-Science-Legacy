/*
 * AE2 Crystal Science - AE2CSPartModelRegistration (1.12.2)
 * Registers all addon part models (@PartModels fields) with AE2's PartModels
 * registry so the cable bus baked model knows about them.
 * Must be called during preInit.
 */
package ae2csl.me.part;

import ae2.api.parts.PartModels;
import ae2.items.parts.PartModelsHelper;

public final class AE2CSPartModelRegistration {

    private AE2CSPartModelRegistration() {}

    public static void register() {
        PartModels.registerModels(PartModelsHelper.createModels(EnderInterfacePart.class));
        PartModels.registerModels(PartModelsHelper.createModels(ExtendedEnderInterfacePart.class));
        PartModels.registerModels(PartModelsHelper.createModels(IntegratedInterfacePart.class));
        PartModels.registerModels(PartModelsHelper.createModels(ExtendedIntegratedInterfacePart.class));
        PartModels.registerModels(PartModelsHelper.createModels(SimplePatternProviderPart.class));
        PartModels.registerModels(PartModelsHelper.createModels(MirrorPatternProviderPart.class));
        PartModels.registerModels(PartModelsHelper.createModels(MeteoritePatternProviderPart.class));
        PartModels.registerModels(PartModelsHelper.createModels(ResonatingPatternProviderPart.class));
        PartModels.registerModels(PartModelsHelper.createModels(ExtendedResonatingPatternProviderPart.class));
        PartModels.registerModels(PartModelsHelper.createModels(QuartzOscillatorClockPart.class));
        PartModels.registerModels(PartModelsHelper.createModels(ResonantTemplateCodingTerminalPart.class));
    }
}
