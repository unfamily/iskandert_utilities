package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.Containers;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.MobReaperBlockEntity;
import net.unfamily.iskautils.block.entity.ModBlockEntities;

import javax.annotation.Nullable;

public class MobReaperBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty VERTICAL = BooleanProperty.create("vertical");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<MobReaperBlock> CODEC = simpleCodec(MobReaperBlock::new);

    public MobReaperBlock(Properties properties) {
        super(properties.sound(SoundType.METAL));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(VERTICAL, false)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VERTICAL, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction horizontalDirection = context.getHorizontalDirection();
        Player player = context.getPlayer();

        if (clickedFace.getAxis() != Direction.Axis.Y && Config.verticalConveyorEnabled) {
            // FACING points away from the wall (kill direction), same as horizontal front.
            return this.defaultBlockState()
                    .setValue(FACING, clickedFace)
                    .setValue(VERTICAL, true)
                    .setValue(POWERED, false);
        }

        Direction facing = horizontalDirection;
        if (player != null && player.isShiftKeyDown()) {
            facing = horizontalDirection.getOpposite();
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(VERTICAL, false)
                .setValue(POWERED, false);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    private static boolean isSupportPlate(Block block) {
        return block instanceof VectorBlock;
    }

    private static void updateMountedOnPlate(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide() || state.getValue(VERTICAL)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MobReaperBlockEntity reaper) {
            reaper.setMountedOnPlate(isSupportPlate(level.getBlockState(pos.below()).getBlock()));
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        updateMountedOnPlate(level, pos, state);
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader levelReader, BlockPos pos, BlockPos neighbor) {
        if (!(levelReader instanceof Level level) || level.isClientSide() || state.getValue(VERTICAL)) {
            return;
        }
        if (!neighbor.equals(pos.below()) || !level.getBlockState(neighbor).isAir()) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MobReaperBlockEntity reaper && reaper.isMountedOnPlate()) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation orientation,
                                boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MobReaperBlockEntity reaper) {
                ItemStackHandler handler = reaper.getModuleHandler();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
                        handler.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MobReaperBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MOB_REAPER_BE.get(), MobReaperBlockEntity::tick);
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
        if (!(blockEntity instanceof MobReaperBlockEntity reaperEntity) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        reaperEntity.ensureOwner(serverPlayer.getUUID());
        serverPlayer.openMenu(reaperEntity, pos);
        return InteractionResult.CONSUME;
    }
}
