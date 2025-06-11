package net.unfamily.iskautils.events;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.unfamily.iskautils.item.custom.ScannerChipItem;
import net.unfamily.iskautils.item.custom.ScannerItem;

import javax.annotation.Nullable;
import java.util.UUID;

@EventBusSubscriber
public class SetScannerOrScannerChip {
	@SubscribeEvent
	public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
		Player player = event.getEntity();
		ItemStack itemStack = event.getItemStack();
		
		if(!player.isCrouching()) {
			return;
		}
		
		// check if the interaction is with a living entity
		if (!(event.getTarget() instanceof LivingEntity entity) || entity instanceof Player) {
			return;
		}

		
		// handle Scanner
		if (itemStack.getItem() instanceof ScannerItem) {
			// select the mob as target
			String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
			
			// get the NBT tag of the scanner
			var tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
			
			// remove the block target if present
			tag.remove("TargetBlock");
			
			// set the mob target
			tag.putString("TargetMob", entityId);
			
			// ensure the scanner has a unique ID
			if (!tag.contains("ScannerId")) {
				tag.putUUID("ScannerId", UUID.randomUUID());
			}
			
			// save the data in the scanner
			itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			
			// notify the player
			if (!player.level().isClientSide) {
				player.displayClientMessage(Component.translatable("item.iska_utils.scanner.mob_target_set", entity.getName()), true);
			}
			
			// cancel the event to prevent the normal interaction
			event.setCanceled(true);
		}
		// handle ScannerChip
		else if (itemStack.getItem() instanceof ScannerChipItem) {
			// select the mob as target
			String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
			
			// get the NBT tag of the chip
			var tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
			
			// remove the block target if present
			tag.remove("TargetBlock");
			
			// set the mob target
			tag.putString("TargetMob", entityId);
			
			// save the data in the chip
			itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			
			// notify the player
			if (!player.level().isClientSide) {
				player.displayClientMessage(Component.translatable("item.iska_utils.scanner_chip.mob_target_set", entity.getName()), true);
			}
			
			// cancel the event to prevent the normal interaction
			event.setCanceled(true);
		}
	}
}
