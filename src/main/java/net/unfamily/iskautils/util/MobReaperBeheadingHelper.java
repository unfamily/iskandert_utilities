package net.unfamily.iskautils.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.iskautils.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves Mob Reaper beheading drops from {@link Config#reaperBeheadingDrops}
 * ({@code entityOrTag;headItem}). Falls back to vanilla hardcoded heads when the list is empty
 * or no configured entry matches.
 */
public final class MobReaperBeheadingHelper {

    private MobReaperBeheadingHelper() {}

    public static ItemStack resolveHeadDrop(LivingEntity entity, RandomSource random) {
        List<String> list = Config.reaperBeheadingDrops;
        if (list == null || list.isEmpty()) {
            return vanillaFallback(entity);
        }
        return pickConfiguredHead(entity, random, list);
    }

    private static ItemStack pickConfiguredHead(LivingEntity entity, RandomSource random, List<String> list) {
        EntityType<?> type = entity.getType();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        List<Item> matching = new ArrayList<>();

        for (String entry : list) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String trimmed = entry.trim();
            int sep = trimmed.lastIndexOf(';');
            if (sep <= 0 || sep >= trimmed.length() - 1) {
                continue;
            }
            String entityKey = trimmed.substring(0, sep).trim();
            String itemKey = trimmed.substring(sep + 1).trim();
            if (!matches(type, typeId, entityKey)) {
                continue;
            }
            ResourceLocation itemId = ResourceLocation.tryParse(itemKey);
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                continue;
            }
            matching.add(BuiltInRegistries.ITEM.get(itemId));
        }

        if (matching.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item chosen = matching.get(random.nextInt(matching.size()));
        return new ItemStack(chosen);
    }

    private static boolean matches(EntityType<?> type, ResourceLocation typeId, String key) {
        if (key.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(key.substring(1));
            if (tagId == null) {
                return false;
            }
            return type.is(TagKey.create(Registries.ENTITY_TYPE, tagId));
        }
        if (typeId == null) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(key);
        return id != null && typeId.equals(id);
    }

    private static ItemStack vanillaFallback(LivingEntity entity) {
        if (entity.getType() == EntityType.ZOMBIE
                || entity.getType() == EntityType.ZOMBIE_VILLAGER
                || entity.getType() == EntityType.HUSK
                || entity.getType() == EntityType.DROWNED) {
            return new ItemStack(Items.ZOMBIE_HEAD);
        }
        if (entity.getType() == EntityType.SKELETON) {
            return new ItemStack(Items.SKELETON_SKULL);
        }
        if (entity.getType() == EntityType.WITHER_SKELETON) {
            return new ItemStack(Items.WITHER_SKELETON_SKULL);
        }
        if (entity.getType() == EntityType.CREEPER) {
            return new ItemStack(Items.CREEPER_HEAD);
        }
        if (entity.getType() == EntityType.PIGLIN
                || entity.getType() == EntityType.PIGLIN_BRUTE
                || entity.getType() == EntityType.ZOMBIFIED_PIGLIN) {
            return new ItemStack(Items.PIGLIN_HEAD);
        }
        return ItemStack.EMPTY;
    }
}
