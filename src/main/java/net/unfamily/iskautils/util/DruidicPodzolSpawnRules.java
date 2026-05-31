package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.unfamily.iskautils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DruidicPodzolSpawnRules {
    public record SpawnRule(String biomeRef, ResourceLocation entityId) {}

    private static final MobCategory[] SPAWN_CATEGORIES = {
            MobCategory.CREATURE
    };

    private DruidicPodzolSpawnRules() {}

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
            ResourceLocation entityId = ResourceLocation.parse(line.substring(sep + 1).trim());
            rules.add(new SpawnRule(biomeRef, entityId));
        }
        return rules;
    }

    public static boolean isBlockedByDeny(ServerLevel level, BlockPos soilPos, EntityType<?> type) {
        for (SpawnRule rule : parseRules(Config.druidicPodzolSpawnDeny)) {
            if (matches(level, soilPos, rule, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(ServerLevel level, BlockPos pos, SpawnRule rule, EntityType<?> type) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (typeId == null || !rule.entityId().equals(typeId)) {
            return false;
        }
        return matchesBiome(level, pos, rule.biomeRef());
    }

    private static boolean matchesBiome(ServerLevel level, BlockPos pos, String biomeRef) {
        Holder<Biome> biome = level.getBiome(pos);
        if (biomeRef.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.parse(biomeRef.substring(1));
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, tagId);
            return biome.is(tag);
        }
        ResourceLocation biomeId = ResourceLocation.parse(biomeRef);
        return biome.is(ResourceKey.create(Registries.BIOME, biomeId));
    }

    public static Optional<MobSpawnSettings.SpawnerData> pickSpawnEntry(
            ServerLevel level, BlockPos soilPos, RandomSource random) {
        Holder<Biome> biome = level.getBiome(soilPos);
        List<MobSpawnSettings.SpawnerData> pool = new ArrayList<>();

        for (MobCategory category : SPAWN_CATEGORIES) {
            for (MobSpawnSettings.SpawnerData data : biome.value().getMobSettings().getMobs(category).unwrap()) {
                EntityType<?> entityType = data.type;
                if (entityType == null || !isAnimal(entityType) || isBlockedByDeny(level, soilPos, entityType)) {
                    continue;
                }
                pool.add(data);
            }
        }

        for (SpawnRule rule : parseRules(Config.druidicPodzolSpawnAllow)) {
            if (!matchesBiome(level, soilPos, rule.biomeRef())) {
                continue;
            }
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(rule.entityId());
            if (entityType == null || !isAnimal(entityType) || isBlockedByDeny(level, soilPos, entityType)) {
                continue;
            }
            pool.add(new MobSpawnSettings.SpawnerData(entityType, 100, 1, 4));
        }

        if (pool.isEmpty()) {
            return Optional.empty();
        }
        int totalWeight = 0;
        for (MobSpawnSettings.SpawnerData data : pool) {
            totalWeight += data.getWeight().asInt();
        }
        if (totalWeight <= 0) {
            return Optional.of(pool.get(random.nextInt(pool.size())));
        }
        int roll = random.nextInt(totalWeight);
        for (MobSpawnSettings.SpawnerData data : pool) {
            roll -= data.getWeight().asInt();
            if (roll < 0) {
                return Optional.of(data);
            }
        }
        return Optional.of(pool.getLast());
    }

    public static boolean isValidSpawnContext(ServerLevel level, EntityType<?> type, BlockPos spawnPos, BlockPos soilPos) {
        if (isBlockedByDeny(level, soilPos, type)) {
            return false;
        }
        if (!isAnimal(type)) {
            return false;
        }
        BlockState soilState = level.getBlockState(soilPos);
        if (!DruidicPodzolUtil.isDruidicPodzol(soilState)
                || !soilState.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || !DruidicPodzolUtil.isIlluminated(level, soilPos)
                || DruidicPodzolUtil.hasSolidCoverAbove(level, soilPos)) {
            return false;
        }
        BlockPos expectedSpawn = DruidicPodzolUtil.findMobSpawnPos(level, soilPos);
        if (expectedSpawn == null || !expectedSpawn.equals(spawnPos)) {
            return false;
        }
        return true;
    }

    private static boolean isAnimal(EntityType<?> type) {
        return type.getCategory() == MobCategory.CREATURE;
    }
}
