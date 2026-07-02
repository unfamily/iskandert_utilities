package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.world.NullifierChunkIndex;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class WanderNullifierEvents {
    private WanderNullifierEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof WanderingTrader)) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!isNaturalSpawn(mob.getSpawnType())) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (NullifierChunkIndex.isWithinActiveCoverage(level, mob.getX(), mob.getY(), mob.getZ(), NullifierChunkIndex.Kind.WANDER)) {
            event.setCanceled(true);
        }
    }

    private static boolean isNaturalSpawn(MobSpawnType type) {
        return type == null || type == MobSpawnType.NATURAL;
    }
}
