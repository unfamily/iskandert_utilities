package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.world.NullifierChunkIndex;

/**
 * Blocks vanilla {@code WanderingTraderSpawner} spawns ({@link MobSpawnType#EVENT})
 * and optional NATURAL edge cases for traders and trader llamas.
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class WanderNullifierEvents {
    private WanderNullifierEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.EVENT && type != MobSpawnType.NATURAL) {
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
