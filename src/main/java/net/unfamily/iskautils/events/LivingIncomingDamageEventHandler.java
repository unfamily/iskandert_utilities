package net.unfamily.iskautils.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.stage.StageRegistry;

/**
 * Unified handler for intercepting LivingIncomingDamageEvent.
 * Handles multiple items that modify incoming damage:
 * - Greedy Shield: Chance to block or reduce incoming damage (highest priority)
 * - Necrotic Crystal Heart: Blocks lethal damage at the cost of max health
 */
@EventBusSubscriber
public class LivingIncomingDamageEventHandler {
    // Key for the Necrotic Crystal Heart counter
    private static final String NECRO_CRYSTAL_HEART_COUNTER = "necro_crystal_heart_hex";
    
    // Minimum health threshold before it becomes lethal
    private static final float MIN_HEALTH_THRESHOLD = 2.0f;

    /**
     * Intercepts incoming damage events for an entity.
     * Processes all damage-modifying items in order of priority.
     *
     * @param event The incoming damage event
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        // Get the entity taking damage
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        // Process Greedy Shield first (has highest priority)
        processGreedyShield(event, player);
        
        // Process Necrotic Crystal Heart (only if damage wasn't already blocked)
        if (event.getAmount() > 0.0f) {
            processNecroticCrystalHeart(event, player);
        }
    }

    /**
     * Processes Necrotic Crystal Heart logic.
     * Blocks lethal damage at the cost of reducing max health.
     */
    private static void processNecroticCrystalHeart(LivingIncomingDamageEvent event, Player player) {
        // Check if damage would be lethal
        boolean isDamageLethal = player.getHealth() <= event.getAmount();
        
        if (!isDamageLethal) {
            return;
        }

        // Check if player has the Necrotic Crystal Heart equipped
        if (!StageRegistry.playerHasStage(player, "iska_utils_internal-necro_crystal_heart_equip")) {
            // If player doesn't have the heart but has hex counter, reset it on death
            float hexCounter = getCurrentUsageCounter(player);
            if (hexCounter > 0) {
                player.level().getServer().tell(new net.minecraft.server.TickTask(1, () -> {
                    if (player.isDeadOrDying() && hexCounter > 0) {
                        // Reset hex counter
                        setUsageCounter(player, 0.0f);
                        
                        // Reset max health to original value
                        AttributeInstance playerHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
                        if (playerHealthAttr != null) {
                            playerHealthAttr.setBaseValue(20.0);
                        }
                    }
                }));
            }
            return;
        }

        // Base health is 20.0 (10 hearts)
        double baseHealth = 20.0;
        
        // Get current hex counter value
        float hexCounter = getCurrentUsageCounter(player);
        
        // Get current maximum health
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }
        
        // Increment counter by 2.0f (one heart)
        float newHexCounter = hexCounter + 2.0f;
        
        // Set the new counter value
        setUsageCounter(player, newHexCounter);
        
        // Calculate new maximum health (20 - hex)
        double newMaxHealth = baseHealth - newHexCounter;
        
        // If new max health drops below minimum threshold, player must die
        if (newMaxHealth < MIN_HEALTH_THRESHOLD) {
            // Don't zero out damage, allowing player to die
            
            // Schedule reset after death
            player.level().getServer().tell(new net.minecraft.server.TickTask(1, () -> {
                // Reset hex counter and max health
                setUsageCounter(player, 0.0f);
                
                AttributeInstance playerHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
                if (playerHealthAttr != null) {
                    playerHealthAttr.setBaseValue(baseHealth);
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

    /**
     * Processes Greedy Shield logic.
     * Has a chance to completely block damage, or if that fails, reduce it.
     */
    private static void processGreedyShield(LivingIncomingDamageEvent event, Player player) {
        // Check if player has the Greedy Shield equipped
        if (!StageRegistry.playerHasStage(player, "iska_utils_internal-greedy_shield_equip")) {
            return;
        }

        float originalDamage = event.getAmount();
        if (originalDamage <= 0.0f) {
            return; // No damage to process
        }

        RandomSource random = player.getRandom();
        
        // First check: chance to completely block damage
        if (random.nextDouble() < Config.greedyShieldBlockChance) {
            // Completely block the damage
            event.setAmount(0.0f);
            if (player.level().isClientSide) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.iska_utils.greedy_shield.blocked"),
                    true // actionbar
                );
            }
            return;
        }

        // Second check: if block failed, chance to reduce damage
        if (random.nextDouble() < Config.greedyShieldReduceChance) {
            // Reduce damage by the configured amount
            float reducedDamage = originalDamage * (1.0f - (float)Config.greedyShieldReduceAmount);
            event.setAmount(reducedDamage);
            if (player.level().isClientSide) {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.iska_utils.greedy_shield.reduced"),
                    true // actionbar
                );
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
