package net.unfamily.iskautils.data.load.ancienttablet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class AncientTabletRequirementParser {
    private static final Logger LOGGER = LogUtils.getLogger();

    private AncientTabletRequirementParser() {}

    /**
     * Expands JSON into a flat ordered list of per-slot requirements.
     *
     * @param allowTags false for {@code produce}
     */
    public static List<AncientTabletRequirement> parseArray(
            String contextId,
            JsonElement element,
            boolean allowTags) {
        List<AncientTabletRequirement> out = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return out;
        }
        if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                out.addAll(parseOne(contextId, e, allowTags));
            }
            return out;
        }
        out.addAll(parseOne(contextId, element, allowTags));
        return out;
    }

    private static List<AncientTabletRequirement> parseOne(String contextId, JsonElement e, boolean allowTags) {
        List<AncientTabletRequirement> out = new ArrayList<>();
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            expandString(contextId, e.getAsString(), allowTags, out);
            return out;
        }
        if (!e.isJsonObject()) {
            LOGGER.warn("Ancient Tablet: invalid requirement in {} (not string/object)", contextId);
            return out;
        }
        JsonObject obj = e.getAsJsonObject();
        String id = obj.has("id") ? obj.get("id").getAsString() : (obj.has("item") ? obj.get("item").getAsString() : "");
        int count = obj.has("count") ? Math.max(1, obj.get("count").getAsInt()) : 1;
        int minDamage = -1;
        for (int i = 0; i < count; i++) {
            List<AncientTabletRequirement> one = parseStringId(contextId, id, allowTags, minDamage);
            out.addAll(one);
        }
        return out;
    }

    private static void expandString(String contextId, String raw, boolean allowTags, List<AncientTabletRequirement> out) {
        List<AncientTabletRequirement> one = parseStringId(contextId, raw, allowTags, -1);
        out.addAll(one);
    }

    private static List<AncientTabletRequirement> parseStringId(
            String contextId,
            String raw,
            boolean allowTags,
            int baseMinDamage) {
        List<AncientTabletRequirement> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        String idPart = raw;
        int minDamage = baseMinDamage;
        int bracket = raw.indexOf('[');
        if (bracket > 0 && raw.endsWith("]")) {
            idPart = raw.substring(0, bracket);
            minDamage = parseBracketMinDamage(raw.substring(bracket + 1, raw.length() - 1), baseMinDamage);
        }
        if (idPart.startsWith("#")) {
            if (!allowTags) {
                LOGGER.warn("Ancient Tablet: tags not allowed in produce ({}, {})", contextId, raw);
                return out;
            }
            String tagPath = idPart.substring(1);
            Identifier tagId = Identifier.tryParse(tagPath);
            if (tagId == null) {
                LOGGER.warn("Ancient Tablet: invalid tag {} in {}", raw, contextId);
                return out;
            }
            out.add(new AncientTabletRequirement.TagRequirement(TagKey.create(Registries.ITEM, tagId)));
            return out;
        }
        Identifier itemId = Identifier.tryParse(idPart);
        if (itemId == null) {
            LOGGER.warn("Ancient Tablet: invalid item id {} in {}", raw, contextId);
            return out;
        }
        AncientTabletRequirement req = AncientTabletRequirement.itemId(itemId, minDamage);
        if (req != null) {
            out.add(req);
        } else {
            LOGGER.warn("Ancient Tablet: unknown item {} in {}", itemId, contextId);
        }
        return out;
    }

    private static int parseBracketMinDamage(String inner, int fallback) {
        for (String part : inner.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("damage=")) {
                try {
                    return Integer.parseInt(trimmed.substring("damage=".length()).trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return fallback;
    }
}
