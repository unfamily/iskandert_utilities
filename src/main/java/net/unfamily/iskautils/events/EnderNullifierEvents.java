package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.unfamily.iskautils.util.NullifierEntityIgnore;
import net.unfamily.iskautils.world.NullifierSpatialIndex;

public final class EnderNullifierEvents {
    private EnderNullifierEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityTeleport(EntityTeleportEvent event) {
        if (event instanceof EntityTeleportEvent.TeleportCommand
                || event instanceof EntityTeleportEvent.SpreadPlayersCommand) {
            return;
        }
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Level level = serverLevel;
        boolean blocked;
        if (event.getEntity() instanceof Player) {
            blocked = NullifierSpatialIndex.isTeleportBlockedForPlayers(level.dimension(), event.getPrev())
                    || NullifierSpatialIndex.isTeleportBlockedForPlayers(level.dimension(), event.getTarget());
        } else if (event.getEntity() instanceof Mob mob) {
            if (NullifierEntityIgnore.isIgnored(mob)) {
                return;
            }
            blocked = NullifierSpatialIndex.isTeleportBlockedForMobs(level.dimension(), event.getPrev())
                    || NullifierSpatialIndex.isTeleportBlockedForMobs(level.dimension(), event.getTarget());
        } else {
            return;
        }

        if (blocked) {
            event.setCanceled(true);
        }
    }
}
