package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.obtaining.SuspiciousDeliveryScriptRunner;

@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public final class SuspiciousDeliveryScriptEvents {
    private SuspiciousDeliveryScriptEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            SuspiciousDeliveryScriptRunner.tick(player);
        }
    }
}
