package net.unfamily.iskautils.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.EntropicSoilBlockEntity;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.util.EntropicSoilUtil;
import org.jetbrains.annotations.Nullable;

public class EntropicSoilBlock extends BaseEntityBlock {
    public static final MapCodec<EntropicSoilBlock> CODEC = simpleCodec(EntropicSoilBlock::new);

    public EntropicSoilBlock(BlockBehaviour.Properties properties) {
        super(properties.randomTicks().sound(SoundType.GRASS));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        EntropicSoilUtil.trySlowSpread(
                level,
                pos,
                random,
                Config.entropicSoilSlowSpreadChance,
                Config.entropicSoilEdgeSpreadChance,
                Config.entropicSoilDirtSpreadChance);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction direction) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EntropicSoilBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.ENTROPIC_SOIL_BE.get(), EntropicSoilBlockEntity::tickServer);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server) {
            EntropicSoilBlockEntity.onSoilPlaced(server, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);
        if (level instanceof ServerLevel server) {
            EntropicSoilBlockEntity.requestAcceleratedWave(server, pos);
        }
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        if (level instanceof ServerLevel server) {
            EntropicSoilBlockEntity.requestAcceleratedWave(server, pos);
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == type ? (BlockEntityTicker<A>) ticker : null;
    }
}
