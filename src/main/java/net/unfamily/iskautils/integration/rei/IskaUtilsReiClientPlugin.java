package net.unfamily.iskautils.integration.rei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterScreen;
import net.unfamily.iskautils.integration.recipeviewer.PatternCrafterRecipeViewerTransfer;
import net.unfamily.iskautils.integration.recipeviewer.PatternCrafterRecipeViewerTransfer.PreparedTransfer;
import net.unfamily.iskautils.integration.recipeviewer.PatternCrafterRecipeViewerTransfer.TransferStatus;

@REIPluginClient
public final class IskaUtilsReiClientPlugin implements REIClientPlugin {

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        if (!ModList.get().isLoaded("roughlyenoughitems")
                || !PatternCrafterRecipeViewerTransfer.isTransferEnabled()) {
            return;
        }
        registry.register(new PatternCrafterReiTransferHandler());
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        if (!ModList.get().isLoaded("roughlyenoughitems")
                || !PatternCrafterRecipeViewerTransfer.isTransferEnabled()) {
            return;
        }
        registry.registerDraggableStackVisitor(new IskaUtilsReiGhostVisitor());
    }

    private static final class PatternCrafterReiTransferHandler implements TransferHandler {
        @Override
        public Result handle(Context context) {
            if (!(context.getMenu() instanceof ImprovedPatternCrafterMenu menu)) {
                return Result.createNotApplicable();
            }
            Display display = context.getDisplay();
            if (!BuiltinPlugin.CRAFTING.equals(display.getCategoryIdentifier())) {
                return Result.createNotApplicable();
            }
            ImprovedPatternCrafterBlockEntity be = menu.getBlockEntity();
            if (be == null) {
                return Result.createFailed(Component.translatable("jei.iska_utils.pattern_crafter.transfer.wrong_menu"));
            }

            List<ItemStack> grid = PatternCrafterRecipeViewerTransfer.emptyGrid();
            List<String> filterSpecs = PatternCrafterRecipeViewerTransfer.emptyFilterSpecs();
            fillFromDisplay(display, be, grid, filterSpecs);

            int craftingMode = craftingModeFrom(display);
            PreparedTransfer prepared =
                    PatternCrafterRecipeViewerTransfer.prepare(be, grid, filterSpecs, craftingMode);
            if (prepared == null) {
                return Result.createFailed(Component.translatable("jei.iska_utils.pattern_crafter.transfer.no_variables"));
            }
            if (!context.isActuallyCrafting()) {
                return Result.createSuccessful();
            }
            ImprovedPatternCrafterScreen screen = context.getContainerScreen() instanceof ImprovedPatternCrafterScreen pc
                    ? pc
                    : null;
            TransferStatus status = PatternCrafterRecipeViewerTransfer.applyPrepared(be, prepared, screen);
            if (status != TransferStatus.OK) {
                return Result.createFailed(Component.translatable("jei.iska_utils.pattern_crafter.transfer.wrong_menu"));
            }
            return Result.createSuccessful().blocksFurtherHandling();
        }

        private static void fillFromDisplay(
                Display display,
                ImprovedPatternCrafterBlockEntity be,
                List<ItemStack> grid,
                List<String> filterSpecs) {
            if (display instanceof DefaultCraftingDisplay<?> crafting) {
                List<EntryIngredient> organised = crafting.getOrganisedInputEntries(3, 3);
                for (int cell = 0; cell < 9 && cell < organised.size(); cell++) {
                    EntryIngredient ingredient = organised.get(cell);
                    List<ItemStack> choices = stacksFrom(ingredient);
                    ItemStack chosen = PatternCrafterRecipeViewerTransfer.chooseFromStacks(be, choices);
                    if (!chosen.isEmpty()) {
                        grid.set(cell, chosen);
                        filterSpecs.set(
                                cell, PatternCrafterRecipeViewerTransfer.filterSpecFromStacks(choices, chosen));
                    }
                }
                return;
            }
            List<EntryIngredient> inputs = display.getInputEntries();
            int cell = 0;
            for (EntryIngredient ingredient : inputs) {
                if (cell >= 9) break;
                List<ItemStack> choices = stacksFrom(ingredient);
                ItemStack chosen = PatternCrafterRecipeViewerTransfer.chooseFromStacks(be, choices);
                if (!chosen.isEmpty()) {
                    grid.set(cell, chosen);
                    filterSpecs.set(cell, PatternCrafterRecipeViewerTransfer.filterSpecFromStacks(choices, chosen));
                }
                cell++;
            }
        }

        private static List<ItemStack> stacksFrom(EntryIngredient ingredient) {
            List<ItemStack> choices = new ArrayList<>();
            if (ingredient == null) {
                return choices;
            }
            for (EntryStack<?> stack : ingredient) {
                Object value = stack.getValue();
                if (value instanceof ItemStack item && !item.isEmpty()) {
                    choices.add(item.copyWithCount(1));
                }
            }
            return choices;
        }

        private static int craftingModeFrom(Display display) {
            if (display instanceof DefaultCraftingDisplay<?> crafting) {
                Optional<? extends RecipeHolder<?>> opt = crafting.getOptionalRecipe();
                if (opt.isPresent() && opt.get().value() instanceof CraftingRecipe recipe) {
                    return PatternCrafterRecipeViewerTransfer.resolveCraftingMode(recipe);
                }
                return PatternCrafterRecipeViewerTransfer.resolveCraftingMode(crafting.isShapeless());
            }
            return PatternCrafterRecipeViewerTransfer.resolveCraftingMode(false);
        }
    }
}
