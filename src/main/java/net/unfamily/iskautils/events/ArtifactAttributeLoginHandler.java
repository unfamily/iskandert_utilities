package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.unfamily.iskautils.util.AttributeSyncGrace;
import net.unfamily.iskautils.util.BonusHealthResync;

/** Clears attribute sync state and restores bonus max-health fill after login. */
@EventBusSubscriber
public final class ArtifactAttributeLoginHandler {
    private ArtifactAttributeLoginHandler() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            AttributeSyncGrace.clearPlayer(sp.getUUID());
            BonusHealthResync.beginResync(sp);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            AttributeSyncGrace.clearPlayer(sp.getUUID());
            BonusHealthResync.beginResync(sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            BonusHealthResync.snapshotHealthRatio(sp);
            AttributeSyncGrace.clearPlayer(sp.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer sp) {
            BonusHealthResync.tick(sp);
        }
    }
}
