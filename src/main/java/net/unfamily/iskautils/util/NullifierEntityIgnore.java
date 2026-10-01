package net.unfamily.iskautils.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.unfamily.iskautils.Config;

import java.util.List;

/**
 * Shared ignore-list matching for Ender / Flight / Climbing Nullifiers.
 * Same entry format as Crystal Cage blacklist: {@code #tag} or entity type id.
 */
public final class NullifierEntityIgnore {
    private NullifierEntityIgnore() {}

    /** True when this entity is exempt from Ender / Flight / Climbing nullifiers. */
    public static boolean isIgnored(Entity entity) {
        if (entity == null) {
            return false;
        }
        EntityType<?> type = entity.getType();
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        List<String> list = Config.nullifierIgnoredEntities;
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (String entry : list) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String trimmed = entry.trim();
            if (trimmed.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(trimmed.substring(1));
                if (tagId == null) {
                    continue;
                }
                TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);
                if (type.builtInRegistryHolder().is(tag)) {
                    return true;
                }
            } else if (typeId != null) {
                Identifier id = Identifier.tryParse(trimmed);
                if (id != null && typeId.equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }
}
