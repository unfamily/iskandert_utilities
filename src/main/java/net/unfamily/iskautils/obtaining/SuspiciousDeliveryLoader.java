package net.unfamily.iskautils.obtaining;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Loads Suspicious Delivery loot from {@code data/<namespace>/load/iska_utils_suspicious_delivery/}
 * (or flat {@code load/*.json} with {@code "type": "iska_utils:suspicious_delivery"}).
 */
public final class SuspiciousDeliveryLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile SuspiciousDeliveryDefinition SUSPICIOUS_DELIVERY = new SuspiciousDeliveryDefinition(java.util.List.of());

    private SuspiciousDeliveryLoader() {}

    public static void loadAll(ResourceManager rm) {
        Map<ResourceLocation, JsonElement> merged =
                IskaUtilsLoadJson.collectMergedJsonForSubdir(rm, IskaUtilsLoadPaths.SUSPICIOUS_DELIVERY);
        SuspiciousDeliveryDefinition found = null;
        for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
            JsonElement el = e.getValue();
            if (el == null || !el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "";
            if (!IskaUtilsLoadPaths.TYPE_SUSPICIOUS_DELIVERY.equals(type)) continue;
            found = SuspiciousDeliveryDefinition.fromJson(obj, LOGGER, e.getKey().toString());
            // last pack wins due to merged stacks; orderedEntries keeps namespace order but key uniqueness already resolved.
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

