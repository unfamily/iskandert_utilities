package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.block.ModBlocks;

/**
 * Redstone Signal Item - Places an invisible block that emits a temporary redstone signal
 * Has 128 durability and consumes 1 durability per successful placement.
 * The block removes itself after 3 seconds and emits redstone signal.
 */
public class RedstoneSignalItem extends Item {

    private static final int MAX_DURABILITY = 128;

    public RedstoneSignalItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        var player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();

        // Only work on server side
        if (level.isClientSide || !(player instanceof ServerPlayer)) {
            return InteractionResult.SUCCESS;
        }

        // Get the clicked block state
        BlockState clickedState = level.getBlockState(clickedPos);

        // If clicking on a redstone activator signal block, remove it and restore durability
        if (clickedState.getBlock() == ModBlocks.REDSTONE_ACTIVATOR_SIGNAL.get()) {
            level.destroyBlock(clickedPos, false);
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
            return InteractionResult.SUCCESS;
        }

        // Check if item has durability left
        if (stack.getDamageValue() >= MAX_DURABILITY) {
            return InteractionResult.FAIL;
        }

        // Get the position where we want to place the signal (above the clicked block)
        BlockPos placePos = clickedPos.above();

        // Check if the target position is air
        BlockState targetState = level.getBlockState(placePos);
        if (!targetState.isAir()) {
            return InteractionResult.FAIL;
        }

        // Place the redstone activator signal block
        BlockState signalState = ModBlocks.REDSTONE_ACTIVATOR_SIGNAL.get().defaultBlockState();
        level.setBlock(placePos, signalState, 3);

        // Consume durability
        stack.setDamageValue(stack.getDamageValue() + 1);

        return InteractionResult.SUCCESS;
    }
}
