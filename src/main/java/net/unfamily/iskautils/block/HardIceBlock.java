package net.unfamily.iskautils.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hard Ice Block - Indestructible block that cannot be broken
 * Shows a message when player tries to break it
 * Automatically disappears if current date is not between December 20-30
 */
public class HardIceBlock extends Block {
    
    public static final MapCodec<HardIceBlock> CODEC = simpleCodec(HardIceBlock::new);
    
    // Cooldown per i messaggi (3 secondi = 60 ticks)
    private static final Map<UUID, Long> MESSAGE_COOLDOWNS = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_TICKS = 60;
    
    // Delayed messages to show (player UUID -> tick when to show the message)
    private static final Map<UUID, Long> DELAYED_MESSAGES = new HashMap<>();
    private static final long DELAY_TICKS = 40; // 2 seconds = 40 ticks
    
    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public HardIceBlock(Properties properties) {
        super(properties);
    }
    
    /**
     * Called when the block is placed
     * Schedules the first tick to check the date
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // Schedule first tick after 1 second (20 ticks)
            serverLevel.scheduleTick(pos, this, 20);
        }
    }
    
    /**
     * Called periodically to check if the block should disappear
     * Checks if current date is between December 20-30
     * Also processes delayed messages
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        LocalDate currentDate = LocalDate.now();
        int month = currentDate.getMonthValue();
        int day = currentDate.getDayOfMonth();
        
        // Check if date is NOT between December 20-30
        if (month != 12 || day < 20 || day > 30) {
            // Remove the block
            level.removeBlock(pos, false);
        } else {
            // Schedule next check after 1 second (20 ticks)
            level.scheduleTick(pos, this, 20);
        }
        
        // Process delayed messages
        long currentTick = level.getGameTime();
        DELAYED_MESSAGES.entrySet().removeIf(entry -> {
            if (currentTick >= entry.getValue()) {
                UUID playerUuid = entry.getKey();
                Player player = level.getPlayerByUUID(playerUuid);
                if (player != null && player.isAlive()) {
                    player.displayClientMessage(
                        Component.translatable("message.iska_utils.hard_ice.use_dolly"),
                        true // actionbar
                    );
                }
                return true; // Remove this entry
            }
            return false; // Keep this entry
        });
    }
    
    /**
     * Called when a player attempts to break the block
     * Returns -1 to prevent breaking and shows a message
     */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // Show message to player (only on server side)
        // Use cooldown to prevent spam
        if (player != null && level instanceof net.minecraft.world.level.Level worldLevel && !worldLevel.isClientSide()) {
            UUID playerUuid = player.getUUID();
            long currentTime = worldLevel.getGameTime();
            Long lastMessageTime = MESSAGE_COOLDOWNS.get(playerUuid);
            
            if (lastMessageTime == null || (currentTime - lastMessageTime) >= MESSAGE_COOLDOWN_TICKS) {
                // Show first message immediately
                player.displayClientMessage(
                    Component.translatable("message.iska_utils.hard_ice.cannot_break"),
                    true // actionbar
                );
                
                // Schedule second message after DELAY_TICKS (2 seconds)
                if (worldLevel instanceof ServerLevel serverLevel) {
                    // Schedule a tick to ensure the tick method is called
                    serverLevel.scheduleTick(pos, this, 1);
                    // Store the delayed message
                    DELAYED_MESSAGES.put(playerUuid, currentTime + DELAY_TICKS);
                }
                
                MESSAGE_COOLDOWNS.put(playerUuid, currentTime);
            }
        }
        return -1.0F; // Cannot be broken
    }
}
