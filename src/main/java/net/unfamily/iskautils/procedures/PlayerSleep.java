package net.unfamily.iskautils.procedures;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

@EventBusSubscriber
public class PlayerSleep {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerSleep.class);
	private static final String NECRO_CRYSTAL_HEART_COUNTER = "necro_crystal_heart_hex";
	private static final double BASE_HEALTH = 20.0;

	@SubscribeEvent
	public static void onPlayerInBed(CanPlayerSleepEvent event) {
		// Non facciamo nulla quando il giocatore va a letto
		// Il reset avverrà al risveglio
	}
	
	/**
	 * Handles player waking up event
	 * When a player wakes up, resets their health and Necrotic Crystal Heart counter
	 */
	@SubscribeEvent
	public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			if (!player.level().isClientSide()) {
				// Check if player has the hex counter
				float hexCounter = getCurrentHexCounter(player);
				
				// Only apply reset if player has hex
				if (hexCounter > 0) {
					// Reset max health to base value
					AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
					if (maxHealthAttr != null) {
						maxHealthAttr.setBaseValue(BASE_HEALTH);
					}
					
					// Reset hex counter
					resetHexCounter(player);
					
					LOGGER.info("Reset max health and Necrotic Hex for player {} after sleeping", 
						player.getName().getString());
				}
			}
		}
	}
	
	/**
	 * Gets the current hex counter value for a player
	 */
	private static float getCurrentHexCounter(Player player) {
		CompoundTag persistentData = player.getPersistentData();
		if (!persistentData.contains("iskautils")) {
			return 0.0f;
		}
		
		CompoundTag iskaData = persistentData.getCompound("iskautils");
		if (!iskaData.contains("floatValues")) {
			return 0.0f;
		}
		
		CompoundTag floatValues = iskaData.getCompound("floatValues");
		return floatValues.contains(NECRO_CRYSTAL_HEART_COUNTER) 
			? floatValues.getFloat(NECRO_CRYSTAL_HEART_COUNTER) 
			: 0.0f;
	}
	
	/**
	 * Resets the hex counter for a player
	 */
	private static void resetHexCounter(Player player) {
		CompoundTag persistentData = player.getPersistentData();
		if (!persistentData.contains("iskautils")) {
			return;
		}
		
		CompoundTag iskaData = persistentData.getCompound("iskautils");
		if (!iskaData.contains("floatValues")) {
			return;
		}
		
		CompoundTag floatValues = iskaData.getCompound("floatValues");
		if (floatValues.contains(NECRO_CRYSTAL_HEART_COUNTER)) {
			floatValues.putFloat(NECRO_CRYSTAL_HEART_COUNTER, 0.0f);
			iskaData.put("floatValues", floatValues);
			persistentData.put("iskautils", iskaData);
		}
	}
}
