package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.world.NullifierChunkIndex;

/**
 * Blocks vanilla {@code WanderingTraderSpawner} spawns ({@link EntitySpawnReason#EVENT})
 * and optional NATURAL edge cases for traders and trader llamas.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class WanderNullifierEvents {
    private WanderNullifierEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        EntitySpawnReason type = event.getSpawnType();
        if (type != EntitySpawnReason.EVENT && type != EntitySpawnReason.NATURAL) {
            return;
        }
        var entity = event.getEntity();
        if (!(entity instanceof WanderingTrader) && !(entity instanceof TraderLlama)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (NullifierChunkIndex.isWithinActiveCoverage(
                level, entity.getX(), entity.getY(), entity.getZ(), NullifierChunkIndex.Kind.WANDER)) {
            event.setSpawnCancelled(true);
        }
    }
}
