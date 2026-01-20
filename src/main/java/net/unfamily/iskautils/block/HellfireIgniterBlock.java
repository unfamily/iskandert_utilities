package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.HellfireIgniterBlockEntity;
import net.unfamily.iskautils.block.entity.ModBlockEntities;

import javax.annotation.Nullable;

public class HellfireIgniterBlock extends DirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<HellfireIgniterBlock> CODEC = simpleCodec(HellfireIgniterBlock::new);

    public HellfireIgniterBlock(Properties properties) {
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
        
        // If player is crouching (holding shift), place in opposite direction
        if (player != null && player.isShiftKeyDown()) {
            // Opposite direction: use the direction player is looking at directly (without getOpposite)
            facing = context.getNearestLookingDirection();
        } else {
            // Normal behavior: opposite direction to where player is looking
            facing = context.getNearestLookingDirection().getOpposite();
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
        if (!level.isClientSide) {
            boolean isPowered = level.hasNeighborSignal(pos);
            
            if (isPowered != state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, isPowered), 3);
                
                // Only handle PULSE mode here (other modes are handled in tick)
                if (isPowered) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof HellfireIgniterBlockEntity igniter) {
                        // Only ignite in PULSE mode (mode 3)
                        if (igniter.getRedstoneMode() == 3) {
                            igniter.ignite();
                        }
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HellfireIgniterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.HELLFIRE_IGNITER_BE.get(),
                HellfireIgniterBlockEntity::tick
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
        
        // Shift+click to cycle redstone mode
        if (player.isShiftKeyDown()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof HellfireIgniterBlockEntity igniter) {
                // Vanilla-like mode: always force PULSE mode (3) and don't show message
                if (Config.hellfireIgniterVanillaLike) {
                    igniter.setRedstoneMode(3); // Force PULSE mode
                    // Don't show message in vanilla-like mode
                } else {
                    igniter.cycleRedstoneMode();
                    int mode = igniter.getRedstoneMode();
                    Component modeName = switch (mode) {
                        case 0 -> Component.translatable("gui.iska_utils.generic.redstone_mode.none");
                        case 1 -> Component.translatable("gui.iska_utils.generic.redstone_mode.low");
                        case 2 -> Component.translatable("gui.iska_utils.generic.redstone_mode.high");
                        case 3 -> Component.translatable("gui.iska_utils.generic.redstone_mode.pulse");
                        case 4 -> Component.translatable("gui.iska_utils.generic.redstone_mode.disabled");
                        default -> Component.literal("Unknown");
                    };
                    player.displayClientMessage(Component.translatable("gui.iska_utils.generic.redstone_mode", modeName), true);
                }
                return InteractionResult.CONSUME;
            }
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * Supporto per le capabilities di energia da altre mod
     */
    @Nullable
    public <T> T getCapability(BlockState state, Level level, BlockPos pos, BlockCapability<T, Direction> capability, @Nullable Direction facing) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        
        if (capability == Capabilities.EnergyStorage.BLOCK) {
            if (blockEntity instanceof HellfireIgniterBlockEntity igniter) {
                IEnergyStorage energyStorage = igniter.getEnergyStorage();
                return (T) energyStorage;
            }
        }
        
        return null;
    }
} 