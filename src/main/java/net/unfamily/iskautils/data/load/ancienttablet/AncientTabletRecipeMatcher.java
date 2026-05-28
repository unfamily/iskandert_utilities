package net.unfamily.iskautils.data.load.ancienttablet;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AncientTabletRecipeMatcher {

    public enum MatchResult {
        SUCCESS,
        NO_MATCH,
        WRONG_ORDER
    }

    public record MatchOutcome(MatchResult result, List<Integer> consumedSlotIndices) {}

    private AncientTabletRecipeMatcher() {}

    public static MatchOutcome tryMatch(AncientTabletRecipeEntry recipe, List<ItemStack> tabletSlots) {
        if (recipe.require().isEmpty()) {
            return new MatchOutcome(MatchResult.NO_MATCH, List.of());
        }
        if (recipe.mustOrdered()) {
            return matchOrdered(recipe, tabletSlots);
        }
        return matchUnordered(recipe, tabletSlots);
    }

    private static MatchOutcome matchOrdered(AncientTabletRecipeEntry recipe, List<ItemStack> slots) {
        List<AncientTabletRequirement> req = recipe.require();
        if (slots.size() < req.size()) {
            return new MatchOutcome(MatchResult.NO_MATCH, List.of());
        }
        List<Integer> used = new ArrayList<>();
        for (int i = 0; i < req.size(); i++) {
            if (!req.get(i).matches(slots.get(i))) {
                boolean anyLater = false;
                for (int j = 0; j < slots.size(); j++) {
                    if (req.get(i).matches(slots.get(j))) {
                        anyLater = true;
                        break;
                    }
                }
                if (anyLater) {
                    return new MatchOutcome(MatchResult.WRONG_ORDER, List.of());
                }
                return new MatchOutcome(MatchResult.NO_MATCH, List.of());
            }
            used.add(i);
        }
        return new MatchOutcome(MatchResult.SUCCESS, used);
    }

    private static MatchOutcome matchUnordered(AncientTabletRecipeEntry recipe, List<ItemStack> slots) {
        List<AncientTabletRequirement> needed = new ArrayList<>(recipe.require());
        List<Integer> usedIndices = new ArrayList<>();
        boolean[] used = new boolean[slots.size()];
        for (AncientTabletRequirement req : needed) {
            boolean found = false;
            for (int i = 0; i < slots.size(); i++) {
                if (used[i]) {
                    continue;
                }
                if (req.matches(slots.get(i))) {
                    used[i] = true;
                    usedIndices.add(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return new MatchOutcome(MatchResult.NO_MATCH, List.of());
            }
        }
        return new MatchOutcome(MatchResult.SUCCESS, usedIndices);
    }

    public static List<ItemStack> expandToExampleStacks(List<AncientTabletRequirement> requirements) {
        List<ItemStack> out = new ArrayList<>();
        for (AncientTabletRequirement req : requirements) {
            switch (req) {
                case AncientTabletRequirement.ItemRequirement ir -> out.add(new ItemStack(ir.item()));
                case AncientTabletRequirement.TagRequirement tr ->
                        BuiltInRegistries.ITEM.getTag(tr.tag())
                                .map(named -> named.stream().findFirst()
                                        .map(h -> new ItemStack(h.value()))
                                        .orElse(ItemStack.EMPTY))
                                .orElse(ItemStack.EMPTY);
            }
        }
        return out;
    }

    /** Collapse consecutive identical requirements for JEI display. */
    public static List<GroupedRequirement> groupConsecutive(List<AncientTabletRequirement> flat) {
        if (flat.isEmpty()) {
            return List.of();
        }
        List<GroupedRequirement> groups = new ArrayList<>();
        AncientTabletRequirement key = flat.get(0).displayKey();
        int count = 1;
        for (int i = 1; i < flat.size(); i++) {
            AncientTabletRequirement cur = flat.get(i).displayKey();
            if (requirementsEqual(key, cur)) {
                count++;
            } else {
                groups.add(new GroupedRequirement(key, count));
                key = cur;
                count = 1;
            }
        }
        groups.add(new GroupedRequirement(key, count));
        return Collections.unmodifiableList(groups);
    }

    private static boolean requirementsEqual(AncientTabletRequirement a, AncientTabletRequirement b) {
        if (a instanceof AncientTabletRequirement.ItemRequirement ia && b instanceof AncientTabletRequirement.ItemRequirement ib) {
            return ia.item() == ib.item();
        }
        if (a instanceof AncientTabletRequirement.TagRequirement ta && b instanceof AncientTabletRequirement.TagRequirement tb) {
            return ta.tag().equals(tb.tag());
        }
        return false;
    }

    public record GroupedRequirement(AncientTabletRequirement requirement, int count) {}
}
