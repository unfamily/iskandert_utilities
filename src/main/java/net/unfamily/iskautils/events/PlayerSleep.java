package net.unfamily.iskautils.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.unfamily.iskalib.stage.StageRegistry;

/**
 * Resets Necrotic Crystal Heart hex and related state when the player wakes up.
 */
@EventBusSubscriber
public class PlayerSleep {

	@SubscribeEvent
	public static void onPlayerInBed(CanPlayerSleepEvent event) {
		// Reset happens on wake
	}
	
	/**
	 * When a player wakes up, resets hex / max health if they had necrotic hex,
	 * and clears a stale necrotic equip stage if hex was already zero.
	 */
	@SubscribeEvent
	public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
			return;
		}

		float hexCounter = LivingIncomingDamageEventHandler.getCurrentUsageCounter(player);

		if (hexCounter > 0) {
			LivingIncomingDamageEventHandler.resetNecroticCrystalHeartProgress(player);
		} else {
			StageRegistry.removePlayerStage(player, LivingIncomingDamageEventHandler.NECRO_CRYSTAL_HEART_EQUIP_STAGE, true);
		}
	}
}
