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
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterMenu;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterScreen;
import net.unfamily.iskautils.client.gui.ModMenuTypes;
import net.unfamily.iskautils.network.packet.PatternCrafterJeiTransferC2SPacket;
import net.unfamily.iskautils.pattern.PatternData;
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
        ImprovedPatternCrafterBlockEntity be = menu.getBlockEntity();
        if (be == null) {
            return error("jei.iska_utils.pattern_crafter.transfer.wrong_menu");
        }

        List<ItemStack> grid = new ArrayList<>(9);
        List<String> filterSpecs = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            grid.add(ItemStack.EMPTY);
            filterSpecs.add("");
        }

        ContextMap displayContext = SlotDisplayContext.fromLevel(player.level());
        Map<Integer, SlotDisplay> displays = helper.getGuiSlotIndexToIngredientMap(recipe);
        Map<Integer, Ingredient> ingredientsByCell = mapIngredientsToGuiSlots(recipe.value());

        for (Map.Entry<Integer, SlotDisplay> entry : displays.entrySet()) {
            int cell = entry.getKey();
            if (cell < 0 || cell >= 9) continue;
            SlotDisplay display = entry.getValue();
            List<ItemStack> choices = display.resolveForStacks(displayContext);
            ItemStack chosen = chooseIngredient(be, choices);
            if (chosen.isEmpty()) continue;
            grid.set(cell, chosen.copyWithCount(1));

            String spec = "";
            Ingredient ingredient = ingredientsByCell.get(cell);
            if (ingredient != null && !ingredient.isEmpty()) {
                spec = filterSpecFromIngredient(ingredient, chosen);
            }
            if (spec.isEmpty() || spec.startsWith("-")) {
                String fromDisplay = filterSpecFromSlotDisplay(display, chosen);
                if (fromDisplay.startsWith("#") || spec.isEmpty()) {
                    spec = fromDisplay;
                }
            }
            filterSpecs.set(cell, spec);
        }

        boolean hasAny = false;
        for (ItemStack stack : grid) {
            if (!stack.isEmpty()) {
                hasAny = true;
                break;
            }
        }
        if (!hasAny) {
            return error("jei.iska_utils.pattern_crafter.transfer.no_variables");
        }

        Set<String> newSpecs = new HashSet<>();
        int keyCount = be.getEffectiveKeyInputCount();
        int freeSlots = 0;
        boolean[] usedLetters = new boolean[PatternData.MAX_LETTER + 1];
        for (int i = 0; i < keyCount; i++) {
            if (be.getInputFilterString(i).isEmpty()) freeSlots++;
            int letter = be.getFilterLetter(i);
            if (letter > 0 && letter <= PatternData.MAX_LETTER) usedLetters[letter] = true;
        }
        for (String spec : filterSpecs) {
            if (spec == null || spec.isEmpty()) continue;
            if (!hasExactFilterSpec(be, keyCount, spec)) newSpecs.add(spec);
        }
        int freeLetters = 0;
        for (int i = 1; i <= PatternData.MAX_LETTER; i++) {
            if (!usedLetters[i]) freeLetters++;
        }
        if (newSpecs.size() > freeSlots || newSpecs.size() > freeLetters) {
            return error("jei.iska_utils.pattern_crafter.transfer.no_variables");
        }

        ImprovedPatternCrafterBlockEntity.JeiTransferPreview preview =
                be.previewJeiTransfer(grid, filterSpecs);
        if (preview == null) {
            return error("jei.iska_utils.pattern_crafter.transfer.no_variables");
        }

        if (doTransfer) {
            ImprovedPatternCrafterScreen screen = findPatternCrafterScreen();
            if (screen == null) {
                return error("jei.iska_utils.pattern_crafter.transfer.wrong_menu");
            }
            int craftingMode = resolveCraftingMode(recipe.value());
            be.applyJeiVariablesOnly(grid, filterSpecs);
            ClientPacketDistributor.sendToServer(
                    new PatternCrafterJeiTransferC2SPacket(be.getBlockPos(), grid, filterSpecs, craftingMode));
            screen.applyJeiPending(preview.cellLetters(), craftingMode, grid);
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

    private static int resolveCraftingMode(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe) {
            return PatternData.CRAFTING_MODE_SHAPED_ONLY;
        }
        if (recipe instanceof ShapelessRecipe) {
            return PatternData.CRAFTING_MODE_SHAPELESS_ONLY;
        }
        return PatternData.CRAFTING_MODE_BOTH;
    }

    private static Map<Integer, Ingredient> mapIngredientsToGuiSlots(CraftingRecipe recipe) {
        Map<Integer, Ingredient> result = new java.util.LinkedHashMap<>();
        if (recipe instanceof ShapedRecipe shaped) {
            int width = shaped.pattern.width();
            int height = shaped.pattern.height();
            List<Optional<Ingredient>> ingredients = shaped.pattern.ingredients();
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient ingredient = ingredients.get(i).orElse(null);
                if (ingredient == null || ingredient.isEmpty()) continue;
                result.put(craftingIndex(i, width, height), ingredient);
            }
            return result;
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            List<Ingredient> ingredients = shapeless.placementInfo().ingredients();
            int size = shapelessSize(ingredients.size());
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient ingredient = ingredients.get(i);
                if (ingredient == null || ingredient.isEmpty()) continue;
                result.put(craftingIndex(i, size, size), ingredient);
            }
        }
        return result;
    }

    private static int shapelessSize(int total) {
        if (total > 4) return 3;
        if (total > 1) return 2;
        return 1;
    }

    private static int craftingIndex(int ingredientIndex, int width, int height) {
        int x = ingredientIndex % width;
        int y = ingredientIndex / width;
        return x + y * 3;
    }

    private static ItemStack chooseIngredient(ImprovedPatternCrafterBlockEntity be, List<ItemStack> choices) {
        int keyCount = be.getEffectiveKeyInputCount();
        for (ItemStack choice : choices) {
            if (choice.isEmpty()) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(choice.getItem()).toString();
            for (int i = 0; i < keyCount; i++) {
                if (be.getFilterLetter(i) <= 0) continue;
                if (ImprovedPatternCrafterBlockEntity.isExactSimpleItemFilter(be.getInputFilterString(i), itemId)) {
                    return choice;
                }
            }
        }
        return choices.isEmpty() ? ItemStack.EMPTY : choices.getFirst();
    }

    private static boolean hasExactFilterSpec(ImprovedPatternCrafterBlockEntity be, int keyCount, String spec) {
        if (spec == null || spec.isEmpty()) return true;
        for (int i = 0; i < keyCount; i++) {
            if (be.getFilterLetter(i) > 0 && spec.equals(be.getInputFilterString(i))) {
                return true;
            }
        }
        return false;
    }

    private static String filterSpecFromIngredient(Ingredient ingredient, ItemStack chosen) {
        TagKey<Item> tag = findSingleTag(ingredient);
        if (tag != null) return "#" + tag.location();
        List<ItemStack> stacks = ingredient.items().map(h -> new ItemStack(h.value())).toList();
        tag = findSmallestCoveringTag(stacks);
        if (tag != null) return "#" + tag.location();
        if (chosen.isEmpty()) return "";
        return "-" + BuiltInRegistries.ITEM.getKey(chosen.getItem());
    }

    private static String filterSpecFromSlotDisplay(SlotDisplay display, ItemStack chosen) {
        TagKey<Item> tag = extractTag(display);
        if (tag != null) return "#" + tag.location();
        if (chosen.isEmpty()) return "";
        return "-" + BuiltInRegistries.ITEM.getKey(chosen.getItem());
    }

    @Nullable
    private static TagKey<Item> findSingleTag(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return null;
        if (ingredient.isCustom()) {
            ICustomIngredient custom = ingredient.getCustomIngredient();
            if (custom instanceof CompoundIngredient compound) {
                TagKey<Item> only = null;
                for (Ingredient child : compound.children()) {
                    TagKey<Item> childTag = findSingleTag(child);
                    if (childTag == null) return null;
                    if (only == null) only = childTag;
                    else if (!only.equals(childTag)) return null;
                }
                return only;
            }
            return null;
        }
        HolderSet<Item> values = ingredient.getValues();
        return values.unwrap().left().orElse(null);
    }

    @Nullable
    private static TagKey<Item> extractTag(SlotDisplay display) {
        if (display instanceof SlotDisplay.TagSlotDisplay tagDisplay) {
            return tagDisplay.tag();
        }
        if (display instanceof SlotDisplay.WithRemainder withRemainder) {
            return extractTag(withRemainder.input());
        }
        if (display instanceof SlotDisplay.Composite composite) {
            TagKey<Item> only = null;
            for (SlotDisplay child : composite.contents()) {
                if (child instanceof SlotDisplay.Empty) continue;
                TagKey<Item> childTag = extractTag(child);
                if (childTag == null) return null;
                if (only == null) only = childTag;
                else if (!only.equals(childTag)) return null;
            }
            return only;
        }
        return null;
    }

    @Nullable
    private static TagKey<Item> findSmallestCoveringTag(List<ItemStack> choices) {
        Set<Item> needed = new HashSet<>();
        for (ItemStack stack : choices) {
            if (stack != null && !stack.isEmpty()) needed.add(stack.getItem());
        }
        if (needed.size() < 2) return null;

        Item first = needed.iterator().next();
        Holder<Item> firstHolder = first.builtInRegistryHolder();
        TagKey<Item> best = null;
        int bestSize = Integer.MAX_VALUE;
        for (TagKey<Item> tag : firstHolder.tags().toList()) {
            boolean covers = true;
            for (Item item : needed) {
                if (!item.builtInRegistryHolder().is(tag)) {
                    covers = false;
                    break;
                }
            }
            if (!covers) continue;
            int tagSize = 0;
            for (Holder<Item> ignored : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                tagSize++;
                if (tagSize > bestSize) break;
            }
            if (tagSize >= needed.size() && tagSize < bestSize && tagSize <= needed.size() * 3) {
                best = tag;
                bestSize = tagSize;
            }
        }
        return best;
    }

    private IRecipeTransferError error(String key) {
        return helper.createUserErrorWithTooltip(Component.translatable(key));
    }
}
