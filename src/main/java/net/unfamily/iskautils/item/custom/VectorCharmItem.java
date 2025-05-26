package net.unfamily.iskautils.item.custom;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Special item for the Vector Charm that can be worn as a Curio when available.
 * When worn or held in hand, provides increased speed to vector plates.
 * Now includes energy storage and consumption system.
 */
public class VectorCharmItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmItem.class);

    /**
     * Force multiplier when the charm is worn or in hand
     */
    private static final float BOOST_MULTIPLIER = 1.5f;
    
    // Energy storage constants
    private static final String ENERGY_TAG = "Energy";
    
    // Effective consumption and capacity values
    private final int effectiveEnergyCapacity;
    private final List<Integer> effectiveEnergyConsume;

    public VectorCharmItem(Properties properties) {
        super(properties);
        
        // Initialize with default values - will be calculated lazily when config is loaded
        this.effectiveEnergyCapacity = -1; // -1 indicates not yet calculated
        this.effectiveEnergyConsume = new java.util.ArrayList<>();
        
        // LOGGER.debug("Vector Charm item created - energy values will be calculated when config loads");
    }
    
    // Determine effective energy capacity based on configurations (lazy initialization)
    private int determineEffectiveCapacity() {
        // Check if config is loaded
        if (Config.vectorCharmEnergyConsume == null) {
            return 0; // Config not loaded yet, assume no energy system
        }
        
        // If all consumptions are 0, we don't need capacity
        if (Config.vectorCharmEnergyConsume.stream().allMatch(consume -> consume <= 0)) {
            return 0;
        }
        
        // If configured capacity is 0, we don't store energy
        if (Config.vectorCharmEnergyCapacity <= 0) {
            return 0;
        }
        
        // Otherwise, use the configured value
        return Config.vectorCharmEnergyCapacity;
    }
    
    // Determine effective energy consumption based on configurations (lazy initialization)
    private List<Integer> determineEffectiveConsumption() {
        java.util.List<Integer> effectiveConsume = new java.util.ArrayList<>();
        
        // Check if config is loaded
        if (Config.vectorCharmEnergyConsume == null) {
            // Config not loaded yet, return default values
            return java.util.Arrays.asList(0, 0, 0, 0, 0, 0, 0);
        }
        
        int capacity = determineEffectiveCapacity();
        
        for (Integer consume : Config.vectorCharmEnergyConsume) {
            // If capacity is 0, we can't consume energy
            if (capacity <= 0) {
                effectiveConsume.add(0);
                continue;
            }
            
            // If consumption is 0, we don't consume energy
            if (consume <= 0) {
                effectiveConsume.add(0);
                continue;
            }
            
            // If configured consumption is greater than capacity, limit it to capacity
            if (consume > capacity) {
                effectiveConsume.add(capacity);
            } else {
                effectiveConsume.add(consume);
            }
        }
        
        return effectiveConsume;
    }
    
    // Check if the item can store energy
    public boolean canStoreEnergy() {
        int capacity = (effectiveEnergyCapacity == -1) ? determineEffectiveCapacity() : effectiveEnergyCapacity;
        return capacity > 0;
    }
    
    // Check if the item requires energy to function
    public boolean requiresEnergyToFunction() {
        List<Integer> consumption = getEffectiveEnergyConsume();
        return consumption.stream().anyMatch(consume -> consume > 0);
    }
    
    // Get energy consumption for specific speed level
    public int getEnergyConsumption(int speedLevel) {
        List<Integer> consumption = getEffectiveEnergyConsume();
        if (speedLevel < 0 || speedLevel >= consumption.size()) {
            return 0;
        }
        return consumption.get(speedLevel);
    }
    
    // Get effective energy capacity (lazy initialization)
    private int getEffectiveEnergyCapacity() {
        if (effectiveEnergyCapacity == -1) {
            return determineEffectiveCapacity();
        }
        return effectiveEnergyCapacity;
    }
    
    // Get effective energy consumption (lazy initialization)
    private List<Integer> getEffectiveEnergyConsume() {
        if (effectiveEnergyConsume.isEmpty() || Config.vectorCharmEnergyConsume == null) {
            return determineEffectiveConsumption();
        }
        return effectiveEnergyConsume;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Add the 'enchanted' effect to the charm
        return true;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        if (canStoreEnergy()) {
            int energy = getEnergyStored(stack);
            int maxEnergy = getMaxEnergyStored(stack);
            
            tooltipComponents.add(Component.literal(String.format("Energy: %,d / %,d RF", energy, maxEnergy)));
            
            if (requiresEnergyToFunction()) {
                List<Integer> consumption = getEffectiveEnergyConsume();
                tooltipComponents.add(Component.literal("§7Energy consumption per tick:"));
                
                // Safe access to array elements with fallback to 0
                String[] speedNames = {"None", "Slow", "Moderate", "Fast", "Extreme", "Ultra", "Hover"};
                for (int i = 0; i < speedNames.length; i++) {
                    int energyConsumption = (i < consumption.size()) ? consumption.get(i) : 0;
                    tooltipComponents.add(Component.literal("§8  " + speedNames[i] + ": " + energyConsumption + " RF"));
                }
            }
        } else {
            tooltipComponents.add(Component.literal("§7No energy required"));
        }
    }
    
    /**
     * This method is called every tick for every item in the inventory
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        if (entity instanceof Player player) {
            // Apply movement (includes energy consumption)
            net.unfamily.iskautils.client.VectorCharmMovement.applyMovement(player, stack);
        }
    }
    

    
    /**
     * Consumes energy for Vector Charm movement
     * @param stack The ItemStack
     * @param speedLevel The speed level for energy consumption
     * @return true if energy was consumed or no energy is required, false if insufficient energy
     */
    public boolean consumeEnergyForMovement(ItemStack stack, int speedLevel) {
        if (!requiresEnergyToFunction()) {
            return true; // No energy required
        }
        
        int consumption = getEnergyConsumption(speedLevel);
        if (consumption <= 0) {
            return true; // No consumption for this level
        }
        
        int currentEnergy = getEnergyStored(stack);
        if (currentEnergy >= consumption) {
            setEnergyStored(stack, currentEnergy - consumption);
            return true;
        }
        
        return false; // Insufficient energy
    }
    
    /**
     * Consumes energy from the Vector Charm for specific speed level
     * @param stack The ItemStack
     * @param speedLevel The speed level (0=none, 1=slow, 2=moderate, 3=fast, 4=extreme, 5=ultra, 6=hover)
     * @return true if energy was consumed or no energy is required, false if insufficient energy
     */
    public boolean consumeEnergy(ItemStack stack, int speedLevel) {
        if (!requiresEnergyToFunction()) {
            return true; // No energy required, always allow
        }
        
        int consumption = getEnergyConsumption(speedLevel);
        if (consumption <= 0) {
            return true; // No consumption for this level
        }
        
        int currentEnergy = getEnergyStored(stack);
        if (currentEnergy >= consumption) {
            setEnergyStored(stack, currentEnergy - consumption);
            return true;
        }
        
        return false; // Insufficient energy
    }
    
    /**
     * Gets the energy stored in the ItemStack
     */
    public int getEnergyStored(ItemStack stack) {
        if (!canStoreEnergy()) {
            return 0;
        }
        
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return tag.getInt(ENERGY_TAG);
    }
    
    /**
     * Sets the energy stored in the ItemStack
     */
    public void setEnergyStored(ItemStack stack, int energy) {
        if (!canStoreEnergy()) {
            return;
        }
        
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        int maxCapacity = getEffectiveEnergyCapacity();
        tag.putInt(ENERGY_TAG, Math.max(0, Math.min(energy, maxCapacity)));
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    
    /**
     * Gets the maximum energy that can be stored
     */
    public int getMaxEnergyStored(ItemStack stack) {
        return getEffectiveEnergyCapacity();
    }
    
    /**
     * Checks if the player has a Vector Charm equipped or in hand with sufficient energy
     * @param player The player to check
     * @param speedLevel The speed level to check energy for
     * @return ItemStack of the Vector Charm if found and has energy, null otherwise
     */
    public static ItemStack getActiveVectorCharm(Player player, int speedLevel) {
        // Check hands (highest priority)
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof VectorCharmItem charm) {
            if (charm.hasEnoughEnergy(mainHand, speedLevel)) {
                return mainHand;
            }
        }
        
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof VectorCharmItem charm) {
            if (charm.hasEnoughEnergy(offHand, speedLevel)) {
                return offHand;
            }
        }
        
        // If Curios is loaded, check Curios slots (second priority)
        if (ModUtils.isCuriosLoaded()) {
            ItemStack curioCharm = checkCuriosSlots(player, speedLevel);
            if (curioCharm != null) {
                return curioCharm;
            }
        }
        
        // Check player inventory (lowest priority)
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof VectorCharmItem charm) {
                if (charm.hasEnoughEnergy(stack, speedLevel)) {
                    return stack;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Checks if the charm has enough energy for the specified speed level without consuming it
     * @param stack The ItemStack
     * @param speedLevel The speed level to check
     * @return true if there's enough energy or no energy is required
     */
    public boolean hasEnoughEnergy(ItemStack stack, int speedLevel) {
        if (!requiresEnergyToFunction()) {
            return true; // No energy required
        }
        
        int consumption = getEnergyConsumption(speedLevel);
        if (consumption <= 0) {
            return true; // No consumption for this level
        }
        
        int currentEnergy = getEnergyStored(stack);
        return currentEnergy >= consumption;
    }
    
    /**
     * Checks if the player has a Vector Charm equipped or in hand (legacy method for compatibility)
     * @param player The player to check
     * @return true if the player has a Vector Charm
     */
    public static boolean hasVectorCharm(Player player) {
        return getActiveVectorCharm(player, 0) != null; // Check with no energy consumption
    }
    
    /**
     * Uses reflection to check if the Vector Charm is equipped in a Curios slot and consume energy
     */
    private static ItemStack checkCuriosSlots(Player player, int speedLevel) {
        try {
            // Approccio alternativo che usa getCuriosHandler invece di getAllEquipped
            Class<?> curioApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            
            // Ottiene l'handler delle Curios per il player
            Method getCuriosHandlerMethod = curioApiClass.getMethod("getCuriosHelper");
            Object curiosHelper = getCuriosHandlerMethod.invoke(null);
            
            Method getEquippedCurios = curiosHelper.getClass().getMethod("getEquippedCurios", LivingEntity.class);
            Object equippedCurios = getEquippedCurios.invoke(curiosHelper, player);
            
            if (equippedCurios instanceof Iterable<?> items) {
                for (Object itemPair : items) {
                    // Extract stack from each pair
                    Method getStackMethod = itemPair.getClass().getMethod("getRight");
                    ItemStack stack = (ItemStack) getStackMethod.invoke(itemPair);
                    
                    if (stack.getItem() instanceof VectorCharmItem charm) {
                        if (charm.hasEnoughEnergy(stack, speedLevel)) {
                            return stack;
                        }
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            // If there's an error in reflection, log and continue
            LOGGER.error("Error checking Curios slots: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
            return null;
        }
    }
    
    /**
     * Applies boost effect to vector speed with energy consumption
     * @param entity The entity crossing the vector plate
     * @param motion The original movement vector
     * @param speedLevel The speed level for energy consumption
     * @return The boosted movement vector
     */
    public static Vec3 applyBoost(LivingEntity entity, Vec3 motion, int speedLevel) {
        if (entity instanceof Player player) {
            ItemStack activeCharm = getActiveVectorCharm(player, speedLevel);
            if (activeCharm != null && activeCharm.getItem() instanceof VectorCharmItem charm) {
                // Consume energy for vector plate usage (separate from tick consumption)
                if (charm.consumeEnergyForVectorPlate(activeCharm, speedLevel)) {
                    return motion.multiply(BOOST_MULTIPLIER, BOOST_MULTIPLIER, BOOST_MULTIPLIER);
                }
            }
        }
        return motion;
    }
    
    /**
     * Consumes energy specifically for vector plate usage
     * @param stack The ItemStack
     * @param speedLevel The speed level
     * @return true if energy was consumed or no energy is required
     */
    private boolean consumeEnergyForVectorPlate(ItemStack stack, int speedLevel) {
        if (!requiresEnergyToFunction()) {
            return true; // No energy required
        }
        
        // Use a reduced consumption for vector plates (since tick consumption is separate)
        int consumption = getEnergyConsumption(speedLevel) / 4; // 25% of normal consumption
        if (consumption <= 0) {
            return true; // No consumption for this level
        }
        
        int currentEnergy = getEnergyStored(stack);
        if (currentEnergy >= consumption) {
            setEnergyStored(stack, currentEnergy - consumption);
            return true;
        }
        
        return false; // Insufficient energy
    }
    
    /**
     * Legacy method for compatibility
     */
    public static Vec3 applyBoost(LivingEntity entity, Vec3 motion) {
        return applyBoost(entity, motion, 1); // Default to slow speed level
    }

    /**
     * Gets debug information about where the charm is located
     * @param player The player to check
     * @param stack The charm stack to locate
     * @return String describing the location
     */
    public static String getCharmLocation(Player player, ItemStack stack) {
        // Check hands first
        if (player.getMainHandItem() == stack) {
            return "Main Hand";
        }
        if (player.getOffhandItem() == stack) {
            return "Off Hand";
        }
        
        // Check if it's in Curios
        if (ModUtils.isCuriosLoaded()) {
            try {
                Class<?> curioApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                
                // Ottiene l'handler delle Curios per il player
                Method getCuriosHandlerMethod = curioApiClass.getMethod("getCuriosHelper");
                Object curiosHelper = getCuriosHandlerMethod.invoke(null);
                
                Method getEquippedCurios = curiosHelper.getClass().getMethod("getEquippedCurios", LivingEntity.class);
                Object equippedCurios = getEquippedCurios.invoke(curiosHelper, player);
                
                if (equippedCurios instanceof Iterable<?> items) {
                    for (Object itemPair : items) {
                        Method getStackMethod = itemPair.getClass().getMethod("getRight");
                        ItemStack curioStack = (ItemStack) getStackMethod.invoke(itemPair);
                        if (curioStack == stack) {
                            return "Curios";
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore reflection errors
            }
        }
        
        // Check inventory
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (player.getInventory().items.get(i) == stack) {
                return "Inventory Slot " + i;
            }
        }
        
        return "Unknown";
    }
} 