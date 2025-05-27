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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Raft Block - Un blocco sottile che può fluttuare in aria e essere piazzato a parete
 */
public class RaftBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RaftBlock> CODEC = simpleCodec(RaftBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty VERTICAL = BooleanProperty.create("vertical");
    
    // Forma del blocco orizzontale (0.5 pixel di altezza)
    protected static final VoxelShape SHAPE_HORIZONTAL = Block.box(0, 0, 0, 16, 0.5, 16);
    
    // Forme verticali per i vari orientamenti (nord, sud, est, ovest)
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
        // Otteniamo la faccia su cui si sta cliccando
        Direction clickedFace = context.getClickedFace();
        
        // Determiniamo se è un piazzamento verticale (su una parete)
        boolean vertical = clickedFace.getAxis().isHorizontal();
        
        Direction direction;
        
        if (vertical) {
            // Per piazzamenti su parete, utilizziamo la direzione OPPOSTA della faccia cliccata
            // In questo modo: nord -> sud, est -> ovest, ecc.
            direction = clickedFace.getOpposite();
        } else {
            // Per piazzamenti orizzontali (pavimento o soffitto), 
            // usiamo la direzione orizzontale del giocatore
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
    
    // La raft può esistere ovunque, anche sospesa in aria
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }
    
    // Non distruggere la raft se il blocco sotto viene rimosso
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, 
                                 LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return state;
    }

    // Aggiungiamo il metodo per permettere la rotazione con shift+tasto destro
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Verifichiamo se il giocatore sta premendo shift
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                // Otteniamo la direzione corrente
                Direction currentDirection = state.getValue(FACING);
                Direction newDirection = currentDirection.getClockWise();
                
                // Aggiorniamo lo stato del blocco con la nuova direzione, mantenendo lo stato verticale
                level.setBlock(pos, state.setValue(FACING, newDirection), 3);
                
                // Riproduciamo un suono di click quando il blocco viene ruotato
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.3F, 0.8F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        
        return InteractionResult.PASS;
    }
} 