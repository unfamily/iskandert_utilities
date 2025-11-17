package net.unfamily.iskautils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Burning Flame Block - A block that provides light like a torch
 * and is fragile, being washed away by liquids.
 */
public class BurningFlameBlock extends Block {

    // Flame shape: 6x6 base, 11 pixels high, positioned 4 pixels above ground
    private static final VoxelShape FLAME_SHAPE = Block.box(5.0D, 4.0D, 5.0D, 11.0D, 15.0D, 11.0D);

    public BurningFlameBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLAME_SHAPE;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
	public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
		return adjacentBlockState.getBlock() == this ? true : super.skipRendering(state, adjacentBlockState, side);
	}

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);

        // Check if any neighboring block is a liquid
        for (var direction : net.minecraft.core.Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            FluidState fluidState = level.getFluidState(adjacentPos);

            // If there's a source fluid or flowing fluid adjacent, remove this block
            if (!fluidState.isEmpty() && (fluidState.isSource() || fluidState.getAmount() > 0)) {
                level.destroyBlock(pos, false);
                return;
            }
        }

        // Also check if this block position has fluid
        FluidState ownFluidState = level.getFluidState(pos);
        if (!ownFluidState.isEmpty()) {
            level.destroyBlock(pos, false);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        super.randomTick(state, level, pos, random);

        // Check for fluids every random tick
        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty()) {
            level.destroyBlock(pos, false);
            return;
        }

        // Check adjacent fluids
        for (var direction : net.minecraft.core.Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            FluidState adjacentFluid = level.getFluidState(adjacentPos);

            if (!adjacentFluid.isEmpty() && (adjacentFluid.isSource() || adjacentFluid.getAmount() > 0)) {
                level.destroyBlock(pos, false);
                return;
            }
        }
    }
}
