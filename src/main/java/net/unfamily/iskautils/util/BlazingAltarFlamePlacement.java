package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockEntity;
import net.unfamily.iskautils.world.BlazingAltarSpatialIndex;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Server-side flame placement rules for the Blazing Altar. */
public final class BlazingAltarFlamePlacement {
    private BlazingAltarFlamePlacement() {}

    public static boolean canPlaceFlameAt(Level level, BlockPos placePos, boolean groundOnly, BlockState flameState) {
        BlockState placeState = level.getBlockState(placePos);
        if (!placeState.isAir() && !placeState.canBeReplaced()) {
            return false;
        }
        if (!placeState.getFluidState().isEmpty()) {
            return false;
        }

        if (groundOnly && !isGroundPlacement(level, placePos)) {
            return false;
        }

        // Block light only — sky light must not block outdoor placement (getMaxLocalRawBrightness is sky+block).
        if (level.getBrightness(LightLayer.BLOCK, placePos) >= 8) {
            return false;
        }

        int emission = flameLightEmission(flameState);
        return !wouldIlluminateLightSensitive(level, placePos, emission);
    }

    /** Air above solid at same Y as support (no wall/ceiling flames). */
    public static boolean isGroundPlacement(Level level, BlockPos placePos) {
        return hasSolidTopSupport(level, placePos.below());
    }

    private static boolean hasSolidTopSupport(Level level, BlockPos supportPos) {
        BlockState support = level.getBlockState(supportPos);
        if (support.isAir() || !support.getFluidState().isEmpty()) {
            return false;
        }
        if (support.isFaceSturdy(level, supportPos, Direction.UP)) {
            return true;
        }
        VoxelShape shape = support.getCollisionShape(level, supportPos, CollisionContext.empty());
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 0.875;
    }

    private static int flameLightEmission(BlockState flameState) {
        int emission = flameState.getLightEmission();
        return emission > 0 ? emission : 15;
    }

    /**
     * Rejects placement when a light-sensitive block (or the air above it) would have block light
     * strictly above {@link Config#blazingAltarLightSensitiveMaxBlockLight} after the new flame
     * (default 0 = must stay fully dark on the block-light layer).
     */
    public static boolean wouldIlluminateLightSensitive(Level level, BlockPos flamePos, int flameEmission) {
        int radius = Config.blazingAltarExclusionRadius;
        int maxBlockLight = Config.blazingAltarLightSensitiveMaxBlockLight;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos blockPos = flamePos.offset(dx, dy, dz);
                    if (blockPos.equals(flamePos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(blockPos);
                    if (state.isAir() || !isLightSensitiveBlock(state)) {
                        continue;
                    }
                    for (BlockPos sample : lightSamplePositions(blockPos)) {
                        int fromFlame = propagateBlockLight(level, flamePos, flameEmission, sample);
                        if (fromFlame > maxBlockLight) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static BlockPos[] lightSamplePositions(BlockPos blockPos) {
        return new BlockPos[]{blockPos, blockPos.above()};
    }

    public static boolean isLightSensitiveBlock(BlockState state) {
        for (String entry : Config.blazingAltarLightSensitiveEntries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if (matchesConfigEntry(state, entry.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesConfigEntry(BlockState state, String entry) {
        if (entry.startsWith("#")) {
            Identifier id = Identifier.tryParse(entry.substring(1));
            if (id == null) {
                return false;
            }
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, id);
            return state.is(tag);
        }
        Identifier id = Identifier.tryParse(entry);
        if (id == null) {
            return false;
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        return block != null && state.is(block);
    }

    /** Block-light flood fill from a new flame (air/no occlusion at source). */
    private static int propagateBlockLight(BlockGetter level, BlockPos source, int emission, BlockPos target) {
        if (emission <= 0) {
            return 0;
        }
        if (source.equals(target)) {
            return emission;
        }

        int best = 0;
        Map<Long, Integer> bestAt = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        ArrayDeque<Integer> lightQueue = new ArrayDeque<>();
        queue.add(source);
        lightQueue.add(emission);
        bestAt.put(source.asLong(), emission);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int light = lightQueue.poll();
            if (pos.equals(target)) {
                best = Math.max(best, light);
            }
            if (light <= 1) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                BlockState state = level.getBlockState(next);
                int opacity = state.getLightDampening();
                if (opacity >= 15 && state.isSolid()) {
                    continue;
                }
                int nextLight = light - Math.max(1, opacity);
                if (nextLight <= 0) {
                    continue;
                }
                long key = next.asLong();
                Integer previous = bestAt.get(key);
                if (previous != null && previous >= nextLight) {
                    continue;
                }
                bestAt.put(key, nextLight);
                queue.add(next);
                lightQueue.add(nextLight);
            }
        }
        return best;
    }

    /**
     * True if an opaque or full-light-blocking block exists strictly between two positions.
     */
    public static boolean hasOpaqueBetween(BlockGetter level, BlockPos from, BlockPos to) {
        if (from.equals(to)) {
            return false;
        }
        int steps = Math.max(
                Math.abs(to.getX() - from.getX()),
                Math.max(Math.abs(to.getY() - from.getY()), Math.abs(to.getZ() - from.getZ())));
        if (steps <= 0) {
            return false;
        }
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            BlockPos sample = BlockPos.containing(
                    from.getX() + (to.getX() - from.getX()) * t,
                    from.getY() + (to.getY() - from.getY()) * t,
                    from.getZ() + (to.getZ() - from.getZ()) * t);
            BlockState state = level.getBlockState(sample);
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                continue;
            }
            if (state.isSolidRender() || state.getLightDampening() >= 15) {
                return true;
            }
            if (!state.getCollisionShape(level, sample, CollisionContext.empty()).isEmpty()
                    && state.isSolid()) {
                return true;
            }
        }
        return false;
    }

    public static Block flameBlockForPlacer(net.minecraft.world.item.Item item) {
        if (item == net.unfamily.iskautils.item.ModItems.BURNING_BRAZIER.get()) {
            return ModBlocks.BURNING_FLAME.get();
        }
        return ModBlocks.CURSED_BURNING_FLAME.get();
    }

    /** Brazier flames in range of a non-operational altar must not emit block light. */
    public static boolean isBrazierFlameLightSuppressed(BlockGetter level, BlockPos flamePos) {
        if (!(level instanceof Level world)) {
            return false;
        }
        BlockState flameState = world.getBlockState(flamePos);
        if (!flameState.is(ModBlocks.BURNING_FLAME.get())) {
            return false;
        }
        int flameChunkX = flamePos.getX() >> 4;
        int flameChunkZ = flamePos.getZ() >> 4;
        int searchRadius = maxAltarChunkReach();
        for (int dcx = -searchRadius; dcx <= searchRadius; dcx++) {
            for (int dcz = -searchRadius; dcz <= searchRadius; dcz++) {
                Set<BlockPos> altars = BlazingAltarSpatialIndex.getAltarsInChunk(
                        world.dimension(), new ChunkPos(flameChunkX + dcx, flameChunkZ + dcz));
                for (BlockPos altarPos : altars) {
                    BlockEntity be = world.getBlockEntity(altarPos);
                    if (!(be instanceof BlazingAltarBlockEntity altar) || altar.isOperational()) {
                        continue;
                    }
                    if (BlazingAltarSpatialIndex.isWithinChunkRadius(altarPos, flamePos, altar.getChunkRadius())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Recomputes block light for brazier flames after altar operational state changes. */
    public static void refreshBrazierFlameLightInRadius(ServerLevel level, BlockPos altarPos, int chunkRadius, boolean groundOnly) {
        Block burningFlame = ModBlocks.BURNING_FLAME.get();
        for (ChunkPos chunk : BlazingAltarChunks.collectOrdered(altarPos, chunkRadius)) {
            BlockPos probe = new BlockPos(chunk.getMinBlockX(), altarPos.getY(), chunk.getMinBlockZ());
            if (!level.isLoaded(probe)) {
                continue;
            }
            int baseX = chunk.getMinBlockX();
            int baseZ = chunk.getMinBlockZ();
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int x = baseX + localX;
                    int z = baseZ + localZ;
                    int minY = level.getMinY();
                    int maxY = groundOnly
                            ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 2
                            : level.getMaxY();
                    for (int y = minY; y <= maxY; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (!state.is(burningFlame)) {
                            continue;
                        }
                        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static int maxAltarChunkReach() {
        return Config.blazingAltarMaxChunkRadius
                + Config.blazingAltarRangeUpgradeMax * Config.blazingAltarRangeModuleChunkBonus;
    }
}
