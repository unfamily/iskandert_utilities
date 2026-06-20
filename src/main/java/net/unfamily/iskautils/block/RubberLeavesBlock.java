package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;

/**
 * Rubber leaves block with flammability properties.
 * Note: The decay distance is controlled by the vanilla LeavesBlock system (5 blocks).
 * Additional rubber_wood blocks are placed within the canopy to ensure all leaves stay within range.
 */
public class RubberLeavesBlock extends LeavesBlock {
    public static final MapCodec<RubberLeavesBlock> CODEC = simpleCodec(RubberLeavesBlock::new);
    /** Falling leaf particles (26.x); block texture is untinted — matches rubber_leaves.png. */
    private static final int FALLING_LEAF_PARTICLE_COLOR = ARGB.color(0x14, 0x3D, 0x18);
    
    public RubberLeavesBlock(Properties properties) {
        super(0.01F, properties);
    }
    
    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 80;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return 50;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        ParticleUtils.spawnParticleBelow(
                level,
                pos,
                random,
                ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, FALLING_LEAF_PARTICLE_COLOR));
    }
} 