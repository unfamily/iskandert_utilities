package net.unfamily.iskautils.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines an action that can be executed by a command item
 * 
 * Actions can be triggered by the following events:
 * - onFirstTick: Executed the first tick the item is in the player's inventory
 * - onTick: Executed every tick while the item is in the player's inventory (respects cooldown)
 * - onUse: Executed when the player uses the item (right click)
 * - onFinishUse: Executed when the player finishes using the item (after use duration)
 * - onUseOn: Executed when the player uses the item on a block
 * - onHitEntity: Executed when the player hits an entity with the item
 * - onSwing: Executed when the player swings the item (left click)
 * - onDrop: Executed when the player drops the item
 * - onReleaseUsing: Executed when the player stops using the item before the use duration ends
 * 
 * Note: For events like onFinishUse and onReleaseUsing, it's necessary to specify
 * the use duration (in ticks) through the useDuration parameter in the item definition.
 */
public class CommandItemAction {
    
    /**
     * Type of action to execute
     */
    public enum ActionType {
        EXECUTE,    // Execute a command
        DELAY,      // Wait for a delay
        ITEM,       // Perform an action on the item (consume, drop, etc.)
        IF          // Conditional action with sub-actions
    }
    
    /**
     * Type of item action to perform
     */
    public enum ItemActionType {
        DELETE,     // Remove the item (old compatibility)
        DELETE_ALL, // Remove all items of this type (old compatibility)
        DROP,       // Drop the item
        DROP_ALL,   // Drop all items of this type (old compatibility)
        CONSUME,    // Consume the item (same as DELETE but with better name)
        DAMAGE      // Damage the item if possible
    }
    
    private ActionType type;
    private String command;
    private int delay;
    private ItemActionType itemAction;
    private List<CommandItemDefinition.StageCondition> stages = new ArrayList<>();
    
    // For IF action type
    private List<Integer> conditionIndices = new ArrayList<>();
    private List<CommandItemAction> subActions = new ArrayList<>();
    
    /**
     * Creates a new action
     */
    public CommandItemAction() {
        // Default constructor
    }
    
    /**
     * Sets the type of action to execute
     */
    public void setType(ActionType type) {
        this.type = type;
    }
    
    /**
     * Gets the type of action to execute
     */
    public ActionType getType() {
        return type;
    }
    
    /**
     * Sets the command to execute (for EXECUTE type)
     */
    public void setCommand(String command) {
        this.command = command;
    }
    
    /**
     * Gets the command to execute
     */
    public String getCommand() {
        return command;
    }
    
    /**
     * Sets the delay in ticks (for DELAY type)
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }
    
    /**
     * Gets the delay in ticks
     */
    public int getDelay() {
        return delay;
    }
    
    /**
     * Sets the item action to perform (for ITEM type)
     */
    public void setItemAction(ItemActionType itemAction) {
        this.itemAction = itemAction;
    }
    
    /**
     * Gets the item action to perform
     */
    public ItemActionType getItemAction() {
        return itemAction;
    }
    
    /**
     * Adds a stage condition for this action (used with DEF stages logic)
     */
    public void addStage(CommandItemDefinition.StageCondition stage) {
        stages.add(stage);
    }
    
    /**
     * Adds a stage condition with specified parameters (used with DEF stages logic)
     */
    public void addStage(String stageType, String stage, boolean shouldBeSet) {
        stages.add(new CommandItemDefinition.StageCondition(stageType, stage, shouldBeSet));
    }
    
    /**
     * Gets the stage conditions for this action
     */
    public List<CommandItemDefinition.StageCondition> getStages() {
        return stages;
    }
    
    /**
     * Sets the condition indices for IF action type
     */
    public void setConditionIndices(List<Integer> indices) {
        this.conditionIndices = indices;
    }
    
    /**
     * Gets the condition indices for IF action type
     */
    public List<Integer> getConditionIndices() {
        return conditionIndices;
    }
    
    /**
     * Adds a sub-action for IF action type
     */
    public void addSubAction(CommandItemAction subAction) {
        this.subActions.add(subAction);
    }
    
    /**
     * Gets the sub-actions for IF action type
     */
    public List<CommandItemAction> getSubActions() {
        return subActions;
    }
    
    /**
     * Checks if all stage requirements for this action are met
     * 
     * @param player The player to check stages for
     * @param definition The command item definition (used for checking stages)
     * @return true if all requirements are met or if there are no requirements
     */
    public boolean checkActionStages(net.minecraft.server.level.ServerPlayer player, CommandItemDefinition definition) {
        // No stages means always active
        if (stages.isEmpty()) {
            return true;
        }
        
        // Check all stages for this action (using AND logic within the action)
        for (CommandItemDefinition.StageCondition stage : stages) {
            if (!definition.checkSingleStage(player, stage)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if the conditions specified by indices are met using AND logic
     * 
     * @param player The player to check conditions for
     * @param definition The command item definition
     * @return true if all conditions are met
     */
    public boolean checkConditionsByIndices(net.minecraft.server.level.ServerPlayer player, CommandItemDefinition definition) {
        com.mojang.logging.LogUtils.getLogger().debug("Checking conditions by indices: {} with logic: {}", conditionIndices, definition.getStagesLogic());
        
        if (conditionIndices.isEmpty()) {
            com.mojang.logging.LogUtils.getLogger().debug("No conditions to check, returning true");
            return true;
        }
        
        // Otteniamo direttamente il registro degli stage per verifiche dirette
        net.unfamily.iskautils.stage.StageRegistry registry = 
            net.unfamily.iskautils.stage.StageRegistry.getInstance(player.getServer());
        
        List<CommandItemDefinition.StageCondition> allStages = definition.getStages();
        
        // Per debug, mostriamo lo stato attuale di tutti gli stage definiti
        if (com.mojang.logging.LogUtils.getLogger().isDebugEnabled()) {
            com.mojang.logging.LogUtils.getLogger().debug("Current state of ALL defined stages:");
            for (int i = 0; i < allStages.size(); i++) {
                CommandItemDefinition.StageCondition condition = allStages.get(i);
                boolean currentState = false;
                if ("world".equalsIgnoreCase(condition.getStageType())) {
                    currentState = registry.hasWorldStage(condition.getStage());
                } else if ("player".equalsIgnoreCase(condition.getStageType())) {
                    currentState = registry.hasPlayerStage(player, condition.getStage());
                }
                com.mojang.logging.LogUtils.getLogger().debug("  Stage[{}] {}.{}: actual={}, expected={}", 
                    i, condition.getStageType(), condition.getStage(), 
                    currentState, condition.shouldBeSet());
            }
            
            com.mojang.logging.LogUtils.getLogger().debug("Current state of stages being checked (indices {}):", conditionIndices);
            for (Integer index : conditionIndices) {
                if (index >= 0 && index < allStages.size()) {
                    CommandItemDefinition.StageCondition condition = allStages.get(index);
                    boolean currentState = false;
                    if ("world".equalsIgnoreCase(condition.getStageType())) {
                        currentState = registry.hasWorldStage(condition.getStage());
                    } else if ("player".equalsIgnoreCase(condition.getStageType())) {
                        currentState = registry.hasPlayerStage(player, condition.getStage());
                    }
                    com.mojang.logging.LogUtils.getLogger().debug("  Stage[{}] {}.{}: actual={}, expected={}, match={}", 
                        index, condition.getStageType(), condition.getStage(), 
                        currentState, condition.shouldBeSet(), currentState == condition.shouldBeSet());
                }
            }
        }
        
        // *** IMPLEMENTAZIONE CORRETTA DELLE LOGICHE ***
        // DEF_AND: Tutte le condizioni devono essere soddisfatte (true = true && true && ...)
        // DEF_OR: Almeno una condizione deve essere soddisfatta (true = true || true || ...)
        
        // Ignoriamo il campo stages_logic di definition e usiamo sempre AND per DEF_AND e OR per DEF_OR,
        // questo perché "DEF_" sta per "deferred" e indica solo che le condizioni sono espresse per indice
        // invece che direttamente.
        
        // DEF_AND: tutte le condizioni devono essere soddisfatte
        if (definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_AND) {
            com.mojang.logging.LogUtils.getLogger().debug("Using DEF_AND logic for conditions: {} (all must match)", conditionIndices);
            
            boolean allMatch = true; // Assume true unless we find a mismatch
            
            for (Integer index : conditionIndices) {
                if (index < 0 || index >= allStages.size()) {
                    // Invalid index
                    com.mojang.logging.LogUtils.getLogger().debug("Invalid index: {} (max: {})", index, allStages.size() - 1);
                    allMatch = false;
                    break;
                }
                
                CommandItemDefinition.StageCondition condition = allStages.get(index);
                
                // Verifica diretta dallo stage registry per avere lo stato più aggiornato
                boolean currentState = false;
                if ("world".equalsIgnoreCase(condition.getStageType())) {
                    currentState = registry.hasWorldStage(condition.getStage());
                } else if ("player".equalsIgnoreCase(condition.getStageType())) {
                    currentState = registry.hasPlayerStage(player, condition.getStage());
                } else if ("dimension".equalsIgnoreCase(condition.getStageType())) {
                    String dimensionKey = player.level().dimension().location().toString();
                    currentState = dimensionKey.equals(condition.getStage());
                }
                
                // La condizione è soddisfatta se lo stato attuale corrisponde a quello atteso
                boolean conditionMet = (currentState == condition.shouldBeSet());
                
                com.mojang.logging.LogUtils.getLogger().debug("DEF_AND condition at index {} ({}: {}): actual={}, expected={}, match={}", 
                    index, condition.getStageType(), condition.getStage(), 
                    currentState, condition.shouldBeSet(), conditionMet);
                
                if (!conditionMet) {
                    allMatch = false;
                    break;
                }
            }
            
            com.mojang.logging.LogUtils.getLogger().debug("DEF_AND final result for conditions {}: {}", conditionIndices, allMatch);
            return allMatch;
        } 
        // DEF_OR: almeno una condizione deve essere soddisfatta
        else if (definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_OR) {
            com.mojang.logging.LogUtils.getLogger().debug("Using DEF_OR logic for conditions: {} (at least one must match)", conditionIndices);
            
            if (conditionIndices.isEmpty()) {
                com.mojang.logging.LogUtils.getLogger().debug("Empty conditions list with DEF_OR, returning true");
                return true; // Empty conditions list is always true for OR
            }
            
            boolean anyMatch = false; // Assume false unless we find a match
            
            for (Integer index : conditionIndices) {
                if (index < 0 || index >= allStages.size()) {
                    // Skip invalid index
                    com.mojang.logging.LogUtils.getLogger().debug("Invalid index: {} (max: {}), skipping", index, allStages.size() - 1);
                    continue;
                }
                
                CommandItemDefinition.StageCondition condition = allStages.get(index);
                
                // Verifica diretta dallo stage registry per avere lo stato più aggiornato
                boolean currentState = false;
                if ("world".equalsIgnoreCase(condition.getStageType())) {
                    currentState = registry.hasWorldStage(condition.getStage());
                } else if ("player".equalsIgnoreCase(condition.getStageType())) {
                    currentState = registry.hasPlayerStage(player, condition.getStage());
                } else if ("dimension".equalsIgnoreCase(condition.getStageType())) {
                    String dimensionKey = player.level().dimension().location().toString();
                    currentState = dimensionKey.equals(condition.getStage());
                }
                
                // La condizione è soddisfatta se lo stato attuale corrisponde a quello atteso
                boolean conditionMet = (currentState == condition.shouldBeSet());
                
                com.mojang.logging.LogUtils.getLogger().debug("DEF_OR condition at index {} ({}: {}): actual={}, expected={}, match={}", 
                    index, condition.getStageType(), condition.getStage(), 
                    currentState, condition.shouldBeSet(), conditionMet);
                
                if (conditionMet) {
                    anyMatch = true;
                    break;
                }
            }
            
            com.mojang.logging.LogUtils.getLogger().debug("DEF_OR final result for conditions {}: {}", conditionIndices, anyMatch);
            return anyMatch;
        }
        
        // Default to AND logic for any other case
        com.mojang.logging.LogUtils.getLogger().debug("Using default AND logic for conditions: {}", conditionIndices);
        
        boolean allMatch = true; // Assume true unless we find a mismatch
        
        for (Integer index : conditionIndices) {
            if (index < 0 || index >= allStages.size()) {
                // Invalid index
                com.mojang.logging.LogUtils.getLogger().debug("Invalid index: {} (max: {})", index, allStages.size() - 1);
                allMatch = false;
                break;
            }
            
            CommandItemDefinition.StageCondition condition = allStages.get(index);
            
            // Verifica diretta dallo stage registry per avere lo stato più aggiornato
            boolean currentState = false;
            if ("world".equalsIgnoreCase(condition.getStageType())) {
                currentState = registry.hasWorldStage(condition.getStage());
            } else if ("player".equalsIgnoreCase(condition.getStageType())) {
                currentState = registry.hasPlayerStage(player, condition.getStage());
            } else if ("dimension".equalsIgnoreCase(condition.getStageType())) {
                String dimensionKey = player.level().dimension().location().toString();
                currentState = dimensionKey.equals(condition.getStage());
            }
            
            // La condizione è soddisfatta se lo stato attuale corrisponde a quello atteso
            boolean conditionMet = (currentState == condition.shouldBeSet());
            
            com.mojang.logging.LogUtils.getLogger().debug("Default AND condition at index {} ({}: {}): actual={}, expected={}, match={}", 
                index, condition.getStageType(), condition.getStage(), 
                currentState, condition.shouldBeSet(), conditionMet);
            
            if (!conditionMet) {
                allMatch = false;
                break;
            }
        }
        
        com.mojang.logging.LogUtils.getLogger().debug("Default AND final result for conditions {}: {}", conditionIndices, allMatch);
        return allMatch;
    }
} 