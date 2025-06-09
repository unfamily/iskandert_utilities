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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * An item that can execute automatic actions based on conditions
 * Now uses fixed definitions assigned at registration
 */
public class CommandItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Map<String, Long>> PLAYER_COOLDOWNS = new HashMap<>();
    
    // To track already processed ItemStacks, we use a combination of player UUID and stack hash
    private static final Map<UUID, Set<Integer>> FIRST_TICK_PROCESSED = new HashMap<>();
    
    // Delayed action queue system
    private static class DelayedAction {
        final ServerPlayer player;
        final CommandItemAction action;
        final ItemStack stack;
        final int slot;
        final long executeAtTick;
        
        DelayedAction(ServerPlayer player, CommandItemAction action, ItemStack stack, int slot, long executeAtTick) {
            this.player = player;
            this.action = action;
            this.stack = stack;
            this.slot = slot;
            this.executeAtTick = executeAtTick;
        }
    }
    
    // Queue for delayed actions - CopyOnWriteArrayList is inherently thread-safe
    private static final Map<UUID, CopyOnWriteArrayList<DelayedAction>> DELAYED_ACTIONS = new ConcurrentHashMap<>();
    
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
    public boolean isFoil(ItemStack stack) {
        return definition.isGlowing() || super.isFoil(stack);
    }
    
    /**
     * Generate a unique hash for the ItemStack to track processed items
     */
    private int getItemStackHash(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        
        int hash = stack.getItem().hashCode();
        hash = 31 * hash + stack.getCount();
        return hash;
    }
    
    /**
     * Check if an ItemStack has already been processed
     */
    private boolean isItemStackProcessed(UUID playerUuid, ItemStack stack) {
        Set<Integer> processedItems = FIRST_TICK_PROCESSED.get(playerUuid);
        if (processedItems == null) return false;
        
        int stackHash = getItemStackHash(stack);
        return processedItems.contains(stackHash);
    }
    
    /**
     * Mark an ItemStack as processed
     */            
    private void markItemStackProcessed(UUID playerUuid, ItemStack stack) {
        int stackHash = getItemStackHash(stack);
        FIRST_TICK_PROCESSED.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(stackHash);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        
        // Process only on server side and for players
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        
        // Process delayed actions for this player
        processDelayedActions(player, level);
        
        // Check if conditions are met
        if (!areConditionsMet(player)) {
            return;
        }
        
        UUID playerUuid = player.getUUID();
        
        // Check if first tick actions have been processed for this specific itemstack
        boolean processedFirstTick = isItemStackProcessed(playerUuid, stack);
        
        if (!processedFirstTick) {
            // First time we see this item - execute first tick actions
            if (!definition.getFirstTickActions().isEmpty()) {
                LOGGER.debug("Executing first tick actions for command item {} for player {}", 
                        definition.getId(), player.getName().getString());
                
                // Execute first tick actions
                executeFirstTickActions(player, definition.getFirstTickActions(), stack, slot);
                
                // If the item was consumed, do not continue
                if (stack.isEmpty()) {
                    return;
                }
                
                // Mark as processed only if the item still exists
                markItemStackProcessed(playerUuid, stack);
            } else {
                // If there are no first tick actions, mark as processed anyway
                markItemStackProcessed(playerUuid, stack);
            }
        } else {
            // For the DEF_AND and DEF_OR logic, stages are checked for each action
            // and no global check is needed here
            if (definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_AND || 
                definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_OR) {
                // No check needed, proceed
            } 
            // For the AND and OR logic, we check that all required stages are satisfied
            else if (definition.getStagesLogic() == CommandItemDefinition.StagesLogic.AND || 
                     definition.getStagesLogic() == CommandItemDefinition.StagesLogic.OR) {
                // We use the areConditionsMet method we've already called above
                // This is just an extra check in case stages have changed
                if (!areConditionsMet(player)) {
                    LOGGER.debug("Item {} stages no longer met, skipping tick actions");
                    return;
                }
            }
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
    
    /**
     * Execute only first tick actions with special attention
     */
    private void executeFirstTickActions(ServerPlayer player, List<CommandItemAction> actions, ItemStack stack, int slot) {
        boolean itemDeleted = false;
        boolean hasExplicitDeleteAction = false;
        
        // Preliminary check if there are delete/consume actions
        for (CommandItemAction action : actions) {
            if (action.getType() == CommandItemAction.ActionType.ITEM) {
                CommandItemAction.ItemActionType itemAction = action.getItemAction();
                if (itemAction == CommandItemAction.ItemActionType.DELETE || 
                    itemAction == CommandItemAction.ItemActionType.CONSUME ||
                    itemAction == CommandItemAction.ItemActionType.DELETE_ALL) {
                    hasExplicitDeleteAction = true;
                    break;
                }
            } else if (action.getType() == CommandItemAction.ActionType.IF) {
                // Check in sub-actions as well
                for (CommandItemAction subAction : action.getSubActions()) {
                    if (subAction.getType() == CommandItemAction.ActionType.ITEM) {
                        CommandItemAction.ItemActionType itemAction = subAction.getItemAction();
                        if (itemAction == CommandItemAction.ItemActionType.DELETE || 
                            itemAction == CommandItemAction.ItemActionType.CONSUME ||
                            itemAction == CommandItemAction.ItemActionType.DELETE_ALL) {
                            hasExplicitDeleteAction = true;
                            break;
                        }
                    }
                }
                if (hasExplicitDeleteAction) break;
            }
        }
        
        LOGGER.debug("First tick actions for item {}: has explicit delete action = {}", 
                definition.getId(), hasExplicitDeleteAction);
        
        for (CommandItemAction action : actions) {
            if (stack.isEmpty() && !itemDeleted) {
                // Item consumed in a previous action, mark as deleted but continue
                itemDeleted = true;
                LOGGER.debug("Item consumed in previous first tick action, continuing with remaining actions");
            }
            
            // Check if this action has its own stage requirements (for DEF logic)
            if ((definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_AND || 
                 definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_OR) &&
                !action.getStages().isEmpty() &&
                !action.checkActionStages(player, definition)) {
                // Skip this action if its requirements are not met
                LOGGER.debug("Skipping action due to stage requirements not met");
                continue;
            }
            
            // Special case for IF actions
            if (action.getType() == CommandItemAction.ActionType.IF) {
                // Check if conditions are met based on the indices
                boolean conditionsMet = action.checkConditionsByIndices(player, definition);
                LOGGER.debug("IF condition check result for conditions {}: {}", 
                    action.getConditionIndices(), conditionsMet);
                    
                if (conditionsMet) {
                    // Find consumed actions in sub-actions
                    boolean hasConsumed = false;
                    for (CommandItemAction subAction : action.getSubActions()) {
                        if (subAction.getType() == CommandItemAction.ActionType.ITEM) {
                            CommandItemAction.ItemActionType itemAction = subAction.getItemAction();
                            if (itemAction == CommandItemAction.ItemActionType.DELETE || 
                                itemAction == CommandItemAction.ItemActionType.CONSUME) {
                                if (!stack.isEmpty()) {
                                    processItemAction(player, itemAction, stack, slot);
                                    hasConsumed = true;
                                }
                                break;
                            }
                        }
                    }
                    
                    boolean subActionItemDeleted = itemDeleted || (hasConsumed && stack.isEmpty());
                    
                    // Execute all other sub-actions
                    for (CommandItemAction subAction : action.getSubActions()) {
                        if (stack.isEmpty() && !subActionItemDeleted) {
                            subActionItemDeleted = true;
                            LOGGER.debug("Subaction detected item was consumed, continuing with remaining subactions");
                        }
                        
                        // Skip consumed actions we've already executed
                        if (subAction.getType() == CommandItemAction.ActionType.ITEM) {
                            CommandItemAction.ItemActionType itemAction = subAction.getItemAction();
                            if ((itemAction == CommandItemAction.ItemActionType.DELETE || 
                                itemAction == CommandItemAction.ItemActionType.CONSUME) && 
                                hasConsumed) {
                                continue;
                            }
                        }
                        
                        // Execute the action based on type and item availability
                        if (!stack.isEmpty() || subAction.getType() == CommandItemAction.ActionType.EXECUTE) {
                            executeAction(player, subAction, stack, slot);
                        } else {
                            LOGGER.debug("Skipping subaction that requires item because item was already deleted");
                        }
                    }
                    continue; // We handled the IF action, proceed to the next
                }
            }
            
            // Give precedence to DELETE/CONSUME actions
            if (action.getType() == CommandItemAction.ActionType.ITEM) {
                CommandItemAction.ItemActionType itemAction = action.getItemAction();
                if (itemAction == CommandItemAction.ItemActionType.DELETE || 
                    itemAction == CommandItemAction.ItemActionType.CONSUME) {
                    
                    if (!stack.isEmpty()) {
                        processItemAction(player, itemAction, stack, slot);
                        if (stack.isEmpty()) {
                            itemDeleted = true;
                            LOGGER.debug("Item deleted by first tick action, continuing with remaining actions");
                        }
                    }
                }
            }
            
            // Execute the action based on type and item availability
            if (!stack.isEmpty() || action.getType() == CommandItemAction.ActionType.EXECUTE) {
                executeAction(player, action, stack, slot);
            } else {
                LOGGER.debug("Skipping action that requires item because item was already deleted");
            }
        }
        
        if (stack.isEmpty() && !hasExplicitDeleteAction && !itemDeleted) {
            LOGGER.debug("Item was unexpectedly deleted without explicit delete action, this is a bug!");
        }
    }
    
    /**
     * Processes all pending delayed actions for a player
     */
    private void processDelayedActions(ServerPlayer player, Level level) {
        UUID playerUuid = player.getUUID();
        CopyOnWriteArrayList<DelayedAction> actions = DELAYED_ACTIONS.get(playerUuid);
        
        if (actions == null || actions.isEmpty()) {
            return;
        }
        
        long currentTick = level.getGameTime();
        List<DelayedAction> actionsToRemove = new ArrayList<>();
        
        for (DelayedAction action : actions) {
            if (currentTick >= action.executeAtTick) {
                try {
                    // Execute the scheduled action
                    LOGGER.debug("Executing delayed action for player {} scheduled for tick {} (current tick: {})",
                            player.getName().getString(), action.executeAtTick, currentTick);
                    
                    // We can execute the action independently of the item's state
                    // for EXECUTE and IF types
                    ItemStack stack = action.stack;
                    if (action.action.getType() == CommandItemAction.ActionType.EXECUTE || 
                        action.action.getType() == CommandItemAction.ActionType.IF) {
                        // Execute these actions even if the item might be gone
                        LOGGER.debug("Executing delayed {} action even if item might be gone", action.action.getType());
                        executeAction(action.player, action.action, stack, action.slot);
                    } else if (!stack.isEmpty()) {
                        // For other types of action, we check that the item still exists
                        executeAction(action.player, action.action, stack, action.slot);
                    } else {
                        LOGGER.debug("Skipping delayed action of type {} because item is gone", action.action.getType());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error executing delayed action: {}", e.getMessage());
                    if (LOGGER.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                } finally {
                    // Always add to the list of actions to remove, even in case of error
                    actionsToRemove.add(action);
                }
            }
        }
        
        // Remove completed actions (CopyOnWriteArrayList handles concurrency)
        if (!actionsToRemove.isEmpty()) {
            actions.removeAll(actionsToRemove);
            
            // Remove the list if empty
            if (actions.isEmpty()) {
                DELAYED_ACTIONS.remove(playerUuid);
            }
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
        // For the DEF logic, the check is done for each action
        // so here we simply return true
        if (definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_AND || 
            definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_OR) {
            LOGGER.debug("Item {} uses DEF logic, stage checks will be done per action");
            return true;
        }
        
        // Otherwise we use the checkAllStages method of the definition
        boolean result = definition.checkAllStages(player);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Item {} stage check result: {} (logic: {})", 
                definition.getId(), result, definition.getStagesLogic());
            
            // Detailed log of all stages
            List<CommandItemDefinition.StageCondition> stages = definition.getStages();
            if (!stages.isEmpty()) {
                for (int i = 0; i < stages.size(); i++) {
                    CommandItemDefinition.StageCondition stage = stages.get(i);
                    boolean stageResult = definition.checkSingleStage(player, stage);
                    LOGGER.debug("Stage[{}]: {}.{} should be {} = {}", 
                        i, stage.getStageType(), stage.getStage(), stage.shouldBeSet(), stageResult);
                }
            } else {
                LOGGER.debug("Item has no stages defined, always active");
            }
        }
        
        return result;
    }
    
    /**
     * Executes a list of actions
     */
    private void executeActions(ServerPlayer player, List<CommandItemAction> actions, ItemStack stack, int slot) {
        boolean itemDeleted = false;
        
        LOGGER.debug("Executing {} actions for player {} with item {}", actions.size(), player.getName().getString(), definition.getId());
        
        // Debug: show all IF block indices
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("All IF blocks in actions list:");
            for (int i = 0; i < actions.size(); i++) {
                CommandItemAction action = actions.get(i);
                if (action.getType() == CommandItemAction.ActionType.IF) {
                    LOGGER.debug("  IF block {} with conditions {}", i, action.getConditionIndices());
                }
            }
        }
        
        // For each action, we perform an updated condition check (for IF actions)
        // and then execute the action
        for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
            CommandItemAction action = actions.get(actionIndex);
            
            if (stack.isEmpty() && !itemDeleted) {
                // Item consumed in a previous action, mark as deleted but continue
                itemDeleted = true;
                LOGGER.debug("Item consumed in previous action, continuing with remaining actions");
            }
            
            // Check if this action has its own stage requirements (for DEF logic)
            if ((definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_AND || 
                 definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_OR) &&
                !action.getStages().isEmpty() &&
                !action.checkActionStages(player, definition)) {
                // Skip this action if its requirements are not met
                LOGGER.debug("Skipping action due to stage requirements not met");
                continue;
            }
            
            // Debug for action type
            LOGGER.debug("Processing action of type: {}", action.getType());
            
            // If the action is of type IF, we perform an updated condition check
            if (action.getType() == CommandItemAction.ActionType.IF) {
                // Recheck conditions to ensure they are still valid
                // This is important especially for consecutive actions that might
                // modify the same stages
                boolean conditionsMet = action.checkConditionsByIndices(player, definition);
                LOGGER.debug("Checking IF action at index {} with conditions {}: {}", 
                    actionIndex, action.getConditionIndices(), conditionsMet);
                
                if (conditionsMet) {
                    // Execute the IF block with its sub-actions
                    if (!action.getSubActions().isEmpty()) {
                        LOGGER.debug("Executing IF block with {} sub-actions", action.getSubActions().size());
                        
                        // For delay actions, we'll handle them separately
                        boolean hasDelay = false;
                        int delayIndex = -1;
                        for (int i = 0; i < action.getSubActions().size(); i++) {
                            if (action.getSubActions().get(i).getType() == CommandItemAction.ActionType.DELAY) {
                                hasDelay = true;
                                delayIndex = i;
                                break;
                            }
                        }
                        
                        if (hasDelay) {
                            // If there's a delay, use executeIfActionsWithDelays that handles delays correctly
                            LOGGER.debug("IF block has DELAY at index {}, using special handling", delayIndex);
                            executeIfActionsWithDelays(player, action.getSubActions(), stack, slot);
                        } else {
                            // If there are no delays, execute all sub-actions immediately
                            LOGGER.debug("IF block has no DELAY, executing all sub-actions immediately");
                            for (CommandItemAction subAction : action.getSubActions()) {
                                if (!stack.isEmpty() || subAction.getType() == CommandItemAction.ActionType.EXECUTE || subAction.getType() == CommandItemAction.ActionType.IF) {
                                    executeAction(player, subAction, stack, slot);
                                } else {
                                    LOGGER.debug("Skipping sub-action that requires item because item is gone");
                                }
                            }
                        }
                        
                        // Check if stack is now empty
                        if (!itemDeleted && stack.isEmpty()) {
                            itemDeleted = true;
                            LOGGER.debug("Item deleted during IF block execution, marking as deleted");
                        }
                    }
                } else {
                    LOGGER.debug("Skipping IF block at index {} because conditions are not met", actionIndex);
                }
                
                // Continue with the next action
                continue;
            }
            
            // For other types of action, use normal logic
            if (!stack.isEmpty() || action.getType() == CommandItemAction.ActionType.EXECUTE || action.getType() == CommandItemAction.ActionType.IF) {
                // All EXECUTE and IF actions can be executed even after item deletion
                executeAction(player, action, stack, slot);
                // Check if this action deleted the item
                if (!itemDeleted && stack.isEmpty() && action.getType() == CommandItemAction.ActionType.ITEM) {
                    itemDeleted = true;
                    LOGGER.debug("Item deleted by action, continuing with remaining actions");
                }
            } else if (itemDeleted && action.getType() == CommandItemAction.ActionType.ITEM) {
                // Skip item actions if item is deleted
                LOGGER.debug("Skipping item action because item was already deleted: {}", 
                    action.getItemAction() != null ? action.getItemAction().name() : "unknown");
            }
        }
    }
    
    /**
     * Executes a single action
     */
    private void executeAction(ServerPlayer player, CommandItemAction action, ItemStack stack, int slot) {
        switch (action.getType()) {
            case EXECUTE:
                executeCommand(player, action.getCommand());
                break;
                
            case DELAY:
                queueDelayedAction(player, action, stack, slot);
                break;
                
            case ITEM:
                processItemAction(player, action.getItemAction(), stack, slot);
                break;
                
            case IF:
                // Check if conditions are met based on the indices
                boolean conditionsMet = action.checkConditionsByIndices(player, definition);
                LOGGER.debug("Checking IF condition: {} = {}", action.getConditionIndices(), conditionsMet);
                
                if (conditionsMet) {
                    // Execute all sub-actions if conditions are met, but handle delays specially
                    List<CommandItemAction> subActions = action.getSubActions();
                    if (!subActions.isEmpty()) {
                        LOGGER.debug("Executing IF block with {} sub-actions", subActions.size());
                        executeIfActionsWithDelays(player, subActions, stack, slot);
                    }
                } else if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Skipping IF block actions because conditions are not met");
                }
                break;
                
            default:
                LOGGER.warn("Unknown action type: {}", action.getType());
                break;
        }
    }
    
    /**
     * Execute actions in an IF block, correctly handling delays
     * in modo che le azioni successive a un delay vengano eseguite dopo il ritardo
     */
    private void executeIfActionsWithDelays(ServerPlayer player, List<CommandItemAction> actions, ItemStack stack, int slot) {
        // If there are no actions, exit
        if (actions.isEmpty()) {
            return;
        }
        
        boolean itemDeleted = false;
        boolean hasExplicitDeleteAction = false;
        
        // Check if there are delete actions
        for (CommandItemAction action : actions) {
            if (action.getType() == CommandItemAction.ActionType.ITEM) {
                CommandItemAction.ItemActionType itemAction = action.getItemAction();
                if (itemAction == CommandItemAction.ItemActionType.DELETE || 
                    itemAction == CommandItemAction.ItemActionType.CONSUME ||
                    itemAction == CommandItemAction.ItemActionType.DELETE_ALL) {
                    hasExplicitDeleteAction = true;
                    LOGGER.debug("IF block contains explicit delete action: {}", itemAction);
                    break;
                }
            }
        }
        
        // Debug for all actions in the IF block
        LOGGER.debug("Executing IF block with {} actions", actions.size());
        for (int i = 0; i < actions.size(); i++) {
            CommandItemAction currentAction = actions.get(i);
            LOGGER.debug("IF sub-action {}: type={}", i, currentAction.getType());
        }
        
        // Start with executing actions until the first delay
        int i = 0;
        for (; i < actions.size(); i++) {
            CommandItemAction currentAction = actions.get(i);
            
            // If we find a delay, break the loop here
            if (currentAction.getType() == CommandItemAction.ActionType.DELAY) {
                LOGGER.debug("Found DELAY action in IF block at position {}, will handle remaining actions after delay", i);
                break;
            }
            
            // Check if the item was deleted
            if (stack.isEmpty() && !itemDeleted) {
                itemDeleted = true;
                LOGGER.debug("Item deleted during IF action execution, continuing with remaining actions");
            }
            
            // Execute the action based on type and item availability
            if (!stack.isEmpty() || currentAction.getType() == CommandItemAction.ActionType.EXECUTE || currentAction.getType() == CommandItemAction.ActionType.IF) {
                executeAction(player, currentAction, stack, slot);
                
                // If this action deleted the item, update the state
                if (!itemDeleted && stack.isEmpty() && 
                    currentAction.getType() == CommandItemAction.ActionType.ITEM && 
                    (currentAction.getItemAction() == CommandItemAction.ItemActionType.DELETE || 
                     currentAction.getItemAction() == CommandItemAction.ItemActionType.CONSUME)) {
                    itemDeleted = true;
                    LOGGER.debug("Item explicitly deleted by IF subaction");
                }
            } else {
                LOGGER.debug("Skipping IF action that requires item because item was already deleted");
            }
        }
        
        // If the item was deleted without explicit delete actions, it might be a bug
        if (stack.isEmpty() && !hasExplicitDeleteAction && !itemDeleted) {
            LOGGER.debug("Item was unexpectedly deleted in IF block without explicit delete action, this might be a bug!");
        }
        
        // If we finished all actions, exit
        if (i >= actions.size()) {
            return;
        }
        
        // We found a delay, take the rest of the actions
        CommandItemAction delayAction = actions.get(i);
        List<CommandItemAction> remainingActions = actions.subList(i + 1, actions.size());
        
        // If there are no remaining actions after the delay, execute only the delay
        if (remainingActions.isEmpty()) {
            executeAction(player, delayAction, stack, slot);
            return;
        }
        
        // Create an IF delayed action with the remaining actions
        CommandItemAction delayedIfAction = new CommandItemAction();
        delayedIfAction.setType(CommandItemAction.ActionType.IF);
        
        // Set the same conditions as the original IF action
        // (for simplicity, assume it's always true at this point)
        List<Integer> emptyConditions = new ArrayList<>();
        delayedIfAction.setConditionIndices(emptyConditions);
        
        // Add all remaining actions as sub-actions
        for (CommandItemAction remainingAction : remainingActions) {
            delayedIfAction.addSubAction(remainingAction);
        }
        
        // Now execute the delay action, which will queue the delayed IF action
        int delayTicks = delayAction.getDelay();
        UUID playerUuid = player.getUUID();
        long currentTick = player.level().getGameTime();
        
        // Ensure the delay is at least 1 tick
        if (delayTicks < 1) {
            delayTicks = 1;
        }
        
        long executeAtTick = currentTick + delayTicks;
        
        // Create delayed action with our new IF action
        DelayedAction delayedAction = new DelayedAction(
            player, 
            delayedIfAction, 
            stack.copy(), // Create a copy of the stack to prevent issues if original is modified
            slot,
            executeAtTick
        );
        
        // Add to queue (CopyOnWriteArrayList is already thread-safe)
        CopyOnWriteArrayList<DelayedAction> delayedActions = DELAYED_ACTIONS.computeIfAbsent(
            playerUuid, k -> new CopyOnWriteArrayList<>());
        delayedActions.add(delayedAction);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Queued delayed IF action for player {} to execute in {} ticks (current: {}, target: {}) with {} remaining actions",
                player.getName().getString(), delayTicks, currentTick, executeAtTick, remainingActions.size());
        }
    }
    
    /**
     * Queues an action to be executed after the specified delay
     */
    private void queueDelayedAction(ServerPlayer player, CommandItemAction action, ItemStack stack, int slot) {
        int delayTicks = action.getDelay();
        if (delayTicks <= 0) {
            LOGGER.warn("Invalid delay value: {}", delayTicks);
            return;
        }
        
        UUID playerUuid = player.getUUID();
        long currentTick = player.level().getGameTime();
        
        // Ensure the delay is at least 1 tick
        if (delayTicks < 1) {
            delayTicks = 1;
        }
        
        long executeAtTick = currentTick + delayTicks;
        
        // Create delayed action
        DelayedAction delayedAction = new DelayedAction(
            player, 
            action, 
            stack.copy(), // Create a copy of the stack to prevent issues if original is modified
            slot,
            executeAtTick
        );
        
        // Add to queue (CopyOnWriteArrayList is already thread-safe)
        CopyOnWriteArrayList<DelayedAction> actions = DELAYED_ACTIONS.computeIfAbsent(
            playerUuid, k -> new CopyOnWriteArrayList<>());
        actions.add(delayedAction);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Queued delayed action for player {} to execute in {} ticks (current: {}, target: {})",
                player.getName().getString(), delayTicks, currentTick, executeAtTick);
        }
    }
    
    /**
     * Execute a command as the player
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
     * Process an item action
     * @return true if the item was consumed
     */
    private boolean processItemAction(ServerPlayer player, CommandItemAction.ItemActionType actionType, ItemStack stack, int slot) {
        // Check if the stack is empty before executing DELETE/CONSUME/DELETE_ALL actions
        if (stack == null || stack.isEmpty()) {
            if (actionType == CommandItemAction.ItemActionType.DELETE || 
                actionType == CommandItemAction.ItemActionType.CONSUME ||
                actionType == CommandItemAction.ItemActionType.DELETE_ALL) {
                LOGGER.debug("Attempt to delete an already empty item ignored for action type: {}", actionType);
                return false;
            }
        }
        
        switch (actionType) {
            case DELETE:
            case CONSUME:
                // Consume the item (remove from inventory)
                if (slot >= 0) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                    LOGGER.debug("Item consumed from slot {}", slot);
                } else {
                    // If it's in offhand or elsewhere, consume it directly
                    stack.shrink(1);
                    LOGGER.debug("Item shrunk by 1");
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
                // Check if there are actually items to remove
                boolean itemsFound = false;
                int itemCount = 0;
                String currentItemId = definition.getId();
                
                // Check if there are items of this type in inventory
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (!invStack.isEmpty() && invStack.getItem() instanceof CommandItem) {
                        CommandItem cmdItem = (CommandItem) invStack.getItem();
                        // Only items with the specific ID
                        if (cmdItem.getDefinitionId().equals(currentItemId)) {
                            itemsFound = true;
                            itemCount += invStack.getCount();
                        }
                    }
                }
                
                if (!itemsFound) {
                    LOGGER.debug("Attempt to delete all items but no items found");
                    return false;
                }
                
                // Remove all items of this type from inventory
                int removed = 0;
                
                // Search in all inventory slots
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (!invStack.isEmpty() && invStack.getItem() instanceof CommandItem) {
                        CommandItem cmdItem = (CommandItem) invStack.getItem();
                        // Only items with the specific ID
                        if (cmdItem.getDefinitionId().equals(currentItemId)) {
                            removed += invStack.getCount();
                            player.getInventory().setItem(i, ItemStack.EMPTY);
                        }
                    }
                }
                
                LOGGER.debug("Removed {} command items {} from player inventory (found: {})", 
                        removed, definition.getId(), itemCount);
                return true;
                
            case DROP_ALL:
                // Drop all items of this type from inventory
                int dropped = 0;
                String dropItemId = definition.getId();
                
                // Search in all inventory slots
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (!invStack.isEmpty() && invStack.getItem() instanceof CommandItem) {
                        CommandItem cmdItem = (CommandItem) invStack.getItem();
                        // Only items with the specific ID
                        if (cmdItem.getDefinitionId().equals(dropItemId)) {
                            ItemStack toDrop = invStack.copy();
                            dropped += toDrop.getCount();
                            player.getInventory().setItem(i, ItemStack.EMPTY);
                            spawnItemEntity(player, toDrop);
                        }
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
     * Spawn an item entity near the player
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
        Map<String, Long> cooldowns = PLAYER_COOLDOWNS.get(playerUuid);
        if (cooldowns == null) {
            return false;
        }
        
        Long lastUse = cooldowns.get(itemId);
        if (lastUse == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        int cooldownMillis = definition.getCooldown() * 50; // Convert ticks to milliseconds
        
        return currentTime - lastUse < cooldownMillis;
    }
    
    /**
     * Updates the cooldown for an item
     */
    private void updateCooldown(UUID playerUuid, String itemId) {
        PLAYER_COOLDOWNS.computeIfAbsent(playerUuid, k -> new HashMap<>())
                .put(itemId, System.currentTimeMillis());
    }
    
    /**
     * Clear all data associated with a player (called when they disconnect)
     */
    public static void clearPlayerData(UUID playerUuid) {
        if (playerUuid == null) return;
        
        PLAYER_COOLDOWNS.remove(playerUuid);
        DELAYED_ACTIONS.remove(playerUuid);
        FIRST_TICK_PROCESSED.remove(playerUuid);
    }
} 