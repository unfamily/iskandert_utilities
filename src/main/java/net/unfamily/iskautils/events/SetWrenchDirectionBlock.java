package net.unfamily.iskautils.events;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.item.custom.SwissWrenchItem;
import net.unfamily.iskautils.network.packet.SwissWrenchCycleModeC2SPacket;

@EventBusSubscriber
public class SetWrenchDirectionBlock {
	// constant for saving the direction in the NBT
	private static final String DIRECTION_KEY = "SelectedDirection";

	/** Cycle order: RADIAL first, then legacy modes. Ordinals stay stable for existing NBT. */
	private static final RotationMode[] CYCLE_ORDER = {
			RotationMode.RADIAL,
			RotationMode.ROTATE_RIGHT,
			RotationMode.ROTATE_LEFT,
			RotationMode.NORTH,
			RotationMode.EAST,
			RotationMode.SOUTH,
			RotationMode.WEST,
			RotationMode.UP,
			RotationMode.DOWN
	};
	
	// possible rotation modes
	public enum RotationMode {
		ROTATE_RIGHT, 
		ROTATE_LEFT, 
		NORTH, 
		EAST, 
		SOUTH, 
		WEST, 
		UP, 
		DOWN,
		/** Universal radial property picker (default when NBT absent). Appended for NBT stability. */
		RADIAL;
		
		// Get the display name for the rotation mode
		public Component getDisplayName() {
			return Component.translatable("item.iska_utils.swiss_wrench.rotation_mode." + name().toLowerCase());
		}
		
		// Get the Direction associated with this mode, or null for ROTATE_RIGHT and ROTATE_LEFT
		public Direction getDirection() {
			return switch(this) {
				case NORTH -> Direction.NORTH;
				case EAST -> Direction.EAST;
				case SOUTH -> Direction.SOUTH;
				case WEST -> Direction.WEST;
				case UP -> Direction.UP;
				case DOWN -> Direction.DOWN;
				default -> null; // ROTATE_RIGHT, ROTATE_LEFT, RADIAL
			};
		}
	}
	
	@SubscribeEvent
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (!Config.swissWrenchLegacyModes) {
			return;
		}
		if (!event.getEntity().level().isClientSide()) {
			tryCycleRotationMode(event.getEntity());
		}
	}

	@SubscribeEvent
	public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
		if (!Config.swissWrenchLegacyModes) {
			return;
		}
		if (!event.getLevel().isClientSide()) {
			return;
		}
		if (event.getEntity().getMainHandItem().getItem() instanceof SwissWrenchItem) {
			ClientPacketDistributor.sendToServer(new SwissWrenchCycleModeC2SPacket());
		}
	}

	/** Cycles rotation mode when main hand holds a Swiss Wrench (server only). */
	public static void tryCycleRotationMode(Player player) {
		if (!Config.swissWrenchLegacyModes) {
			return;
		}
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof SwissWrenchItem)) {
			return;
		}
		if (player.level().isClientSide()) {
			return;
		}
		if (!ClickDelayManager.canClick(player.getUUID())) {
			return;
		}

		RotationMode newMode = cycleRotationMode(stack);
		player.sendOverlayMessage(
				Component.translatable("item.iska_utils.swiss_wrench.message.mode_set", newMode.getDisplayName()));
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.LANTERN_PLACE, SoundSource.PLAYERS, 0.5f, 1.2f);
		ClickDelayManager.updateClickTime(player.getUUID());
	}
	
	/**
	 * get the currently selected rotation mode from the wrench
	 */
	public static RotationMode getSelectedRotationMode(ItemStack stack) {
		if (!Config.swissWrenchLegacyModes) {
			return RotationMode.RADIAL;
		}
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (tag.contains(DIRECTION_KEY)) {
			int index = tag.getInt(DIRECTION_KEY).orElse(0);
			if (index >= 0 && index < RotationMode.values().length) {
				return RotationMode.values()[index];
			}
		}
		// default to RADIAL if not set
		return RotationMode.RADIAL;
	}
	
	/**
	 * set the selected rotation mode in the wrench
	 */
	private static void setSelectedRotationMode(ItemStack stack, RotationMode mode) {
		int index = mode.ordinal();
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		tag.putInt(DIRECTION_KEY, index);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}
	
	/**
	 * cycle to the next rotation mode (RADIAL first in order)
	 */
	private static RotationMode cycleRotationMode(ItemStack stack) {
		RotationMode current = getSelectedRotationMode(stack);
		int idx = 0;
		for (int i = 0; i < CYCLE_ORDER.length; i++) {
			if (CYCLE_ORDER[i] == current) {
				idx = i;
				break;
			}
		}
		RotationMode next = CYCLE_ORDER[(idx + 1) % CYCLE_ORDER.length];
		setSelectedRotationMode(stack, next);
		return next;
	}
}
