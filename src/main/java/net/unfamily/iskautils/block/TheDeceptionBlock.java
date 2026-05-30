package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cursed artifact chair block for The Deception.
 */
public class TheDeceptionBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TheDeceptionBlock> CODEC = simpleCodec(TheDeceptionBlock::new);

    private static final VoxelShape SHAPE_NORTH = buildShape(Direction.NORTH);
    private static final VoxelShape SHAPE_EAST = buildShape(Direction.EAST);
    private static final VoxelShape SHAPE_SOUTH = buildShape(Direction.SOUTH);
    private static final VoxelShape SHAPE_WEST = buildShape(Direction.WEST);

    public TheDeceptionBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<TheDeceptionBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return TheDeceptionSeatUtil.trySit(state, level, pos, player)
                ? InteractionResult.SUCCESS_SERVER
                : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return TheDeceptionSeatUtil.trySit(state, level, pos, player)
                ? InteractionResult.SUCCESS_SERVER
                : InteractionResult.PASS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        TheDeceptionSeatUtil.stopSitAt(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForFacing(state.getValue(FACING));
    }

    private static VoxelShape shapeForFacing(Direction facing) {
        return switch (facing) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    /** Matches {@code models/block/the_deception.json} element bounds (north-facing model). */
    private static VoxelShape buildShape(Direction facing) {
        return Shapes.or(
                element(2, 7, 2, 14, 9, 14, facing),
                element(3, 0, 3, 5, 7, 5, facing),
                element(3, 0, 11, 5, 7, 13, facing),
                element(11, 0, 11, 13, 7, 13, facing),
                element(11, 0, 3, 13, 7, 5, facing),
                element(3, 8, 14, 4, 14, 15, facing),
                element(12, 8, 14, 13, 14, 15, facing),
                element(1.5, 13.5, 13.5, 13.5, 23.5, 15.5, facing));
    }

    private static VoxelShape element(
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            Direction facing) {
        double[] cornerA = rotateXZ(x1, z1, facing);
        double[] cornerB = rotateXZ(x2, z2, facing);
        return Block.box(
                Math.min(cornerA[0], cornerB[0]), y1, Math.min(cornerA[1], cornerB[1]),
                Math.max(cornerA[0], cornerB[0]), y2, Math.max(cornerA[1], cornerB[1]));
    }

    private static double[] rotateXZ(double x, double z, Direction facing) {
        return switch (facing) {
            case NORTH -> new double[] {x, z};
            case SOUTH -> new double[] {16.0D - x, 16.0D - z};
            case EAST -> new double[] {16.0D - z, x};
            case WEST -> new double[] {z, 16.0D - x};
            default -> new double[] {x, z};
        };
    }
}
