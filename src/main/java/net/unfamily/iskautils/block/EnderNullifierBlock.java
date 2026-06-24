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
import net.unfamily.iskautils.block.entity.EnderNullifierBlockEntity;
import net.unfamily.iskautils.block.entity.EnderNullifierRedstoneMode;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class EnderNullifierBlock extends DirectionalBlock implements EntityBlock {
    public static final BooleanProperty ON = BooleanProperty.create("on");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<EnderNullifierBlock> CODEC = simpleCodec(EnderNullifierBlock::new);

    public EnderNullifierBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ON, false)
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
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ON, false)
                .setValue(POWERED, false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
                nullifier.syncSpatialIndexFromState();
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
}
