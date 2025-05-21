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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

import java.util.List;
import java.util.Collections;
import java.util.function.Supplier;

public class VectorBlock extends HorizontalDirectionalBlock {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<VectorBlock> CODEC = simpleCodec(VectorBlock::new);
    
    // Block shape based on the new model (0.5 pixel height)
    protected static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 0.5, 16);
    
    // Variable for block speed
    private final Supplier<Double> speedSupplier;
    
    // Whether this vector plate affects only players
    private final boolean affectsPlayers;

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public VectorBlock(Properties properties, Supplier<Double> speedSupplier, boolean affectsPlayers) {
        super(properties.sound(SoundType.STONE)); // Set the sound type to STONE
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
            if (!level.isClientSide) {
                // Get current direction and calculate next direction (rotate clockwise)
                Direction currentDirection = state.getValue(FACING);
                Direction newDirection = currentDirection.getClockWise();
                
                // Update the block state with the new direction
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
        // Use the same shape regardless of direction since the model is symmetrical
        return SHAPE;
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
     * Always make the bottom face of the block visible
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        // Always show the bottom face
        return side != Direction.DOWN && adjacentBlockState.is(this) ? true : super.skipRendering(state, adjacentBlockState, side);
    }
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Ensure the block always drops itself
        return Collections.singletonList(new ItemStack(this));
    }
    
    /**
     * Check if the block can be placed at this position
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        
        // Check if the block below is a solid block that can support the vector block
        VoxelShape belowShape = belowState.getCollisionShape(level, belowPos);
        
        // If the block below is empty or not complete at the top, it cannot support our block
        if (belowShape.isEmpty() || !hasFullFaceOnTop(belowShape)) {
            return false;
        }
        
        return true;
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
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
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
        // Check if the changed block is below our vector block
        else if (fromPos.equals(pos.below())) {
            // Check if the block can still survive
            if (!this.canSurvive(state, level, pos) && !level.isClientSide) {
                level.destroyBlock(pos, true); // Break the vector block and drop the item
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
            
            // Calculate speed based on the configuration
            double speed = speedSupplier.get();
            
            // If player is sneaking (shift), don't apply movement
            if (isPlayer && ((Player)entity).isShiftKeyDown()) {
                return;
            }
            
            // players might require a different approach for pushing
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
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Get player direction and use it for the block facing
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
} 