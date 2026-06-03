package net.unfamily.iskautils.block.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.BlazingAltarFlameVisual;
import net.unfamily.iskautils.block.custom.BlazingAltarBlock;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.BlazingAltarFlamePlacement;

/** Applies derived block state (visual + powered) from machine operational state. */
public final class BlazingAltarBlockSync {
    private BlazingAltarBlockSync() {}

    public static void sync(BlazingAltarBlockEntity altar) {
        if (altar.getLevel() == null || altar.getLevel().isClientSide()) {
            return;
        }
        BlockState state = altar.getBlockState();
        if (!(state.getBlock() instanceof BlazingAltarBlock)) {
            return;
        }

        BlazingAltarFlameVisual visual = computeVisual(altar);
        boolean powered = altar.isOperational();
        boolean wasPowered = state.getValue(BlazingAltarBlock.POWERED);
        boolean visualChanged = state.getValue(BlazingAltarBlock.FLAME_VISUAL) != visual;

        if (wasPowered != powered && altar.getLevel() instanceof ServerLevel serverLevel) {
            BlazingAltarFlamePlacement.refreshBrazierFlameLightInRadius(
                    serverLevel,
                    altar.getBlockPos(),
                    altar.getChunkRadius(),
                    altar.isGroundOnly());
        }

        if (visualChanged || wasPowered != powered) {
            BlockState updated = state;
            if (visualChanged) {
                updated = updated.setValue(BlazingAltarBlock.FLAME_VISUAL, visual);
            }
            if (wasPowered != powered) {
                updated = updated.setValue(BlazingAltarBlock.POWERED, powered);
            }
            altar.getLevel().setBlock(altar.getBlockPos(), updated, 3);
            if (visualChanged) {
                altar.getLevel().sendBlockUpdated(altar.getBlockPos(), state, altar.getLevel().getBlockState(altar.getBlockPos()), 3);
            }
        }
    }

    private static BlazingAltarFlameVisual computeVisual(BlazingAltarBlockEntity altar) {
        if (!altar.isOperational()) {
            return BlazingAltarFlameVisual.HIDDEN;
        }
        ItemStack placer = altar.getPlacerHandler().getStackInSlot(BlazingAltarBlockEntity.PLACER_SLOT);
        if (placer.isEmpty()) {
            return BlazingAltarFlameVisual.GLOW;
        }
        if (placer.is(ModItems.BURNING_BRAZIER.get())) {
            return BlazingAltarFlameVisual.BURNING;
        }
        if (placer.is(ModItems.CURSED_CANDLE.get())) {
            return BlazingAltarFlameVisual.CURSED;
        }
        return BlazingAltarFlameVisual.GLOW;
    }
}
