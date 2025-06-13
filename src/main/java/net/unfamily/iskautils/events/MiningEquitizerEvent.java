package net.unfamily.iskautils.events;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.unfamily.iskautils.stage.StageRegistry;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.item.ModItems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber
public class MiningEquitizerEvent {
	private static final Logger LOGGER = LoggerFactory.getLogger(MiningEquitizerEvent.class);
	
	@SubscribeEvent
	public static void onPlayerBreakSpeed(PlayerEvent.BreakSpeed event) {
		Entity entity = event.getEntity();


		if(entity.onGround() && !entity.isInWater()) {
			return;
		}

		if (entity instanceof Player _playerHasItem ? _playerHasItem.getInventory().contains(new ItemStack(ModItems.MINING_EQUITIZER.get())) : false) {
			float originalSpeed = event.getOriginalSpeed();
			float newSpeed = originalSpeed * 5.0f;
			event.setNewSpeed(newSpeed);
		}
	}
}
