package net.unfamily.iskautils.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskalib.stage.StageRegistry;

/**
 * Small first-pass implementation of artifact effects from legacy design notes.
 * More complex cursed behaviors can be layered later without breaking saves.
 */
@EventBusSubscriber
public class ArtifactEvents {
    private static final String THE_ROOTS_STAGE = "iska_utils_internal-the_roots_equip";

    @SubscribeEvent
    public static void onPlayerBreakSpeed(PlayerEvent.BreakSpeed event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!StageRegistry.playerHasStage(player, THE_ROOTS_STAGE)) {
            return;
        }

        float mult = (float) (Config.theRootsBreakSpeedMinMultiplier
                + player.getRandom().nextDouble() * Config.theRootsBreakSpeedMaxBonus);
        event.setNewSpeed(event.getNewSpeed() * mult);
    }
}
