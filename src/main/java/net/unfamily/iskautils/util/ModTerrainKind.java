package net.unfamily.iskautils.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;

/**
 * Terrain classification for mob-farm soils. Mod blocks are resolved before vanilla tags
 * so entropic/druidic entries in {@code #minecraft:grass} / {@code #minecraft:dirt} do not misclassify.
 */
public enum ModTerrainKind {
    ENTROPIC_SOIL,
    ENTROPIC_DIRT,
    DRUIDIC_PODZOL,
    TAGGED_GRASS,
    TAGGED_DIRT,
    OTHER
}

final class ModTerrainKindUtil {
    private static final TagKey<Block> MINECRAFT_GRASS_BLOCKS_TAG =
            TagKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.withDefaultNamespace("grass_blocks"));
    private static final TagKey<Block> MINECRAFT_GRASS_TAG =
            TagKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.withDefaultNamespace("grass"));
    private static final TagKey<Block> MINECRAFT_DIRT_TAG =
            TagKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.withDefaultNamespace("dirt"));

    private ModTerrainKindUtil() {}

    static ModTerrainKind classify(BlockState state) {
        if (state.is(ModBlocks.ENTROPIC_SOIL.get())) {
            return ModTerrainKind.ENTROPIC_SOIL;
        }
        if (state.is(ModBlocks.ENTROPIC_DIRT.get())) {
            return ModTerrainKind.ENTROPIC_DIRT;
        }
        if (state.is(ModBlocks.DRUIDIC_PODZOL.get())) {
            return ModTerrainKind.DRUIDIC_PODZOL;
        }
        if (state.is(ModBlocks.GRAVEYARD_SOIL.get())) {
            return ModTerrainKind.OTHER;
        }
        if (state.is(MINECRAFT_GRASS_BLOCKS_TAG) || state.is(MINECRAFT_GRASS_TAG)) {
            return ModTerrainKind.TAGGED_GRASS;
        }
        if (state.is(MINECRAFT_DIRT_TAG)) {
            return ModTerrainKind.TAGGED_DIRT;
        }
        return ModTerrainKind.OTHER;
    }

    static boolean isAgglomerationTraversable(ModTerrainKind kind) {
        return switch (kind) {
            case ENTROPIC_SOIL, ENTROPIC_DIRT, DRUIDIC_PODZOL, TAGGED_GRASS, TAGGED_DIRT -> true;
            default -> false;
        };
    }

    static boolean isEntropicNaturalSpreadTarget(ModTerrainKind kind) {
        return kind == ModTerrainKind.TAGGED_GRASS
                || kind == ModTerrainKind.TAGGED_DIRT
                || kind == ModTerrainKind.ENTROPIC_DIRT;
    }

    static boolean isDruidicNaturalSpreadTarget(ModTerrainKind kind) {
        return kind == ModTerrainKind.TAGGED_DIRT;
    }
}
