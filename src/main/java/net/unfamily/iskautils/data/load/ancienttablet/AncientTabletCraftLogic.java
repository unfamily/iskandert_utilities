package net.unfamily.iskautils.data.load.ancienttablet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.item.component.AncientTabletContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shared Ancient Tablet recipe matching and consumption for manual tablet and Ancient Table.
 */
public final class AncientTabletCraftLogic {

    public record CraftSuccess(AncientTabletRecipeEntry entry, AncientTabletRecipeEntry.ResolvedCraft resolved, List<Integer> consumedSlotIndices) {}

    private AncientTabletCraftLogic() {}

    public static List<AncientTabletContents.SlotView> expandContainerSlots(List<ItemStack> slots) {
        if (slots == null || slots.isEmpty()) {
            return List.of();
        }
        List<AncientTabletContents.SlotView> out = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack s = slots.get(i);
            if (s == null || s.isEmpty()) {
                continue;
            }
            for (int k = 0; k < s.getCount(); k++) {
                out.add(new AncientTabletContents.SlotView(i, s.copyWithCount(1)));
            }
        }
        return List.copyOf(out);
    }

    public static List<AncientTabletContents.SlotView> expandTabletSlots(
            ItemStack tablet, net.minecraft.core.HolderLookup.Provider provider) {
        return AncientTabletContents.expandForMatching(tablet, provider);
    }

    public static void consumeContainerAtIndices(List<ItemStack> slots, List<Integer> indices) {
        if (indices == null || indices.isEmpty() || slots == null || slots.isEmpty()) {
            return;
        }
        Map<Integer, Integer> toConsume = new HashMap<>();
        for (int idx : indices) {
            toConsume.merge(idx, 1, Integer::sum);
        }
        for (Map.Entry<Integer, Integer> e : toConsume.entrySet()) {
            int idx = e.getKey();
            int count = e.getValue();
            if (idx < 0 || idx >= slots.size()) {
                continue;
            }
            ItemStack s = slots.get(idx);
            if (s == null || s.isEmpty()) {
                continue;
            }
            s.shrink(count);
            if (s.isEmpty()) {
                slots.set(idx, ItemStack.EMPTY);
            }
        }
    }

    public static void consumeTabletAtIndices(
            ItemStack tablet, net.minecraft.core.HolderLookup.Provider provider, List<Integer> indices) {
        AncientTabletContents.consumeSlotsAtIndices(tablet, provider, indices);
    }

    /** First unordered match wins (Ancient Table). */
    public static Optional<CraftSuccess> tryCraftUnordered(
            List<AncientTabletContents.SlotView> views,
            List<AncientTabletRecipeEntry> entries,
            @Nullable ServerPlayer player) {
        if (views.isEmpty()) {
            return Optional.empty();
        }
        for (AncientTabletRecipeEntry entry : entries) {
            Optional<AncientTabletRecipeEntry.ResolvedCraft> resolved = entry.resolveForPlayer(player);
            if (resolved.isEmpty()) {
                continue;
            }
            AncientTabletRecipeMatcher.MatchOutcome outcome =
                    AncientTabletRecipeMatcher.tryMatchResolved(resolved.get(), views);
            if (outcome.result() == AncientTabletRecipeMatcher.MatchResult.SUCCESS) {
                return Optional.of(new CraftSuccess(entry, resolved.get(), outcome.consumedSlotIndices()));
            }
        }
        return Optional.empty();
    }

    public static Optional<CraftSuccess> tryCraftTablet(
            List<AncientTabletContents.SlotView> views,
            List<AncientTabletRecipeEntry> entries,
            ServerPlayer player) {
        if (views.isEmpty()) {
            return Optional.empty();
        }
        for (AncientTabletRecipeEntry entry : entries) {
            Optional<AncientTabletRecipeEntry.ResolvedCraft> resolved = entry.resolveForPlayer(player);
            if (resolved.isEmpty()) {
                continue;
            }
            AncientTabletRecipeMatcher.MatchOutcome outcome =
                    AncientTabletRecipeMatcher.tryMatchResolved(resolved.get(), views);
            if (outcome.result() == AncientTabletRecipeMatcher.MatchResult.SUCCESS) {
                return Optional.of(new CraftSuccess(entry, resolved.get(), outcome.consumedSlotIndices()));
            }
        }
        return Optional.empty();
    }

    public static Optional<AncientTabletRecipeEntry> findWrongOrderMatch(
            List<AncientTabletContents.SlotView> views,
            List<AncientTabletRecipeEntry> entries,
            ServerPlayer player) {
        for (AncientTabletRecipeEntry entry : entries) {
            Optional<AncientTabletRecipeEntry.ResolvedCraft> resolved = entry.resolveForPlayer(player);
            if (resolved.isEmpty() || !resolved.get().mustOrdered()) {
                continue;
            }
            AncientTabletRecipeMatcher.MatchOutcome outcome =
                    AncientTabletRecipeMatcher.tryMatchResolved(resolved.get(), views);
            if (outcome.result() == AncientTabletRecipeMatcher.MatchResult.WRONG_ORDER) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public static List<ItemStack> outputStacks(AncientTabletRecipeEntry.ResolvedCraft resolved) {
        return AncientTabletRecipeMatcher.expandToExampleStacks(resolved.produce());
    }

    public static List<ItemStack> outputStacks(AncientTabletRecipeEntry recipe) {
        return AncientTabletRecipeMatcher.expandToExampleStacks(recipe.produce());
    }

    public static void giveOutputsToPlayer(Player player, Level level, List<ItemStack> outputs) {
        for (ItemStack out : outputs) {
            if (out.isEmpty()) {
                continue;
            }
            ItemStack copy = out.copy();
            if (!player.getInventory().add(copy)) {
                ItemEntity drop = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), copy);
                level.addFreshEntity(drop);
            }
        }
    }
}
