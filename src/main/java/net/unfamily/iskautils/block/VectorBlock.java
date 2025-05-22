package net.unfamily.iskautils.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

import java.util.List;
import java.util.Collections;
import java.util.function.Supplier;
import net.minecraft.world.level.block.state.properties.Property;
import net.unfamily.iskautils.Config;

public class VectorBlock extends HorizontalDirectionalBlock {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty VERTICAL = BooleanProperty.create("vertical");
    public static final MapCodec<VectorBlock> CODEC = simpleCodec(VectorBlock::new);
    
    // Block shape based on the model (0.5 pixel height)
    protected static final VoxelShape SHAPE_HORIZONTAL = Block.box(0, 0, 0, 16, 0.5, 16);
    
    // Vertical shapes for walls (north, south, east, west)
    protected static final VoxelShape SHAPE_VERTICAL_NORTH = Block.box(0, 0, 0, 16, 16, 1);
    protected static final VoxelShape SHAPE_VERTICAL_SOUTH = Block.box(0, 0, 15, 16, 16, 16);
    protected static final VoxelShape SHAPE_VERTICAL_EAST = Block.box(15, 0, 0, 16, 16, 16);
    protected static final VoxelShape SHAPE_VERTICAL_WEST = Block.box(0, 0, 0, 1, 16, 16);
    
    // Variable for block speed
    private final Supplier<Double> speedSupplier;
    
    // Whether this vector plate affects only players
    private final boolean affectsPlayers;

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public VectorBlock(Properties properties, Supplier<Double> speedSupplier, boolean affectsPlayers) {
        super(properties.sound(SoundType.DEEPSLATE));
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(VERTICAL, false));
        this.speedSupplier = speedSupplier;
        this.affectsPlayers = affectsPlayers;
    }
    
    public VectorBlock(Properties properties, Supplier<Double> speedSupplier) {
        this(properties, speedSupplier, false);
    }
    
    public VectorBlock(Properties properties) {
        this(properties, () -> 0.05D, false);
    }

    // Add right-click rotation behavior with shift
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Check if player is sneaking (shift) and it's the main hand
        if (player.isShiftKeyDown()) {
            // Don't allow rotation for vertical plates
            boolean isVertical = state.getValue(VERTICAL);
            if (isVertical) {
                return InteractionResult.PASS;
            }
            
            if (!level.isClientSide) {
                // Get current direction and calculate next direction (rotate clockwise)
                Direction currentDirection = state.getValue(FACING);
                Direction newDirection = currentDirection.getClockWise();
                
                // Update the block state with the new direction, preserving vertical state
                level.setBlock(pos, state.setValue(FACING, newDirection), 3);
                
                // Play a stone click sound
                level.playSound(null, pos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        
        return InteractionResult.PASS;
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
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Return an empty shape to allow entities to pass through the block
        return Shapes.empty();
    }
    
    @Override
    public boolean isPossibleToRespawnInThis(BlockState state) {
        // Allow players to respawn in this block
        return true;
    }
    
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        // Use shape for light occlusion
        return false;
    }
    
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        // Allow light to pass through the block
        return true;
    }
    
    /**
     * Handle face visibility for rendering
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        boolean vertical = state.getValue(VERTICAL);
        
        // Always show the bottom face
        if (!vertical && side != Direction.DOWN) {
            return adjacentBlockState.is(this) ? true : super.skipRendering(state, adjacentBlockState, side);
        } else if (vertical) {
            Direction facing = state.getValue(FACING);
            if (side != facing.getOpposite()) {
                return adjacentBlockState.is(this) ? true : super.skipRendering(state, adjacentBlockState, side);
            }
        }
        return super.skipRendering(state, adjacentBlockState, side);
    }
    
    /**
     * Check if the block can be placed at this position
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        boolean vertical = state.getValue(VERTICAL);
        
        if (!vertical) {
            // Horizontal placement - check below
            BlockPos belowPos = pos.below();
            BlockState belowState = level.getBlockState(belowPos);
            
            // Check if the block below is a solid block that can support the vector block
            VoxelShape belowShape = belowState.getCollisionShape(level, belowPos);
            
            // If the block below is empty or not complete at the top, it cannot support our block
            if (belowShape.isEmpty() || !hasFullFaceOnTop(belowShape)) {
                return false;
            }
            
            return true;
        } else {
            // Vertical placement - check the block it's attached to
            Direction attachmentDirection = state.getValue(FACING);
            BlockPos attachedPos = pos.relative(attachmentDirection);
            BlockState attachedState = level.getBlockState(attachedPos);
            
            // Check if the block it's attached to has a solid face
            VoxelShape attachedShape = attachedState.getCollisionShape(level, attachedPos);
            
            if (attachedShape.isEmpty()) {
                return false;
            }
            
            // Check if the attached block has a solid face on the side we're attaching to
            return hasFullFaceOnSide(attachedShape, attachmentDirection.getOpposite());
        }
    }
    
    /**
     * Check if a VoxelShape has a full face on top
     */
    private boolean hasFullFaceOnTop(VoxelShape shape) {
        // Check if the top face completely covers the block
        return shape.max(Direction.Axis.Y) >= 1.0 && 
               shape.min(Direction.Axis.X) <= 0.01 && shape.max(Direction.Axis.X) >= 0.99 &&
               shape.min(Direction.Axis.Z) <= 0.01 && shape.max(Direction.Axis.Z) >= 0.99;
    }
    
    /**
     * Check if a VoxelShape has a full face on the given side
     */
    private boolean hasFullFaceOnSide(VoxelShape shape, Direction direction) {
        if (shape.isEmpty()) {
            return false;
        }
        
        switch (direction.getAxis()) {
            case X:
                return shape.min(Direction.Axis.Y) <= 0.01 && shape.max(Direction.Axis.Y) >= 0.99 &&
                       shape.min(Direction.Axis.Z) <= 0.01 && shape.max(Direction.Axis.Z) >= 0.99;
            case Z:
                return shape.min(Direction.Axis.X) <= 0.01 && shape.max(Direction.Axis.X) >= 0.99 &&
                       shape.min(Direction.Axis.Y) <= 0.01 && shape.max(Direction.Axis.Y) >= 0.99;
            default:
                return false;
        }
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        boolean vertical = state.getValue(VERTICAL);
        
        // Check if the block can still survive
        if (!this.canSurvive(state, level, pos)) {
            if (!level.isClientSide) {
                level.destroyBlock(pos, true); // Break the block and drop its item
            }
            return;
        }
        
        if (!vertical) {
            // Horizontal placement logic
            // Check if the changed block is above our vector block
            if (fromPos.equals(pos.above())) {
                BlockState aboveState = level.getBlockState(fromPos);
                
                // Check if the block above has a non-full or ethereal shape (no collision shape)
                boolean shouldBreak = false;
                
                // Check if the block above has an empty or non-full collision shape (like vector plates)
                VoxelShape collisionShape = aboveState.getCollisionShape(level, fromPos);
                if (collisionShape.isEmpty() || !isFullBlock(collisionShape)) {
                    shouldBreak = true;
                }
                
                // If the block should break, break the vector block and drop the item
                if (shouldBreak && !level.isClientSide) {
                    level.destroyBlock(pos, true); // The second parameter 'true' makes the block drop its item
                }
            }
        }
        
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }
    
    /**
     * Check if a VoxelShape represents a full block (1x1x1)
     */
    private boolean isFullBlock(VoxelShape shape) {
        return shape.min(Direction.Axis.X) <= 0.01 && shape.max(Direction.Axis.X) >= 0.99 &&
               shape.min(Direction.Axis.Y) <= 0.01 && shape.max(Direction.Axis.Y) >= 0.99 &&
               shape.min(Direction.Axis.Z) <= 0.01 && shape.max(Direction.Axis.Z) >= 0.99;
    }
    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide) {
            // if affectsPlayers is true, push only players
            // if affectsPlayers is false, push only non-player entities
            boolean isPlayer = entity instanceof Player;
            
            // if the plate is for players but the entity is not a player, skip
            // if the plate is not for players but the entity is a player, skip
            if ((affectsPlayers && !isPlayer) || (!affectsPlayers && isPlayer)) {
                return; // Skip the entity if it doesn't match the type we want to push
            }
            
            // Get the block direction
            Direction direction = state.getValue(FACING);
            boolean vertical = state.getValue(VERTICAL);
            
            // Calculate speed based on the configuration
            double speed = speedSupplier.get();
            
            // If player is sneaking (shift), don't apply movement
            if (isPlayer && ((Player)entity).isShiftKeyDown()) {
                return;
            }
            
            // Different logic for horizontal vs vertical plates
            if (!vertical) {
                // Horizontal plate logic
                applyHorizontalMovement(entity, direction, speed, isPlayer);
            } else {
                // Vertical plate logic
                applyVerticalMovement(entity, direction, speed, isPlayer);
            }
        }
    }
    
    private void applyHorizontalMovement(Entity entity, Direction direction, double speed, boolean isPlayer) {
        if (isPlayer) {
            Player player = (Player) entity;
            
            // Get current player motion
            net.minecraft.world.phys.Vec3 currentMotion = player.getDeltaMovement();
            
            // Constant acceleration for all plate types
            double accelerationFactor = 0.6;
            double conserveFactor = 0.75; // Keep 75% of lateral velocity
            
            // Maintain momentum in the direction of travel for smoother movement
            switch (direction) {
                case NORTH -> {
                    double targetZ = -speed;
                    double newZ = (currentMotion.z * (1 - accelerationFactor)) + (targetZ * accelerationFactor);
                    // Keep current Y to avoid hopping
                    player.setDeltaMovement(
                        currentMotion.x * conserveFactor, 
                        currentMotion.y, 
                        newZ
                    );
                }
                case SOUTH -> {
                    double targetZ = speed;
                    double newZ = (currentMotion.z * (1 - accelerationFactor)) + (targetZ * accelerationFactor);
                    // Keep current Y to avoid hopping
                    player.setDeltaMovement(
                        currentMotion.x * conserveFactor, 
                        currentMotion.y, 
                        newZ
                    );
                }
                case WEST -> {
                    double targetX = -speed;
                    double newX = (currentMotion.x * (1 - accelerationFactor)) + (targetX * accelerationFactor);
                    // Keep current Y to avoid hopping
                    player.setDeltaMovement(
                        newX, 
                        currentMotion.y, 
                        currentMotion.z * conserveFactor
                    );
                }
                case EAST -> {
                    double targetX = speed;
                    double newX = (currentMotion.x * (1 - accelerationFactor)) + (targetX * accelerationFactor);
                    // Keep current Y to avoid hopping
                    player.setDeltaMovement(
                        newX, 
                        currentMotion.y, 
                        currentMotion.z * conserveFactor
                    );
                }
                default -> {} // Should never happen
            }
            
            // Prevent fall damage
            player.fallDistance = 0;
            
            // Set flag to confirm physics updates
            player.hurtMarked = true;
            
        } else {
            // For other entities, we use a more complete method
            // Plate speed for entities
            double entitySpeed = speed * 1.5;
            
            // Get current entity motion
            net.minecraft.world.phys.Vec3 currentMotion = entity.getDeltaMovement();
            
            // Slower acceleration factor for non-player entities
            double entityAccelerationFactor = 0.3;
            
            switch (direction) {
                case NORTH -> {
                    double targetZ = -entitySpeed;
                    double newZ = (currentMotion.z * (1 - entityAccelerationFactor)) + (targetZ * entityAccelerationFactor);
                    // Keep current Y to avoid hopping
                    entity.setDeltaMovement(currentMotion.x, currentMotion.y, newZ);
                }
                case SOUTH -> {
                    double targetZ = entitySpeed;
                    double newZ = (currentMotion.z * (1 - entityAccelerationFactor)) + (targetZ * entityAccelerationFactor);
                    // Keep current Y to avoid hopping
                    entity.setDeltaMovement(currentMotion.x, currentMotion.y, newZ);
                }
                case WEST -> {
                    double targetX = -entitySpeed;
                    double newX = (currentMotion.x * (1 - entityAccelerationFactor)) + (targetX * entityAccelerationFactor);
                    // Keep current Y to avoid hopping
                    entity.setDeltaMovement(newX, currentMotion.y, currentMotion.z);
                }
                case EAST -> {
                    double targetX = entitySpeed;
                    double newX = (currentMotion.x * (1 - entityAccelerationFactor)) + (targetX * entityAccelerationFactor);
                    // Keep current Y to avoid hopping
                    entity.setDeltaMovement(newX, currentMotion.y, currentMotion.z);
                }
                default -> {} // Should never happen
            }
            
            // Avoid excessive friction without making the entity hop
            if (entity.onGround()) {
                // This helps prevent entities from "sticking" to the ground
                entity.setDeltaMovement(entity.getDeltaMovement().x, Math.min(entity.getDeltaMovement().y, 0), entity.getDeltaMovement().z);
            }
            
            // Set fall distance to 0 to prevent fall damage
            entity.fallDistance = 0;
        }
    }
    
    private void applyVerticalMovement(Entity entity, Direction direction, double speed, boolean isPlayer) {
        // For vertical plates, we push away from the wall and up
        // Direction is where the plate is facing (opposite of the wall)
        
        // Vertical plates provide upward movement with different factors for players vs mobs
        double verticalBoostFactor = isPlayer ? Config.verticalBoostFactor : Config.entityVerticalBoostFactor;
        double verticalBoost = speed * verticalBoostFactor;
        
        // Calculate horizontal speed - for vertical plates, push in the SAME horizontal direction
        double horizontalSpeed = speed * 1.2;
        
        // Get current motion
        net.minecraft.world.phys.Vec3 currentMotion = entity.getDeltaMovement();
        
        // Acceleration factors
        double accelerationFactor = isPlayer ? 0.6 : 0.3;
        double conserveFactor = 0.75; // Keep 75% of other velocity components
        
        // For vertical plates, push in the SAME direction as the plate is facing
        // This is counter-intuitive but makes sense in the context of vertical placement
        switch (direction) {
            case NORTH -> {
                // Plate is on North wall (facing north), push North (same direction)
                double targetZ = -horizontalSpeed;  // Negative Z is NORTH
                double newZ = (currentMotion.z * (1 - accelerationFactor)) + (targetZ * accelerationFactor);
                double newY = Math.max(currentMotion.y, verticalBoost);
                
                entity.setDeltaMovement(
                    currentMotion.x * conserveFactor,
                    newY,
                    newZ
                );
            }
            case SOUTH -> {
                // Plate is on South wall (facing south), push South (same direction)
                double targetZ = horizontalSpeed;  // Positive Z is SOUTH
                double newZ = (currentMotion.z * (1 - accelerationFactor)) + (targetZ * accelerationFactor);
                double newY = Math.max(currentMotion.y, verticalBoost);
                
                entity.setDeltaMovement(
                    currentMotion.x * conserveFactor,
                    newY,
                    newZ
                );
            }
            case EAST -> {
                // Plate is on East wall (facing east), push East (same direction)
                double targetX = horizontalSpeed;  // Positive X is EAST
                double newX = (currentMotion.x * (1 - accelerationFactor)) + (targetX * accelerationFactor);
                double newY = Math.max(currentMotion.y, verticalBoost);
                
                entity.setDeltaMovement(
                    newX,
                    newY,
                    currentMotion.z * conserveFactor
                );
            }
            case WEST -> {
                // Plate is on West wall (facing west), push West (same direction)
                double targetX = -horizontalSpeed;  // Negative X is WEST
                double newX = (currentMotion.x * (1 - accelerationFactor)) + (targetX * accelerationFactor);
                double newY = Math.max(currentMotion.y, verticalBoost);
                
                entity.setDeltaMovement(
                    newX,
                    newY,
                    currentMotion.z * conserveFactor
                );
            }
            default -> {} // Should never happen
        }
        
        // Prevent fall damage
        entity.fallDistance = 0;
        
        // Set flag to confirm physics updates for players
        if (isPlayer) {
            ((Player)entity).hurtMarked = true;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction horizontalDirection = context.getHorizontalDirection();
        
        // If the player clicked on a side face, place vertically (if enabled in config)
        // If vertical conveyors are disabled, all placements will be horizontal
        if (clickedFace.getAxis() != Direction.Axis.Y && Config.verticalConveyorEnabled) {
            // When clicking on a face, the plate should face the opposite direction
            Direction oppositeFace = clickedFace.getOpposite();
            
            BlockState state = this.defaultBlockState()
                .setValue(FACING, oppositeFace)
                .setValue(VERTICAL, true);
                
            return state;
        } else {
            // For horizontal placement, just use the player's facing direction
            BlockState state = this.defaultBlockState()
                .setValue(FACING, horizontalDirection)
                .setValue(VERTICAL, false);
                
            return state;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VERTICAL);
    }
} 