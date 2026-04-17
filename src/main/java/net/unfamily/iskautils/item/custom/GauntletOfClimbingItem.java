package net.unfamily.iskautils.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.client.KeyBindings;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.data.GauntletClimbingData;

import java.util.List;
import java.util.function.Consumer;

/**
 * Gauntlet of Climbing - Allows player to climb walls when held in inventory
 */
public class GauntletOfClimbingItem extends Item {
    // Default climbing speed (fallback if config not loaded)
    private static final double DEFAULT_CLIMB_SPEED = 0.15D;
    public GauntletOfClimbingItem(Properties properties) {
        super(properties);
    }
    
    private boolean isOn(Player player) {
        return GauntletClimbingData.isClimbingEnabled(player);
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
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        
        // Only work for players
        if (!(entity instanceof Player player)) {
            return;
        }
        
        // Check if climbing is enabled and player is colliding horizontally with a wall
        // Works on both client and server (like Cyclic)
        if (!isOn(player)) {
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
        
        // Get the keybind name
        String keybindName = KeyBindings.GAUNTLET_CLIMBING_TOGGLE_KEY.getTranslatedKeyMessage().getString();
        
        // Show description
        tooltip.accept(Component.translatable("tooltip.iska_utils.gauntlet_of_climbing.desc"));
        tooltip.accept(Component.translatable("tooltip.iska_utils.gauntlet_of_climbing.toggle", keybindName));
        
        // Show current status (like Cyclic)
        Component status = Component.translatable("tooltip.iska_utils.gauntlet_of_climbing.status." + (isOn(net.minecraft.client.Minecraft.getInstance().player) ? "enabled" : "disabled"));
        tooltip.accept(status);
    }

}
