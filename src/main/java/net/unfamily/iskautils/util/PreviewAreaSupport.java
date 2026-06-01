package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.CollectingCrateBlock;
import net.unfamily.iskautils.block.FanBlock;
import net.unfamily.iskautils.block.StructurePlacerMachineBlock;
import net.unfamily.iskautils.network.ModMessages;

/**
 * Clears client-side area preview markers when the owning machine block is removed.
 */
public final class PreviewAreaSupport {

    private PreviewAreaSupport() {}

    public static boolean isPreviewOwnerBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        return state.getBlock() instanceof FanBlock
                || state.getBlock() instanceof CollectingCrateBlock
                || state.getBlock() instanceof StructurePlacerMachineBlock;
    }

    public static void broadcastClearPreviewForOwner(ServerLevel level, BlockPos owner) {
        if (level == null || owner == null) {
            return;
        }
        for (var player : level.players()) {
            ModMessages.clearPreviewForBuilder(player, owner);
        }
    }

    /** Call from block {@code onRemove} when the block type changes (not piston move). */
    public static void onPreviewOwnerBlockRemoved(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (level.isClientSide() || oldState.is(newState.getBlock())) {
            return;
        }
        onPreviewOwnerBlockBroken(level, pos, oldState);
    }

    /** Call when the owner block is broken (e.g. {@code affectNeighborsAfterRemoval} on 26.x). */
    public static void onPreviewOwnerBlockBroken(Level level, BlockPos pos, BlockState removedState) {
        if (level.isClientSide() || !isPreviewOwnerBlock(removedState)) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            broadcastClearPreviewForOwner(serverLevel, pos);
        }
    }
}
