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
 * Il Rubber Sap Extractor è un macchinario che estrae automaticamente
 * la linfa dai tronchi di gomma pieni (RubberLogFilled)
 */
public class RubberSapExtractorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<RubberSapExtractorBlock> CODEC = simpleCodec(RubberSapExtractorBlock::new);
    
    // VoxelShape per la hitbox del blocco (corpo principale)
    private static final VoxelShape BASE_SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    
    // VoxelShape per l'estensione superiore (tubo)
    private static final VoxelShape PIPE_NORTH = Block.box(5, 16, 0, 11, 27, 16);
    private static final VoxelShape PIPE_SOUTH = Block.box(5, 16, 0, 11, 27, 16);
    private static final VoxelShape PIPE_WEST = Block.box(0, 16, 5, 16, 27, 11);
    private static final VoxelShape PIPE_EAST = Block.box(0, 16, 5, 16, 27, 11);
    
    // VoxelShape combinati
    private static final VoxelShape SHAPE_NORTH = Shapes.or(BASE_SHAPE, PIPE_NORTH);
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(BASE_SHAPE, PIPE_SOUTH);
    private static final VoxelShape SHAPE_WEST = Shapes.or(BASE_SHAPE, PIPE_WEST);
    private static final VoxelShape SHAPE_EAST = Shapes.or(BASE_SHAPE, PIPE_EAST);
    
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
		return 15;
	}
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Il blocco deve affacciarsi nella direzione opposta a quella in cui il giocatore sta guardando
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
     * Gestisce il click destro sul blocco
     */
    @Override   
    public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        if (world.getBlockEntity(pos) instanceof RubberSapExtractorBlockEntity blockEntity) {
            // Verifichiamo se c'è del sap da estrarre
            ItemStack sapStack = blockEntity.removeItem(0, 1);
            if (!sapStack.isEmpty()) {
                // Creiamo un'entità per il sap estratto
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.5;
                
                ItemEntity itemEntity = new ItemEntity(world, x, y, z, sapStack);
                itemEntity.setDefaultPickUpDelay();
                world.addFreshEntity(itemEntity);
                
                // Suono di estrazione
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
    
    // Quando il blocco riceve un segnale di redstone
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        
        if (!level.isClientSide()) {
            boolean isPowered = level.hasNeighborSignal(pos);
            
            // Se lo stato di powered è cambiato
            if (isPowered != state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, isPowered), Block.UPDATE_ALL);
                
                // Aggiorna il BlockEntity
                if (level.getBlockEntity(pos) instanceof RubberSapExtractorBlockEntity blockEntity) {
                    blockEntity.onPoweredStateChanged(isPowered);
                }
            }
        }
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Rilascia gli item dall'inventario quando il blocco viene distrutto
            if (level.getBlockEntity(pos) instanceof RubberSapExtractorBlockEntity blockEntity) {
                for (int i = 0; i < blockEntity.getItems().size(); i++) {
                    ItemStack stack = blockEntity.getItems().get(i);
                    if (!stack.isEmpty()) {
                        // Crea un'entità item nel mondo
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