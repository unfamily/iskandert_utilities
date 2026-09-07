package net.unfamily.iskautils.integration.jei;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.network.PacketDistributor;
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

        Map<Integer, Ingredient> ingredients = helper.getGuiSlotIndexToIngredientMap(recipe);
        boolean anyFromMap = false;
        for (Map.Entry<Integer, Ingredient> entry : ingredients.entrySet()) {
            int cell = entry.getKey();
            if (cell < 0 || cell >= 9) continue;
            Ingredient ingredient = entry.getValue();
            if (ingredient.isEmpty()) continue;
            ItemStack chosen = chooseIngredient(be, ingredient);
            if (chosen.isEmpty()) continue;
            grid.set(cell, chosen.copyWithCount(1));
            filterSpecs.set(cell, filterSpecFromIngredient(ingredient, chosen));
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
                filterSpecs.set(cell, filterSpecFromStacks(choices, chosen));
                cell++;
            }
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
            // Variables immediately (client optimistic + server packet).
            be.applyJeiVariablesOnly(grid, filterSpecs);
            PacketDistributor.sendToServer(
                    new PatternCrafterJeiTransferC2SPacket(be.getBlockPos(), grid, filterSpecs, craftingMode));
            // Pattern grid + mode stay pending until Save.
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

    private static ItemStack chooseIngredient(ImprovedPatternCrafterBlockEntity be, Ingredient ingredient) {
        ItemStack[] choices = ingredient.getItems();
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
        return choices.length == 0 ? ItemStack.EMPTY : choices[0];
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

    /** Prefer {@code #tag} when the ingredient is (or unwraps to) a single tag; else {@code -itemId}. */
    private static String filterSpecFromIngredient(Ingredient ingredient, ItemStack chosen) {
        TagKey<Item> tag = findSingleTag(ingredient);
        if (tag != null) {
            return "#" + tag.location();
        }
        tag = findSmallestCoveringTag(ingredient.getItems());
        if (tag != null) {
            return "#" + tag.location();
        }
        if (chosen.isEmpty()) return "";
        return "-" + BuiltInRegistries.ITEM.getKey(chosen.getItem());
    }

    private static String filterSpecFromStacks(List<ItemStack> choices, ItemStack chosen) {
        TagKey<Item> tag = findSmallestCoveringTag(choices.toArray(ItemStack[]::new));
        if (tag != null) {
            return "#" + tag.location();
        }
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
        try {
            Ingredient.Value[] values = ingredient.getValues();
            TagKey<Item> onlyTag = null;
            boolean onlyTags = values.length > 0;
            for (Ingredient.Value value : values) {
                if (value instanceof Ingredient.TagValue tagValue) {
                    if (onlyTag == null) onlyTag = tagValue.tag();
                    else if (!onlyTag.equals(tagValue.tag())) onlyTags = false;
                } else {
                    onlyTags = false;
                }
            }
            if (onlyTag != null && (values.length == 1 || onlyTags)) {
                return onlyTag;
            }
        } catch (IllegalStateException ignored) {
            // custom ingredients throw from getValues()
        }
        return null;
    }

    /**
     * When an ingredient was expanded to item lists, recover a tag that contains every choice
     * and is no larger than the choice set (exact or near-exact tag match).
     */
    @Nullable
    private static TagKey<Item> findSmallestCoveringTag(ItemStack[] choices) {
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
            // Prefer tags that match the choice set closely (avoid huge tags like #c:foods).
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
