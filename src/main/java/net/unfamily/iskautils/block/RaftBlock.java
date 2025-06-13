package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.List;

/**
 * Raft Block - A thin block that can float in air and be placed on walls
 */
public class RaftBlock extends HorizontalDirectionalBlock {
    
    public static final MapCodec<RaftBlock> CODEC = simpleCodec(RaftBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty VERTICAL = BooleanProperty.create("vertical");
    
    // Horizontal block shape (0.5 pixel height)
    protected static final VoxelShape SHAPE_HORIZONTAL = Block.box(0, 0, 0, 16, 0.5, 16);
    
    // Vertical shapes for different orientations (north, south, east, west)
    protected static final VoxelShape SHAPE_VERTICAL_NORTH = Block.box(0, 0, 0, 16, 16, 0.5);
    protected static final VoxelShape SHAPE_VERTICAL_SOUTH = Block.box(0, 0, 15.5, 16, 16, 16);
    protected static final VoxelShape SHAPE_VERTICAL_EAST = Block.box(15.5, 0, 0, 16, 16, 16);
    protected static final VoxelShape SHAPE_VERTICAL_WEST = Block.box(0, 0, 0, 0.5, 16, 16);

    public RaftBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(VERTICAL, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        boolean vertical = state.getValue(VERTICAL);
        
        // Check if this is a vertical placement
        if (vertical) {
            // Return the appropriate shape based on the facing direction
            return switch (facing) {
                case NORTH -> SHAPE_VERTICAL_NORTH;
                case SOUTH -> SHAPE_VERTICAL_SOUTH;
                case EAST -> SHAPE_VERTICAL_EAST;
                case WEST -> SHAPE_VERTICAL_WEST;
                default -> SHAPE_HORIZONTAL; // Fallback
            };
        } else {
            // Horizontal placement
            return SHAPE_HORIZONTAL;
        }
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Get the face being clicked
        Direction clickedFace = context.getClickedFace();
        
        // Determine if it's a vertical placement (on a wall)
        boolean vertical = clickedFace.getAxis().isHorizontal();
        
        Direction direction;
        
        if (vertical) {
            // For wall placements, use the OPPOSITE direction of the clicked face
            // This way: north -> south, east -> west, etc.
            direction = clickedFace.getOpposite();
        } else {
            // For horizontal placements (floor or ceiling),
            // use the horizontal direction of the player
            direction = context.getHorizontalDirection();
        }
        
        return this.defaultBlockState()
                .setValue(FACING, direction)
                .setValue(VERTICAL, vertical);
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VERTICAL);
    }
    
    // The raft can exist anywhere, even suspended in air
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }
    
    // Don't destroy the raft if the block below is removed
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, 
                                 LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return state;
    }

    // Add method to allow rotation with shift+right click
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Check if the player is pressing shift
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                // Get the current direction
                Direction currentDirection = state.getValue(FACING);
                Direction newDirection = currentDirection.getClockWise();
                
                // Update the block state with the new direction, keeping the vertical state
                level.setBlock(pos, state.setValue(FACING, newDirection), 3);
                
                // Play a click sound when the block is rotated
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.3F, 0.8F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        
        return InteractionResult.PASS;
    }
} 