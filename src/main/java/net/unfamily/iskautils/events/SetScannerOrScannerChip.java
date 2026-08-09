package net.unfamily.iskautils.events;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.unfamily.iskautils.item.custom.CrystalCageItem;
import net.unfamily.iskautils.item.custom.ScannerChipItem;
import net.unfamily.iskautils.item.custom.ScannerItem;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Cancels vanilla mob interactions (mount horses, sit/stand pets, etc.) when using
 * Crystal Cage / Scanner / Scanner Chip, so the item action wins.
 * Entity interact runs before {@link ItemStack#interactLivingEntity}, so we must cancel here.
 */
@EventBusSubscriber
public class SetScannerOrScannerChip {

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		handle(event.getEntity(), event.getItemStack(), event.getTarget(), event.getHand(),
				() -> {
					event.setCanceled(true);
					event.setCancellationResult(InteractionResult.SUCCESS);
				});
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		handle(event.getEntity(), event.getItemStack(), event.getTarget(), event.getHand(),
				() -> {
					event.setCanceled(true);
					event.setCancellationResult(InteractionResult.SUCCESS);
				});
	}

	private static void handle(Player player, ItemStack itemStack, Entity target,
	                           InteractionHand hand, Runnable cancel) {
		if (!(target instanceof LivingEntity entity) || entity instanceof Player) {
			return;
		}

		if (itemStack.getItem() instanceof CrystalCageItem) {
			if (CrystalCageItem.isFilled(itemStack)) {
				return;
			}
			cancel.run();
			if (!player.level().isClientSide) {
				itemStack.interactLivingEntity(player, entity, hand);
			}
			return;
		}

		if (!player.isCrouching()) {
			return;
		}

		if (itemStack.getItem() instanceof ScannerItem) {
			applyScannerTarget(player, itemStack, entity);
			cancel.run();
		} else if (itemStack.getItem() instanceof ScannerChipItem) {
			applyScannerChipTarget(player, itemStack, entity);
			cancel.run();
		}
	}

	private static void applyScannerTarget(Player player, ItemStack itemStack, LivingEntity entity) {
		String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
		var tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		tag.remove("TargetBlock");
		tag.putString("TargetMob", entityId);
		if (!tag.contains("ScannerId")) {
			tag.putUUID("ScannerId", UUID.randomUUID());
		}
		itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		if (!player.level().isClientSide) {
			player.displayClientMessage(
					Component.translatable("item.iska_utils.scanner.mob_target_set", entity.getName()), true);
		}
	}

	private static void applyScannerChipTarget(Player player, ItemStack itemStack, LivingEntity entity) {
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
		String itemPath = itemId.getPath();
		boolean isSpecializedChip = itemPath.contains("scanner_chip_ores") || itemPath.contains("scanner_chip_mobs");

		if (!isSpecializedChip) {
			String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
			var tag = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
			tag.remove("TargetBlock");
			tag.putString("TargetMob", entityId);
			itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			if (!player.level().isClientSide) {
				player.displayClientMessage(
						Component.translatable("item.iska_utils.scanner_chip.mob_target_set", entity.getName()), true);
			}
		} else if (!player.level().isClientSide) {
			player.displayClientMessage(
					Component.translatable("item.iska_utils.scanner_chip.specialized_cannot_overwrite"), true);
		}
	}
}
