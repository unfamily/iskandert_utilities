package net.unfamily.iskautils.integration.emi;

import java.util.ArrayList;
import java.util.List;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterScreen;
import net.unfamily.iskautils.integration.recipeviewer.PatternCrafterRecipeViewerTransfer;
import net.unfamily.iskautils.integration.recipeviewer.PatternCrafterRecipeViewerTransfer.PreparedTransfer;
import net.unfamily.iskautils.integration.recipeviewer.PatternCrafterRecipeViewerTransfer.TransferStatus;

/**
 * EMI crafting fill → Pattern Crafter (variables immediate, grid pending until Save).
 */
public final class PatternCrafterEmiRecipeHandler implements EmiRecipeHandler<ImprovedPatternCrafterMenu> {

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<ImprovedPatternCrafterMenu> screen) {
        // Do NOT call EmiPlayerInventory.of(player): that re-enters the first recipe handler's
        // getInventory and overflows when Pattern Crafter is open.
        var player = screen.getMinecraft().player;
        if (player == null) {
            return new EmiPlayerInventory(List.of());
        }
        List<EmiStack> stacks = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                stacks.add(EmiStack.of(stack));
            }
        }
        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING
                && PatternCrafterRecipeViewerTransfer.isTransferEnabled();
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<ImprovedPatternCrafterMenu> context) {
        return prepare(recipe, context.getScreenHandler()) != null;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<ImprovedPatternCrafterMenu> context) {
        ImprovedPatternCrafterMenu menu = context.getScreenHandler();
        PreparedTransfer prepared = prepare(recipe, menu);
        if (prepared == null) {
            return false;
        }
        ImprovedPatternCrafterScreen screen = context.getScreen() instanceof ImprovedPatternCrafterScreen pc
                ? pc
                : null;
        return PatternCrafterRecipeViewerTransfer.applyPrepared(menu.getBlockEntity(), prepared, screen)
                == TransferStatus.OK;
    }

    private static PreparedTransfer prepare(EmiRecipe recipe, ImprovedPatternCrafterMenu menu) {
        ImprovedPatternCrafterBlockEntity be = menu.getBlockEntity();
        if (be == null) {
            return null;
        }
        List<ItemStack> grid = PatternCrafterRecipeViewerTransfer.emptyGrid();
        List<String> filterSpecs = PatternCrafterRecipeViewerTransfer.emptyFilterSpecs();
        List<EmiIngredient> inputs = recipe.getInputs();
        int cell = 0;
        for (EmiIngredient ingredient : inputs) {
            if (cell >= 9) break;
            List<ItemStack> choices = new ArrayList<>();
            for (EmiStack stack : ingredient.getEmiStacks()) {
                ItemStack item = stack.getItemStack();
                if (item != null && !item.isEmpty()) {
                    choices.add(item.copyWithCount(1));
                }
            }
            ItemStack chosen = PatternCrafterRecipeViewerTransfer.chooseFromStacks(be, choices);
            if (!chosen.isEmpty()) {
                grid.set(cell, chosen);
                filterSpecs.set(cell, PatternCrafterRecipeViewerTransfer.filterSpecFromStacks(choices, chosen));
            }
            cell++;
        }
        int craftingMode = craftingModeFrom(recipe);
        return PatternCrafterRecipeViewerTransfer.prepare(be, grid, filterSpecs, craftingMode);
    }

    private static int craftingModeFrom(EmiRecipe recipe) {
        RecipeHolder<?> backing = recipe.getBackingRecipe();
        if (backing != null && backing.value() instanceof CraftingRecipe crafting) {
            return PatternCrafterRecipeViewerTransfer.resolveCraftingMode(crafting);
        }
        return PatternCrafterRecipeViewerTransfer.resolveCraftingMode(false);
    }
}
