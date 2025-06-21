package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.unfamily.iskautils.events.SetWrenchDirectionBlock;

import java.util.List;

public class SwissWrenchItem extends Item {
    // tag for blocks that should not be rotated
    private static final TagKey<Block> WRENCH_NOT_ROTATE = BlockTags.create(
            ResourceLocation.tryParse("c:wrench_not_rotate"));

    public SwissWrenchItem(Properties properties) {
        super(properties);
    }
    
    /**
     * Get the next direction in clockwise rotation
     */
    private Direction rotateClockwise(Direction current) {
        return switch (current) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case UP, DOWN -> current; // Non cambia per UP e DOWN
        };
    }
    
    /**
     * Get the next direction in counter-clockwise rotation
     */
    private Direction rotateCounterClockwise(Direction current) {
        return switch (current) {
            case NORTH -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case UP, DOWN -> current; // Non cambia per UP e DOWN
        };
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState blockState = level.getBlockState(blockPos);
        Block block = blockState.getBlock();
        ItemStack itemStack = context.getItemInHand();
        
        // check if the player is using Shift + right click
        if (player != null && player.isShiftKeyDown()) {
            // check if the block belongs to the tag that excludes rotation
            if (blockState.is(WRENCH_NOT_ROTATE)) {
                return InteractionResult.PASS;
            }
            
            // do nothing on the client, only on the server
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            
            // Get the current rotation mode
            SetWrenchDirectionBlock.RotationMode mode = SetWrenchDirectionBlock.getSelectedRotationMode(itemStack);
            boolean changed = false;
            
            // Handle different rotation modes
            if (mode == SetWrenchDirectionBlock.RotationMode.ROTATE_RIGHT || 
                mode == SetWrenchDirectionBlock.RotationMode.ROTATE_LEFT) {
                // Determine rotation direction
                boolean clockwise = (mode == SetWrenchDirectionBlock.RotationMode.ROTATE_RIGHT);
                
                // Try to rotate horizontal facing
                if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    Direction current = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                    Direction rotated = clockwise ? rotateClockwise(current) : rotateCounterClockwise(current);
                    level.setBlock(blockPos, blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, rotated), 3);
                    changed = true;
                }
                // Try to rotate facing
                else if (blockState.hasProperty(BlockStateProperties.FACING)) {
                    Direction current = blockState.getValue(BlockStateProperties.FACING);
                    Direction rotated = clockwise ? rotateClockwise(current) : rotateCounterClockwise(current);
                    if (current.getAxis() != rotated.getAxis() || BlockStateProperties.FACING.getPossibleValues().contains(rotated)) {
                        level.setBlock(blockPos, blockState.setValue(BlockStateProperties.FACING, rotated), 3);
                        changed = true;
                    }
                }
                // Try to rotate axis for pillar blocks
                else if (blockState.hasProperty(BlockStateProperties.AXIS)) {
                    Direction.Axis current = blockState.getValue(BlockStateProperties.AXIS);
                    
                    // Rotate only horizontal axes: X ↔ Z
                    if (current == Direction.Axis.X || current == Direction.Axis.Z) {
                        Direction.Axis rotated = (current == Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
                        level.setBlock(blockPos, blockState.setValue(BlockStateProperties.AXIS, rotated), 3);
                        changed = true;
                    }
                    // Y axis doesn't rotate (stays vertical)
                }
            } else {
                // Handle specific direction mode
                Direction targetDirection = mode.getDirection();
                if (targetDirection == null) {
                    return InteractionResult.FAIL;
                }
                
                // Try to set the direction of the block based on its properties
                if (blockState.hasProperty(BlockStateProperties.FACING)) {
                    level.setBlock(blockPos, blockState.setValue(BlockStateProperties.FACING, targetDirection), 3);
                    changed = true;
                } 
                // Try to set the direction of the block based on its properties
                else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && 
                         targetDirection.getAxis().isHorizontal()) {
                    level.setBlock(blockPos, blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, targetDirection), 3);
                    changed = true;
                }
                // Try to set the direction of the block based on its properties
                else if (blockState.hasProperty(HorizontalDirectionalBlock.FACING) && 
                         targetDirection.getAxis().isHorizontal()) {
                    level.setBlock(blockPos, blockState.setValue(HorizontalDirectionalBlock.FACING, targetDirection), 3);
                    changed = true;
                }
                // Try to set the direction of the block based on its properties
                else if (blockState.hasProperty(BlockStateProperties.AXIS)) {
                    // For UP/DOWN directions, rotate horizontal logs to vertical
                    if (targetDirection == Direction.UP || targetDirection == Direction.DOWN) {
                        // If log is horizontal (X or Z axis), make it vertical (Y axis)
                        Direction.Axis currentAxis = blockState.getValue(BlockStateProperties.AXIS);
                        if (currentAxis == Direction.Axis.X || currentAxis == Direction.Axis.Z) {
                            level.setBlock(blockPos, blockState.setValue(BlockStateProperties.AXIS, Direction.Axis.Y), 3);
                            changed = true;
                        }
                    } else if (targetDirection.getAxis() != Direction.Axis.Y) {
                        level.setBlock(blockPos, blockState.setValue(BlockStateProperties.AXIS, targetDirection.getAxis()), 3);
                        changed = true;
                    }
                }
            }
            
            // if the block was changed, play a sound and send a message to the player
            if (changed) {
                level.playSound(null, blockPos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.displayClientMessage(
                    Component.translatable("item.iska_utils.swiss_wrench.message.block_rotated"), true);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(
                    Component.translatable("item.iska_utils.swiss_wrench.message.cannot_rotate"), true);
                return InteractionResult.FAIL;
            }
        }
        
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // Show current mode
        SetWrenchDirectionBlock.RotationMode currentMode = SetWrenchDirectionBlock.getSelectedRotationMode(stack);
        
        Component modeText = Component.translatable("item.iska_utils.swiss_wrench.tooltip.current_mode", 
                currentMode.getDisplayName());
        
        tooltipComponents.add(modeText);
        tooltipComponents.add(Component.translatable("item.iska_utils.swiss_wrench.tooltip.desc0"));
        tooltipComponents.add(Component.translatable("item.iska_utils.swiss_wrench.tooltip.desc1"));
    }
}