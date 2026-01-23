package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.RubberLogSacredBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sacred rubber log block used for the grid structure in sacred rubber trees.
 * Uses BlockEntity to store fixed root coordinates and checks only that position.
 * Not visible in creative tab and drops normal rubber_log.
 */
public class RubberLogSacredBlock extends RotatedPillarBlock implements net.minecraft.world.level.block.EntityBlock {
    public static final MapCodec<RubberLogSacredBlock> CODEC = simpleCodec(RubberLogSacredBlock::new);
    
    public RubberLogSacredBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }
    
    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new RubberLogSacredBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.RUBBER_LOG_SACRED_BE.get(),
                RubberLogSacredBlockEntity::tick
        );
    }
    
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // Drop normal rubber_log instead of this block
        return List.of(new ItemStack(ModBlocks.RUBBER_LOG.get()));
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 15;
    }
    
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 10;
    }
}
