package net.unfamily.iskautils.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.IEventBus;
import net.unfamily.iskautils.IskaUtils;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;


@EventBusSubscriber
public class RubberNegateFallHandler {
    private static final Map<UUID, PlayerBounceData> NEGATE_FALL_PLAYERS = new IdentityHashMap<>();
    
    // Register events
    public static void register(IEventBus eventBus) {
        eventBus.register(RubberNegateFallHandler.class);
    }
    
    /**
     * adds a player to the list of bouncers
     * @param player The player
     * @param velocity 
     */
    public static void addBouncingPlayer(Player player, double velocity) {
        NEGATE_FALL_PLAYERS.put(player.getUUID(), new PlayerBounceData(player, velocity));
    }

    /**
     * Handles the tick of the players
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUUID();
        
        if (!NEGATE_FALL_PLAYERS.containsKey(playerUUID)) return;
        
        PlayerBounceData negateFallData = NEGATE_FALL_PLAYERS.get(playerUUID);

        if (player.isSwimming() || player.isInWaterOrBubble() || player.onClimbable() || 
            player.isSpectator() || player.isFallFlying() || player.getAbilities().flying) {
            NEGATE_FALL_PLAYERS.remove(playerUUID);
            return;
        }
        
        if (player.tickCount == negateFallData.bounceTick) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, negateFallData.velocity, motion.z);
            negateFallData.bounceTick = 0;
        }
        
        // Applies the slowdown when the player is in the air
        if (!player.onGround() && player.tickCount != negateFallData.bounceTick && 
            (negateFallData.lastMoveX != player.getDeltaMovement().x || negateFallData.lastMoveZ != player.getDeltaMovement().z)) {
            
            double horizontalSlowdown = 0.935D;
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x / horizontalSlowdown, motion.y, motion.z / horizontalSlowdown);
            player.hasImpulse = true;
            
            negateFallData.lastMoveX = player.getDeltaMovement().x;
            negateFallData.lastMoveZ = player.getDeltaMovement().z;
        }
        
        // Remove the player from the list when they land
        if (negateFallData.wasInAir && player.onGround()) {
            if (negateFallData.timer == 0) {
                negateFallData.timer = player.tickCount;
            } else if (player.tickCount - negateFallData.timer > 5) {
                NEGATE_FALL_PLAYERS.remove(playerUUID);
            }
            return;
        }
        
            negateFallData.timer = 0;
        negateFallData.wasInAir = true;
    }
    
    /**
     * Class to store the data of the bounce of a player
     */
    private static class PlayerBounceData {
        private final Player player;
        private double velocity;
        private int bounceTick;
        private double lastMoveX;
        private double lastMoveZ;
        private boolean wasInAir;
        private int timer;
        
        public PlayerBounceData(Player player, double velocity) {
            this.player = player;
            this.velocity = velocity;
            this.bounceTick = velocity != 0 ? player.tickCount : 0;
            this.lastMoveX = 0;
            this.lastMoveZ = 0;
            this.wasInAir = false;
            this.timer = 0;
        }
    }
} 