package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;

/**
 * The Weather Detector is a block that emits a redstone signal based on the weather
 * It has four modes that change with the right click:
 * 0 = sunny (sun) - emits a signal when there is sun
 * 1 = rain (rain) - emits a signal when it rains
 * 2 = storm (storm) - emits a signal when there is a storm
 * 3 = both (both) - emits a signal when it rains or there is a storm
 */
public class WeatherDetectorBlock extends Block {
    
    // Property that represents the mode of the block (0=sun, 1=rain, 2=storm, 3=both)
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 3);
    public static final MapCodec<WeatherDetectorBlock> CODEC = simpleCodec(WeatherDetectorBlock::new);
    
    // VoxelShape for the block (similar to the daylight detector)
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    @Override
    public MapCodec<? extends WeatherDetectorBlock> codec() {
        return CODEC;
    }
    
    public WeatherDetectorBlock(Properties properties) {
        super(properties);
        // Register the default mode (0 = sun)
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(MODE, 0));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
            case 0 -> Component.translatable("block.iska_utils.weather_detector.mode.sunny").withStyle(ChatFormatting.YELLOW);
            case 1 -> Component.translatable("block.iska_utils.weather_detector.mode.rain").withStyle(ChatFormatting.GRAY);
            case 2 -> Component.translatable("block.iska_utils.weather_detector.mode.storm").withStyle(ChatFormatting.BLUE);
            case 3 -> Component.translatable("block.iska_utils.weather_detector.mode.both").withStyle(ChatFormatting.DARK_PURPLE);
            default -> Component.translatable("block.iska_utils.weather_detector.mode.unknown");
        };
        
        // Send the feedback message to the player in the action bar
        player.displayClientMessage(modeMessage, true);
        
        // Force an update of the redstone signal
        level.updateNeighborsAt(pos, this);
        
        return InteractionResult.CONSUME;
    }
    
    /**
     * Checks if the block is emitting a redstone signal
     * based on the weather and the current mode
     */
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }
    
    /**
     * Returns the power of the redstone signal (15 if active, 0 if inactive)
     */
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return getRedstoneStrength(state, level, pos);
    }
    
    /**
     * Returns the power of the indirect redstone signal (15 if active, 0 if inactive)
     */
    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return direction == net.minecraft.core.Direction.UP ? getRedstoneStrength(state, level, pos) : 0;
    }
    
    /**
     * Calculates the power of the redstone signal based on the weather
     * and the current mode
     */
    private int getRedstoneStrength(BlockState state, BlockGetter level, BlockPos pos) {
        if (!(level instanceof Level)) {
            return 0;
        }
        
        Level world = (Level) level;
        int mode = state.getValue(MODE);
        
        // Check the weather conditions based on the mode
        boolean isActive = switch (mode) {
            case 0 -> !world.isRaining(); // Mode sun: active when there is no rain
            case 1 -> world.isRaining() && !world.isThundering(); // Mode rain: active when it rains but there is no storm
            case 2 -> world.isThundering(); // Mode storm: active when there is a storm
            case 3 -> world.isRaining() || world.isThundering(); // Mode both: active when it rains or there is a storm
            default -> false;
        };
        
        return isActive ? 15 : 0;
    }
    
    /**
     * Updates the redstone signal when the weather changes
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        // Calculate the current power of the signal
        int currentPower = getRedstoneStrength(state, level, pos);
        
        // Simple and reliable method: update the neighbors
        // when the block receives a tick and the weather is relevant
        level.updateNeighborsAt(pos, this);
        
        // Schedule the next tick to continue checking the weather
        level.scheduleTick(pos, this, 20); // Check every second (20 tick)
    }
    
    /**
     * Registers the block to receive tick when it is loaded in the world
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            // Schedule the first tick when the block is placed
            level.scheduleTick(pos, this, 20); // Check after 1 second (20 tick)
        }
    }
} 