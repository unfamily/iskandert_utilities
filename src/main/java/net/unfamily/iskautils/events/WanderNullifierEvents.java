package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.world.NullifierChunkIndex;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class WanderNullifierEvents {
    private WanderNullifierEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() != EntitySpawnReason.NATURAL) {
            return;
        }
        if (!(event.getEntity() instanceof WanderingTrader trader)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (NullifierChunkIndex.isWithinActiveCoverage(level, trader.getX(), trader.getY(), trader.getZ(), NullifierChunkIndex.Kind.WANDER)) {
            event.setSpawnCancelled(true);
        }
    }
}
