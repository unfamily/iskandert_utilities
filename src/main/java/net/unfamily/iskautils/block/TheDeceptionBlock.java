package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decorative chair block for The Deception. No sitting behavior.
 */
public class TheDeceptionBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TheDeceptionBlock> CODEC = simpleCodec(TheDeceptionBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 9, 14),
            Block.box(1.5, 9, 13.5, 13.5, 16, 15.5));

    public TheDeceptionBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<TheDeceptionBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
