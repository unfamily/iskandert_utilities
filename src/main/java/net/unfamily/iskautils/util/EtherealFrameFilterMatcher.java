package net.unfamily.iskautils.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Matches Ethereal Frame filter entries: entity type ids, {@code #entity_type_tags},
 * and {@code $special} keys (including explicit {@code $is_not_*} variants).
 */
public final class EtherealFrameFilterMatcher {

    public static final List<String> SPECIAL_KEYS = List.of(
            "$have_armor",
            "$is_not_have_armor",
            "$have_tool",
            "$is_not_have_tool",
            "$is_baby",
            "$is_adult",
            "$is_monster",
            "$is_animal",
            "$is_neutral",
            "$on_fire",
            "$is_not_on_fire",
            "$is_crouching",
            "$is_not_crouching"
    );

    public static final List<String> COMMON_ENTITY_TAGS = List.of(
            "#minecraft:raiders",
            "#minecraft:skeletons",
            "#minecraft:undead",
            "#minecraft:arrows",
            "#minecraft:impact_projectiles",
            "#minecraft:beehive_inhabitors",
            "#minecraft:frog_food",
            "#minecraft:powder_snow_walkable_mobs"
    );

    private EtherealFrameFilterMatcher() {}

    public static boolean shouldEntityPass(Entity entity, List<String> filterEntries, boolean allowMode) {
        boolean inList = filterEntries.stream().anyMatch(entry -> matchesEntry(entity, entry));
        return allowMode ? inList : !inList;
    }

    /**
     * Maps legacy {@code !$…} keys to the current {@code $is_not_*} / complementary names.
     * Returns {@code null} when the entry should be dropped (redundant legacy duplicates).
     */
    public static String normalizeEntry(String entry) {
        if (entry == null || entry.isEmpty()) {
            return entry;
        }
        return switch (entry) {
            case "!$have_armor" -> "$is_not_have_armor";
            case "!$have_tool" -> "$is_not_have_tool";
            case "!$is_baby" -> "$is_adult";
            case "!$is_adult" -> "$is_baby";
            case "!$is_monster" -> null;
            case "!$on_fire" -> "$is_not_on_fire";
            case "!$is_crouching" -> "$is_not_crouching";
            default -> entry;
        };
    }

    public static List<String> normalizeEntries(List<String> entries) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (entries == null) {
            return new ArrayList<>();
        }
        for (String entry : entries) {
            String normalized = normalizeEntry(entry);
            if (normalized != null && !normalized.isEmpty()) {
                out.add(normalized);
            }
        }
        return new ArrayList<>(out);
    }

    public static boolean matchesEntry(Entity entity, String entry) {
        if (entry == null || entry.isEmpty()) {
            return false;
        }
        String normalized = normalizeEntry(entry);
        if (normalized == null) {
            // Legacy !$is_monster: keep old behavior for unsaved/in-memory lists.
            if ("!$is_monster".equals(entry)) {
                return entity.getType().getCategory() != MobCategory.MONSTER;
            }
            return false;
        }
        if (normalized.startsWith("$")) {
            return matchesSpecial(entity, normalized);
        }
        if (normalized.startsWith("#")) {
            return matchesEntityTag(entity, normalized.substring(1));
        }
        Identifier typeId = Identifier.tryParse(normalized);
        if (typeId == null) {
            return false;
        }
        Identifier entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return typeId.equals(entityKey);
    }

    private static boolean matchesEntityTag(Entity entity, String tagIdString) {
        Identifier tagId = Identifier.tryParse(tagIdString);
        if (tagId == null) {
            return false;
        }
        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);
        return entity.getType().builtInRegistryHolder().is(tag);
    }

    private static boolean matchesSpecial(Entity entity, String key) {
        return switch (key) {
            case "$have_armor" -> hasArmor(entity);
            case "$is_not_have_armor" -> !hasArmor(entity);
            case "$have_tool" -> hasTool(entity);
            case "$is_not_have_tool" -> !hasTool(entity);
            case "$is_baby" -> entity instanceof LivingEntity living && living.isBaby();
            case "$is_adult" -> entity instanceof LivingEntity living && !living.isBaby();
            case "$is_monster" -> entity.getType().getCategory() == MobCategory.MONSTER;
            case "$is_animal" -> entity instanceof Animal;
            case "$is_neutral" -> entity instanceof NeutralMob;
            case "$on_fire" -> entity.isOnFire();
            case "$is_not_on_fire" -> !entity.isOnFire();
            case "$is_crouching" -> entity.isCrouching();
            case "$is_not_crouching" -> !entity.isCrouching();
            default -> false;
        };
    }

    private static boolean hasArmor(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor() && !living.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTool(Entity entity) {
        if (entity instanceof Player player) {
            return !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
        }
        if (entity instanceof LivingEntity living) {
            return !living.getMainHandItem().isEmpty() || !living.getOffhandItem().isEmpty();
        }
        return false;
    }
}
