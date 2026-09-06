package net.unfamily.iskautils.migration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;

import java.util.ArrayList;

/**
 * Converts standalone {@code pattern_crafter:*} blocks to {@code iska_utils:*} without modifying
 * the Pattern Crafter mod. Runs on chunk load and immediately after placing a legacy block.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class PatternCrafterLegacyConversion {
    private static final String LEGACY_NAMESPACE = "pattern_crafter";
    private static final ResourceLocation LEGACY_NORMAL =
            ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, "pattern_crafter");
    private static final ResourceLocation LEGACY_IMPROVED =
            ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, "improved_pattern_crafter");

    private PatternCrafterLegacyConversion() {}

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : new ArrayList<>(event.getChunk().getBlockEntitiesPos())) {
            tryConvertAt(level, pos);
        }
    }

    @SubscribeEvent
    public static void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!isLegacy(event.getPlacedBlock())) {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        // Defer one tick so placement / BE creation from Pattern Crafter finishes first.
        level.getServer().execute(() -> {
            if (level.isLoaded(pos)) {
                tryConvertAt(level, pos);
            }
        });
    }

    /**
     * @return true if a conversion was performed
     */
    public static boolean tryConvertAt(ServerLevel level, BlockPos pos) {
        BlockState oldState = level.getBlockState(pos);
        if (!isLegacy(oldState)) {
            return false;
        }
        boolean improved = LEGACY_IMPROVED.equals(BuiltInRegistries.BLOCK.getKey(oldState.getBlock()));

        BlockEntity oldEntity = level.getBlockEntity(pos);
        CompoundTag tag = oldEntity == null ? null : oldEntity.saveWithFullMetadata(level.registryAccess());

        Block replacement = improved
                ? ModBlocks.IMPROVED_PATTERN_CRAFTER.get()
                : ModBlocks.PATTERN_CRAFTER.get();
        BlockState replacementState = copyFacing(oldState, replacement.defaultBlockState());

        level.removeBlockEntity(pos);
        level.setBlock(pos, replacementState, Block.UPDATE_ALL);

        if (tag != null) {
            tag.putString("id", IskaUtils.MOD_ID + ":" + (improved ? "improved_pattern_crafter" : "pattern_crafter"));
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            expandUpgradeSlotsIfNeeded(tag);
            BlockEntity converted = BlockEntity.loadStatic(pos, replacementState, tag, level.registryAccess());
            if (converted != null) {
                level.setBlockEntity(converted);
                converted.setChanged();
            }
        }

        level.sendBlockUpdated(pos, replacementState, replacementState, Block.UPDATE_ALL);
        return true;
    }

    private static boolean isLegacy(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return LEGACY_NORMAL.equals(id) || LEGACY_IMPROVED.equals(id);
    }

    /** Old Pattern Crafter used 2 upgrade slots; Utils uses 3 (logic / speed / production). */
    private static void expandUpgradeSlotsIfNeeded(CompoundTag tag) {
        if (!tag.contains("upgrades")) {
            return;
        }
        CompoundTag upgrades = tag.getCompound("upgrades");
        if (upgrades.contains("Size") && upgrades.getInt("Size") < 3) {
            upgrades.putInt("Size", 3);
        }
    }

    private static BlockState copyFacing(BlockState oldState, BlockState replacement) {
        if (oldState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && replacement.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return replacement.setValue(
                    BlockStateProperties.HORIZONTAL_FACING,
                    oldState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        return replacement;
    }
}
