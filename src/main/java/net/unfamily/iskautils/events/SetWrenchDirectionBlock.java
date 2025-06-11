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
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.unfamily.iskautils.item.custom.SwissWrenchItem;

@EventBusSubscriber
public class SetWrenchDirectionBlock {
	// constant for saving the direction in the NBT
	private static final String DIRECTION_KEY = "SelectedDirection";
	
	// possible rotation modes
	public enum RotationMode {
		ROTATE_RIGHT, 
		ROTATE_LEFT, 
		NORTH, 
		EAST, 
		SOUTH, 
		WEST, 
		UP, 
		DOWN;
		
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
				default -> null; // ROTATE_RIGHT and ROTATE_LEFT don't have an associated direction
			};
		}
	}
	
	@SubscribeEvent
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		Player player = event.getEntity();
		ItemStack stack = player.getMainHandItem();
		
		// check if the player has a Swiss Wrench in hand
		if (stack.getItem() instanceof SwissWrenchItem) {
			if (!player.level().isClientSide()) {
				// check the delay using the shared manager
				if (!ClickDelayManager.canClick(player.getUUID())) {
					return;
				}
				
				// change the rotation mode of the wrench
				RotationMode newMode = cycleRotationMode(stack);
				
				// send a message to the player
				player.displayClientMessage(
					Component.translatable("item.iska_utils.swiss_wrench.message.mode_set", 
					newMode.getDisplayName()), true);
				
				// feedback sound
				player.level().playSound(null, player.blockPosition(), 
					SoundEvents.LANTERN_PLACE, SoundSource.PLAYERS, 0.5f, 1.2f);
				
				// register this click using the shared manager
				ClickDelayManager.updateClickTime(player.getUUID());
			}
		}
	}
	
	/**
	 * get the currently selected rotation mode from the wrench
	 */
	public static RotationMode getSelectedRotationMode(ItemStack stack) {
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		if (tag.contains(DIRECTION_KEY)) {
			int index = tag.getInt(DIRECTION_KEY);
			if (index >= 0 && index < RotationMode.values().length) {
				return RotationMode.values()[index];
			}
		}
		// default to ROTATE_RIGHT if not set
		return RotationMode.ROTATE_RIGHT;
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
	 * cycle to the next rotation mode in order
	 */
	private static RotationMode cycleRotationMode(ItemStack stack) {
		RotationMode current = getSelectedRotationMode(stack);
		int nextIndex = (current.ordinal() + 1) % RotationMode.values().length;
		RotationMode next = RotationMode.values()[nextIndex];
		
		// Save the new rotation mode
		setSelectedRotationMode(stack, next);
		return next;
	}
}

