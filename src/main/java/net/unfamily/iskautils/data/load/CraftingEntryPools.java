package net.unfamily.iskautils.data.load;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabIfVariant;
import net.unfamily.iskautils.data.load.ancienttablet.AncientTabletRecipeEntry;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryDefinition;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryStageHost;
import net.unfamily.iskautils.script.EntryIfBranch;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CraftingEntryPools {
    private CraftingEntryPools() {}

    public static List<SuspiciousDeliveryDefinition.Entry> eligibleDeliveryPool(
            ServerPlayer player,
            SuspiciousDeliveryDefinition definition) {
        List<SuspiciousDeliveryDefinition.Entry> pool = new ArrayList<>();
        for (SuspiciousDeliveryDefinition.Entry entry : definition.entries()) {
            if (entry.isPoolEligible(player)) {
                pool.add(entry);
            }
        }
        return pool;
    }

    public static int deliveryPoolTotalWeight(List<SuspiciousDeliveryDefinition.Entry> pool) {
        int total = 0;
        for (SuspiciousDeliveryDefinition.Entry entry : pool) {
            total += Math.max(0, entry.weight());
        }
        return total;
    }

    public static double deliveryChancePercent(
            SuspiciousDeliveryDefinition.Entry entry,
            List<SuspiciousDeliveryDefinition.Entry> pool) {
        int total = deliveryPoolTotalWeight(pool);
        if (total <= 0) {
            return 0.0D;
        }
        return 100.0D * Math.max(0, entry.weight()) / total;
    }

    public static Optional<AncientTabletRecipeEntry.ResolvedCraft> resolveAncientTabVariant(
            AncientTabletRecipeEntry entry,
            @Nullable ServerPlayer player) {
        return entry.resolveForPlayer(player);
    }

    public static List<AncientTabletRecipeEntry> ancientTabEntriesForPlayer(
            List<AncientTabletRecipeEntry> entries,
            @Nullable ServerPlayer player) {
        List<AncientTabletRecipeEntry> out = new ArrayList<>();
        for (AncientTabletRecipeEntry entry : entries) {
            if (entry.resolveForPlayer(player).isPresent()) {
                out.add(entry);
            }
        }
        return out;
    }

    public static List<FactoryLoader.Output> resolveFactoryOutputs(
            FactoryLoader.Source source,
            @Nullable ServerPlayer player) {
        return source.resolveOutputs(player);
    }

    public static Optional<EntryIfBranch> firstMatchingBranch(
            List<EntryIfBranch> branches,
            SuspiciousDeliveryStageHost host,
            @Nullable ServerPlayer player) {
        if (player == null || branches == null || branches.isEmpty()) {
            return Optional.empty();
        }
        for (EntryIfBranch branch : branches) {
            if (branch.matches(player, host)) {
                return Optional.of(branch);
            }
        }
        return Optional.empty();
    }

    public static boolean visibleForJeiMods(SuspiciousDeliveryStageHost host) {
        return host.checkAllMods();
    }

    @Nullable
    public static ServerPlayer resolveJeiPlayer(@Nullable Minecraft mc) {
        if (mc == null) {
            return null;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            server = mc.getSingleplayerServer();
        }
        if (server == null || mc.player == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(mc.player.getUUID());
    }

    public static boolean ancientTabVariantActive(
            AncientTabIfVariant variant,
            AncientTabletRecipeEntry entry,
            @Nullable ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return variant.matches(player, entry.gateHost());
    }
}
