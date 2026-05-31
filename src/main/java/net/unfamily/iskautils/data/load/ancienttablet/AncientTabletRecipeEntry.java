package net.unfamily.iskautils.data.load.ancienttablet;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.command.CommandItemDefinition;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record AncientTabletRecipeEntry(
        Identifier sourceId,
        boolean mustOrdered,
        boolean destroyIfWrong,
        int fuelCost,
        SuspiciousDeliveryStageHost gateHost,
        List<AncientTabletRequirement> require,
        List<AncientTabletRequirement> produce,
        List<AncientTabIfVariant> ifVariants) {

    public static final int DEFAULT_FUEL_COST = 1;

    private static final SuspiciousDeliveryStageHost NO_GATE =
            new SuspiciousDeliveryStageHost(
                    List.of(),
                    CommandItemDefinition.StagesLogic.AND,
                    List.of(),
                    CommandItemDefinition.StagesLogic.AND);

    public AncientTabletRecipeEntry {
        gateHost = gateHost != null ? gateHost : NO_GATE;
        require = require != null ? List.copyOf(require) : List.of();
        produce = produce != null ? List.copyOf(produce) : List.of();
        ifVariants = ifVariants != null ? List.copyOf(ifVariants) : List.of();
    }

    /** Legacy constructor shape (no gates). */
    public AncientTabletRecipeEntry(
            Identifier sourceId,
            boolean mustOrdered,
            boolean destroyIfWrong,
            int fuelCost,
            List<AncientTabletRequirement> require,
            List<AncientTabletRequirement> produce) {
        this(sourceId, mustOrdered, destroyIfWrong, fuelCost, NO_GATE, require, produce, List.of());
    }

    public boolean hasIfVariants() {
        return !ifVariants.isEmpty();
    }

    public boolean hasGate() {
        return !gateHost.isEmpty() || hasIfVariants();
    }

    public Optional<ResolvedCraft> resolveForPlayer(@Nullable ServerPlayer player) {
        if (hasIfVariants()) {
            if (player == null) {
                return Optional.empty();
            }
            for (AncientTabIfVariant variant : ifVariants) {
                if (variant.matches(player, gateHost)) {
                    if (variant.require().isEmpty() || variant.produce().isEmpty()) {
                        continue;
                    }
                    return Optional.of(new ResolvedCraft(this, variant.require(), variant.produce()));
                }
            }
            return Optional.empty();
        }
        if (require.isEmpty() || produce.isEmpty()) {
            return Optional.empty();
        }
        if (player != null && !gateHost.isEmpty() && !gateHost.isFullyEligible(player)) {
            return Optional.empty();
        }
        if (player == null && !gateHost.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedCraft(this, require, produce));
    }

    public List<AncientTabletRequirement> activeRequire(@Nullable ServerPlayer player) {
        return resolveForPlayer(player).map(ResolvedCraft::require).orElse(List.of());
    }

    public List<AncientTabletRequirement> activeProduce(@Nullable ServerPlayer player) {
        return resolveForPlayer(player).map(ResolvedCraft::produce).orElse(List.of());
    }

    public List<ItemStack> previewProduceStacks(@Nullable ServerPlayer player) {
        return AncientTabletRecipeMatcher.expandToExampleStacks(activeProduce(player));
    }

    public List<ItemStack> previewProduceStacks() {
        return AncientTabletRecipeMatcher.expandToExampleStacks(produce);
    }

    public record ResolvedCraft(
            AncientTabletRecipeEntry entry,
            List<AncientTabletRequirement> require,
            List<AncientTabletRequirement> produce) {

        public boolean mustOrdered() {
            return entry.mustOrdered();
        }

        public boolean destroyIfWrong() {
            return entry.destroyIfWrong();
        }

        public int fuelCost() {
            return entry.fuelCost();
        }
    }
}
