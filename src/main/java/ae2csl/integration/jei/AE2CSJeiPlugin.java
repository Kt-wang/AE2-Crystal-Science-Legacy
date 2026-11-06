/*
 * AE2 Crystal Science - JEI Integration Plugin (1.12.2, JEI 4.31.2)
 * Exposes machine recipes in JEI: Circuit Etcher, Crystal Aggregator,
 * Crystal Pulverizer, and Crystal Growth (seed -> crystal).
 */
package ae2csl.integration.jei;

import ae2.recipes.AERecipeTypes;
import ae2.recipes.entropy.EntropyMode;
import ae2.recipes.entropy.EntropyRecipe;
import ae2csl.Tags;
import ae2csl.client.gui.*;
import ae2csl.init.AE2CSBlocks;
import ae2csl.item.crystal.CrystalSeedItem;
import ae2csl.recipe.AE2CSRecipeRegistry;
import ae2csl.recipe.AE2CSRecipeTypes;
import ae2csl.recipe.circuit_etcher.CircuitEtcherRecipe;
import ae2csl.recipe.crystal_aggregator.CrystalAggregatorRecipe;
import ae2csl.recipe.crystal_pulverizer.CrystalPulverizerRecipe;
import ae2csl.tile.vibration.TileCrystalVibrationChamber;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@JEIPlugin
public class AE2CSJeiPlugin implements IModPlugin {

    static final String UID_ETCHER = Tags.MOD_ID + ".circuit_etcher";
    static final String UID_AGGREGATOR = Tags.MOD_ID + ".crystal_aggregator";
    static final String UID_PULVERIZER = Tags.MOD_ID + ".crystal_pulverizer";
    static final String UID_VIBRATION = Tags.MOD_ID + ".crystal_vibration";
    static final String UID_GROWTH = Tags.MOD_ID + ".crystal_growth";
    static final String UID_ENTROPY = Tags.MOD_ID + ".entropy_variation_chamber";

    // ---- Plugin entry point ----

    @Override
    public void register(@Nonnull IModRegistry registry) {
        IGuiHelper gui = registry.getJeiHelpers().getGuiHelper();

        // -- Categories --
        registry.addRecipeCategories(
                new EtcherCategory(gui),
                new AggregatorCategory(gui),
                new PulverizerCategory(gui),
                new VibrationCategory(gui),
                new GrowthCategory(gui),
                new EntropyCategory(gui)
        );

        // -- Recipes --
        registry.addRecipes(wrapEtcher(), UID_ETCHER);
        registry.addRecipes(wrapAgg(), UID_AGGREGATOR);
        registry.addRecipes(wrapPulv(), UID_PULVERIZER);
        registry.addRecipes(wrapVibration(), UID_VIBRATION);
        registry.addRecipes(wrapGrowth(), UID_GROWTH);
        registry.addRecipes(wrapEntropy(), UID_ENTROPY);

        // -- Recipe catalysts (machine blocks that show recipe tab) --
        registry.addRecipeCatalyst(stack(AE2CSBlocks.CIRCUIT_ETCHER), UID_ETCHER);
        registry.addRecipeCatalyst(stack(AE2CSBlocks.CRYSTAL_AGGREGATOR), UID_AGGREGATOR);
        registry.addRecipeCatalyst(stack(AE2CSBlocks.CRYSTAL_PULVERIZER), UID_PULVERIZER);
        registry.addRecipeCatalyst(stack(AE2CSBlocks.QUARTZ_GRINDSTONE), UID_PULVERIZER);
        registry.addRecipeCatalyst(stack(AE2CSBlocks.CRYSTAL_VIBRATION_CHAMBER), UID_VIBRATION);
        registry.addRecipeCatalyst(stack(AE2CSBlocks.CRYSTAL_GROWTH_CHAMBER), UID_GROWTH);
        registry.addRecipeCatalyst(stack(AE2CSBlocks.ENTROPY_VARIATION_CHAMBER), UID_ENTROPY);

        // -- Click areas so players can open recipes from machine GUIs --
        registry.addRecipeClickArea(GuiCircuitEtcher.class, 77, 34, 22, 33,
            UID_ETCHER);
        registry.addRecipeClickArea(GuiCrystalAggregator.class, 79, 38, 22, 16,
            UID_AGGREGATOR);
        registry.addRecipeClickArea(GuiCrystalPulverizer.class, 71, 41, 22, 16,
            UID_PULVERIZER);
        registry.addRecipeClickArea(GuiQuartzGrindstone.class, 79, 64, 18, 6,
            UID_PULVERIZER);
        registry.addRecipeClickArea(GuiCrystalVibrationChamber.class, 62, 36, 33, 18,
            UID_VIBRATION);
        registry.addRecipeClickArea(GuiEntropyVariationChamber.class, 75, 43, 16, 15,
            UID_ENTROPY);

        // -- JEI exclusion areas for the upgrade panel (push JEI aside) --
        registry.addAdvancedGuiHandlers(
            new UpgradePanelHandler<>(GuiCrystalPulverizer.class),
            new UpgradePanelHandler<>(GuiCrystalGrowthChamber.class),
            new UpgradePanelHandler<>(GuiCircuitEtcher.class),
            new UpgradePanelHandler<>(GuiCrystalAggregator.class),
            new UpgradePanelHandler<>(GuiCrystalVibrationChamber.class),
            new UpgradePanelHandler<>(GuiEntropyVariationChamber.class)
        );
    }

    // ---- JEI exclusion handler for GUIs with an upgrade panel ----

    private static class UpgradePanelHandler<T extends net.minecraft.client.gui.inventory.GuiContainer & ae2csl.client.gui.IUpgradePanelGui>
            implements mezz.jei.api.gui.IAdvancedGuiHandler<T> {
        private final Class<T> guiClass;

        UpgradePanelHandler(Class<T> guiClass) {
            this.guiClass = guiClass;
        }

        @Override public Class<T> getGuiContainerClass() {
            return guiClass;
        }

        @Override public List<java.awt.Rectangle> getGuiExtraAreas(T gui) {
            return Collections.singletonList(gui.getUpgradePanelArea());
        }
    }

    // ---- Helpers ----

    private static ItemStack stack(Object block) {
        return new ItemStack((net.minecraft.block.Block) block);
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(Tags.MOD_ID, "textures/gui/recipe/" + name + ".png");
    }

    // ---- Collect recipes ----

    private static List<EtcherWrapper> wrapEtcher() {
        List<EtcherWrapper> out = new ArrayList<>();
        for (CircuitEtcherRecipe r : AE2CSRecipeRegistry.<CircuitEtcherRecipe>getRecipes(AE2CSRecipeTypes.CIRCUIT_ETCHER))
            if (r != null && !r.getResult().isEmpty()) out.add(new EtcherWrapper(r));
        return out;
    }

    private static List<AggregatorWrapper> wrapAgg() {
        List<AggregatorWrapper> out = new ArrayList<>();
        for (CrystalAggregatorRecipe r : AE2CSRecipeRegistry.<CrystalAggregatorRecipe>getRecipes(AE2CSRecipeTypes.CRYSTAL_AGGREGATOR))
            if (r != null && !r.getResult().isEmpty()) out.add(new AggregatorWrapper(r));
        return out;
    }

    private static List<PulverizerWrapper> wrapPulv() {
        List<PulverizerWrapper> out = new ArrayList<>();
        for (CrystalPulverizerRecipe r : AE2CSRecipeRegistry.<CrystalPulverizerRecipe>getRecipes(AE2CSRecipeTypes.CRYSTAL_PULVERIZER))
            if (r != null && !r.getResult().isEmpty()) out.add(new PulverizerWrapper(r));
        return out;
    }

    private static List<VibrationWrapper> wrapVibration() {
        List<VibrationWrapper> out = new ArrayList<>();
        for (net.minecraft.item.Item item : net.minecraft.item.Item.REGISTRY) {
            ItemStack s = new ItemStack(item);
            if (TileCrystalVibrationChamber.isFuel(s)) out.add(new VibrationWrapper(s));
        }
        return out;
    }

    private static List<GrowthWrapper> wrapGrowth() {
        List<GrowthWrapper> out = new ArrayList<>();
        for (net.minecraft.item.Item item : net.minecraft.item.Item.REGISTRY) {
            if (item instanceof CrystalSeedItem) {
                ItemStack s = new ItemStack(item);
                if (!s.isEmpty()) out.add(new GrowthWrapper(s));
            }
        }
        return out;
    }

    private static List<EntropyWrapper> wrapEntropy() {
        List<EntropyWrapper> out = new ArrayList<>();
        for (EntropyRecipe recipe : AERecipeTypes.ENTROPY.getRecipes()) {
            ItemStack input = entropyInputToStack(recipe);
            ItemStack output = entropyOutputToStack(recipe);
            if (input.isEmpty()) continue;
            out.add(new EntropyWrapper(recipe, input, output));
        }
        return out;
    }

    private static ItemStack blockToDisplayStack(Block block) {
        Fluid fluid = FluidRegistry.lookupFluidForBlock(block);
        if (fluid == FluidRegistry.WATER) return new ItemStack(Items.WATER_BUCKET);
        if (fluid == FluidRegistry.LAVA) return new ItemStack(Items.LAVA_BUCKET);
        Item item = Item.getItemFromBlock(block);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack fluidToDisplayStack(Fluid fluid) {
        if (fluid == FluidRegistry.WATER) return new ItemStack(Items.WATER_BUCKET);
        if (fluid == FluidRegistry.LAVA) return new ItemStack(Items.LAVA_BUCKET);
        return ItemStack.EMPTY;
    }

    private static ItemStack entropyInputToStack(EntropyRecipe recipe) {
        Optional<EntropyRecipe.BlockInput> blockIn = recipe.input().block();
        if (blockIn.isPresent()) {
            return blockToDisplayStack(blockIn.get().block());
        }
        Optional<EntropyRecipe.FluidInput> fluidIn = recipe.input().fluid();
        if (fluidIn.isPresent()) {
            return fluidToDisplayStack(fluidIn.get().fluid());
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack entropyOutputToStack(EntropyRecipe recipe) {
        IBlockState inState = null;
        Optional<EntropyRecipe.BlockInput> blockIn = recipe.input().block();
        if (blockIn.isPresent()) {
            inState = blockIn.get().block().getDefaultState();
        }

        IBlockState outState = recipe.getOutputBlockState(inState);
        if (outState != null && outState.getBlock() != Blocks.AIR) {
            ItemStack s = blockToDisplayStack(outState.getBlock());
            if (!s.isEmpty()) return s;
        }
        FluidStack fs = recipe.getOutputFluidStack(null);
        if (fs != null) {
            ItemStack s = fluidToDisplayStack(fs.getFluid());
            if (!s.isEmpty()) return s;
        }
        List<ItemStack> drops = recipe.getDrops();
        if (drops != null && !drops.isEmpty()) {
            return drops.get(0).copy();
        }
        return ItemStack.EMPTY;
    }

    // ---- Ingredient helpers ----

    static List<ItemStack> toStackList(Ingredient ing) {
        if (ing == null || ing == Ingredient.EMPTY)
            return Collections.singletonList(ItemStack.EMPTY);
        ItemStack[] arr = ing.getMatchingStacks();
        if (arr.length == 0) return Collections.singletonList(ItemStack.EMPTY);
        List<ItemStack> l = new ArrayList<>();
        for (ItemStack s : arr) {
            if (!s.isEmpty()) l.add(s.copy());
        }
        return l.isEmpty() ? Collections.singletonList(ItemStack.EMPTY) : l;
    }

    static ItemStack toFirst(Ingredient ing) {
        if (ing == null || ing == Ingredient.EMPTY) return ItemStack.EMPTY;
        ItemStack[] arr = ing.getMatchingStacks();
        return arr.length > 0 ? arr[0].copy() : ItemStack.EMPTY;
    }

    // ==============  Categories  ==============

    /**
     * Circuit Etcher: up to 3 inputs → 1 output, background 135×58
     */
    static class EtcherCategory implements IRecipeCategory<EtcherWrapper> {
        private final IDrawableStatic bg;

        EtcherCategory(IGuiHelper h) {
            bg = h.createDrawable(tex("circuit_etcher"), 0, 0, 135, 58);
        }

        @Nonnull
        public String getUid() {
            return UID_ETCHER;
        }

        @Nonnull
        public String getTitle() {
            return net.minecraft.client.resources.I18n.format("jei." + UID_ETCHER);
        }

        @Nonnull
        public String getModName() {
            return Tags.MOD_NAME;
        }

        @Nonnull
        public IDrawable getBackground() {
            return bg;
        }

        @Override
        public void setRecipe(@Nonnull IRecipeLayout layout, @Nonnull EtcherWrapper w,
                              @Nonnull IIngredients ing) {
            // JEI 4.x init(x, y) is the 18×18 slot cell origin; the item renders at (x+1, y+1).
            // The recipe textures were drawn with the item position at (30,3)/(86,22) etc.,
            // so every slot is passed one pixel up-left of the intended item position.
            layout.getItemStacks().init(0, true, 29, 2);
            layout.getItemStacks().init(1, true, 29, 20);
            layout.getItemStacks().init(2, true, 29, 38);
            layout.getItemStacks().init(3, false, 85, 21);
            layout.getItemStacks().set(ing);
        }
    }

    /**
     * Crystal Aggregator: up to 4 inputs → 1 output, background 135×58
     */
    static class AggregatorCategory implements IRecipeCategory<AggregatorWrapper> {
        private final IDrawableStatic bg;

        AggregatorCategory(IGuiHelper h) {
            bg = h.createDrawable(tex("crystal_aggregator"), 0, 0, 135, 58);
        }

        @Nonnull
        public String getUid() {
            return UID_AGGREGATOR;
        }

        @Nonnull
        public String getTitle() {
            return net.minecraft.client.resources.I18n.format("jei." + UID_AGGREGATOR);
        }

        @Nonnull
        public String getModName() {
            return Tags.MOD_NAME;
        }

        @Nonnull
        public IDrawable getBackground() {
            return bg;
        }

        @Override
        public void setRecipe(@Nonnull IRecipeLayout layout, @Nonnull AggregatorWrapper w,
                              @Nonnull IIngredients ing) {
            // 3 inputs vertical + 1 output (matches source: 3-input inventory)
            layout.getItemStacks().init(0, true, 29, 2);
            layout.getItemStacks().init(1, true, 29, 20);
            layout.getItemStacks().init(2, true, 29, 38);
            layout.getItemStacks().init(3, false, 85, 21);
            layout.getItemStacks().set(ing);
        }
    }

    /**
     * Crystal Pulverizer: 1 input → 1 output, background 135×58
     */
    static class PulverizerCategory implements IRecipeCategory<PulverizerWrapper> {
        private final IDrawableStatic bg;

        PulverizerCategory(IGuiHelper h) {
            bg = h.createDrawable(tex("crystal_pulverizer"), 0, 0, 135, 58);
        }

        @Nonnull
        public String getUid() {
            return UID_PULVERIZER;
        }

        @Nonnull
        public String getTitle() {
            return net.minecraft.client.resources.I18n.format("jei." + UID_PULVERIZER);
        }

        @Nonnull
        public String getModName() {
            return Tags.MOD_NAME;
        }

        @Nonnull
        public IDrawable getBackground() {
            return bg;
        }

        @Override
        public void setRecipe(@Nonnull IRecipeLayout layout, @Nonnull PulverizerWrapper w,
                              @Nonnull IIngredients ing) {
            layout.getItemStacks().init(0, true, 22, 21);
            layout.getItemStacks().init(1, false, 85, 21);
            layout.getItemStacks().set(ing);
        }
    }

    /**
     * Crystal Vibration Chamber: fuel item → AE power, background 135×58
     */
    static class VibrationCategory implements IRecipeCategory<VibrationWrapper> {
        private final IDrawableStatic bg;

        VibrationCategory(IGuiHelper h) {
            bg = h.createDrawable(tex("crystal_pulverizer"), 0, 0, 135, 58);
        }

        @Nonnull public String getUid() { return UID_VIBRATION; }
        @Nonnull public String getTitle() { return net.minecraft.client.resources.I18n.format("jei." + UID_VIBRATION); }
        @Nonnull public String getModName() { return Tags.MOD_NAME; }
        @Nonnull public IDrawable getBackground() { return bg; }

        @Override public void setRecipe(@Nonnull IRecipeLayout layout, @Nonnull VibrationWrapper w,
                                        @Nonnull IIngredients ing) {
            layout.getItemStacks().init(0, true, 22, 21);
            layout.getItemStacks().set(ing);
        }
    }

    /**
     * Crystal Growth: seed → crystal, background 135×58
     */
    static class GrowthCategory implements IRecipeCategory<GrowthWrapper> {
        private final IDrawableStatic bg;

        GrowthCategory(IGuiHelper h) {
            bg = h.createDrawable(tex("crystal_growth"), 0, 0, 135, 58);
        }

        @Nonnull
        public String getUid() {
            return UID_GROWTH;
        }

        @Nonnull
        public String getTitle() {
            return net.minecraft.client.resources.I18n.format("jei." + UID_GROWTH);
        }

        @Nonnull
        public String getModName() {
            return Tags.MOD_NAME;
        }

        @Nonnull
        public IDrawable getBackground() {
            return bg;
        }

        @Override
        public void setRecipe(@Nonnull IRecipeLayout layout, @Nonnull GrowthWrapper w,
                              @Nonnull IIngredients ing) {
            layout.getItemStacks().init(0, true, 22, 21);
            layout.getItemStacks().init(1, false, 85, 21);
            layout.getItemStacks().set(ing);
        }
    }

    /**
     * Entropy Variation Chamber: block/fluid -> block/fluid/drops, background 135x58
     */
    static class EntropyCategory implements IRecipeCategory<EntropyWrapper> {
        private final IDrawableStatic bg;

        EntropyCategory(IGuiHelper h) {
            bg = h.createDrawable(tex("crystal_pulverizer"), 0, 0, 135, 58);
        }

        @Nonnull public String getUid() { return UID_ENTROPY; }
        @Nonnull public String getTitle() { return net.minecraft.client.resources.I18n.format("jei." + UID_ENTROPY); }
        @Nonnull public String getModName() { return Tags.MOD_NAME; }
        @Nonnull public IDrawable getBackground() { return bg; }

        @Override
        public void setRecipe(@Nonnull IRecipeLayout layout, @Nonnull EntropyWrapper w,
                              @Nonnull IIngredients ing) {
            layout.getItemStacks().init(0, true, 22, 21);
            layout.getItemStacks().init(1, false, 85, 21);
            layout.getItemStacks().set(ing);
        }
    }

    // ==============  Recipe Wrappers  ==============

    static class EtcherWrapper implements IRecipeWrapper {
        final CircuitEtcherRecipe r;

        EtcherWrapper(CircuitEtcherRecipe r) {
            this.r = r;
        }

        @Override
        public void getIngredients(@Nonnull IIngredients ing) {
            List<List<ItemStack>> in = new ArrayList<>();
            List<Integer> counts = r.getEffectiveCounts();
            for (int idx = 0; idx < r.getEffective().size(); idx++) {
                List<ItemStack> stacks = toStackList(r.getEffective().get(idx));
                int count = counts.get(idx);
                for (ItemStack s : stacks) {
                    if (!s.isEmpty()) s.setCount(count);
                }
                in.add(stacks);
            }
            while (in.size() < 3) in.add(Collections.singletonList(ItemStack.EMPTY));
            ing.setInputLists(ItemStack.class, in);
            ing.setOutput(ItemStack.class, r.getResult());
        }
    }

    static class AggregatorWrapper implements IRecipeWrapper {
        final CrystalAggregatorRecipe r;

        AggregatorWrapper(CrystalAggregatorRecipe r) {
            this.r = r;
        }

        @Override
        public void getIngredients(@Nonnull IIngredients ing) {
            List<List<ItemStack>> in = new ArrayList<>();
            List<Integer> counts = r.getEffectiveCounts();
            for (int idx = 0; idx < r.getInputs().size(); idx++) {
                List<ItemStack> stacks = toStackList(r.getInputs().get(idx));
                int count = counts.get(idx);
                for (ItemStack s : stacks) {
                    if (!s.isEmpty()) s.setCount(count);
                }
                in.add(stacks);
            }
            while (in.size() < 3) in.add(Collections.singletonList(ItemStack.EMPTY));
            ing.setInputLists(ItemStack.class, in);
            ing.setOutput(ItemStack.class, r.getResult());
        }
    }

    static class PulverizerWrapper implements IRecipeWrapper {
        final CrystalPulverizerRecipe r;

        PulverizerWrapper(CrystalPulverizerRecipe r) {
            this.r = r;
        }

        @Override
        public void getIngredients(@Nonnull IIngredients ing) {
            // Show all matching items for the input (matches source: Arrays.asList(recipe.input().getItems()))
            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(toStackList(r.getInput()));
            ing.setInputLists(ItemStack.class, inputs);
            ing.setOutput(ItemStack.class, r.getResult());
        }
    }

    static class VibrationWrapper implements IRecipeWrapper {
        final ItemStack fuel;

        VibrationWrapper(ItemStack fuel) { this.fuel = fuel; }

        @Override public void getIngredients(@Nonnull IIngredients ing) {
            ing.setInput(ItemStack.class, fuel);
        }
    }

    static class GrowthWrapper implements IRecipeWrapper {
        final ItemStack seed;

        GrowthWrapper(ItemStack seed) {
            this.seed = seed;
        }

        @Override
        public void getIngredients(@Nonnull IIngredients ing) {
            ing.setInput(ItemStack.class, seed);
            if (seed.getItem() instanceof CrystalSeedItem) {
                CrystalSeedItem seedItem = (CrystalSeedItem) seed.getItem();
                ItemStack output = new ItemStack(seedItem.getGrowTo());
                ing.setOutput(ItemStack.class, output);
            }
        }
    }

    static class EntropyWrapper implements IRecipeWrapper {
        final EntropyRecipe recipe;
        final ItemStack input;
        final ItemStack output;

        EntropyWrapper(EntropyRecipe recipe, ItemStack input, ItemStack output) {
            this.recipe = recipe;
            this.input = input;
            this.output = output;
        }

        @Override
        public void getIngredients(@Nonnull IIngredients ing) {
            ing.setInput(ItemStack.class, input);
            if (!output.isEmpty()) ing.setOutput(ItemStack.class, output);
        }

        @Override
        public void drawInfo(@Nonnull Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
            String label = recipe.mode() == EntropyMode.HEAT
                ? net.minecraft.client.resources.I18n.format("ae2csl.machine_settings.entropy_change_mode.increase.desc")
                : net.minecraft.client.resources.I18n.format("ae2csl.machine_settings.entropy_change_mode.decrease.desc");
            mc.fontRenderer.drawString(label, 2, 40, 0x404040);
        }
    }
}
