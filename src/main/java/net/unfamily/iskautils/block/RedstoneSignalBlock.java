package net.unfamily.iskautils.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Redstone Signal Block - A visible block with a small hitbox (4x4x4) that emits redstone signal
 * Removes itself after 3 seconds (60 ticks) and emits redstone signal when placed
 */
public class RedstoneSignalBlock extends Block {

    // Small hitbox: 4x4 base, 4 pixels high, centered
    private static final VoxelShape SIGNAL_SHAPE = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 4.0D, 10.0D);
    private static final int REMOVAL_TICKS = 60; // 3 seconds (60 ticks)

    public RedstoneSignalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SIGNAL_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // No collision
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Emit redstone signal when placed
            level.updateNeighborsAt(pos, this);
            
            // Schedule block removal after 3 seconds
            serverLevel.scheduleTick(pos, this, REMOVAL_TICKS);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Remove the block and emit redstone signal change
        level.removeBlock(pos, false);
        level.updateNeighborsAt(pos, this);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Emit redstone particles
        if (random.nextInt(10) == 0) {
            double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;
            double y = pos.getY() + 0.2D + random.nextDouble() * 0.3D;
            double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D;
            
            // Redstone dust particle (red color)
            level.addParticle(
                new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f),
                x, y, z,
                0.0D, 0.0D, 0.0D
            );
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return false; // We use scheduled ticks instead
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true; // This block emits redstone signal
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 15; // Emit full redstone signal (15)
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 15; // Emit full direct redstone signal (15)
    }
}
