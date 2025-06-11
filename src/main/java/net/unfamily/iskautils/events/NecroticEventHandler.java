package net.unfamily.iskautils.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.custom.NecroticCrystalHeartCurioHandler;
import net.unfamily.iskautils.item.custom.NecroticCrystalHeartItem;
import net.unfamily.iskautils.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for intercepting damage events and managing the Necrotic Crystal Heart.
 */
@EventBusSubscriber
public class NecroticEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NecroticEventHandler.class);

    // Key for the Necrotic Crystal Heart counter
    private static final String NECRO_CRYSTAL_HEART_COUNTER = "necro_crystal_heart_hex";
    
    // Minimum health threshold before it becomes lethal
    private static final float MIN_HEALTH_THRESHOLD = 2.0f;

    /**
     * Intercepts incoming damage events for an entity.
     * If the entity has equipped a Necrotic Crystal Heart as a curio,
     * completely negates lethal damage and increments the counter.
     *
     * @param event The incoming damage event
     */
    @SubscribeEvent
    public static void onEntityAttacked(LivingIncomingDamageEvent event) {
        // Get the entity taking damage
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            // Check if damage would be lethal
            boolean isDamageLethal = player.getHealth() <= event.getAmount();
            
            if (StageRegistry.playerHasStage(player, "iska_utils_internal-necro_crystal_heart_equip") && isDamageLethal) {
                // Get current maximum health
                AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealthAttr == null) {
                    return;
                }
                
                // Get current hex counter value
                float hexCounter = getCurrentUsageCounter(player);
                
                // Increment counter by 2.0f (one heart)
                float newHexCounter = hexCounter + 2.0f;
                
                // Set the new counter value
                setUsageCounter(player, newHexCounter);
                
                // Calculate new maximum health (20 - hex)
                // Base health is 20.0 (10 hearts)
                double baseHealth = 20.0;
                double newMaxHealth = baseHealth - newHexCounter;
                
                // If new max health drops below minimum threshold, player must die
                if (newMaxHealth < MIN_HEALTH_THRESHOLD) {
                    // Don't zero out damage, allowing player to die
                    
                    // Reset hex counter and max health
                    // Actual reset will happen after death, so schedule it
                    player.level().getServer().tell(new net.minecraft.server.TickTask(1, () -> {
                        // Check if player is dead
                        if (player.isDeadOrDying()) {
                            // Reset hex counter
                            setUsageCounter(player, 0.0f);
                            
                            // Reset max health to original value
                            AttributeInstance playerHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
                            if (playerHealthAttr != null) {
                                playerHealthAttr.setBaseValue(baseHealth);
                            }
                        }
                    }));
                } else {
                    // Zero out damage, player survives
                    event.setAmount(0.0f);
                    
                    // Directly modify player's maximum health
                    maxHealthAttr.setBaseValue(newMaxHealth);
                    
                    // Remove stage after use
                    StageRegistry.removePlayerStage(player, "iska_utils_internal-necro_crystal_heart_equip");
                    
                    // Adjust current health to new maximum if necessary
                    if (player.getHealth() > player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                }
            }
        }
    }
    
    /**
     * Gets the current usage counter for the Necrotic Crystal Heart
     * 
     * @param player The player
     * @return The current counter value
     */
    private static float getCurrentUsageCounter(Player player) {
        try {
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
        } catch (Exception e) {
            return 0.0f;
        }
    }
    
    /**
     * Sets the usage counter for the Necrotic Crystal Heart
     * 
     * @param player The player
     * @param value The new counter value
     */
    private static void setUsageCounter(Player player, float value) {
        try {
            CompoundTag persistentData = player.getPersistentData();
            if (!persistentData.contains("iskautils")) {
                persistentData.put("iskautils", new CompoundTag());
            }
            
            CompoundTag iskaData = persistentData.getCompound("iskautils");
            if (!iskaData.contains("floatValues")) {
                iskaData.put("floatValues", new CompoundTag());
            }
            
            CompoundTag floatValues = iskaData.getCompound("floatValues");
            floatValues.putFloat(NECRO_CRYSTAL_HEART_COUNTER, value);
            iskaData.put("floatValues", floatValues);
            persistentData.put("iskautils", iskaData);
        } catch (Exception e) {
            // Silently fail
        }
    }
} 