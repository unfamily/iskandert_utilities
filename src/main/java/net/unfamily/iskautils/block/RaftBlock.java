package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Raft Block - Un blocco sottile che galleggia sull'acqua
 */
public class RaftBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RaftBlock> CODEC = simpleCodec(RaftBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    // Forma del blocco basata sul modello (0.5 pixel di altezza)
    protected static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 0.5, 16);

    public RaftBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Controllo se c'è un blocco solido sotto
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        
        // Se c'è acqua sotto, la zattera può galleggiare
        if (belowState.getFluidState().isSource()) {
            return true;
        }
        
        // Altrimenti, verifica se il blocco sotto è solido
        return belowState.isFaceSturdy(level, belowPos, Direction.UP);
    }
    
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, 
                                 LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        // Se il blocco sotto viene rimosso, distruggi la zattera
        if (direction == Direction.DOWN && !this.canSurvive(state, level, currentPos)) {
            return Block.pushEntitiesUp(state, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), level, currentPos);
        }
        
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }
} 