package net.unfamily.iskautils.data.load.ancienttablet;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.item.component.AncientTabletContents;

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

    public static MatchOutcome tryMatch(AncientTabletRecipeEntry recipe, List<AncientTabletContents.SlotView> tabletSlots) {
        if (recipe.require().isEmpty()) {
            return new MatchOutcome(MatchResult.NO_MATCH, List.of());
        }
        if (recipe.mustOrdered()) {
            return matchOrdered(recipe.require(), tabletSlots);
        }
        return matchUnordered(recipe.require(), tabletSlots);
    }

    public static MatchOutcome tryMatchResolved(
            AncientTabletRecipeEntry.ResolvedCraft recipe,
            List<AncientTabletContents.SlotView> tabletSlots) {
        if (recipe.require().isEmpty()) {
            return new MatchOutcome(MatchResult.NO_MATCH, List.of());
        }
        if (recipe.mustOrdered()) {
            return matchOrdered(recipe.require(), tabletSlots);
        }
        return matchUnordered(recipe.require(), tabletSlots);
    }

    private static MatchOutcome matchOrdered(
            List<AncientTabletRequirement> req, List<AncientTabletContents.SlotView> slots) {
        if (slots.size() < req.size()) {
            return new MatchOutcome(MatchResult.NO_MATCH, List.of());
        }
        List<Integer> used = new ArrayList<>();
        for (int i = 0; i < req.size(); i++) {
            AncientTabletContents.SlotView view = slots.get(i);
            if (!req.get(i).matches(view.stack1())) {
                boolean anyLater = false;
                for (int j = 0; j < slots.size(); j++) {
                    if (req.get(i).matches(slots.get(j).stack1())) {
                        anyLater = true;
                        break;
                    }
                }
                if (anyLater) {
                    return new MatchOutcome(MatchResult.WRONG_ORDER, List.of());
                }
                return new MatchOutcome(MatchResult.NO_MATCH, List.of());
            }
            used.add(view.slotIndex());
        }
        return new MatchOutcome(MatchResult.SUCCESS, used);
    }

    public static MatchOutcome matchUnordered(
            List<AncientTabletRequirement> needed, List<AncientTabletContents.SlotView> slots) {
        List<AncientTabletRequirement> requirements = new ArrayList<>(needed);
        List<Integer> usedIndices = new ArrayList<>();
        boolean[] used = new boolean[slots.size()];
        for (AncientTabletRequirement req : requirements) {
            boolean found = false;
            for (int i = 0; i < slots.size(); i++) {
                if (used[i]) {
                    continue;
                }
                AncientTabletContents.SlotView view = slots.get(i);
                if (req.matches(view.stack1())) {
                    used[i] = true;
                    usedIndices.add(view.slotIndex());
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

    public static ItemStack exampleStackFromTag(AncientTabletRequirement.TagRequirement tr) {
        return BuiltInRegistries.ITEM.get(tr.tag())
                .flatMap(holders -> holders.stream().findFirst())
                .map(h -> new ItemStack(h.value()))
                .orElse(ItemStack.EMPTY);
    }

    public static List<ItemStack> expandToExampleStacks(List<AncientTabletRequirement> requirements) {
        List<ItemStack> out = new ArrayList<>();
        for (AncientTabletRequirement req : requirements) {
            switch (req) {
                case AncientTabletRequirement.ItemRequirement ir -> out.add(new ItemStack(ir.item()));
                case AncientTabletRequirement.TagRequirement tr -> exampleStackFromTag(tr);
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
