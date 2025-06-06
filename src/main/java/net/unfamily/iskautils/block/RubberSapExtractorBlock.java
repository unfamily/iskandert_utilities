package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.RubberSapExtractorBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * The Rubber Sap Extractor is a machine that automatically extracts sap from filled rubber logs (RubberLogFilled)
 */
public class RubberSapExtractorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<RubberSapExtractorBlock> CODEC = simpleCodec(RubberSapExtractorBlock::new);
    
    // VoxelShape for the block hitbox (main body)
    private static final VoxelShape BASE_SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    
    // VoxelShape for the upper extension (pipe)
    private static final VoxelShape PIPE_NORTH = Block.box(5, 16, 0, 11, 27, 16);
    private static final VoxelShape PIPE_SOUTH = Block.box(5, 16, 0, 11, 27, 16);
    private static final VoxelShape PIPE_WEST = Block.box(0, 16, 5, 16, 27, 11);
    private static final VoxelShape PIPE_EAST = Block.box(0, 16, 5, 16, 27, 11);
    
    // combined VoxelShape
    private static final VoxelShape SHAPE_NORTH = Shapes.or(BASE_SHAPE, PIPE_NORTH);
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(BASE_SHAPE, PIPE_SOUTH);
    private static final VoxelShape SHAPE_WEST = Shapes.or(BASE_SHAPE, PIPE_WEST);
    private static final VoxelShape SHAPE_EAST = Shapes.or(BASE_SHAPE, PIPE_EAST);

    @Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}
    
    public RubberSapExtractorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, Boolean.FALSE)
        );
    }
    
    @Override
    public MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
	public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
		return adjacentBlockState.getBlock() == this ? true : super.skipRendering(state, adjacentBlockState, side);
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}
    
	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return true;
	}

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // the block must face the opposite direction to the player
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(POWERED, Boolean.FALSE);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Handles the right click on the block
     */
    @Override   
    public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        if (world.getBlockEntity(pos) instanceof RubberSapExtractorBlockEntity blockEntity) {
            ItemStack sapStack = blockEntity.getItem(0);
            if (!sapStack.isEmpty()) {
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.5;
                
                // create an item entity for the whole sap stack
                ItemStack droppedStack = sapStack.copy();
                ItemEntity itemEntity = new ItemEntity(world, x, y, z, droppedStack);
                itemEntity.setDefaultPickUpDelay();
                world.addFreshEntity(itemEntity);
                
                // remove all sap from inventory
                blockEntity.removeItem(0, sapStack.getCount());
                
                // extraction sound
                world.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 0.5f, 1.0f);
                
                return InteractionResult.CONSUME;
            }
        }
        
        return InteractionResult.PASS;
    }
    

    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RubberSapExtractorBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        
        return createTickerHelper(blockEntityType, ModBlockEntities.RUBBER_SAP_EXTRACTOR.get(), 
                RubberSapExtractorBlockEntity::serverTick);
    }
    
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
    
    // when the block receives a redstone signal
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        
        if (!level.isClientSide()) {
            boolean isPowered = level.hasNeighborSignal(pos);
            
            // if the powered state has changed
            if (isPowered != state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, isPowered), Block.UPDATE_ALL);
                
                // update the BlockEntity
                if (level.getBlockEntity(pos) instanceof RubberSapExtractorBlockEntity blockEntity) {
                    blockEntity.onPoweredStateChanged(isPowered);
                }
            }
        }
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // release items from inventory when the block is destroyed
            if (level.getBlockEntity(pos) instanceof RubberSapExtractorBlockEntity blockEntity) {
                for (int i = 0; i < blockEntity.getItems().size(); i++) {
                    ItemStack stack = blockEntity.getItems().get(i);
                    if (!stack.isEmpty()) {
                        // create an item entity in the world
                        double x = pos.getX() + 0.5;
                        double y = pos.getY() + 0.5;
                        double z = pos.getZ() + 0.5;
                        
                        ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
                        itemEntity.setDefaultPickUpDelay();
                        level.addFreshEntity(itemEntity);
                    }
                }
            }
            
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
} 