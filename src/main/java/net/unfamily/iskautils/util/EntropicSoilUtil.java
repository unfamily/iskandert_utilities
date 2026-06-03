package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.entity.EntropicSoilBlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public final class EntropicSoilUtil {
    public static final TagKey<Block> CONVERTIBLE_TAG =
            TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "entropic_soil_convertible"));

    private static final TagKey<Block> MINECRAFT_DIRT_TAG =
            TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    Identifier.withDefaultNamespace("dirt"));

    private static final TagKey<Block> MINECRAFT_GRASS_TAG =
            TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    Identifier.withDefaultNamespace("grass"));

    private static final int SPREAD_RADIUS = 7;
    private static final int SPREAD_RADIUS_SQ = SPREAD_RADIUS * SPREAD_RADIUS;
    /** Hard cap for same-Y patch size inside the spread circle (15×15 = 225). */
    private static final int MAX_PATCH_BLOCKS = (SPREAD_RADIUS * 2 + 1) * (SPREAD_RADIUS * 2 + 1);

    /** Hard cap for connected entropic soil BFS — avoids TPS collapse on huge patches. */
    private static final int MAX_CONNECTED_SOIL_BLOCKS = 4096;

    /** 8-connected horizontal offsets (same Y). */
    private static final int[][] HORIZONTAL_NEIGHBOR_OFFSETS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    /** Max collision span (block space) still treated as passable cover — thick vector/potion plates, not cubes. */
    private static final double MAX_PASSABLE_COVER_HEIGHT = 13.0D / 16.0D;
    private static final int REDSTONE_VERTICAL_SCAN_DEPTH = 8;


    public static int rollInclusive(int min, int max, RandomSource random) {
        return RandomRollUtil.rollInclusive(min, max, random);
    }

    /** Datapack hint; runtime uses {@link ModTerrainKind}. */
    public static boolean isConvertible(BlockState state) {
        return state.is(CONVERTIBLE_TAG)
                || state.is(MINECRAFT_DIRT_TAG)
                || state.is(MINECRAFT_GRASS_TAG);
    }

    public static boolean isAgglomerationTraversable(Level level, BlockPos pos, BlockState state) {
        return ModTerrainKindUtil.isAgglomerationTraversable(ModTerrainKindUtil.classify(state))
                && !hasSolidCoverAbove(level, pos);
    }

    public static boolean isNaturalSpreadTarget(Level level, BlockPos pos, BlockState state) {
        return ModTerrainKindUtil.isEntropicNaturalSpreadTarget(ModTerrainKindUtil.classify(state))
                && !hasSolidCoverAbove(level, pos);
    }

    public static boolean isEntropicSoil(BlockState state) {
        return state.is(ModBlocks.ENTROPIC_SOIL.get());
    }

    public static boolean isEntropicDirt(BlockState state) {
        return state.is(ModBlocks.ENTROPIC_DIRT.get());
    }

    /**
     * Uses combined light on the surface above the soil (same idea as grass).
     * Moonlight tops out around 11; sunlight reaches 15 — threshold 12 skips night decay.
     */
    public static boolean isIlluminated(Level level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos.above()) >= 12;
    }

    /** Grass cannot spread onto blocks with a solid cover above. */
    public static boolean hasSolidCoverAbove(Level level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return !above.isAir() && above.isSolidRender();
    }

    /** Connected flood fill for agglomeration (ignores corrupt/blessed spread rules). */
    public static List<BlockPos> collectConnectedConvertible(ServerLevel level, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        if (!isAgglomerationTraversable(level, origin, originState)) {
            return List.of();
        }

        int y = origin.getY();
        int originX = origin.getX();
        int originZ = origin.getZ();
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos start = origin.immutable();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && visited.size() <= MAX_PATCH_BLOCKS) {
            BlockPos current = queue.poll();
            int dx = current.getX() - originX;
            int dz = current.getZ() - originZ;
            if (dx * dx + dz * dz > SPREAD_RADIUS_SQ) {
                continue;
            }
            BlockState state = level.getBlockState(current);
            if (!isAgglomerationTraversable(level, current, state)) {
                continue;
            }
            out.add(current.immutable());
            for (int[] offset : HORIZONTAL_NEIGHBOR_OFFSETS) {
                BlockPos neighbor = current.offset(offset[0], 0, offset[1]).immutable();
                if (neighbor.getY() != y) {
                    continue;
                }
                int ndx = neighbor.getX() - originX;
                int ndz = neighbor.getZ() - originZ;
                if (ndx * ndx + ndz * ndz > SPREAD_RADIUS_SQ) {
                    continue;
                }
                if (visited.contains(neighbor)) {
                    continue;
                }
                BlockState neighborState = level.getBlockState(neighbor);
                if (!isAgglomerationTraversable(level, neighbor, neighborState)) {
                    continue;
                }
                visited.add(neighbor);
                queue.add(neighbor);
            }
        }
        return out;
    }

    /** BFS-ordered blocks that still need conversion (skips existing entropic soil). */
    public static List<BlockPos> collectAgglomerationSpreadTargets(ServerLevel level, BlockPos origin) {
        List<BlockPos> connected = collectConnectedConvertible(level, origin);
        if (connected.isEmpty()) {
            return List.of();
        }
        List<BlockPos> targets = new ArrayList<>(connected.size());
        for (BlockPos pos : connected) {
            if (!isEntropicSoil(level.getBlockState(pos))) {
                targets.add(pos.immutable());
            }
        }
        return targets;
    }

    /** Targets grouped by circular distance ring from origin (ring 0 = click point). */
    public static List<List<BlockPos>> collectAgglomerationSpreadRings(ServerLevel level, BlockPos origin) {
        List<BlockPos> targets = collectAgglomerationSpreadTargets(level, origin);
        if (targets.isEmpty()) {
            return List.of();
        }
        int originX = origin.getX();
        int originZ = origin.getZ();
        TreeMap<Integer, List<BlockPos>> byRing = new TreeMap<>();
        for (BlockPos pos : targets) {
            int dx = pos.getX() - originX;
            int dz = pos.getZ() - originZ;
            int ring = circularRingIndex(dx, dz);
            byRing.computeIfAbsent(ring, ignored -> new ArrayList<>()).add(pos);
        }
        return new ArrayList<>(byRing.values());
    }

    /** Rounded Euclidean distance from origin — expands as concentric circles. */
    private static int circularRingIndex(int dx, int dz) {
        return (int) Math.floor(Math.sqrt(dx * dx + (double) dz * dz) + 0.5D);
    }

    public static boolean trySlowSpread(
            ServerLevel level,
            BlockPos origin,
            RandomSource random,
            int normalChanceDenominator,
            int edgeChanceDenominator,
            int dirtChanceDenominator) {
        BlockPos target = pickSlowSpreadTarget(origin, random);
        if (target.equals(origin)) {
            return false;
        }
        BlockState targetState = level.getBlockState(target);
        if (!isNaturalSpreadTarget(level, target, targetState)) {
            return false;
        }
        int denominator = spreadChanceDenominator(
                ModTerrainKindUtil.classify(targetState),
                hasAdjacentEntropicSoil(level, target),
                normalChanceDenominator,
                edgeChanceDenominator,
                dirtChanceDenominator);
        if (denominator <= 0 || random.nextInt(denominator) != 0) {
            return false;
        }
        return convertToEntropicSoil(level, target);
    }

    private static int spreadChanceDenominator(
            ModTerrainKind targetKind,
            boolean adjacentToEntropicSoil,
            int normalChanceDenominator,
            int edgeChanceDenominator,
            int dirtChanceDenominator) {
        if (targetKind == ModTerrainKind.ENTROPIC_DIRT) {
            return dirtChanceDenominator;
        }
        return adjacentToEntropicSoil ? edgeChanceDenominator : normalChanceDenominator;
    }

    public static boolean hasAdjacentEntropicSoil(ServerLevel level, BlockPos pos) {
        for (int[] offset : HORIZONTAL_NEIGHBOR_OFFSETS) {
            BlockPos neighbor = pos.offset(offset[0], 0, offset[1]);
            if (isEntropicSoil(level.getBlockState(neighbor))) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos pickSlowSpreadTarget(BlockPos origin, RandomSource random) {
        for (int attempt = 0; attempt < 4; attempt++) {
            int dx = random.nextInt(3) - 1;
            int dz = random.nextInt(3) - 1;
            if (dx == 0 && dz == 0) {
                continue;
            }
            return origin.offset(dx, 0, dz);
        }
        return origin.offset(random.nextBoolean() ? 1 : -1, 0, 0);
    }

    /** Agglomeration: converts any traversable block except existing entropic soil. */
    public static boolean convertToEntropicSoilForAgglomeration(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isEntropicSoil(state) || !isAgglomerationTraversable(level, pos, state)) {
            return false;
        }
        level.setBlock(pos, ModBlocks.ENTROPIC_SOIL.get().defaultBlockState(), Block.UPDATE_ALL);
        EntropicSoilBlockEntity.onSoilPlaced(level, pos);
        return true;
    }

    public static boolean convertToEntropicSoil(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isNaturalSpreadTarget(level, pos, state)) {
            return false;
        }
        level.setBlock(pos, ModBlocks.ENTROPIC_SOIL.get().defaultBlockState(), Block.UPDATE_ALL);
        EntropicSoilBlockEntity.onSoilPlaced(level, pos);
        return true;
    }

    public static List<BlockPos> filterActiveSoil(ServerLevel level, List<BlockPos> component) {
        List<BlockPos> active = new ArrayList<>(component.size());
        for (BlockPos pos : component) {
            if (isEntropicSoil(level.getBlockState(pos))) {
                active.add(pos);
            }
        }
        return active;
    }

    public static List<BlockPos> filterDarkSpawnCandidates(ServerLevel level, List<BlockPos> component) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : component) {
            if (!isDark(level, pos)) {
                continue;
            }
            if (hasSolidCoverAbove(level, pos)) {
                continue;
            }
            if (findMobSpawnPos(level, pos) == null) {
                continue;
            }
            candidates.add(pos);
        }
        return candidates;
    }

    /** Feet position for spawning on soil; allows non-solid covers such as vector plates. */
    public static BlockPos findMobSpawnPos(ServerLevel level, BlockPos soilPos) {
        BlockPos feetPos = soilPos.above();
        for (int offset = 0; offset < 6; offset++) {
            BlockPos candidate = feetPos.above(offset);
            BlockState feetState = level.getBlockState(candidate);
            if (!feetState.isAir() && feetState.isSolidRender()) {
                return null;
            }
            if (hasMobSpawnClearance(level, candidate)) {
                return candidate;
            }
            if (!feetState.isAir() && blocksMobSpawnVolume(level, candidate, feetState)) {
                break;
            }
        }
        return hasMobSpawnClearance(level, feetPos) ? feetPos : null;
    }

    private static boolean hasMobSpawnClearance(Level level, BlockPos feetPos) {
        for (int dy = 0; dy < 2; dy++) {
            BlockPos check = feetPos.above(dy);
            BlockState state = level.getBlockState(check);
            if (blocksMobSpawnVolume(level, check, state)) {
                return false;
            }
        }
        return true;
    }

    /** Full cubes block spawning; thin/thick non-solid covers (vector plates, etc.) do not. */
    private static boolean blocksMobSpawnVolume(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.isSolidRender()) {
            return true;
        }
        VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.empty());
        if (shape.isEmpty()) {
            return false;
        }
        AABB bounds = shape.bounds();
        double height = bounds.maxY - bounds.minY;
        return height > MAX_PASSABLE_COVER_HEIGHT;
    }

    public static boolean isDarkSpawnCandidate(ServerLevel level, BlockPos soilPos) {
        return isEntropicSoil(level.getBlockState(soilPos))
                && isDark(level, soilPos)
                && !hasSolidCoverAbove(level, soilPos)
                && findMobSpawnPos(level, soilPos) != null;
    }

    public static void refreshPatchRedstoneState(ServerLevel level, BlockPos origin) {
        if (!isEntropicSoil(level.getBlockState(origin))) {
            return;
        }
        List<BlockPos> component = collectConnectedSoil(level, origin);
        if (component.isEmpty()) {
            return;
        }
        boolean active = Config.entropicSoilRedstoneAccelEnabled && hasRedstoneSignal(level, component);
        for (BlockPos pos : component) {
            if (level.getBlockEntity(pos) instanceof EntropicSoilBlockEntity be) {
                be.applyPatchRedstoneState(active);
                be.setChanged();
            }
        }
    }

    public static void queueImmediateAccelSpawn(ServerLevel level, BlockPos origin) {
        refreshPatchRedstoneState(level, origin);
        List<BlockPos> component = collectConnectedSoil(level, origin);
        for (BlockPos pos : component) {
            if (level.getBlockEntity(pos) instanceof EntropicSoilBlockEntity be) {
                be.queueImmediateAccelSpawn();
            }
        }
    }

    /** 4-connected entropic soil cluster on same Y layers (allows ±0 Y only for same block column). */
    public static List<BlockPos> collectConnectedSoil(ServerLevel level, BlockPos origin) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos start = origin.immutable();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && visited.size() <= MAX_CONNECTED_SOIL_BLOCKS) {
            BlockPos current = queue.poll();
            BlockState state = level.getBlockState(current);
            if (!isEntropicSoil(state)) {
                continue;
            }
            out.add(current);
            for (BlockPos neighbor : horizontalNeighbors(current)) {
                BlockPos immutable = neighbor.immutable();
                if (visited.add(immutable)) {
                    queue.add(neighbor);
                }
            }
        }
        return out;
    }

    public static List<BlockPos> horizontalNeighbors(BlockPos pos) {
        return List.of(
                pos.north(),
                pos.south(),
                pos.east(),
                pos.west());
    }

    public static boolean hasRedstoneSignal(ServerLevel level, List<BlockPos> component) {
        for (BlockPos pos : component) {
            if (hasRedstoneSignalAt(level, pos)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasRedstoneSignalAt(ServerLevel level, BlockPos pos) {
        if (level.getBestNeighborSignal(pos) > 0 || level.hasNeighborSignal(pos)) {
            return true;
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!belowState.isAir() && isPoweredRedstoneAt(level, below, belowState, Direction.UP)) {
            return true;
        }

        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (!aboveState.isAir() && isPoweredRedstoneAt(level, above, aboveState, Direction.DOWN)) {
            return true;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighbor);
            if (!neighborState.isAir() && isPoweredRedstoneAt(level, neighbor, neighborState, direction.getOpposite())) {
                return true;
            }
        }

        return scanVerticalRedstone(level, pos, Direction.UP)
                || scanVerticalRedstone(level, pos, Direction.DOWN);
    }

    private static boolean scanVerticalRedstone(ServerLevel level, BlockPos origin, Direction direction) {
        BlockPos cursor = origin.relative(direction);
        Direction towardOrigin = direction.getOpposite();
        for (int step = 0; step < REDSTONE_VERTICAL_SCAN_DEPTH; step++) {
            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) {
                cursor = cursor.relative(direction);
                continue;
            }
            if (isPoweredRedstoneAt(level, cursor, state, towardOrigin)) {
                return true;
            }
            if (!isPassableRedstoneCover(level, cursor, state)) {
                break;
            }
            cursor = cursor.relative(direction);
        }
        return false;
    }

    private static boolean isPoweredRedstoneAt(Level level, BlockPos pos, BlockState state, Direction towardTarget) {
        if (state.is(Blocks.REDSTONE_WIRE)) {
            return state.getValue(RedStoneWireBlock.POWER) > 0;
        }
        if (state.is(Blocks.REDSTONE_BLOCK)) {
            return true;
        }
        if (level.getControlInputSignal(pos, towardTarget, false) > 0) {
            return true;
        }
        if (level.getSignal(pos, towardTarget) > 0) {
            return true;
        }
        return state.isSignalSource() && state.getSignal(level, pos, towardTarget) > 0;
    }

    private static boolean isPassableRedstoneCover(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        return !blocksMobSpawnVolume(level, pos, state);
    }

    public static boolean isDark(ServerLevel level, BlockPos pos) {
        return !isIlluminated(level, pos);
    }
}
