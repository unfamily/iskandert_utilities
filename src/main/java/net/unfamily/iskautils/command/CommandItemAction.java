package net.unfamily.iskautils.command;

import net.minecraft.resources.Identifier;

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
        MESSAGE,    // Send localized message (chat/actionbar/title)
        IF,         // Conditional action with sub-actions
        DROP        // Drop an item at the player position (datapack item id)
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
    private net.unfamily.iskautils.obtaining.MessageSpec message;
    private Identifier dropItemId;
    private List<CommandItemDefinition.StageCondition> stages = new ArrayList<>();
    
    // For IF action type
    private List<Integer> conditionIndices = new ArrayList<>();
    private List<Integer> modConditionIndices = new ArrayList<>();
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

    public void setMessage(net.unfamily.iskautils.obtaining.MessageSpec message) {
        this.message = message;
    }

    public net.unfamily.iskautils.obtaining.MessageSpec getMessage() {
        return message;
    }

    public void setDropItemId(Identifier dropItemId) {
        this.dropItemId = dropItemId;
    }

    public Identifier getDropItemId() {
        return dropItemId;
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

    public void setModConditionIndices(List<Integer> indices) {
        this.modConditionIndices = indices != null ? indices : new ArrayList<>();
    }

    public List<Integer> getModConditionIndices() {
        return modConditionIndices;
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
        if (conditionIndices.isEmpty()) {
            return true;
        }

        net.unfamily.iskalib.stage.StageRegistry registry =
                net.unfamily.iskalib.stage.StageRegistry.getInstance(
                        ((net.minecraft.server.level.ServerLevel) player.level()).getServer());
        List<CommandItemDefinition.StageCondition> allStages = definition.getStages();

        if (definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_AND) {
            for (Integer index : conditionIndices) {
                if (index < 0 || index >= allStages.size()) {
                    return false;
                }
                if (!isStageConditionMet(player, registry, allStages.get(index))) {
                    return false;
                }
            }
            return true;
        }

        if (definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_OR) {
            for (Integer index : conditionIndices) {
                if (index < 0 || index >= allStages.size()) {
                    continue;
                }
                if (isStageConditionMet(player, registry, allStages.get(index))) {
                    return true;
                }
            }
            return false;
        }

        for (Integer index : conditionIndices) {
            if (index < 0 || index >= allStages.size()) {
                return false;
            }
            if (!isStageConditionMet(player, registry, allStages.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isStageConditionMet(
            net.minecraft.server.level.ServerPlayer player,
            net.unfamily.iskalib.stage.StageRegistry registry,
            CommandItemDefinition.StageCondition condition) {
        boolean currentState = false;
        if ("world".equalsIgnoreCase(condition.getStageType())) {
            currentState = registry.hasWorldStage(condition.getStage());
        } else if ("player".equalsIgnoreCase(condition.getStageType())) {
            currentState = registry.hasPlayerStage(player, condition.getStage());
        } else if ("team".equalsIgnoreCase(condition.getStageType())) {
            currentState = registry.hasPlayerTeamStage(player, condition.getStage());
        }
        return currentState == condition.shouldBeSet();
    }

    public boolean checkModConditionsByIndices(CommandItemDefinition definition) {
        return net.unfamily.iskautils.script.LoadModGate.checkModIndices(
                definition.getMods(),
                definition.getModsLogic(),
                modConditionIndices);
    }

    public boolean checkIfConditions(net.minecraft.server.level.ServerPlayer player, CommandItemDefinition definition) {
        return checkConditionsByIndices(player, definition)
                && checkModConditionsByIndices(definition);
    }
}