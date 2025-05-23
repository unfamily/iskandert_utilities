package net.unfamily.iskautils.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Special item for the Vector Charm that can be worn as a Curio when available.
 * When worn or held in hand, provides increased speed to vector plates.
 */
public class VectorCharmItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmItem.class);

    /**
     * Force multiplier when the charm is worn or in hand
     */
    private static final float BOOST_MULTIPLIER = 1.5f;

    public VectorCharmItem(Properties properties) {
        super(properties);
        // LOGGER.debug("Vector Charm item created");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Add the 'enchanted' effect to the charm
        return true;
    }
    
    /**
     * This method is called every tick for every item in the inventory
     */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        // Execute only on client and only for players
        if (level.isClientSide && entity instanceof Player player) {
            // Apply Vector Charm movement directly when the item is in inventory
            net.unfamily.iskautils.client.VectorCharmMovement.applyMovement(player);
        }
    }
    
    /**
     * Checks if the player has a Vector Charm equipped or in hand
     * @param player The player to check
     * @return true if the player has a Vector Charm
     */
    public static boolean hasVectorCharm(Player player) {
        // Check hands (highest priority)
        if (player.getMainHandItem().getItem() instanceof VectorCharmItem ||
            player.getOffhandItem().getItem() instanceof VectorCharmItem) {
            return true;
        }
        
        // If Curios is loaded, check Curios slots (second priority)
        if (ModUtils.isCuriosLoaded() && checkCuriosSlots(player)) {
            return true;
        }
        
        // Check player inventory (lowest priority)
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof VectorCharmItem) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Uses reflection to check if the Vector Charm is equipped in a Curios slot
     */
    private static boolean checkCuriosSlots(Player player) {
        try {
            // Use reflection to access Curios API
            Class<?> curioApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            
            // Approach 1: Use hasAnyEquipped method (preferred)
            try {
                Method hasItemMethod = curioApiClass.getMethod("hasAnyEquipped", LivingEntity.class, java.util.function.Predicate.class);
                if (hasItemMethod != null) {
                    // Create a predicate for VectorCharmItem
                    java.util.function.Predicate<ItemStack> predicate = 
                        (ItemStack stack) -> stack.getItem() instanceof VectorCharmItem;
                    
                    // Invoke the method
                    Boolean result = (Boolean) hasItemMethod.invoke(null, player, predicate);
                    return result;
                }
            } catch (Exception e) {
                // LOGGER.warn("Could not use hasAnyEquipped method: {}", e.getMessage());
                // Fallback to method 2
            }
            
            // Approach 2: Try to find Vector Charm through getAllEquipped
            try {
                Method getAllEquippedMethod = curioApiClass.getMethod("getAllEquipped", LivingEntity.class);
                if (getAllEquippedMethod != null) {
                    Object allEquipped = getAllEquippedMethod.invoke(null, player);
                    if (allEquipped instanceof Iterable<?> items) {
                        for (Object itemPair : items) {
                            // Extract stack from each pair
                            Method getStackMethod = itemPair.getClass().getMethod("getRight");
                            ItemStack stack = (ItemStack) getStackMethod.invoke(itemPair);
                            
                            if (stack.getItem() instanceof VectorCharmItem) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // LOGGER.warn("Could not use getAllEquipped method: {}", e.getMessage());
                // Fallback to method 3
            }
            
            // Approach 3: Manual approach using getCurios
            try {
                Method getCuriosMethod = curioApiClass.getMethod("getCurios", LivingEntity.class);
                Object curiosHelper = getCuriosMethod.invoke(null, player);

                if (curiosHelper != null) {
                    // Check each curio slot
                    Class<?> iCurioHelperClass = Class.forName("top.theillusivec4.curios.api.type.capability.ICurioHelper");
                    Method getEquippedCuriosMethod = iCurioHelperClass.getMethod("getEquippedCurios", LivingEntity.class);
                    Object equippedCurios = getEquippedCuriosMethod.invoke(curiosHelper, player);

                    if (equippedCurios instanceof Iterable<?> curios) {
                        for (Object curio : curios) {
                            // Get the ItemStack of the curio
                            Method getStackMethod = curio.getClass().getMethod("getStack");
                            ItemStack stack = (ItemStack) getStackMethod.invoke(curio);
                            
                            if (stack.getItem() instanceof VectorCharmItem) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // LOGGER.warn("Could not use getCurios method: {}", e.getMessage());
            }
            
            return false;
            
        } catch (Exception e) {
            // If there's an error in reflection, log and continue
            LOGGER.error("Error checking Curios slots: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Applies boost effect to vector speed
     * @param entity The entity crossing the vector plate
     * @param motion The original movement vector
     * @return The boosted movement vector
     */
    public static Vec3 applyBoost(LivingEntity entity, Vec3 motion) {
        if (entity instanceof Player player && hasVectorCharm(player)) {
            return motion.multiply(BOOST_MULTIPLIER, BOOST_MULTIPLIER, BOOST_MULTIPLIER);
        }
        return motion;
    }
} 