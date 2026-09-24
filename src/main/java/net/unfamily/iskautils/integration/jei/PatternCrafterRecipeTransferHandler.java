package net.unfamily.iskautils.integration.jei;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterScreen;
import net.unfamily.iskautils.client.gui.ModMenuTypes;
import net.unfamily.iskautils.integration.recipeviewer.PatternCrafterRecipeViewerTransfer;
import net.unfamily.iskautils.integration.recipeviewer.PatternCrafterRecipeViewerTransfer.PreparedTransfer;
import net.unfamily.iskautils.integration.recipeviewer.PatternCrafterRecipeViewerTransfer.TransferStatus;
import org.jetbrains.annotations.Nullable;

/**
 * JEI crafting → Pattern Crafter:
 * variables/filters (including {@code #tag}) apply immediately;
 * pattern grid + crafting mode stay pending until Save.
 */
public final class PatternCrafterRecipeTransferHandler
        implements IRecipeTransferHandler<ImprovedPatternCrafterMenu, RecipeHolder<CraftingRecipe>> {
    private final IRecipeTransferHandlerHelper helper;

    public PatternCrafterRecipeTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override public Class<? extends ImprovedPatternCrafterMenu> getContainerClass() {
        return ImprovedPatternCrafterMenu.class;
    }

    @Override public Optional<MenuType<ImprovedPatternCrafterMenu>> getMenuType() {
        return Optional.of(ModMenuTypes.IMPROVED_PATTERN_CRAFTER_MENU.get());
    }

    @Override public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(
            ImprovedPatternCrafterMenu menu,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {
        if (!PatternCrafterRecipeViewerTransfer.isTransferEnabled()) {
            return error("jei.iska_utils.pattern_crafter.transfer.wrong_menu");
        }
        ImprovedPatternCrafterBlockEntity be = menu.getBlockEntity();
        if (be == null) {
            return error("jei.iska_utils.pattern_crafter.transfer.wrong_menu");
        }

        List<ItemStack> grid = PatternCrafterRecipeViewerTransfer.emptyGrid();
        List<String> filterSpecs = PatternCrafterRecipeViewerTransfer.emptyFilterSpecs();

        Map<Integer, Ingredient> ingredients = helper.getGuiSlotIndexToIngredientMap(recipe);
        boolean anyFromMap = false;
        for (Map.Entry<Integer, Ingredient> entry : ingredients.entrySet()) {
            int cell = entry.getKey();
            if (cell < 0 || cell >= 9) continue;
            Ingredient ingredient = entry.getValue();
            if (ingredient.isEmpty()) continue;
            ItemStack chosen = PatternCrafterRecipeViewerTransfer.chooseIngredient(be, ingredient);
            if (chosen.isEmpty()) continue;
            grid.set(cell, chosen.copyWithCount(1));
            filterSpecs.set(cell, PatternCrafterRecipeViewerTransfer.filterSpecFromIngredient(ingredient, chosen));
            anyFromMap = true;
        }

        if (!anyFromMap) {
            List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
            int cell = 0;
            for (IRecipeSlotView slotView : inputs) {
                if (cell >= 9) break;
                List<ItemStack> choices = slotView.getItemStacks().map(s -> s.copyWithCount(1)).toList();
                ItemStack chosen = slotView.getDisplayedItemStack()
                        .map(s -> s.copyWithCount(1))
                        .orElse(choices.isEmpty() ? ItemStack.EMPTY : choices.getFirst());
                if (chosen.isEmpty()) {
                    cell++;
                    continue;
                }
                grid.set(cell, chosen);
                filterSpecs.set(cell, PatternCrafterRecipeViewerTransfer.filterSpecFromStacks(choices, chosen));
                cell++;
            }
        }

        int craftingMode = PatternCrafterRecipeViewerTransfer.resolveCraftingMode(recipe.value());
        PreparedTransfer prepared = PatternCrafterRecipeViewerTransfer.prepare(be, grid, filterSpecs, craftingMode);
        if (prepared == null) {
            return error("jei.iska_utils.pattern_crafter.transfer.no_variables");
        }

        if (doTransfer) {
            ImprovedPatternCrafterScreen screen = findPatternCrafterScreen();
            TransferStatus status = PatternCrafterRecipeViewerTransfer.applyPrepared(be, prepared, screen);
            if (status != TransferStatus.OK) {
                return error("jei.iska_utils.pattern_crafter.transfer.wrong_menu");
            }
        }
        return null;
    }

    private static @Nullable ImprovedPatternCrafterScreen findPatternCrafterScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        Screen screen = mc.screen;
        if (screen instanceof ImprovedPatternCrafterScreen pc) return pc;
        if (screen instanceof IRecipesGui recipesGui) {
            Screen parent = recipesGui.getParentScreen().orElse(null);
            if (parent instanceof ImprovedPatternCrafterScreen pc) return pc;
        }
        return null;
    }

    private IRecipeTransferError error(String key) {
        return helper.createUserErrorWithTooltip(Component.translatable(key));
    }
}
