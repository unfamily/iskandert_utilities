package net.unfamily.iskautils.item;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.unfamily.iskautils.command.CommandItemAction;
import net.unfamily.iskautils.command.CommandItemDefinition;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Consumer;

/**
 * An item that can execute automatic actions based on conditions
 * Now uses fixed definitions assigned at registration
 */
public class CommandItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Map<String, Long>> PLAYER_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Set<String>> PROCESSED_FIRST_TICK = new HashMap<>();
    
    // Fixed definition assigned at registration
    private final CommandItemDefinition definition;
    
    public CommandItem(Properties properties, CommandItemDefinition definition) {
        super(properties);
        this.definition = definition;
    }
    
    /**
     * Gets the definition for this command item
     */
    public CommandItemDefinition getDefinition() {
        return definition;
    }
    
    /**
     * Gets the ID of this command item
     */
    public String getDefinitionId() {
        return definition.getId();
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // Add text to the tooltip
        tooltip.add(Component.literal("Command Item: " + getDefinitionId())
                .withStyle(ChatFormatting.GRAY));
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        
        // Process only on server side and for players
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        
        // Get the player's UUID
        UUID playerUuid = player.getUUID();
        
        // Check if conditions are met
        if (!areConditionsMet(player)) {
            return;
        }
        
        // Check if we've already processed first tick actions for this item
        boolean processedFirstTick = hasProcessedFirstTick(playerUuid, definition.getId());
        
        // Process first tick actions if it's the first time
        if (!processedFirstTick) {
            // Mark as processed
            markAsProcessedFirstTick(playerUuid, definition.getId());
            
            // Execute first tick actions
            if (!definition.getFirstTickActions().isEmpty()) {
                LOGGER.debug("Executing first tick actions for command item {} for player {}", 
                        definition.getId(), player.getName().getString());
                executeActions(player, definition.getFirstTickActions(), stack, slot);
            }
            
            // Don't return here to allow regular tick actions to also execute
            // in the same tick if needed
        }
        
        // Check cooldown for regular tick actions
        if (isOnCooldown(playerUuid, definition.getId())) {
            return;
        }
        
        // Execute regular tick actions
        if (!definition.getTickActions().isEmpty()) {
            LOGGER.debug("Executing tick actions for command item {} for player {}", 
                    definition.getId(), player.getName().getString());
            executeActions(player, definition.getTickActions(), stack, slot);
            
            // Update cooldown
            updateCooldown(playerUuid, definition.getId());
        }
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Process only on server side
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }
        
        // Check if conditions are met
        if (!areConditionsMet(serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        
        // Execute use actions
        if (!definition.getUseActions().isEmpty()) {
            LOGGER.debug("Executing use actions for command item {} for player {}", 
                    definition.getId(), player.getName().getString());
            executeActions(serverPlayer, definition.getUseActions(), stack, 
                    hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1);
            
            // Update cooldown
            updateCooldown(serverPlayer.getUUID(), definition.getId());
            
            return InteractionResultHolder.success(stack);
        }
        
        return InteractionResultHolder.pass(stack);
    }
    
    /**
     * Checks if all conditions are met for this command item
     */
    private boolean areConditionsMet(ServerPlayer player) {
        // Use the definition's checkAllStages method
        return definition.checkAllStages(player);
    }
    
    /**
     * Executes a list of actions
     */
    private void executeActions(ServerPlayer player, List<CommandItemAction> actions, ItemStack stack, int slot) {
        for (CommandItemAction action : actions) {
            if (stack.isEmpty()) {
                // Item consumed in a previous action
                break;
            }
            
            // Check if this action has its own stage requirements (for DEF logic)
            if (definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF &&
                !action.getStages().isEmpty() &&
                !action.checkActionStages(player, definition)) {
                // Skip this action if its requirements are not met
                LOGGER.debug("Skipping action due to stage requirements not met");
                continue;
            }
            
            switch (action.getType()) {
                case EXECUTE:
                    executeCommand(player, action.getCommand());
                    break;
                    
                case DELAY:
                    // Delays are not fully implemented in this simple version
                    // Would require a more complex scheduler
                    LOGGER.debug("Delay actions are not yet fully implemented");
                    break;
                    
                case ITEM:
                    if (processItemAction(player, action.getItemAction(), stack, slot)) {
                        // The item was consumed or modified
                        break;
                    }
                    break;
                    
                default:
                    LOGGER.warn("Unknown action type: {}", action.getType());
                    break;
            }
        }
    }
    
    /**
     * Executes a command as the player
     */
    private void executeCommand(ServerPlayer player, String command) {
        try {
            // Execute the command on the server
            player.getServer().getCommands().performPrefixedCommand(
                    player.getServer().createCommandSourceStack().withEntity(player), command);
        } catch (Exception e) {
            LOGGER.error("Error executing command '{}' for player {}: {}", 
                    command, player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * Processes an item action
     * @return true if the item was consumed
     */
    private boolean processItemAction(ServerPlayer player, CommandItemAction.ItemActionType actionType, ItemStack stack, int slot) {
        switch (actionType) {
            case DELETE:
            case CONSUME:
                // Consume the item (remove from inventory)
                if (slot >= 0) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                } else {
                    // If it's in offhand or elsewhere, consume it directly
                    stack.shrink(1);
                }
                return true;
                
            case DROP:
                // Drop the item on the ground
                ItemStack droppedStack = stack.copy();
                droppedStack.setCount(1);
                
                // Remove one from the original
                stack.shrink(1);
                
                // Create an item entity
                spawnItemEntity(player, droppedStack);
                return stack.isEmpty();
                
            case DAMAGE:
                // Damage the item (if it can be damaged)
                if (stack.isDamageableItem()) {
                    // Damage the item by 1 point
                    stack.setDamageValue(stack.getDamageValue() + 1);
                    
                    // If damage exceeds max durability, destroy the item
                    if (stack.getDamageValue() >= stack.getMaxDamage()) {
                        stack.shrink(1);
                        LOGGER.debug("Item {} destroyed due to excessive damage", definition.getId());
                    }
                }
                return stack.isEmpty();
                
            case DELETE_ALL:
                // Remove all items of this type from inventory
                int removed = 0;
                
                // Search in all inventory slots
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (invStack.getItem() == this) {
                        removed += invStack.getCount();
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
                
                LOGGER.debug("Removed {} command items {} from player inventory", 
                        removed, definition.getId());
                return true;
                
            case DROP_ALL:
                // Drop all items of this type from inventory
                int dropped = 0;
                
                // Search in all inventory slots
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (invStack.getItem() == this) {
                        ItemStack toDrop = invStack.copy();
                        dropped += toDrop.getCount();
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                        spawnItemEntity(player, toDrop);
                    }
                }
                
                LOGGER.debug("Dropped {} command items {} from player inventory", 
                        dropped, definition.getId());
                return true;
                
            default:
                LOGGER.warn("Unknown item action type: {}", actionType);
                return false;
        }
    }
    
    /**
     * Spawns an item entity near the player
     */
    private void spawnItemEntity(ServerPlayer player, ItemStack stack) {
        double x = player.getX();
        double y = player.getY() + 0.5;
        double z = player.getZ();
        
        ItemEntity itemEntity = new ItemEntity(player.level(), x, y, z, stack);
        itemEntity.setPickUpDelay(40); // 2 seconds delay for pickup
        
        player.level().addFreshEntity(itemEntity);
    }
    
    /**
     * Checks if an item is on cooldown for a player
     */
    private boolean isOnCooldown(UUID playerUuid, String itemId) {
        // Get cooldowns for this player
        Map<String, Long> cooldowns = PLAYER_COOLDOWNS.getOrDefault(playerUuid, new HashMap<>());
        
        // Check if the item is on cooldown
        if (cooldowns.containsKey(itemId)) {
            long lastUsed = cooldowns.get(itemId);
            long currentTime = System.currentTimeMillis();
            
            // Convert cooldown from ticks to milliseconds (1 tick = 50 ms)
            long cooldownMs = definition.getCooldown() * 50L;
            
            return (currentTime - lastUsed) < cooldownMs;
        }
        
        return false;
    }
    
    /**
     * Updates the cooldown for an item
     */
    private void updateCooldown(UUID playerUuid, String itemId) {
        // Get cooldowns for this player
        Map<String, Long> cooldowns = PLAYER_COOLDOWNS.computeIfAbsent(playerUuid, k -> new HashMap<>());
        
        // Update the last use time
        cooldowns.put(itemId, System.currentTimeMillis());
    }
    
    /**
     * Checks if the first tick has been processed for an item
     */
    private boolean hasProcessedFirstTick(UUID playerUuid, String itemId) {
        // Get processed items for this player
        Set<String> processed = PROCESSED_FIRST_TICK.getOrDefault(playerUuid, new HashSet<>());
        
        // Check if the item has been processed
        return processed.contains(itemId);
    }
    
    /**
     * Marks an item as processed for the first tick
     */
    private void markAsProcessedFirstTick(UUID playerUuid, String itemId) {
        // Get processed items for this player
        Set<String> processed = PROCESSED_FIRST_TICK.computeIfAbsent(playerUuid, k -> new HashSet<>());
        
        // Mark the item as processed
        processed.add(itemId);
    }
    
    /**
     * Clears player data when they disconnect or are removed
     */
    public static void clearPlayerData(UUID playerUuid) {
        PLAYER_COOLDOWNS.remove(playerUuid);
        PROCESSED_FIRST_TICK.remove(playerUuid);
    }
} 