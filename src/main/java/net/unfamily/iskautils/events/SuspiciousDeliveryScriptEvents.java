package net.unfamily.iskautils.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.SuspiciousDeliveryScriptData;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryScriptRunner;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class SuspiciousDeliveryScriptEvents {
    private SuspiciousDeliveryScriptEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SuspiciousDeliveryScriptRunner.tickAll(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SuspiciousDeliveryScriptData.syncFromRunner(event.getServer());
        SuspiciousDeliveryScriptData.killFakeTntAll(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SuspiciousDeliveryScriptData.killFakeTntAll(event.getServer());
        SuspiciousDeliveryScriptData.restoreToRunner(event.getServer());
    }
}
