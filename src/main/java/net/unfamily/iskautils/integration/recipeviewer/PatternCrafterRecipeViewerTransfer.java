package net.unfamily.iskautils.integration.recipeviewer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.ImprovedPatternCrafterBlockEntity;
import net.unfamily.iskautils.client.gui.ImprovedPatternCrafterScreen;
import net.unfamily.iskautils.network.packet.PatternCrafterJeiTransferC2SPacket;
import net.unfamily.iskautils.pattern.PatternData;
import org.jetbrains.annotations.Nullable;

/**
 * Shared Pattern Crafter fill logic for JEI / EMI / REI (26.x).
 * Variables apply immediately; pattern grid + crafting mode stay pending until Save.
 */
public final class PatternCrafterRecipeViewerTransfer {

    public enum TransferStatus {
        OK,
        WRONG_MENU,
        NO_VARIABLES
    }

    public record PreparedTransfer(
            List<ItemStack> grid,
            List<String> filterSpecs,
            int craftingMode,
            ImprovedPatternCrafterBlockEntity.JeiTransferPreview preview) {}

    private PatternCrafterRecipeViewerTransfer() {}

    public static boolean isTransferEnabled() {
        return Config.enableRecipeViewerTransfer;
    }

    public static List<ItemStack> emptyGrid() {
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            grid.add(ItemStack.EMPTY);
        }
        return grid;
    }

    public static List<String> emptyFilterSpecs() {
        List<String> specs = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            specs.add("");
        }
        return specs;
    }

    public static int resolveCraftingMode(@Nullable CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe) {
            return PatternData.CRAFTING_MODE_SHAPED_ONLY;
        }
        if (recipe instanceof ShapelessRecipe) {
            return PatternData.CRAFTING_MODE_SHAPELESS_ONLY;
        }
        return PatternData.CRAFTING_MODE_BOTH;
    }

    public static int resolveCraftingMode(boolean shapeless) {
        return shapeless ? PatternData.CRAFTING_MODE_SHAPELESS_ONLY : PatternData.CRAFTING_MODE_SHAPED_ONLY;
    }

    public static ItemStack chooseFromStacks(ImprovedPatternCrafterBlockEntity be, List<ItemStack> choices) {
        if (choices == null || choices.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int keyCount = be.getEffectiveKeyInputCount();
        for (ItemStack choice : choices) {
            if (choice == null || choice.isEmpty()) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(choice.getItem()).toString();
            for (int i = 0; i < keyCount; i++) {
                if (be.getFilterLetter(i) <= 0) continue;
                if (ImprovedPatternCrafterBlockEntity.isExactSimpleItemFilter(be.getInputFilterString(i), itemId)) {
                    return choice.copyWithCount(1);
                }
            }
        }
        for (ItemStack choice : choices) {
            if (choice != null && !choice.isEmpty()) {
                return choice.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    public static String filterSpecFromStacks(List<ItemStack> choices, ItemStack chosen) {
        TagKey<Item> tag = findSmallestCoveringTag(choices);
        if (tag != null) {
            return "#" + tag.location();
        }
        if (chosen.isEmpty()) return "";
        return "-" + BuiltInRegistries.ITEM.getKey(chosen.getItem());
    }

    public static TransferStatus validateCapacity(
            ImprovedPatternCrafterBlockEntity be, List<String> filterSpecs) {
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
            return TransferStatus.NO_VARIABLES;
        }
        return TransferStatus.OK;
    }

    public static boolean hasAnyFilled(List<ItemStack> grid) {
        for (ItemStack stack : grid) {
            if (stack != null && !stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static PreparedTransfer prepare(
            ImprovedPatternCrafterBlockEntity be,
            List<ItemStack> grid,
            List<String> filterSpecs,
            int craftingMode) {
        if (be == null) {
            return null;
        }
        if (!hasAnyFilled(grid)) {
            return null;
        }
        if (validateCapacity(be, filterSpecs) != TransferStatus.OK) {
            return null;
        }
        ImprovedPatternCrafterBlockEntity.JeiTransferPreview preview =
                be.previewJeiTransfer(grid, filterSpecs);
        if (preview == null) {
            return null;
        }
        return new PreparedTransfer(grid, filterSpecs, craftingMode, preview);
    }

    public static TransferStatus applyPrepared(
            ImprovedPatternCrafterBlockEntity be,
            PreparedTransfer prepared,
            @Nullable ImprovedPatternCrafterScreen screen) {
        if (be == null || prepared == null) {
            return TransferStatus.WRONG_MENU;
        }
        ImprovedPatternCrafterScreen target = screen != null ? screen : findPatternCrafterScreen();
        if (target == null) {
            return TransferStatus.WRONG_MENU;
        }
        be.applyJeiVariablesOnly(prepared.grid(), prepared.filterSpecs());
        ClientPacketDistributor.sendToServer(new PatternCrafterJeiTransferC2SPacket(
                be.getBlockPos(), prepared.grid(), prepared.filterSpecs(), prepared.craftingMode()));
        target.applyJeiPending(prepared.preview().cellLetters(), prepared.craftingMode(), prepared.grid());
        return TransferStatus.OK;
    }

    @Nullable
    public static ImprovedPatternCrafterScreen findPatternCrafterScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        Screen screen = mc.screen;
        if (screen instanceof ImprovedPatternCrafterScreen pc) {
            return pc;
        }
        return null;
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

    @Nullable
    public static TagKey<Item> findSmallestCoveringTag(List<ItemStack> choices) {
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
}
