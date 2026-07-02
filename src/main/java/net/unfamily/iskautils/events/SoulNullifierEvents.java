package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.world.NullifierChunkIndex;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class SoulNullifierEvents {
    private static final String FA_ENTITY_DESC_PREFIX = "entity.forbidden_arcanus.";

    private SoulNullifierEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!ModList.get().isLoaded("forbidden_arcanus")) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!isForbiddenArcanusLostSoul(living)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (NullifierChunkIndex.isWithinActiveCoverage(level, living.getX(), living.getY(), living.getZ(), NullifierChunkIndex.Kind.SOUL)) {
            event.setCanceled(true);
        }
    }

    private static boolean isForbiddenArcanusLostSoul(LivingEntity entity) {
        String desc = entity.getType().getDescriptionId();
        return desc.startsWith(FA_ENTITY_DESC_PREFIX) && desc.contains("lost_soul");
    }
}
