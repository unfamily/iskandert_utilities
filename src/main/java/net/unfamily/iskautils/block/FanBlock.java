package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.iskautils.block.entity.FanBlockEntity;
import net.unfamily.iskautils.block.entity.ModBlockEntities;

import javax.annotation.Nullable;

public class FanBlock extends DirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<FanBlock> CODEC = simpleCodec(FanBlock::new);

    public FanBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        Direction facing;
        
        // Default behavior: place in opposite direction (inverted)
        // If player is crouching (holding shift), place in normal direction
        if (player != null && player.isShiftKeyDown()) {
            // Normal direction: opposite direction to where player is looking
            facing = context.getNearestLookingDirection().getOpposite();
        } else {
            // Inverted behavior: use the direction player is looking at directly (without getOpposite)
            facing = context.getNearestLookingDirection();
        }
        
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, Boolean.FALSE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        // Don't update POWERED state here - it's handled in tick based on advanced redstone logic
        // This prevents conflicts between primitive and advanced redstone logic
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FanBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.FAN_BE.get(),
                FanBlockEntity::tick
        );
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> typeCheck, BlockEntityType<E> typeExpected, BlockEntityTicker<? super E> ticker) {
        return typeExpected == typeCheck ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof FanBlockEntity fanEntity) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        // Check if clicked from the back of the block (opposite to facing direction)
        Direction facing = state.getValue(FACING);
        Direction clickDirection = hitResult.getDirection();
        boolean isFromBack = clickDirection == facing.getOpposite();
        
        // If clicked from back, reset the message flag
        if (isFromBack) {
            fanEntity.resetBackMessage();
        } else {
            // If not from back, show message on first click, allow opening on second click
            if (!fanEntity.hasShownBackMessage()) {
                // First click: show warning message in yellow action bar
                net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.translatable("message.iska_utils.fan.use_back_side")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW);
                serverPlayer.displayClientMessage(message, true); // true = action bar
                fanEntity.setHasShownBackMessage(true);
                return InteractionResult.CONSUME;
            }
            // Second click: allow opening even if not from back
        }

        // Open GUI
        serverPlayer.openMenu(fanEntity, pos);
        return InteractionResult.CONSUME;
    }
}
