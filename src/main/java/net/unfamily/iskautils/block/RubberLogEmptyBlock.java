package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.RubberLogEmptyBlockEntity;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Blocco di legno di gomma con sap vuoto.
 * Direzionale verso i 4 punti cardinali.
 * Si ricarica dopo un certo tempo e diventa un blocco pieno.
 */
public class RubberLogEmptyBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(RubberLogEmptyBlock.class);
    public static final MapCodec<RubberLogEmptyBlock> CODEC = simpleCodec(RubberLogEmptyBlock::new);
    
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public RubberLogEmptyBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    
    @Override
    public MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof RubberLogEmptyBlockEntity blockEntity) {
            // Verifica se il tempo di ricarica è scaduto
            if (blockEntity.shouldRefill()) {
                // Converti in un blocco pieno usando il metodo della BlockEntity
                blockEntity.fillWithSap(level, pos, state);
            } else {
                // Schedula il prossimo tick
                level.scheduleTick(pos, this, 20); // Controlla ogni secondo
            }
        }
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        // Schedula il primo tick
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 20); // Controlla dopo 1 secondo
        }
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RubberLogEmptyBlockEntity(pos, state);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 5;
    }
} 