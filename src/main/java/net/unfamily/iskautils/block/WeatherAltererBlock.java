package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.unfamily.iskautils.block.entity.ModBlockEntities;
import net.unfamily.iskautils.block.entity.WeatherAltererBlockEntity;
import net.unfamily.iskautils.Config;

import javax.annotation.Nullable;

/**
 * The Weather Alterer is a block that can change the weather when powered by redstone
 * It has three modes that change with the right click:
 * 0 = sunny (sun) - changes weather to clear/sunny
 * 1 = rain (rain) - changes weather to rain
 * 2 = storm (storm) - changes weather to thunderstorm
 */
public class WeatherAltererBlock extends Block implements EntityBlock {
    
    // Property that represents the mode of the block (0=sun, 1=rain, 2=storm)
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 2);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final MapCodec<WeatherAltererBlock> CODEC = simpleCodec(WeatherAltererBlock::new);
    

    @Override
    public MapCodec<? extends WeatherAltererBlock> codec() {
        return CODEC;
    }
    
    public WeatherAltererBlock(Properties properties) {
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
        
        // Calculate the new mode (cycle between 0, 1, 2)
        int newMode = (currentMode + 1) % 3;
        
        // Update the block state
        level.setBlock(pos, state.setValue(MODE, newMode), Block.UPDATE_ALL);
        
        // Sound when changing the mode
        level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
        
        // Colored feedback message based on the new mode using vanilla format
        Component modeMessage = switch (newMode) {
            case 0 -> Component.translatable("block.iska_utils.weather_alterer.mode.sunny").withStyle(ChatFormatting.YELLOW);
            case 1 -> Component.translatable("block.iska_utils.weather_alterer.mode.rain").withStyle(ChatFormatting.GRAY);
            case 2 -> Component.translatable("block.iska_utils.weather_alterer.mode.storm").withStyle(ChatFormatting.BLUE);
            default -> Component.translatable("block.iska_utils.weather_alterer.mode.unknown");
        };
        
        // Send the feedback message to the player in the action bar
        player.displayClientMessage(modeMessage, true);
        
        return InteractionResult.CONSUME;
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            boolean isPowered = level.hasNeighborSignal(pos);
            
            // Only trigger weather change on rising edge (from unpowered to powered)
            if (isPowered && !state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
                
                // Directly activate the weather changer when powered, like HellfireIgniter
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof WeatherAltererBlockEntity weatherAlterer) {
                    weatherAlterer.activateWeatherChange();
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
        return new WeatherAltererBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.WEATHER_ALTERER_BE.get(),
                WeatherAltererBlockEntity::tick
        );
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> typeCheck, BlockEntityType<E> typeExpected, BlockEntityTicker<? super E> ticker) {
        return typeExpected == typeCheck ? (BlockEntityTicker<A>) ticker : null;
    }
} 