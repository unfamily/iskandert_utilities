package net.unfamily.iskautils.procedures;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.SessionVariables;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;
import net.minecraft.tags.FluidTags;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.advancements.AdvancementHolder;
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
		SessionVariables.resetScannerSessionId();
	}
	
	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		// Clear all session variables when server stops
		SessionVariables.clearAll();
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || world == null || !(world instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player))
			return;
		
		try {
			// Get the current session ID
			UUID sessionId = SessionVariables.getScannerSessionId();
			String sessionTag = "session_" + sessionId.toString();
			
			// Prima cerca tutte le entità block_display che hanno il tag temp_scan ma non hanno il tag della sessione corrente
			// Tieni presente che NON possiamo usare tag=!session_xyz perché questo potrebbe includere entità che non hanno affatto il tag
			String listCommand = String.format("execute as @e[type=block_display,tag=temp_scan] unless entity @s[tag=%s] run tag @s add scan_cleanup", sessionTag);
			serverLevel.getServer().getCommands().performPrefixedCommand(
				serverLevel.getServer().createCommandSourceStack(),
				listCommand
			);
			
			// Ora rimuovi dal team tutte le entità marcate per la pulizia
			String teamLeaveCommand = "team leave @e[type=block_display,tag=scan_cleanup]";
			serverLevel.getServer().getCommands().performPrefixedCommand(
				serverLevel.getServer().createCommandSourceStack(),
				teamLeaveCommand
			);
			
			// Infine, uccidi tutte le entità marcate per la pulizia
			String killCommand = "kill @e[type=block_display,tag=scan_cleanup]";
			serverLevel.getServer().getCommands().performPrefixedCommand(
				serverLevel.getServer().createCommandSourceStack(),
				killCommand
			);
			
			// Log that we're cleaning up orphaned markers
			if (serverLevel.getGameTime() % 1200 == 0) { // Log only every minute (20 ticks * 60 seconds)
				LOGGER.debug("Checking for orphaned scanner markers from other sessions");
			}
		} catch (Exception e) {
			LOGGER.error("Error clearing markers: {}", e.getMessage());
		}
	}
}
