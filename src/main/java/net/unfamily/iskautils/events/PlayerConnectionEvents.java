package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.structure.StructurePlacementHistory;

/**
 * Handles player connection events for cleanup purposes
 */
@EventBusSubscriber
public class PlayerConnectionEvents {

    /**
     * Clear structure placement history when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            StructurePlacementHistory.clearPlayerHistory(serverPlayer.getUUID());
        }
    }
} 