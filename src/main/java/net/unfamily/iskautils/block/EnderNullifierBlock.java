package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.iskautils.block.entity.EnderNullifierBlockEntity;
import net.unfamily.iskautils.block.entity.EnderNullifierRedstoneMode;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class EnderNullifierBlock extends DirectionalBlock implements EntityBlock {
    public static final BooleanProperty ON = BooleanProperty.create("on");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<EnderNullifierBlock> CODEC = simpleCodec(EnderNullifierBlock::new);

    private static final VoxelShape SHAPE_NORTH = buildShape(Direction.NORTH);
    private static final VoxelShape SHAPE_EAST = buildShape(Direction.EAST);
    private static final VoxelShape SHAPE_SOUTH = buildShape(Direction.SOUTH);
    private static final VoxelShape SHAPE_WEST = buildShape(Direction.WEST);
    private static final VoxelShape SHAPE_UP = buildShape(Direction.UP);
    private static final VoxelShape SHAPE_DOWN = buildShape(Direction.DOWN);

    public EnderNullifierBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ON, true)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ON, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ON, true)
                .setValue(POWERED, powered);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnderNullifierBlockEntity nullifier) {
                nullifier.reconcileEffectiveState();
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof EnderNullifierBlockEntity nullifier) {
            nullifier.clearSpatialIndex();
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
        if (level.isClientSide()) {
            return;
        }

        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) {
            return;
        }

        BlockState updated = state.setValue(POWERED, powered);
        level.setBlock(pos, updated, 3);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof EnderNullifierBlockEntity nullifier) {
            nullifier.onRedstoneChanged(level, pos, updated, powered);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnderNullifierBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.ENDER_NULLIFIER_BE.get(),
                EnderNullifierBlockEntity::tick
        );
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> typeCheck, BlockEntityType<E> typeExpected, BlockEntityTicker<? super E> ticker) {
        return typeExpected == typeCheck ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof EnderNullifierBlockEntity nullifier)) {
            return InteractionResult.PASS;
        }

        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.4F, 1.0F);

        if (player.isShiftKeyDown()) {
            nullifier.cycleRedstoneMode(level, pos, level.getBlockState(pos));
            EnderNullifierRedstoneMode mode = nullifier.getRedstoneMode();
            Component modeName = switch (mode) {
                case MANUAL -> Component.translatable("gui.iska_utils.generic.redstone_mode.manual");
                case LOW -> Component.translatable("gui.iska_utils.generic.redstone_mode.low");
                case HIGH -> Component.translatable("gui.iska_utils.generic.redstone_mode.high");
                case PULSE -> Component.translatable("gui.iska_utils.generic.redstone_mode.pulse");
            };
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSystemChatPacket(
                        Component.translatable("gui.iska_utils.generic.redstone_mode", modeName), true));
            }
            return InteractionResult.CONSUME;
        }

        nullifier.toggleManualEnabled(level, pos, level.getBlockState(pos));
        Component message = nullifier.isManualEnabled()
                ? Component.translatable("message.iska_utils.ender_nullifier.enabled").withStyle(ChatFormatting.GREEN)
                : Component.translatable("message.iska_utils.ender_nullifier.disabled").withStyle(ChatFormatting.RED);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSystemChatPacket(message, true));
        }
        return InteractionResult.CONSUME;
    }

    private static VoxelShape shapeForFacing(Direction facing) {
        return switch (facing) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            default -> SHAPE_NORTH;
        };
    }

    /** Matches {@code models/block/ender_nullifier_off.json} element bounds (north-facing model). */
    private static VoxelShape buildShape(Direction facing) {
        return Shapes.or(
                element(2, 3, 14, 14, 15, 16, facing),
                element(4, 5, 11, 12, 13, 14, facing));
    }

    private static VoxelShape element(
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            Direction facing) {
        double[] cornerA = transformNorthModel(x1, y1, z1, facing);
        double[] cornerB = transformNorthModel(x2, y2, z2, facing);
        return Block.box(
                Math.min(cornerA[0], cornerB[0]), Math.min(cornerA[1], cornerB[1]), Math.min(cornerA[2], cornerB[2]),
                Math.max(cornerA[0], cornerB[0]), Math.max(cornerA[1], cornerB[1]), Math.max(cornerA[2], cornerB[2]));
    }

    private static double[] transformNorthModel(double x, double y, double z, Direction facing) {
        return switch (facing) {
            case NORTH -> new double[] {x, y, z};
            case SOUTH -> new double[] {16.0D - x, y, 16.0D - z};
            case EAST -> new double[] {16.0D - z, y, x};
            case WEST -> new double[] {z, y, 16.0D - x};
            case DOWN -> new double[] {x, z, 16.0D - y};
            case UP -> new double[] {x, 16.0D - z, y};
        };
    }
}
