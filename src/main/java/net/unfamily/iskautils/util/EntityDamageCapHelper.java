package net.unfamily.iskautils.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.unfamily.iskautils.Config;

import java.util.List;

/**
 * Caps damage dealt by damage trap plates and Mob Reaper for configured
 * entity types / tags. Config entries: {@code entityOrTag;maxDamage}
 * (e.g. {@code #c:bosses;10}). Multiple matching rules use the lowest max.
 * Damage plates may opt out via JSON {@code ignore_entity_damage_cap}.
 */
public final class EntityDamageCapHelper {

    private EntityDamageCapHelper() {}

    public static float cap(LivingEntity target, float requested) {
        if (target == null || requested <= 0.0F) {
            return requested;
        }
        List<String> list = Config.entityDamageCaps;
        if (list == null || list.isEmpty()) {
            return requested;
        }

        EntityType<?> type = target.getType();
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        float limited = requested;
        boolean matched = false;

        for (String entry : list) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String trimmed = entry.trim();
            int sep = trimmed.lastIndexOf(';');
            if (sep <= 0 || sep >= trimmed.length() - 1) {
                continue;
            }
            String key = trimmed.substring(0, sep).trim();
            float max;
            try {
                max = Float.parseFloat(trimmed.substring(sep + 1).trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (max < 0.0F || !matches(type, typeId, key)) {
                continue;
            }
            if (!matched) {
                limited = max;
                matched = true;
            } else {
                limited = Math.min(limited, max);
            }
        }

        return matched ? Math.min(requested, limited) : requested;
    }

    private static boolean matches(EntityType<?> type, Identifier typeId, String key) {
        if (key.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(key.substring(1));
            if (tagId == null) {
                return false;
            }
            TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);
            return type.builtInRegistryHolder().is(tag);
        }
        if (typeId == null) {
            return false;
        }
        Identifier id = Identifier.tryParse(key);
        return id != null && typeId.equals(id);
    }
}
