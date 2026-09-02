package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.CollectingCrateBlock;
import net.unfamily.iskautils.block.EnderNullifierBlock;
import net.unfamily.iskautils.block.FanBlock;
import net.unfamily.iskautils.block.SoulNullifierBlock;
import net.unfamily.iskautils.block.StructurePlacerMachineBlock;
import net.unfamily.iskautils.block.TemporalOverclockerBlock;
import net.unfamily.iskautils.block.WanderNullifierBlock;
import net.unfamily.iskautils.block.custom.BlazingAltarBlock;
import net.unfamily.iskautils.block.entity.BlazingAltarBlockEntity;
import net.unfamily.iskautils.block.entity.CollectingCrateBlockEntity;
import net.unfamily.iskautils.block.entity.FanBlockEntity;
import net.unfamily.iskautils.block.entity.INullifierBE;
import net.unfamily.iskautils.block.entity.StructurePlacerMachineBlockEntity;
import net.unfamily.iskautils.block.entity.TemporalOverclockerBlockEntity;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.util.preview.MachinePreviewServerTracker;

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
                || state.getBlock() instanceof StructurePlacerMachineBlock
                || state.getBlock() instanceof EnderNullifierBlock
                || state.getBlock() instanceof SoulNullifierBlock
                || state.getBlock() instanceof WanderNullifierBlock
                || state.getBlock() instanceof BlazingAltarBlock
                || state.getBlock() instanceof TemporalOverclockerBlock;
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
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!isPreviewOwnerBlock(removedState)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        resetShowAreaFlag(blockEntity);
        MachinePreviewServerTracker.clearGeneration(pos);
        broadcastClearPreviewForOwner(serverLevel, pos);
    }

    private static void resetShowAreaFlag(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return;
        }
        if (blockEntity instanceof FanBlockEntity fan) {
            fan.setShowAreaEnabled(false);
        } else if (blockEntity instanceof StructurePlacerMachineBlockEntity machine) {
            machine.setShowPreview(false);
        } else if (blockEntity instanceof CollectingCrateBlockEntity crate) {
            crate.setPreviewEnabled(false);
        } else if (blockEntity instanceof INullifierBE nullifier) {
            nullifier.setShowAreaEnabled(false);
        } else if (blockEntity instanceof BlazingAltarBlockEntity altar) {
            altar.setShowAreaEnabled(false);
        } else if (blockEntity instanceof TemporalOverclockerBlockEntity overclocker) {
            overclocker.setShowAreaEnabled(false);
        }
    }
}
