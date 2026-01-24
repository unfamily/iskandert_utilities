package net.unfamily.iskautils.item.custom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.unfamily.iskautils.client.KeyBindings;
import net.unfamily.iskautils.Config;

import java.util.List;

/**
 * Gauntlet of Climbing - Allows player to climb walls when held in inventory
 */
public class GauntletOfClimbingItem extends Item {
    // Default climbing speed (fallback if config not loaded)
    private static final double DEFAULT_CLIMB_SPEED = 0.15D;
    // NBT key for toggle state (0 = ON, 1 = OFF, default ON)
    private static final String NBT_STATUS = "onoff";

    public GauntletOfClimbingItem(Properties properties) {
        super(properties);
    }
    
    /**
     * Checks if climbing is enabled (default: true/ON)
     * Uses DataComponents like NeoForge 1.21 requires
     */
    private boolean isOn(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return true; // Default: ON (no data means enabled)
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(NBT_STATUS)) {
            return true; // Default: ON
        }
        return tag.getInt(NBT_STATUS) == 0; // 0 = ON, 1 = OFF
    }
    
    /**
     * Toggles the climbing state
     */
    private void toggle(Player player, ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putInt(NBT_STATUS, (tag.getInt(NBT_STATUS) + 1) % 2);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    
    /**
     * Gets the climbing speed from config, with fallback to default
     */
    private static double getClimbSpeed() {
        // Read from config at runtime to ensure it's loaded
        double speed = Config.gauntletClimbingSpeed;
        // If config not loaded yet, use default
        return speed > 0.0 ? speed : DEFAULT_CLIMB_SPEED;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        // Only work for players
        if (!(entity instanceof Player player)) {
            return;
        }
        
        // Handle keybind - toggle on both client and server (like Cyclic)
        if (KeyBindings.consumeGauntletClimbingToggleKeyClick()) {
            // Toggle the state
            toggle(player, stack);
            boolean nowEnabled = isOn(stack);
            
            // Show feedback on server side (like Burning Brazier) for real-time updates
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                Component message = Component.translatable(nowEnabled ? "message.iska_utils.gauntlet_climbing.enabled" : "message.iska_utils.gauntlet_climbing.disabled")
                    .withStyle(nowEnabled ? ChatFormatting.GREEN : ChatFormatting.RED);
                serverPlayer.displayClientMessage(message, true);
            }
        }
        
        // Check if climbing is enabled and player is colliding horizontally with a wall
        // Works on both client and server (like Cyclic)
        if (!isOn(stack)) {
            return; // Climbing is disabled
        }
        
        if (player.horizontalCollision) {
            // Make the player climb
            makePlayerClimb(player, getClimbSpeed());
        }
    }
    
    /**
     * Makes the player climb by setting their vertical movement.
     * If shift is held, player stays in place and takes no fall damage.
     */
    private void makePlayerClimb(Player player, double climbSpeed) {
        Vec3 motion = player.getDeltaMovement();
        
        if (player.isShiftKeyDown()) {
            // Shift held: stay in place (zero movement) and prevent fall damage
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.fallDistance = 0.0f;
        } else {
            // Original climbing logic: set vertical movement to climb speed
            // Use max to ensure we climb up, but allow natural upward movement
            double newY = Math.max(motion.y, climbSpeed);
            
            // Apply the climbing movement
            player.setDeltaMovement(motion.x, newY, motion.z);
            
            // Reset fall distance to prevent fall damage
            player.fallDistance = 0.0f;
        }
        
        // Mark that physics should be updated
        player.hurtMarked = true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Get the keybind name
        String keybindName = KeyBindings.GAUNTLET_CLIMBING_TOGGLE_KEY.getTranslatedKeyMessage().getString();
        
        // Show description
        tooltip.add(Component.translatable("tooltip.iska_utils.gauntlet_of_climbing.desc"));
        tooltip.add(Component.translatable("tooltip.iska_utils.gauntlet_of_climbing.toggle", keybindName));
        
        // Show current status (like Cyclic)
        Component status = Component.translatable("tooltip.iska_utils.gauntlet_of_climbing.status." + (isOn(stack) ? "enabled" : "disabled"));
        tooltip.add(status);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
