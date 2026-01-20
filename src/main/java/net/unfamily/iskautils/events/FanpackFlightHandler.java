package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.stage.StageRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event handler for Fanpack flight management
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class FanpackFlightHandler {
    
    // Track stage check timing for each player
    private static final Map<UUID, Long> lastStageCheckTime = new HashMap<>();
    private static final long STAGE_CHECK_INTERVAL = 60; // 3 seconds (60 ticks)
    
    // Track players that have flight1 stage (to check after 1 second delay)
    private static final Map<UUID, Long> flight1StageTime = new HashMap<>();
    private static final long FLIGHT1_CHECK_DELAY = 20; // 1 second (20 ticks)
    
    /**
     * Check every tick if players still have Fanpack and manage their flight
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        
        // Only handle on server side
        if (player.level().isClientSide) {
            return;
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        long currentTick = player.level().getGameTime();
        UUID playerId = player.getUUID();
        
        // Every 3 seconds, check for flight0 stage and mark with flight1
        Long lastCheck = lastStageCheckTime.get(playerId);
        if (lastCheck == null || currentTick - lastCheck >= STAGE_CHECK_INTERVAL) {
            lastStageCheckTime.put(playerId, currentTick);
            
            if (StageRegistry.playerHasStage(serverPlayer, "iska_utils_internal-funpack_flight0")) {
                // Mark with flight1 stage
                StageRegistry.addPlayerStage(serverPlayer, "iska_utils_internal-funpack_flight1", true);
                flight1StageTime.put(playerId, currentTick);
            }
        }
        
        // Check if flight1 stage is still present after 1 second delay
        Long flight1Time = flight1StageTime.get(playerId);
        if (flight1Time != null && currentTick - flight1Time >= FLIGHT1_CHECK_DELAY) {
            if (StageRegistry.playerHasStage(serverPlayer, "iska_utils_internal-funpack_flight1")) {
                // Flight1 still present after delay - remove both stages and disable flight
                StageRegistry.removePlayerStage(serverPlayer, "iska_utils_internal-funpack_flight0", true);
                StageRegistry.removePlayerStage(serverPlayer, "iska_utils_internal-funpack_flight1", true);
                flight1StageTime.remove(playerId);
                
                // Disable flight
                if (!player.getAbilities().instabuild) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
                return; // Exit early, don't enable flight
            } else {
                // Flight1 was removed (by fanpack tick), clear the timer
                flight1StageTime.remove(playerId);
            }
        }
        
        // Only enable flight if flight0 stage is present (heartbeat from fanpack)
        // This ensures the fanpack is actually present and ticking
        boolean hasFlight0Stage = StageRegistry.playerHasStage(serverPlayer, "iska_utils_internal-funpack_flight0");
        
        if (!hasFlight0Stage) {
            // No heartbeat - disable flight
            if (!player.getAbilities().instabuild && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            return;
        }
        
        // If flight0 stage is present, enable flight (fanpack is present and ticking)
        // The stage itself is proof that the fanpack is equipped and working
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }
}
