package net.unfamily.iskautils.events;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import javax.annotation.Nullable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@EventBusSubscriber
public class ClearMarkers {
	private static final Logger LOGGER = LogUtils.getLogger();
	
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre event) {
		execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity());
	}
	
	@SubscribeEvent
	public static void onServerStarting(ServerStartingEvent event) {
		// Reset session ID when server starts
		net.unfamily.iskalib.marker.MarkerSession.resetScannerSessionId();
		
		// Create teams for block_display coloring
		net.unfamily.iskalib.marker.ScannerMarkerCleanup.ensureDisplayTeams(event.getServer());
	}
	
	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		// Reset session on stop as well (runtime-only)
		net.unfamily.iskalib.marker.MarkerSession.resetScannerSessionId();
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || world == null || !(world instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player))
			return;
		
		try {
			net.unfamily.iskalib.marker.ScannerMarkerCleanup.cleanupOrphanedMarkers(serverLevel);
			
			// Log that we're cleaning up orphaned markers
			if (serverLevel.getGameTime() % 1200 == 0) { // Log only every minute (20 ticks * 60 seconds)
				LOGGER.debug("Checking for orphaned scanner markers from other sessions");
			}
		} catch (Exception e) {
			LOGGER.error("Error clearing markers: {}", e.getMessage());
		}
	}
}
