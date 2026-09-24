package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.iskalib.stage.StageRegistry;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopHierarchy;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.shop.ShopStage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side stage checks for shop browse UI.
 * Uses a server-sent cache (updated by {@link net.unfamily.iskautils.network.packet.ShopStagesS2CPacket})
 * for multiplayer support. Falls back to integrated-server direct lookup if cache is empty.
 */
public final class ShopClientStages {

    public record StageFailure(String stageType, String stageId, boolean required) {}

    /** Stage key format: "type:id" e.g. "player:my_stage". */
    public static String stageKey(String type, String id) {
        return type.toLowerCase() + ":" + id;
    }

    /** Client cache: stageKey → hasStage. Populated by ShopStagesS2CPacket. */
    private static final Map<String, Boolean> CACHE = new HashMap<>();

    private ShopClientStages() {}

    public static void replaceCache(Map<String, Boolean> newCache) {
        CACHE.clear();
        CACHE.putAll(newCache);
    }

    /** Check if a single stage is satisfied for the client player. */
    private static boolean hasStage(String type, String id) {
        String key = stageKey(type, id);
        if (!CACHE.isEmpty()) {
            // Use packet cache (works on dedicated server)
            return CACHE.getOrDefault(key, false);
        }
        // Integrated server fallback
        return integratedServerHasStage(type, id);
    }

    private static boolean integratedServerHasStage(String type, String id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;
        try {
            MinecraftServer server = mc.getSingleplayerServer();
            if (server == null) return false;
            ServerPlayer serverPlayer = null;
            String localName = mc.player.getName().getString();
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                if (sp.getName().getString().equals(localName)) {
                    serverPlayer = sp;
                    break;
                }
            }
            if (serverPlayer == null) return false;
            StageRegistry registry = StageRegistry.getInstance(server);
            return switch (type.toLowerCase()) {
                case "player" -> registry.hasPlayerStage(serverPlayer, id);
                case "world" -> registry.hasWorldStage(id);
                case "team" -> registry.hasPlayerTeamStage(serverPlayer, id);
                default -> false;
            };
        } catch (Exception ignored) {
            return false;
        }
    }

    // ── Entry checks ────────────────────────────────────────────────────────

    public static boolean isEntryBlocked(@Nullable ShopEntry item) {
        if (item == null) return false;
        // Check entry's own stages
        if (!getEntryOwnFailures(item).isEmpty()) return true;
        // Check ancestor category stages
        return isAncestorCategoryBlocked(item.inCategory);
    }

    public static List<StageFailure> getFailures(@Nullable ShopEntry item) {
        List<StageFailure> failures = new ArrayList<>(getEntryOwnFailures(item));
        if (item != null && failures.isEmpty()) {
            // If entry passes, check ancestors (return first ancestor failure for tooltip)
            failures.addAll(getAncestorCategoryFailures(item.inCategory));
        }
        return failures;
    }

    private static List<StageFailure> getEntryOwnFailures(@Nullable ShopEntry item) {
        List<StageFailure> failures = new ArrayList<>();
        if (item == null || item.stages == null || item.stages.length == 0) return failures;
        for (ShopStage stage : item.stages) {
            if (stage == null || stage.stageType == null) continue;
            boolean has = hasStage(stage.stageType, stage.stage);
            if (has != stage.is) {
                failures.add(new StageFailure(stage.stageType, stage.stage, stage.is));
            }
        }
        return failures;
    }

    // ── Category checks ─────────────────────────────────────────────────────

    public static boolean isCategoryBlocked(@Nullable ShopCategory category) {
        if (category == null) return false;
        if (!getCategoryOwnFailures(category).isEmpty()) return true;
        return isAncestorCategoryBlocked(category.inCategory);
    }

    public static List<StageFailure> getCategoryFailures(@Nullable ShopCategory category) {
        if (category == null) return List.of();
        List<StageFailure> failures = new ArrayList<>(getCategoryOwnFailures(category));
        if (failures.isEmpty()) {
            failures.addAll(getAncestorCategoryFailures(category.inCategory));
        }
        return failures;
    }

    private static List<StageFailure> getCategoryOwnFailures(@Nullable ShopCategory category) {
        List<StageFailure> failures = new ArrayList<>();
        if (category == null || category.stages == null || category.stages.length == 0) return failures;
        for (ShopStage stage : category.stages) {
            if (stage == null || stage.stageType == null) continue;
            boolean has = hasStage(stage.stageType, stage.stage);
            if (has != stage.is) {
                failures.add(new StageFailure(stage.stageType, stage.stage, stage.is));
            }
        }
        return failures;
    }

    // ── Ancestor helpers ────────────────────────────────────────────────────

    private static boolean isAncestorCategoryBlocked(@Nullable String parentId) {
        if (parentId == null) return false;
        Map<String, ShopCategory> categories = ShopLoader.getCategories();
        String current = ShopHierarchy.normalizeParent(parentId);
        int safety = 0;
        while (current != null && safety++ < 32) {
            ShopCategory cat = categories.get(current);
            if (cat == null) break;
            if (!getCategoryOwnFailures(cat).isEmpty()) return true;
            current = ShopHierarchy.normalizeParent(cat.inCategory);
        }
        return false;
    }

    private static List<StageFailure> getAncestorCategoryFailures(@Nullable String parentId) {
        if (parentId == null) return List.of();
        Map<String, ShopCategory> categories = ShopLoader.getCategories();
        String current = ShopHierarchy.normalizeParent(parentId);
        int safety = 0;
        while (current != null && safety++ < 32) {
            ShopCategory cat = categories.get(current);
            if (cat == null) break;
            List<StageFailure> f = getCategoryOwnFailures(cat);
            if (!f.isEmpty()) return f;
            current = ShopHierarchy.normalizeParent(cat.inCategory);
        }
        return List.of();
    }
}
