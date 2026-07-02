package net.unfamily.iskautils.scan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.compat.lootr.LootrScannerCompat;
import net.unfamily.iskautils.util.ScannerLootModes;

import java.util.Optional;

/**
 * Evaluates whether a block position matches a loot scanner mode and resolves marker color keys.
 */
public final class ScannerLootScan {
    public record LootMatch(String colorKey, boolean lootr, boolean entity) {}

    private ScannerLootScan() {}

    public static boolean matches(ServerLevel level, BlockPos pos, ServerPlayer player, String genericTarget) {
        return classify(level, pos, player, genericTarget).isPresent();
    }

    public static Optional<LootMatch> classify(ServerLevel level, BlockPos pos, ServerPlayer player, String genericTarget) {
        int mode = ScannerLootModes.normalizedMode(genericTarget, LootrScannerCompat.isLoaded());
        if (LootrScannerCompat.isLootrContainer(level, pos)) {
            if (!LootrScannerCompat.matchesMode(level, pos, player, mode)) {
                return Optional.empty();
            }
            Block block = level.getBlockState(pos).getBlock();
            String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
            return Optional.of(new LootMatch("lootr:" + blockId, true, false));
        }
        if (!matchesVanilla(level, pos, mode)) {
            return Optional.empty();
        }
        Block block = level.getBlockState(pos).getBlock();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        return Optional.of(new LootMatch("vanilla:" + blockId, false, false));
    }

    public static Optional<LootMatch> classifyEntity(net.minecraft.world.entity.Entity entity, ServerPlayer player, String genericTarget) {
        if (!LootrScannerCompat.isLootrEntity(entity)) {
            return Optional.empty();
        }
        int mode = ScannerLootModes.normalizedMode(genericTarget, LootrScannerCompat.isLoaded());
        if (!LootrScannerCompat.matchesEntityMode(entity, player, mode)) {
            return Optional.empty();
        }
        String entityId = LootrScannerCompat.getEntityTypeId(entity).toString();
        return Optional.of(new LootMatch(entityId, true, true));
    }

    private static boolean matchesVanilla(ServerLevel level, BlockPos pos, int mode) {
        BlockState state = level.getBlockState(pos);
        if (!isLootStorageBlock(level, pos, state)) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) {
            return false;
        }
        boolean hasLootTable = LootrScannerCompat.hasLootTable(blockEntity);
        boolean empty = LootrScannerCompat.isEmptyContainer(container);
        return switch (mode) {
            case 1 -> hasLootTable || !empty;
            case 2 -> !hasLootTable && empty;
            case 3 -> false;
            default -> false;
        };
    }

    private static boolean isLootStorageBlock(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof ChestBlock || block == Blocks.BARREL || block == Blocks.DECORATED_POT) {
            return true;
        }
        if (block instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
            return true;
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        if (path.contains("barrel") || path.contains("chest")) {
            return true;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container) {
            if (LootrScannerCompat.hasLootTable(blockEntity)) {
                return true;
            }
            if (hasConfiguredLootTag(block)) {
                return true;
            }
            if (matchesLootEntryPattern(blockId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesLootEntryPattern(String blockId) {
        String lowerId = blockId.toLowerCase();
        for (String entry : Config.scannerLootEntries) {
            String[] parts = entry.split(";");
            if (parts.length < 1) {
                continue;
            }
            String pattern = parts[0];
            if (pattern.startsWith("$") && lowerId.contains(pattern.substring(1).toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasConfiguredLootTag(Block block) {
        for (String tagName : Config.scannerLootTags) {
            if (tagName.startsWith("#")) {
                tagName = tagName.substring(1);
            }
            if (tagName.endsWith("*")) {
                String prefix = tagName.substring(0, tagName.length() - 1);
                for (TagKey<Block> blockTag : block.builtInRegistryHolder().tags().toList()) {
                    if (blockTag.location().toString().startsWith(prefix)) {
                        return true;
                    }
                }
            } else {
                String[] parts = tagName.split(":", 2);
                String namespace = parts.length > 1 ? parts[0] : "minecraft";
                String path = parts.length > 1 ? parts[1] : tagName;
                ResourceLocation tagId = ResourceLocation.fromNamespaceAndPath(namespace, path);
                if (block.builtInRegistryHolder().is(TagKey.create(Registries.BLOCK, tagId))) {
                    return true;
                }
            }
        }
        return false;
    }
}
