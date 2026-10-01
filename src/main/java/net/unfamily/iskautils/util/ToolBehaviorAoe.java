package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.data.ToolBehaviorDefinition;
import net.unfamily.iskautils.data.ToolBehaviorType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ToolBehaviorAoe {
    private static final int LUMBERJACK_MAX = 256;

    private ToolBehaviorAoe() {}

    public static List<BlockPos> collectTargets(BlockPos origin, ToolBehaviorDefinition def) {
        return switch (def.behavior()) {
            case LUMBERJACK -> lumberjack(origin, def.range());
            case EXCAVATOR -> cube(origin, def.range());
            case PAXEL -> cube(origin, def.range());
            case SCYTHE -> horizontalDisk(origin, def.range());
        };
    }

    public static boolean matchesHarvest(BlockState state, ToolBehaviorDefinition def) {
        List<TagKey<Block>> tags = def.harvestTags();
        if (!tags.isEmpty()) {
            for (TagKey<Block> tag : tags) {
                if (state.is(tag)) {
                    return true;
                }
            }
            return false;
        }
        return defaultHarvestMatch(state, def.behavior());
    }

    private static boolean defaultHarvestMatch(BlockState state, ToolBehaviorType behavior) {
        return switch (behavior) {
            case LUMBERJACK -> state.is(BlockTags.LOGS);
            case EXCAVATOR -> state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case PAXEL -> state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                    || state.is(BlockTags.MINEABLE_WITH_AXE)
                    || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
            case SCYTHE -> state.is(BlockTags.CROPS)
                    || state.is(BlockTags.FLOWERS)
                    || state.is(BlockTags.MINEABLE_WITH_HOE);
        };
    }

    private static List<BlockPos> cube(BlockPos origin, int range) {
        int r = Math.max(0, range);
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    out.add(origin.offset(dx, dy, dz));
                }
            }
        }
        return out;
    }

    private static List<BlockPos> horizontalDisk(BlockPos origin, int range) {
        int r = Math.max(0, range);
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                out.add(origin.offset(dx, 0, dz));
            }
        }
        return out;
    }

    private static List<BlockPos> lumberjack(BlockPos origin, int range) {
        int maxUp = Math.max(1, range) * 16;
        List<BlockPos> out = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && out.size() < LUMBERJACK_MAX) {
            BlockPos current = queue.removeFirst();
            if (current.getY() > origin.getY() + maxUp) {
                continue;
            }
            if (!current.equals(origin)) {
                out.add(current);
            }
            for (BlockPos next : logNeighbors(current)) {
                BlockPos immutable = next.immutable();
                if (immutable.getY() < origin.getY()) {
                    continue;
                }
                if (seen.add(immutable)) {
                    queue.add(immutable);
                }
            }
        }
        return out;
    }

    private static Iterable<BlockPos> logNeighbors(BlockPos pos) {
        return BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1));
    }
}
