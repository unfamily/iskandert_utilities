package net.unfamily.iskautils.obtaining;

import net.unfamily.iskautils.util.ModLogger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;

import java.util.Map;

/**
 * Loads Suspicious Delivery loot from {@code data/<namespace>/load/iska_utils_suspicious_delivery/}
 * (or flat {@code load/*.json} with {@code "type": "iska_utils:suspicious_delivery"}).
 */
public final class SuspiciousDeliveryLoader {
    private static final ModLogger LOGGER = ModLogger.of(SuspiciousDeliveryLoader.class);

    private static volatile SuspiciousDeliveryDefinition SUSPICIOUS_DELIVERY = new SuspiciousDeliveryDefinition(java.util.List.of());

    private SuspiciousDeliveryLoader() {}

    public static void loadAll(ResourceManager rm) {
        Map<Identifier, JsonElement> merged =
                IskaUtilsLoadJson.collectMergedJsonForSubdir(rm, IskaUtilsLoadPaths.SUSPICIOUS_DELIVERY);
        SuspiciousDeliveryDefinition found = null;
        for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
            JsonElement el = e.getValue();
            if (el == null || !el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "";
            if (!IskaUtilsLoadPaths.TYPE_SUSPICIOUS_DELIVERY.equals(type)) continue;
            found = SuspiciousDeliveryDefinition.fromJson(obj, LOGGER.unwrap(), e.getKey().toString());
        }
        if (found != null) {
            SUSPICIOUS_DELIVERY = found;
            LOGGER.info("Loaded Suspicious Delivery definition ({} entries)", found.entries().size());
        } else {
            SUSPICIOUS_DELIVERY = new SuspiciousDeliveryDefinition(java.util.List.of());
            LOGGER.info("No Suspicious Delivery definition found under load/{}, using empty definition", IskaUtilsLoadPaths.SUSPICIOUS_DELIVERY);
        }
    }

    public static SuspiciousDeliveryDefinition get() {
        return SUSPICIOUS_DELIVERY;
    }
}

