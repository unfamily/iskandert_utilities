package net.unfamily.iskautils.item.custom;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;

/**
 * Eventi per lo scanner di blocchi
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class ScannerEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Gestisce il tick lato server
     */
    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        // Aggiorna i TTL dei blocchi scanner ogni tick
        for (ServerLevel level : event.getServer().getAllLevels()) {
            ScannerItem.tick(level);
        }
    }
} 