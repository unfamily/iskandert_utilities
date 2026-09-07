package net.unfamily.iskautils.integration.jei;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterScreen;
import net.unfamily.iskautils.client.gui.ModMenuTypes;
import net.unfamily.iskautils.network.packet.PatternCrafterJeiTransferC2SPacket;
import net.unfamily.iskautils.pattern.PatternData;
import org.jetbrains.annotations.Nullable;

/** Encodes JEI crafting recipes into Pattern Crafter variables instead of moving real items. */
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

    @Override public IRecipeHolderType<CraftingRecipe> getRecipeType() {
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
        if (menu.getBlockEntity() == null) {
            return error("jei.iska_utils.pattern_crafter.transfer.wrong_menu");
        }
        ImprovedPatternCrafterScreen openScreen = Minecraft.getInstance().screen instanceof ImprovedPatternCrafterScreen s
                ? s : null;
        for (int i = 0; i < 9; i++) {
            int cell = openScreen != null ? openScreen.getEffectiveGridCell(i) : menu.getGridCell(i);
            if (cell != PatternData.EMPTY) {
                return error("jei.iska_utils.pattern_crafter.transfer.pattern_slot_assigned");
            }
        }

        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(ItemStack.EMPTY);
        Map<Integer, SlotDisplay> ingredients = helper.getGuiSlotIndexToIngredientMap(recipe);
        ContextMap displayContext = SlotDisplayContext.fromLevel(player.level());
        Set<net.minecraft.world.item.Item> newTypes = new HashSet<>();
        int freeSlots = 0;
        boolean[] usedLetters = new boolean[27];
        for (int i = 0; i < menu.getInputFilterSlotCount(); i++) {
            String filter = menu.getBlockEntity().getInputFilterString(i);
            if (filter.isEmpty() && menu.getFilterLetter(i) == 0) freeSlots++;
            int letter = menu.getFilterLetter(i);
            if (letter > 0 && letter < usedLetters.length) usedLetters[letter] = true;
        }
        for (Map.Entry<Integer, SlotDisplay> entry : ingredients.entrySet()) {
            int cell = entry.getKey();
            if (cell < 0 || cell >= 9) continue;
            List<ItemStack> choices = entry.getValue().resolveForStacks(displayContext);
            ItemStack chosen = chooseIngredient(menu, choices, player);
            if (chosen.isEmpty()) continue;
            grid.set(cell, chosen.copyWithCount(1));
            boolean existing = false;
            for (int i = 0; i < menu.getInputFilterSlotCount(); i++) {
                String filter = menu.getBlockEntity().getInputFilterString(i);
                if (!filter.isEmpty() && menu.getFilterLetter(i) > 0
                        && net.unfamily.iskautils.util.DeepDrawerItemFilter.matchesFilterEntry(
                                chosen, filter, player.level().registryAccess())) {
                    existing = true;
                    break;
                }
            }
            if (!existing) newTypes.add(chosen.getItem());
        }
        int freeLetters = 0;
        for (int i = 1; i < usedLetters.length; i++) if (!usedLetters[i]) freeLetters++;
        if (newTypes.size() > Math.min(freeSlots, freeLetters)) {
            return error("jei.iska_utils.pattern_crafter.transfer.no_variables");
        }
        if (doTransfer) {
            int craftingMode = resolveCraftingMode(recipe.value());
            int[] pendingLetters = menu.getBlockEntity().previewJeiGridLetters(grid);
            if (pendingLetters == null) {
                return error("jei.iska_utils.pattern_crafter.transfer.no_variables");
            }
            ClientPacketDistributor.sendToServer(
                    new PatternCrafterJeiTransferC2SPacket(
                            menu.getBlockEntity().getBlockPos(), grid, craftingMode));
            if (openScreen != null) {
                openScreen.applyJeiPending(pendingLetters, craftingMode);
            } else if (Minecraft.getInstance().screen instanceof ImprovedPatternCrafterScreen screen) {
                screen.applyJeiPending(pendingLetters, craftingMode);
            }
        }
        return null;
    }

    private static int resolveCraftingMode(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe) {
            return PatternData.CRAFTING_MODE_SHAPED_ONLY;
        }
        if (recipe instanceof ShapelessRecipe) {
            return PatternData.CRAFTING_MODE_SHAPELESS_ONLY;
        }
        return PatternData.CRAFTING_MODE_BOTH;
    }

    private static ItemStack chooseIngredient(ImprovedPatternCrafterMenu menu, List<ItemStack> choices, Player player) {
        for (int i = 0; i < menu.getInputFilterSlotCount(); i++) {
            String filter = menu.getBlockEntity().getInputFilterString(i);
            if (filter.isEmpty() || menu.getFilterLetter(i) <= 0) continue;
            for (ItemStack choice : choices) {
                if (!choice.isEmpty() && net.unfamily.iskautils.util.DeepDrawerItemFilter.matchesFilterEntry(
                        choice, filter, player.level().registryAccess())) {
                    return choice;
                }
            }
        }
        return choices.isEmpty() ? ItemStack.EMPTY : choices.getFirst();
    }

    private IRecipeTransferError error(String key) {
        return helper.createUserErrorWithTooltip(Component.translatable(key));
    }
}
