package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.SacredRubberSaplingBlockEntity;
import net.unfamily.iskautils.worldgen.tree.SacredRubberTreeGrower;
import net.unfamily.iskautils.worldgen.tree.SacredRubberTreeScale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Class for the sacred rubber tree sapling.
 * Requires exactly 15 bonemeal uses to grow (no automatic growth).
 * Uses procedural growth via BlockEntity.
 */
public class SacredRubberSaplingBlock extends SaplingBlock implements net.minecraft.world.level.block.EntityBlock {
    public static final MapCodec<SacredRubberSaplingBlock> CODEC = simpleCodec(SacredRubberSaplingBlock::new);
    
    // Property to track the number of bonemeal used (0-15)
    public static final IntegerProperty BONEMEAL_COUNT = IntegerProperty.create("bonemeal_count", 0, 15);
    public static final BooleanProperty MEGA = BooleanProperty.create("mega");

    public static SacredRubberTreeScale getScale(BlockState state) {
        return state.getValue(MEGA) ? SacredRubberTreeScale.MEGA : SacredRubberTreeScale.NORMAL;
    }

    public static boolean isGrowthEnabled(BlockState state) {
        return getScale(state) == SacredRubberTreeScale.MEGA
                ? Config.sacredRubberMegaTreeGrowthEnabled
                : Config.sacredRubberTreeGrowthEnabled;
    }
    
    // Empty TreeGrower that doesn't grow anything (for now)
    private static final TreeGrower EMPTY_GROWER = new TreeGrower(
            "iska_utils:sacred_rubber",  // TreeGrower ID
            Optional.empty(),            // No mega feature
            Optional.empty(),            // No standard configuration
            Optional.empty()            // No fancy configuration
    );
    
    public SacredRubberSaplingBlock(Properties properties) {
        super(EMPTY_GROWER, properties);
        // Register default state with bonemeal_count = 0
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BONEMEAL_COUNT, 0)
                .setValue(MEGA, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BONEMEAL_COUNT);
        builder.add(MEGA);
    }
    
    @Override
    public @NotNull MapCodec<? extends SaplingBlock> codec() {
        return CODEC;
    }
    
    @Override
    protected boolean mayPlaceOn(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        // Check that the ground below is valid (not water or lava)
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || 
               state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || 
               state.is(Blocks.FARMLAND);
    }
    
    /**
     * Override performBonemeal to track bonemeal usage
     * Requires exactly 15 bonemeal uses before growth
     */
    @Override
    public void performBonemeal(@NotNull ServerLevel level, @NotNull RandomSource random, @NotNull BlockPos pos, @NotNull BlockState state) {
        if (level.isClientSide || !isGrowthEnabled(state)) {
            return;
        }
        
        int currentCount = state.getValue(BONEMEAL_COUNT);
        int newCount = currentCount + 1;
        
        // Update the counter
        BlockState newState = state.setValue(BONEMEAL_COUNT, Math.min(newCount, 15));
        level.setBlock(pos, newState, 3);
        
        // If we reached 15 bonemeal, the BlockEntity will start procedural growth
        // We don't do anything here, the BlockEntity will handle the growth
    }
    
    /**
     * Override isBonemealSuccess to allow bonemeal usage only if count < 15
     */
    @Override
    public boolean isBonemealSuccess(@NotNull Level level, @NotNull RandomSource random, @NotNull BlockPos pos, @NotNull BlockState state) {
        return isGrowthEnabled(state) && state.getValue(BONEMEAL_COUNT) < 15;
    }
    
    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }
    
    /**
     * Called when the block is placed
     * Schedules the first tick to ensure BlockEntity gets ticked regularly
     */
    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Schedule first tick after 10 ticks
            serverLevel.scheduleTick(pos, this, 10);
        }
    }
    
    /**
     * Called periodically (every 10 ticks) to ensure BlockEntity gets ticked
     * This provides a constant tick rate instead of relying on randomTicks
     */
    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        // Get the BlockEntity and tick it manually
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SacredRubberSaplingBlockEntity saplingEntity) {
            SacredRubberSaplingBlockEntity.tick(level, pos, state, saplingEntity);
        }
        
        // Schedule next tick in 10 ticks
        level.scheduleTick(pos, this, 10);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new SacredRubberSaplingBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.SACRED_RUBBER_SAPLING_BE.get(),
                SacredRubberSaplingBlockEntity::tick
        );
    }
    
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}
