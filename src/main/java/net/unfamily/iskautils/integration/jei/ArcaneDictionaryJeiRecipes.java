package net.unfamily.iskautils.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.arcane.ArcaneDictionaryDefinition;
import net.unfamily.iskautils.arcane.ArcaneDictionaryLoader;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiDescriptions;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiLines;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArcaneDictionaryJeiRecipes {
    private static final int MAX_CATALYST_SLOTS = 3;

    private static volatile List<ArcaneDictionaryJeiRecipe> CACHE = List.of();
    private static volatile int MAX_HEIGHT = ArcaneDictionaryJeiDescriptions.MIN_HEIGHT;

    private ArcaneDictionaryJeiRecipes() {}

    public static void reloadForClient(Minecraft mc) {
        ensureLoaded();
        CACHE = buildAll(mc);
    }

    public static List<ArcaneDictionaryJeiRecipe> buildAll() {
        return buildAll(Minecraft.getInstance());
    }

    public static List<ArcaneDictionaryJeiRecipe> buildAll(Minecraft mc) {
        ensureLoaded();
        Map<ResourceLocation, ArcaneDictionaryDefinition.Entry> unique = new LinkedHashMap<>();
        for (ArcaneDictionaryDefinition.Entry entry : ArcaneDictionaryLoader.getEntries()) {
            unique.putIfAbsent(entry.enchant(), entry);
        }
        List<ArcaneDictionaryDefinition.Entry> sorted = new ArrayList<>(unique.values());
        sortForJei(sorted);

        var font = mc != null ? mc.font : null;
        List<ArcaneDictionaryJeiRecipe> out = new ArrayList<>();
        int maxHeight = ArcaneDictionaryJeiDescriptions.MIN_HEIGHT;
        for (ArcaneDictionaryDefinition.Entry entry : sorted) {
            ResourceLocation traitId = entry.enchant();
            var lines = ArcaneDictionaryJeiDescriptions.buildLines(traitId, entry);
            var wrapped = font != null
                    ? ArcaneDictionaryJeiDescriptions.wrapLines(font, lines)
                    : ArcaneDictionaryJeiDescriptions.wrapLinesWithoutFont(lines);
            var catalysts = ArcaneDictionaryJeiLines.resolveCatalysts(entry.catalysts(), MAX_CATALYST_SLOTS);
            boolean hasCatalysts = !catalysts.isEmpty();
            int catalystRowY = ArcaneDictionaryJeiDescriptions.computeCatalystRowY(wrapped, hasCatalysts);
            int height = ArcaneDictionaryJeiDescriptions.computeHeight(wrapped, hasCatalysts);
            maxHeight = Math.max(maxHeight, height);
            out.add(new ArcaneDictionaryJeiRecipe(
                    traitId,
                    entry,
                    wrapped,
                    catalystRowY,
                    height,
                    catalysts));
        }
        MAX_HEIGHT = maxHeight;
        return List.copyOf(out);
    }

    public static List<ArcaneDictionaryJeiRecipe> cached() {
        return CACHE;
    }

    public static int maxHeight() {
        return MAX_HEIGHT;
    }

    private static void ensureLoaded() {
        if (!ArcaneDictionaryLoader.getEntries().isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ArcaneDictionaryLoader.loadAll(server.getResourceManager());
            return;
        }
        ArcaneDictionaryLoader.loadAllBootstrap();
    }

    /** Same order as {@link SuspiciousDeliveryJeiRecipes}: rarest first, then highest luck. */
    private static void sortForJei(List<ArcaneDictionaryDefinition.Entry> entries) {
        int total = ArcaneDictionaryJeiLines.poolTotalWeight();
        if (total <= 0) {
            entries.sort(Comparator.comparingInt(ArcaneDictionaryDefinition.Entry::luck).reversed());
            return;
        }
        entries.sort(Comparator
                .comparingDouble((ArcaneDictionaryDefinition.Entry entry) -> 100.0 * entry.weight() / total)
                .thenComparing(Comparator.comparingInt(ArcaneDictionaryDefinition.Entry::luck).reversed()));
    }
}
