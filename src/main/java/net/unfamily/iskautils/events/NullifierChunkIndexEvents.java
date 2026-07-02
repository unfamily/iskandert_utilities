package net.unfamily.iskautils.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.world.NullifierChunkIndex;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class NullifierChunkIndexEvents {
    private NullifierChunkIndexEvents() {}

    @SubscribeEvent
    public static void onServerTickEnd(ServerTickEvent.Post event) {
        NullifierChunkIndex.rebuildSnapshotsAtEndOfTick();
    }
}
