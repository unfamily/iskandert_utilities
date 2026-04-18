package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.item.custom.FanpackItem;
import net.unfamily.iskautils.item.custom.GreedyShieldItem;
import net.unfamily.iskalib.stage.StageRegistry;

/**
 * Event handler for Fanpack flight management.
 * Heartbeat {@code iska_utils_internal-funpack_flight0} is set by {@link FanpackItem} / curio tick when energy allows;
 * this handler enables or disables {@code mayfly} from that signal and from whether a fanpack is still equipped.
 */
public class FlightHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        GreedyShieldItem.syncEquipStage(serverPlayer);

        if (player.isSpectator()) {
            return;
        }

        boolean curseFlight = StageRegistry.playerHasStage(serverPlayer, "iska_utils_internal-curse_flight")
                || StageRegistry.playerTeamHasStage(serverPlayer, "iska_utils_internal-curse_flight")
                || StageRegistry.worldHasStage(serverPlayer.level(), "iska_utils_internal-curse_flight");
        if (curseFlight) {
            if (player.getAbilities().mayfly || player.getAbilities().flying) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            return;
        }

        boolean fanpackEquipped = FanpackItem.getActiveFanpackForFlight(serverPlayer) != null;
        if (!fanpackEquipped) {
            StageRegistry.removePlayerStage(serverPlayer, "iska_utils_internal-funpack_flight0");
            StageRegistry.removePlayerStage(serverPlayer, "iska_utils_internal-funpack_flight1");
            if (!player.getAbilities().instabuild && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            return;
        }

        boolean hasFlight0Stage = StageRegistry.playerHasStage(serverPlayer, "iska_utils_internal-funpack_flight0");

        if (hasFlight0Stage) {
            if (!player.getAbilities().instabuild && !player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else {
            // Fanpack in inventory but no heartbeat (e.g. not enough flight energy)
            if (!player.getAbilities().instabuild && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }
}
