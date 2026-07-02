package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.event.EventHooks;
import net.unfamily.iskautils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EntropicSoilSpawnRules {
    public record SpawnRule(String biomeRef, Identifier entityId) {}

    private static final MobCategory[] SPAWN_CATEGORIES = {
            MobCategory.MONSTER
    };

    private static final TagKey<EntityType<?>> NO_SPAWN_TAG =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("iska_utils", "entropic_soil_no_spawn"));

    private EntropicSoilSpawnRules() {}

    public static List<SpawnRule> parseRules(List<String> raw) {
        List<SpawnRule> rules = new ArrayList<>();
        if (raw == null) {
            return rules;
        }
        for (String line : raw) {
            if (line == null || line.isBlank()) {
                continue;
            }
            int sep = line.indexOf(';');
            if (sep <= 0 || sep >= line.length() - 1) {
                continue;
            }
            String biomeRef = line.substring(0, sep).trim();
            Identifier entityId = Identifier.parse(line.substring(sep + 1).trim());
            rules.add(new SpawnRule(biomeRef, entityId));
        }
        return rules;
    }

    /** Deny wins; biome spawns are allowed unless denied. Allow list adds extra entries in {@link #pickSpawnEntry}. */
    public static boolean isBlockedByDeny(ServerLevel level, BlockPos soilPos, EntityType<?> type) {
        for (SpawnRule rule : parseRules(Config.entropicSoilSpawnDeny)) {
            if (matches(level, soilPos, rule, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(ServerLevel level, BlockPos pos, SpawnRule rule, EntityType<?> type) {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId == null || !rule.entityId().equals(typeId)) {
            return false;
        }
        return matchesBiome(level, pos, rule.biomeRef());
    }

    private static boolean matchesBiome(ServerLevel level, BlockPos pos, String biomeRef) {
        Holder<Biome> biome = level.getBiome(pos);
        if (biomeRef.startsWith("#")) {
            Identifier tagId = Identifier.parse(biomeRef.substring(1));
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, tagId);
            return biome.is(tag);
        }
        Identifier biomeId = Identifier.parse(biomeRef);
        return biome.is(ResourceKey.create(Registries.BIOME, biomeId));
    }

    public static Optional<MobSpawnSettings.SpawnerData> pickSpawnEntry(
            ServerLevel level, BlockPos soilPos, RandomSource random) {
        Holder<Biome> biome = level.getBiome(soilPos);
        WeightedList.Builder<MobSpawnSettings.SpawnerData> pool = WeightedList.builder();

        for (MobCategory category : SPAWN_CATEGORIES) {
            for (Weighted<MobSpawnSettings.SpawnerData> weighted : biome.value().getMobSettings().getMobs(category).unwrap()) {
                MobSpawnSettings.SpawnerData data = weighted.value();
                EntityType<?> entityType = data.type();
                if (entityType == null || !isHostile(entityType) || isBlockedByDeny(level, soilPos, entityType)
                        || isBlockedByNoSpawnTag(entityType)) {
                    continue;
                }
                pool.add(data, weighted.weight());
            }
        }

        for (SpawnRule rule : parseRules(Config.entropicSoilSpawnAllow)) {
            if (!matchesBiome(level, soilPos, rule.biomeRef())) {
                continue;
            }
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(rule.entityId());
            if (entityType == null || !isHostile(entityType) || isBlockedByDeny(level, soilPos, entityType)
                    || isBlockedByNoSpawnTag(entityType)) {
                continue;
            }
            pool.add(new MobSpawnSettings.SpawnerData(entityType, 1, 4), 100);
        }

        WeightedList<MobSpawnSettings.SpawnerData> built = pool.build();
        if (built.isEmpty()) {
            return Optional.empty();
        }
        return built.getRandom(random);
    }

    /**
     * Entropic soil already enforces darkness and spawn clearance; vanilla {@link net.minecraft.world.entity.SpawnPlacements}
     * rejects most biome mobs here (light lottery, {@code ANIMALS_SPAWNABLE_ON}, slime chunks, surface-only rules).
     */
    public static boolean isValidSpawnContext(ServerLevel level, EntityType<?> type, BlockPos spawnPos, BlockPos soilPos) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || isBlockedByDeny(level, soilPos, type) || isBlockedByNoSpawnTag(type)) {
            return false;
        }
        MobCategory category = type.getCategory();
        if (category != MobCategory.MONSTER) {
            return false;
        }
        if (!EntropicSoilUtil.isEntropicSoil(level.getBlockState(soilPos))
                || !EntropicSoilUtil.isDark(level, soilPos)
                || EntropicSoilUtil.hasSolidCoverAbove(level, soilPos)) {
            return false;
        }
        BlockPos expectedSpawn = EntropicSoilUtil.findMobSpawnPos(level, soilPos);
        if (expectedSpawn == null || !expectedSpawn.equals(spawnPos)) {
            return false;
        }
        return true;
    }

    private static boolean isBlockedByNoSpawnTag(EntityType<?> type) {
        return type.builtInRegistryHolder().is(NO_SPAWN_TAG);
    }

    public static boolean canSpawnMobAt(ServerLevel level, Mob mob) {
        if (!EventHooks.checkSpawnPosition(mob, level, EntitySpawnReason.MOB_SUMMONED)) {
            return false;
        }
        return level.getEntitiesOfClass(Mob.class, mob.getBoundingBox(),
                        other -> other.getType() == mob.getType() && other.isAlive()).isEmpty()
                && level.noCollision(mob);
    }

    private static boolean isHostile(EntityType<?> type) {
        return type.getCategory() == MobCategory.MONSTER;
    }
}
