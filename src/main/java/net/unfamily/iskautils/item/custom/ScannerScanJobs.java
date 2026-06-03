package net.unfamily.iskautils.item.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ScannerMobCategories;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side incremental scanner jobs: spreads block/chunk work across ticks and batches highlight packets.
 */
public final class ScannerScanJobs {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<UUID, ScanJob> JOBS_BY_PLAYER = new HashMap<>();

    private ScannerScanJobs() {
    }

    public enum ScanKind {
        BLOCK,
        MOB,
        ALL_ORES,
        ALL_MOBS
    }

    public static boolean enqueue(ServerPlayer player, ItemStack stack, ScanKind kind) {
        if (player == null || stack.isEmpty()) {
            return false;
        }
        UUID scannerId = ScannerItem.getScannerIdFromStack(stack);
        if (scannerId == null) {
            return false;
        }
        int maxMarkers = Config.scannerMaxBlocks;
        if (maxMarkers != -1) {
            int existing = ScannerItem.getActiveMarkerPositions(scannerId).size();
            if (existing >= maxMarkers) {
                ScannerItem.notifyScanLimitReached(player, maxMarkers);
                return false;
            }
        }
        JOBS_BY_PLAYER.put(player.getUUID(), ScanJob.create(player, stack, kind, scannerId));
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (JOBS_BY_PLAYER.isEmpty()) {
            return;
        }
        int blockBudget = Config.scannerBlocksPerTick;
        int markerBudget = Config.scannerMarkersPerTick;
        Iterator<Map.Entry<UUID, ScanJob>> it = JOBS_BY_PLAYER.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ScanJob> entry = it.next();
            ScanJob job = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                it.remove();
                continue;
            }
            ServerLevel level = server.getLevel(job.dimension);
            if (level == null) {
                it.remove();
                continue;
            }
            ItemStack held = findHeldScanner(player, job.scannerId);
            if (held.isEmpty()) {
                it.remove();
                continue;
            }

            job.flushPendingMarkers(player, markerBudget);

            boolean done = switch (job.kind) {
                case BLOCK -> job.advanceBlockTarget(level, player, held, blockBudget, markerBudget);
                case ALL_ORES -> job.advanceOreScan(level, player, held, blockBudget, markerBudget);
                case MOB, ALL_MOBS -> job.advanceMobScan(level, player, held, blockBudget, markerBudget);
            };

            job.flushPendingMarkers(player, markerBudget);

            if (done) {
                job.flushAllPendingMarkers(player);
                job.finish(player, held);
                if (ScannerItem.requiresEnergyForScan(player)) {
                    new ScannerItem().consumeEnergyForOperation(held);
                }
                it.remove();
            }
        }
    }

    private static ItemStack findHeldScanner(ServerPlayer player, UUID scannerId) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && scannerId.equals(ScannerItem.getScannerIdFromStack(stack))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static final class PendingMarker {
        final BlockPos pos;
        final int colorWithAlpha;
        final int ttl;
        final String displayName;
        final boolean billboard;

        PendingMarker(BlockPos pos, int colorWithAlpha, int ttl, String displayName, boolean billboard) {
            this.pos = pos;
            this.colorWithAlpha = colorWithAlpha;
            this.ttl = ttl;
            this.displayName = displayName;
            this.billboard = billboard;
        }
    }

    private static final class ScanJob {
        final UUID playerId;
        final ResourceKey<Level> dimension;
        final UUID scannerId;
        final ScanKind kind;
        final int anchorChunkX;
        final int anchorChunkZ;
        final List<Integer> rangeSteps;
        final Block targetBlock;
        final String targetMobId;
        final String genericTarget;
        final int requiredOreMiningLevel;
        final Set<BlockPos> existingMarkers;

        int ringIndex;
        int chunkX;
        int chunkZ;
        int blockX;
        int blockY;
        int blockZ;
        boolean chunkActive;
        int mobEntityIndex;
        List<LivingEntity> mobChunkEntities;

        int newMarkersFound;
        boolean limitReached;
        final Deque<PendingMarker> pendingMarkers = new ArrayDeque<>();

        private ScanJob(ServerPlayer player, ItemStack stack, ScanKind kind, UUID scannerId) {
            this.playerId = player.getUUID();
            this.dimension = player.level().dimension();
            this.scannerId = scannerId;
            this.kind = kind;
            BlockPos playerPos = player.blockPosition();
            this.anchorChunkX = playerPos.getX() >> 4;
            this.anchorChunkZ = playerPos.getZ() >> 4;
            int scanRange = ScannerItem.getScanRangeFromStack(stack);
            this.rangeSteps = ScannerItem.computeRangeSteps(scanRange);
            this.targetBlock = kind == ScanKind.BLOCK ? ScannerItem.getTargetBlockFromStack(stack) : null;
            this.targetMobId = kind == ScanKind.MOB ? ScannerItem.getTargetMobFromStack(stack) : null;
            this.genericTarget = (kind == ScanKind.ALL_ORES || kind == ScanKind.ALL_MOBS)
                    ? ScannerItem.getGenericTargetFromStack(stack) : null;
            this.requiredOreMiningLevel = parseOreMiningLevel(genericTarget);
            this.existingMarkers = new HashSet<>(ScannerItem.getActiveMarkerPositions(scannerId));

            this.chunkActive = false;
            this.mobEntityIndex = 0;
            this.mobChunkEntities = List.of();
            this.newMarkersFound = 0;
            this.limitReached = false;
        }

        static ScanJob create(ServerPlayer player, ItemStack stack, ScanKind kind, UUID scannerId) {
            return new ScanJob(player, stack, kind, scannerId);
        }

        private static int parseOreMiningLevel(String genericTarget) {
            if (genericTarget == null || !genericTarget.startsWith("ores")) {
                return 0;
            }
            String levelStr = genericTarget.substring(4);
            if (levelStr.isEmpty()) {
                return 0;
            }
            try {
                return Integer.parseInt(levelStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private int currentRingRange() {
            return rangeSteps.get(ringIndex);
        }

        private int currentChunkRadius() {
            return Math.max(1, currentRingRange() / 16);
        }

        /** @return false when all rings/chunks are exhausted */
        private boolean beginNextChunk(ServerLevel level) {
            while (ringIndex < rangeSteps.size()) {
                int radius = currentChunkRadius();
                int minChunkX = anchorChunkX - radius;
                int maxChunkX = anchorChunkX + radius;
                int minChunkZ = anchorChunkZ - radius;
                int maxChunkZ = anchorChunkZ + radius;

                if (!chunkActive) {
                    chunkX = minChunkX;
                    chunkZ = minChunkZ;
                    chunkActive = true;
                } else {
                    chunkZ++;
                    if (chunkZ > maxChunkZ) {
                        chunkZ = minChunkZ;
                        chunkX++;
                    }
                }

                if (chunkX > maxChunkX) {
                    chunkActive = false;
                    ringIndex++;
                    continue;
                }

                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                if (!level.isLoaded(BlockPos.containing(chunkPos.getMiddleBlockX(), 0, chunkPos.getMiddleBlockZ()))) {
                    continue;
                }

                blockX = chunkPos.getMinBlockX();
                blockZ = chunkPos.getMinBlockZ();
                blockY = level.getMinBuildHeight() - 1;
                return true;
            }
            return false;
        }

        private boolean stepBlockInChunk(ServerLevel level) {
            ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
            int maxX = chunkPos.getMaxBlockX();
            int maxZ = chunkPos.getMaxBlockZ();
            int maxY = level.getMaxBuildHeight();

            blockY++;
            if (blockY > maxY) {
                blockZ++;
                if (blockZ > maxZ) {
                    blockX++;
                    if (blockX > maxX) {
                        chunkActive = false;
                        return false;
                    }
                    blockZ = chunkPos.getMinBlockZ();
                }
                blockY = level.getMinBuildHeight();
            }
            return true;
        }

        private BlockPos nextBlockPosition(ServerLevel level) {
            while (true) {
                if (ringIndex >= rangeSteps.size()) {
                    return null;
                }
                if (!chunkActive) {
                    if (!beginNextChunk(level)) {
                        return null;
                    }
                }
                if (!stepBlockInChunk(level)) {
                    continue;
                }
                return new BlockPos(blockX, blockY, blockZ);
            }
        }

        boolean advanceBlockTarget(ServerLevel level, ServerPlayer player, ItemStack stack, int blockBudget, int markerBudget) {
            if (targetBlock == null) {
                return true;
            }
            long ringRangeSq = (long) currentRingRange() * currentRingRange();
            int maxMarkers = Config.scannerMaxBlocks;
            boolean infinite = maxMarkers == -1;
            int baseTtl = Config.scannerMarkerTTL;

            while (blockBudget > 0) {
                BlockPos pos = nextBlockPosition(level);
                if (pos == null) {
                    return true;
                }
                blockBudget--;

                if (ScannerItem.isInScanRange(player, pos, ringRangeSq)
                        && level.getBlockState(pos).getBlock() == targetBlock
                        && !existingMarkers.contains(pos)) {
                    if (!infinite && newMarkersFound >= remainingMarkerCapacity(maxMarkers)) {
                        limitReached = true;
                        return true;
                    }
                    registerBlockMarker(player, pos, level, baseTtl);
                    newMarkersFound++;
                    if (!infinite && newMarkersFound >= remainingMarkerCapacity(maxMarkers)) {
                        limitReached = true;
                        return true;
                    }
                }
            }
            return false;
        }

        boolean advanceOreScan(ServerLevel level, ServerPlayer player, ItemStack stack, int blockBudget, int markerBudget) {
            long ringRangeSq = (long) currentRingRange() * currentRingRange();
            int maxMarkers = Config.scannerMaxBlocks;
            boolean infinite = maxMarkers == -1;
            int baseTtl = Config.scannerMarkerTTL;
            int defaultOreColor = Config.scannerDefaultOreColor;

            while (blockBudget > 0) {
                BlockPos pos = nextBlockPosition(level);
                if (pos == null) {
                    return true;
                }
                blockBudget--;

                if (ScannerItem.isInScanRange(player, pos, ringRangeSq)) {
                    BlockState state = level.getBlockState(pos);
                    if (isOreBlock(state, requiredOreMiningLevel) && !existingMarkers.contains(pos)) {
                        if (!infinite && newMarkersFound >= remainingMarkerCapacity(maxMarkers)) {
                            limitReached = true;
                            return true;
                        }
                        Block block = state.getBlock();
                        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
                        int color = resolveOreColor(blockId, defaultOreColor);
                        int colorWithAlpha = (0x80 << 24) | color;
                        registerMarker(player, pos, colorWithAlpha, baseTtl, block.getName().getString(), false);
                        newMarkersFound++;
                        if (!infinite && newMarkersFound >= remainingMarkerCapacity(maxMarkers)) {
                            limitReached = true;
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        boolean advanceMobScan(ServerLevel level, ServerPlayer player, ItemStack stack, int blockBudget, int markerBudget) {
            int maxMarkers = Config.scannerMaxBlocks;
            boolean infinite = maxMarkers == -1;
            int baseTtl = Config.scannerMarkerTTL;

            while (blockBudget > 0) {
                if (ringIndex >= rangeSteps.size()) {
                    return true;
                }

                if (mobEntityIndex >= mobChunkEntities.size()) {
                    mobChunkEntities = List.of();
                    mobEntityIndex = 0;

                    if (!beginNextChunk(level)) {
                        return true;
                    }

                    int ringRange = currentRingRange();
                    double ringRangeSquared = (double) ringRange * ringRange;
                    ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                    if (!level.isLoaded(BlockPos.containing(chunkPos.getMiddleBlockX(), 0, chunkPos.getMiddleBlockZ()))) {
                        blockBudget--;
                        continue;
                    }

                    mobChunkEntities = level.getEntitiesOfClass(
                            LivingEntity.class,
                            new AABB(
                                    chunkPos.getMinBlockX(), level.getMinBuildHeight(), chunkPos.getMinBlockZ(),
                                    chunkPos.getMaxBlockX(), level.getMaxBuildHeight(), chunkPos.getMaxBlockZ()
                            ),
                            entity -> ScannerItem.isInScanRange(player, entity.getX(), entity.getZ(), ringRangeSquared)
                                    && !(entity instanceof Player)
                                    && matchesMobTarget(entity)
                    );
                    blockBudget--;
                    continue;
                }

                LivingEntity entity = mobChunkEntities.get(mobEntityIndex++);
                BlockPos entityPos = entity.blockPosition();
                blockBudget--;

                if (existingMarkers.contains(entityPos)) {
                    continue;
                }
                if (!infinite && newMarkersFound >= remainingMarkerCapacity(maxMarkers)) {
                    limitReached = true;
                    return true;
                }

                int color = kind == ScanKind.MOB
                        ? (Config.scannerDefaultAlpha << 24) | Config.scannerDefaultMobColor
                        : resolveMobColor(entity);
                registerMarker(player, entityPos, color, baseTtl, entity.getName().getString(), true);
                newMarkersFound++;
                if (!infinite && newMarkersFound >= remainingMarkerCapacity(maxMarkers)) {
                    limitReached = true;
                    return true;
                }
            }
            return false;
        }

        private boolean matchesMobTarget(LivingEntity entity) {
            if (kind == ScanKind.MOB && targetMobId != null) {
                return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(targetMobId);
            }
            if (kind == ScanKind.ALL_MOBS) {
                return ScannerMobCategories.matches(entity, genericTarget != null ? genericTarget : ScannerMobCategories.LEGACY_ALL);
            }
            return false;
        }

        private int remainingMarkerCapacity(int maxMarkers) {
            return maxMarkers - existingMarkers.size();
        }

        private void registerBlockMarker(ServerPlayer player, BlockPos pos, ServerLevel level, int baseTtl) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
            int color = resolveOreColor(blockId, Config.scannerDefaultOreColor);
            int colorWithAlpha = (0x80 << 24) | color;
            registerMarker(player, pos, colorWithAlpha, baseTtl, block.getName().getString(), false);
        }

        private void registerMarker(ServerPlayer player, BlockPos pos, int colorWithAlpha, int baseTtl,
                                    String displayName, boolean billboard) {
            int finalTtl = baseTtl * ScannerItem.getTtlMultiplier();
            existingMarkers.add(pos);
            pendingMarkers.addLast(new PendingMarker(pos, colorWithAlpha, finalTtl, displayName, billboard));
        }

        void flushPendingMarkers(ServerPlayer player, int markerBudget) {
            while (markerBudget > 0 && !pendingMarkers.isEmpty()) {
                sendMarker(player, scannerId, pendingMarkers.removeFirst());
                markerBudget--;
            }
        }

        void flushAllPendingMarkers(ServerPlayer player) {
            while (!pendingMarkers.isEmpty()) {
                sendMarker(player, scannerId, pendingMarkers.removeFirst());
            }
        }

        private static void sendMarker(ServerPlayer player, UUID scannerId, PendingMarker marker) {
            ScannerItem.trackMarker(scannerId, marker.pos, marker.ttl);
            if (marker.billboard) {
                net.unfamily.iskautils.network.ModMessages.sendAddBillboardWithNamePacket(
                        player, marker.pos, marker.colorWithAlpha, marker.ttl, marker.displayName);
            } else {
                net.unfamily.iskautils.network.ModMessages.sendAddHighlightWithNamePacket(
                        player, marker.pos, marker.colorWithAlpha, marker.ttl, marker.displayName);
            }
        }

        void finish(ServerPlayer player, ItemStack stack) {
            int maxLimit = Config.scannerMaxBlocks;
            if (limitReached) {
                ScannerItem.notifyScanLimitReached(player, maxLimit);
                return;
            }
            switch (kind) {
                case BLOCK -> {
                    Block block = targetBlock;
                    if (block != null) {
                        player.displayClientMessage(Component.translatable("item.iska_utils.scanner.found_blocks",
                                newMarkersFound, block.getName()), true);
                    }
                }
                case MOB -> player.displayClientMessage(Component.translatable("item.iska_utils.scanner.found_mobs",
                        newMarkersFound, ScannerItem.localizedMobName(targetMobId)), true);
                case ALL_ORES -> player.displayClientMessage(Component.translatable("item.iska_utils.scanner.found_ores",
                        newMarkersFound), true);
                case ALL_MOBS -> player.displayClientMessage(Component.translatable("item.iska_utils.scanner.found_all_mobs",
                        newMarkersFound), true);
            }
        }

        private static int resolveOreColor(String blockId, int defaultColor) {
            for (String entry : Config.scannerOreEntries) {
                String[] parts = entry.split(";");
                if (parts.length != 2) {
                    continue;
                }
                String oreName = parts[0];
                try {
                    if (oreName.equals(blockId)) {
                        return Integer.parseInt(parts[1], 16);
                    }
                    if (oreName.startsWith("$")) {
                        String searchTerm = oreName.substring(1).toLowerCase();
                        if (blockId.toLowerCase().contains(searchTerm)) {
                            return Integer.parseInt(parts[1], 16);
                        }
                    } else if (blockId.toLowerCase().contains(oreName.toLowerCase())) {
                        return Integer.parseInt(parts[1], 16);
                    }
                } catch (NumberFormatException ignored) {
                    LOGGER.error("Invalid color format in ore entry: {}", entry);
                }
            }
            return defaultColor;
        }

        private static int resolveMobColor(LivingEntity entity) {
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            int color = Config.scannerDefaultMobColor;
            for (String entry : Config.scannerMobEntries) {
                String[] parts = entry.split(";");
                if (parts.length != 2) {
                    continue;
                }
                String mobPattern = parts[0];
                try {
                    if (mobPattern.equals(entityId)) {
                        return Integer.parseInt(parts[1], 16);
                    }
                    if (mobPattern.startsWith("$")) {
                        String searchTerm = mobPattern.substring(1).toLowerCase();
                        if (entityId.toLowerCase().contains(searchTerm)) {
                            return Integer.parseInt(parts[1], 16);
                        }
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Invalid color format in mob entry: {}", entry);
                }
            }
            return (0x80 << 24) | color;
        }

        private static boolean isOreBlock(BlockState blockState, int requiredMiningLevel) {
            Block block = blockState.getBlock();
            String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
            boolean isOre = false;

            for (String tagName : Config.scannerOreTags) {
                if (tagName.startsWith("#")) {
                    tagName = tagName.substring(1);
                }
                if (tagName.endsWith("*")) {
                    String prefix = tagName.substring(0, tagName.length() - 1);
                    for (net.minecraft.tags.TagKey<Block> blockTag : block.builtInRegistryHolder().tags().toList()) {
                        if (blockTag.location().toString().startsWith(prefix)) {
                            isOre = true;
                            break;
                        }
                    }
                } else {
                    String[] parts = tagName.split(":", 2);
                    String namespace = parts.length > 1 ? parts[0] : "minecraft";
                    String path = parts.length > 1 ? parts[1] : tagName;
                    ResourceLocation tagId = ResourceLocation.parse(namespace + ":" + path);
                    if (block.builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(
                            net.minecraft.core.registries.Registries.BLOCK, tagId))) {
                        isOre = true;
                        break;
                    }
                }
            }

            if (!isOre) {
                isOre = blockId.contains("ore")
                        || blockId.contains("raw_block")
                        || (blockId.contains("deepslate") && blockId.contains("ore"));
            }

            if (isOre && requiredMiningLevel > 0) {
                int blockMiningLevel = getBlockMiningLevel(blockState);
                if (requiredMiningLevel == 100) {
                    return blockMiningLevel == 100;
                }
                return blockMiningLevel == requiredMiningLevel;
            }
            return isOre;
        }

        private static int getBlockMiningLevel(BlockState blockState) {
            Holder<Block> holder = blockState.getBlock().builtInRegistryHolder();
            if (holder.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) {
                return 100;
            }
            if (holder.is(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)) {
                return 5;
            }
            if (holder.is(Tags.Blocks.NEEDS_NETHERITE_TOOL)) {
                return 5;
            }
            if (holder.is(BlockTags.INCORRECT_FOR_IRON_TOOL)) {
                return 4;
            }
            if (holder.is(BlockTags.INCORRECT_FOR_STONE_TOOL)) {
                return 3;
            }
            if (holder.is(BlockTags.INCORRECT_FOR_WOODEN_TOOL)) {
                return 2;
            }
            int tier = 0;
            if (blockState.requiresCorrectToolForDrops()) {
                if (holder.is(BlockTags.MINEABLE_WITH_PICKAXE)
                        || holder.is(BlockTags.MINEABLE_WITH_AXE)
                        || holder.is(BlockTags.MINEABLE_WITH_SHOVEL)
                        || holder.is(BlockTags.MINEABLE_WITH_HOE)) {
                    tier = 1;
                }
            }
            if (holder.is(Tags.Blocks.ORES) && (tier == 0 || tier == 1)) {
                return 100;
            }
            return tier;
        }
    }
}
