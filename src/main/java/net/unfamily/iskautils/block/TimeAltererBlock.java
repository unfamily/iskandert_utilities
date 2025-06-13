package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.TimeAltererBlockEntity;
import net.unfamily.iskautils.Config;

import javax.annotation.Nullable;

/**
 * The Time Alterer is a block that can change the time when powered by redstone
 * It has four modes that change with the right click:
 * 0 = day - sets time to day
 * 1 = night - sets time to night
 * 2 = noon - sets time to noon
 * 3 = midnight - sets time to midnight
 */
public class TimeAltererBlock extends Block implements EntityBlock {
    
    // Property that represents the mode of the block (0=day, 1=night, 2=noon, 3=midnight)
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 3);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<TimeAltererBlock> CODEC = simpleCodec(TimeAltererBlock::new);
    

    @Override
    public MapCodec<? extends TimeAltererBlock> codec() {
        return CODEC;
    }
    
    public TimeAltererBlock(Properties properties) {
        super(properties);
        // Register default states
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(MODE, 0)
                .setValue(POWERED, Boolean.FALSE));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE, POWERED);
    }

    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    /**
     * Handles the right click on the block to change the mode
     */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        
        // Get the current mode
        int currentMode = state.getValue(MODE);
        
        // Calculate the new mode (cycle between 0, 1, 2, 3)
        int newMode = (currentMode + 1) % 4;
        
        // Update the block state
        level.setBlock(pos, state.setValue(MODE, newMode), Block.UPDATE_ALL);
        
        // Sound when changing the mode
        level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
        
        // Colored feedback message based on the new mode using vanilla format
        Component modeMessage = switch (newMode) {
            case 0 -> Component.translatable("block.iska_utils.time_alterer.mode.day").withStyle(ChatFormatting.WHITE);
            case 1 -> Component.translatable("block.iska_utils.time_alterer.mode.night").withStyle(ChatFormatting.BLUE);
            case 2 -> Component.translatable("block.iska_utils.time_alterer.mode.noon").withStyle(ChatFormatting.YELLOW);
            case 3 -> Component.translatable("block.iska_utils.time_alterer.mode.midnight").withStyle(ChatFormatting.DARK_PURPLE);
            default -> Component.translatable("block.iska_utils.time_alterer.mode.unknown").withStyle(ChatFormatting.DARK_RED);
        };
        
        // Send the feedback message to the player in the action bar
        player.displayClientMessage(modeMessage, true);
        
        return InteractionResult.CONSUME;
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            boolean isPowered = level.hasNeighborSignal(pos);
            
            // Only trigger time change on rising edge (from unpowered to powered)
            if (isPowered && !state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
                
                // Directly activate the time changer when powered, like HellfireIgniter
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof TimeAltererBlockEntity timeAlterer) {
                    timeAlterer.activateTimeChange();
                }
                
            } else if (!isPowered && state.getValue(POWERED)) {
                // Update powered state when signal is lost
                level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
            }
        }
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TimeAltererBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.TIME_ALTERER_BE.get(),
                TimeAltererBlockEntity::tick
        );
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> typeCheck, BlockEntityType<E> typeExpected, BlockEntityTicker<? super E> ticker) {
        return typeExpected == typeCheck ? (BlockEntityTicker<A>) ticker : null;
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
            if (blockEntity instanceof TimeAltererBlockEntity timeAlterer) {
                IEnergyStorage energyStorage = timeAlterer.getEnergyStorage();
                return (T) energyStorage;
            }
        }
        
        return null;
    }
} 