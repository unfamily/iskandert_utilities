package net.unfamily.iskautils.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.arcane.ArcaneDictionaryDefinition;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectRegistry;
import net.unfamily.iskautils.arcane.ArcaneDictionaryLoader;
import net.unfamily.iskautils.arcane.ArcaneDictionaryPools;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.data.load.CraftingEntryPools;

import java.util.ArrayList;
import java.util.List;

/** JEI-only pool helpers that may reference the local client runtime. */
public final class ArcaneDictionaryJeiPools {
    private ArcaneDictionaryJeiPools() {}

    public static List<ArcaneDictionaryDefinition.Entry> eligibleForCurrentEnvironment() {
        return eligibleForPlayer(CraftingEntryPools.resolveJeiPlayer());
    }

    public static List<ArcaneDictionaryDefinition.Entry> eligibleForJei(Minecraft mc) {
        return eligibleForPlayer(resolveJeiPlayer(mc));
    }

    private static List<ArcaneDictionaryDefinition.Entry> eligibleForPlayer(ServerPlayer player) {
        List<ArcaneDictionaryDefinition.Entry> out = new ArrayList<>();
        for (ArcaneDictionaryDefinition.Entry entry : ArcaneDictionaryPools.uniqueEntries(ArcaneDictionaryLoader.getEntries()).values()) {
            if (!entry.checkAllMods()) {
                continue;
            }
            if (player != null && !entry.gateHost().checkAllStages(player)) {
                continue;
            }
            out.add(entry);
        }
        return out;
    }

    public static double chancePercentForJei(ArcaneDictionaryDefinition.Entry entry, Minecraft mc) {
        List<ArcaneDictionaryDefinition.Entry> pool = eligibleForJei(mc);
        return ArcaneDictionaryPools.chancePercent(entry, pool);
    }

    public static ArcaneDictionaryJeiContext jeiContext(ResourceLocation traitId, ArcaneDictionaryDefinition.Entry entry, Minecraft mc) {
        return new ArcaneDictionaryJeiContext(
                traitId,
                entry,
                ArcaneDictionaryEffectRegistry.resolveConsumePerLevel(traitId),
                net.unfamily.iskautils.Config.arcaneDictionaryMinLevel,
                net.unfamily.iskautils.Config.arcaneDictionaryMaxLevel,
                chancePercentForJei(entry, mc));
    }

    private static ServerPlayer resolveJeiPlayer(Minecraft mc) {
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
}
