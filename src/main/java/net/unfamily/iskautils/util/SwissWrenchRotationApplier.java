package net.unfamily.iskautils.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.unfamily.iskautils.block.VectorBlock;
import net.unfamily.iskautils.events.SetWrenchDirectionBlock;

/** Shared Swiss Wrench block rotation apply (legacy modes + exclusions). */
public final class SwissWrenchRotationApplier {

    private static final TagKey<Block> WRENCH_NOT_ROTATE =
            BlockTags.create(ResourceLocation.tryParse("c:wrench_not_rotate"));

    private SwissWrenchRotationApplier() {
    }

    public static boolean isExcluded(BlockState state) {
        return state.is(WRENCH_NOT_ROTATE) || state.getBlock() instanceof VectorBlock;
    }

    private static Direction rotateClockwise(Direction current) {
        return switch (current) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            case UP, DOWN -> current;
        };
    }

    private static Direction rotateCounterClockwise(Direction current) {
        return switch (current) {
            case NORTH -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case UP, DOWN -> current;
        };
    }

    private static boolean isRotateMode(SetWrenchDirectionBlock.RotationMode mode) {
        return mode == SetWrenchDirectionBlock.RotationMode.ROTATE_LEFT
                || mode == SetWrenchDirectionBlock.RotationMode.ROTATE_RIGHT;
    }

    /**
     * Applies the given mode to the block at pos. Server-side only.
     *
     * @return true if the block state changed
     */
    public static boolean apply(Level level, BlockPos pos, Player player, SetWrenchDirectionBlock.RotationMode mode) {
        BlockState blockState = level.getBlockState(pos);
        if (isExcluded(blockState) || mode == null || mode == SetWrenchDirectionBlock.RotationMode.RADIAL) {
            return false;
        }

        BlockState newState = blockState;

        if (isRotateMode(mode)) {
            boolean clockwise = mode == SetWrenchDirectionBlock.RotationMode.ROTATE_RIGHT;
            if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                Direction current = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                Direction rotated = clockwise ? rotateClockwise(current) : rotateCounterClockwise(current);
                if (rotated != current) {
                    newState = blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, rotated);
                }
            } else if (blockState.hasProperty(BlockStateProperties.FACING)) {
                Direction current = blockState.getValue(BlockStateProperties.FACING);
                Direction rotated = clockwise ? rotateClockwise(current) : rotateCounterClockwise(current);
                if (rotated != current
                        && BlockStateProperties.FACING.getPossibleValues().contains(rotated)) {
                    newState = blockState.setValue(BlockStateProperties.FACING, rotated);
                }
            } else if (blockState.hasProperty(BlockStateProperties.AXIS)) {
                Direction.Axis current = blockState.getValue(BlockStateProperties.AXIS);
                if (current == Direction.Axis.X || current == Direction.Axis.Z) {
                    Direction.Axis rotated = current == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
                    newState = blockState.setValue(BlockStateProperties.AXIS, rotated);
                }
            }
        } else {
            Direction targetDirection = mode.getDirection();
            if (targetDirection == null) {
                return false;
            }
            if (blockState.hasProperty(BlockStateProperties.FACING)) {
                newState = blockState.setValue(BlockStateProperties.FACING, targetDirection);
            } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    && targetDirection.getAxis().isHorizontal()) {
                newState = blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, targetDirection);
            } else if (blockState.hasProperty(HorizontalDirectionalBlock.FACING)
                    && targetDirection.getAxis().isHorizontal()) {
                newState = blockState.setValue(HorizontalDirectionalBlock.FACING, targetDirection);
            } else if (blockState.hasProperty(BlockStateProperties.AXIS)) {
                if (targetDirection == Direction.UP || targetDirection == Direction.DOWN) {
                    Direction.Axis currentAxis = blockState.getValue(BlockStateProperties.AXIS);
                    if (currentAxis == Direction.Axis.X || currentAxis == Direction.Axis.Z) {
                        newState = blockState.setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
                    }
                } else if (targetDirection.getAxis() != Direction.Axis.Y) {
                    newState = blockState.setValue(BlockStateProperties.AXIS, targetDirection.getAxis());
                }
            }
        }

        if (newState.equals(blockState)) {
            // Rotate L/R on UP/DOWN (etc.) is a silent no-op — no sound, no chat spam.
            if (player != null && !isRotateMode(mode)) {
                player.displayClientMessage(
                        Component.translatable("item.iska_utils.swiss_wrench.message.cannot_rotate"), true);
            }
            return false;
        }

        level.setBlock(pos, newState, 3);
        level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }
}
