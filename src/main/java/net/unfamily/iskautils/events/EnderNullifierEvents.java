package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.unfamily.iskautils.world.EnderNullifierSpatialIndex;

public final class EnderNullifierEvents {
    private EnderNullifierEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityTeleport(EntityTeleportEvent event) {
        if (event instanceof EntityTeleportEvent.TeleportCommand
                || event instanceof EntityTeleportEvent.SpreadPlayersCommand
                || event instanceof EntityTeleportEvent.EnderPearl) {
            return;
        }
        if (!(event.getEntity() instanceof Mob)) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Level level = serverLevel;
        if (EnderNullifierSpatialIndex.isTeleportBlocked(level.dimension(), event.getPrev())
                || EnderNullifierSpatialIndex.isTeleportBlocked(level.dimension(), event.getTarget())) {
            event.setCanceled(true);
        }
    }
}
