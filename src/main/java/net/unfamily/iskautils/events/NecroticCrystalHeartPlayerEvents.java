package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Clears Necrotic Crystal Heart progression and internal artifact equip stages on player death.
 */
@EventBusSubscriber
public final class NecroticCrystalHeartPlayerEvents {

    private NecroticCrystalHeartPlayerEvents() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        LivingIncomingDamageEventHandler.resetNecroticCrystalHeartProgress(player);
        LivingIncomingDamageEventHandler.clearStagesAfterDamage(player);
    }
}
