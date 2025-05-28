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
        ITEM        // Perform an action on the item (consume, drop, etc.)
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
} 