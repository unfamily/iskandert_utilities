package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.RubberLogEmptyBlock;
import net.unfamily.iskautils.block.RubberLogFilledBlock;
import net.unfamily.iskautils.item.ModItems;

public class TreeTapItem extends Item {
    public TreeTapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);
        Direction clickedFace = context.getClickedFace();
        ItemStack itemStack = context.getItemInHand();
        InteractionHand hand = context.getHand();
        
        // Check if the block is a filled rubber log
        if (state.is(ModBlocks.RUBBER_LOG_FILLED.get())) {
            // Check if the clicked face matches the facing direction
            if (clickedFace == state.getValue(RubberLogFilledBlock.FACING)) {
                // Use the custom method of the block
                if (player != null) {
                    return ((RubberLogFilledBlock) state.getBlock()).onTapWithTreeTap(state, level, pos, player, hand);
                }
                
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        } 
        // Check if the block is an empty rubber log
        else if (state.is(ModBlocks.RUBBER_LOG_EMPTY.get())) {
            // Check if the clicked face matches the facing direction
            if (clickedFace == state.getValue(RubberLogEmptyBlock.FACING)) {
                // Use the custom method of the block
                if (player != null) {
                    return ((RubberLogEmptyBlock) state.getBlock()).onTapWithTreeTap(state, level, pos, player, hand);
                }
                
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        
        return InteractionResult.PASS;
    }
} 