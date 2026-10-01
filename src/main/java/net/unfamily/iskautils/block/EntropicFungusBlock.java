package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.iskautils.item.ModItems;
import org.jetbrains.annotations.NotNull;

/**
 * Mushroom-like plant. Placeable on any sturdy top face; overworld sunlight destroys it into Drop of Entropy.
 */
public class EntropicFungusBlock extends BushBlock {
    public static final MapCodec<EntropicFungusBlock> CODEC = simpleCodec(EntropicFungusBlock::new);
    private static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);

    public EntropicFungusBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!level.isDay() || !level.canSeeSky(pos)) {
            return;
        }
        level.removeBlock(pos, false);
        Block.popResource(level, pos, new ItemStack(ModItems.DROP_OF_ENTROPY.get()));
    }
}
