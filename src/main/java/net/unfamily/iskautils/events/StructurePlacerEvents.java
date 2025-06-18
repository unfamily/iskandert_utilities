package net.unfamily.iskautils.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.unfamily.iskautils.item.custom.StructurePlacerItem;
import net.unfamily.iskautils.item.custom.StructureMonouseItem;

/**
 * Event handler for the Structure Placer
 */
@EventBusSubscriber
public class StructurePlacerEvents {
    
    /**
     * Handles left-clicking on blocks for structure rotation
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        
        // Check if player has Structure Placer or Structure Monouse Item in hand
        boolean isStructurePlacer = stack.getItem() instanceof StructurePlacerItem;
        boolean isStructureMonouse = stack.getItem() instanceof StructureMonouseItem;
        
        if (!isStructurePlacer && !isStructureMonouse) {
            return;
        }
        
        // Server side only
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Check delay to avoid spam
        if (!ClickDelayManager.canClick(player.getUUID())) {
            return;
        }
        
        if (isStructurePlacer) {
            // Handle Structure Placer rotation
            handleStructurePlacerRotation(stack, serverPlayer);
        } else if (isStructureMonouse) {
            // Handle Structure Monouse rotation
            handleStructureMonouseRotation(stack, serverPlayer);
        }
        
        // Record click for delay
        ClickDelayManager.updateClickTime(player.getUUID());
        
        // Cancel event to prevent block breaking
        event.setCanceled(true);
    }
    
    /**
     * Handles rotation for Structure Placer items
     */
    private static void handleStructurePlacerRotation(ItemStack stack, ServerPlayer player) {
        // Check if there's a selected structure
        String structureId = StructurePlacerItem.getSelectedStructure(stack);
        if (structureId == null || structureId.isEmpty()) {
            return;
        }
        
        // Rotate structure clockwise
        int currentRotation = StructurePlacerItem.getRotation(stack);
        int newRotation = (currentRotation + 90) % 360;
        StructurePlacerItem.setRotation(stack, newRotation);
        
        // Reset double-click timer - rotation invalidates previous timer
        resetStructurePlacerClickTimer(stack);
        
        // Get translated direction text
        String rotationText = switch (newRotation) {
            case 0 -> Component.translatable("direction.iska_utils.north").getString();
            case 90 -> Component.translatable("direction.iska_utils.east").getString(); 
            case 180 -> Component.translatable("direction.iska_utils.south").getString();
            case 270 -> Component.translatable("direction.iska_utils.west").getString();
            default -> String.valueOf(newRotation) + "°";
        };
        
        // Show message to player
        player.displayClientMessage(Component.translatable("item.iska_utils.structure_placer.rotated", rotationText), true);
    }
    
    /**
     * Handles rotation for Structure Monouse items
     */
    private static void handleStructureMonouseRotation(ItemStack stack, ServerPlayer player) {
        // Rotate structure clockwise
        int currentRotation = StructureMonouseItem.getRotation(stack);
        int newRotation = (currentRotation + 90) % 360;
        setStructureMonouseRotation(stack, newRotation);
        
        // Reset double-click timer - rotation invalidates previous timer
        resetStructureMonouseClickTimer(stack);
        
        // Get translated direction text
        String rotationText = switch (newRotation) {
            case 0 -> Component.translatable("direction.iska_utils.north").getString();
            case 90 -> Component.translatable("direction.iska_utils.east").getString(); 
            case 180 -> Component.translatable("direction.iska_utils.south").getString();
            case 270 -> Component.translatable("direction.iska_utils.west").getString();
            default -> String.valueOf(newRotation) + "°";
        };
        
        // Show message to player
        player.displayClientMessage(Component.translatable("item.iska_utils.structure_monouse.rotated", rotationText), true);
    }
    
    /**
     * Sets rotation for Structure Monouse items
     */
    private static void setStructureMonouseRotation(ItemStack stack, int rotation) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                            net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putInt("Rotation", rotation);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                 net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    /**
     * Resets the double-click timer for Structure Placer to invalidate previous clicks
     */
    private static void resetStructurePlacerClickTimer(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                            net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        
        // Reset last click time to 0 to invalidate the 5-second timer
        tag.putLong(StructurePlacerItem.LAST_CLICK_TIME_KEY, 0L);
        tag.putLong(StructurePlacerItem.LAST_CLICK_POS_KEY, 0L);
        
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                 net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    /**
     * Resets the double-click timer for Structure Monouse to invalidate previous clicks
     */
    private static void resetStructureMonouseClickTimer(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                                            net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        
        // Reset last click time to 0 to invalidate the 5-second timer
        tag.putLong("LastClickTime", 0L);
        tag.putLong("LastClickPos", 0L);
        
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                 net.minecraft.world.item.component.CustomData.of(tag));
    }
} 