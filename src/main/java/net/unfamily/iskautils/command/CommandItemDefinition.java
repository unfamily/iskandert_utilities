package net.unfamily.iskautils.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Definition of a command item that can perform actions
 */
public class CommandItemDefinition {
    private String id;
    private int cooldown;
    private boolean creativeTabVisible = true;
    
    private final List<CommandItemAction> firstTickActions = new ArrayList<>();
    private final List<CommandItemAction> tickActions = new ArrayList<>();
    private final List<CommandItemAction> useActions = new ArrayList<>();
    private final List<CommandItemAction> finishUseActions = new ArrayList<>();
    private final List<CommandItemAction> useOnActions = new ArrayList<>();
    private final List<CommandItemAction> hitEntityActions = new ArrayList<>();
    private final List<CommandItemAction> swingActions = new ArrayList<>();
    private final List<CommandItemAction> dropActions = new ArrayList<>();
    private final List<CommandItemAction> releaseUsingActions = new ArrayList<>();
    
    private final List<StageCondition> stages = new ArrayList<>();
    private StagesLogic stagesLogic = StagesLogic.AND;
    
    /**
     * Logic for evaluating stages
     */
    public enum StagesLogic {
        AND, // All stages must be satisfied
        OR,  // At least one stage must be satisfied
        DEF  // Deferred evaluation - stages are defined per action, not at item level
    }
    
    /**
     * Creates a new command item definition
     */
    public CommandItemDefinition(String id) {
        this.id = id;
    }
    
    /**
     * Sets the ID of the command item
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Gets the ID of the command item
     */
    public String getId() {
        return id;
    }
    
    /**
     * Sets the cooldown (in ticks) between consecutive tick actions
     */
    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }
    
    /**
     * Gets the cooldown (in ticks)
     */
    public int getCooldown() {
        return cooldown;
    }
    
    /**
     * Sets whether this item should be visible in the creative tab
     */
    public void setCreativeTabVisible(boolean visible) {
        this.creativeTabVisible = visible;
    }
    
    /**
     * Checks if this item should be visible in the creative tab
     */
    public boolean isCreativeTabVisible() {
        return creativeTabVisible;
    }
    
    /**
     * Adds an action for the first tick
     */
    public void addFirstTickAction(CommandItemAction action) {
        firstTickActions.add(action);
    }
    
    /**
     * Gets the first tick actions
     */
    public List<CommandItemAction> getFirstTickActions() {
        return firstTickActions;
    }
    
    /**
     * Adds a tick action
     */
    public void addTickAction(CommandItemAction action) {
        tickActions.add(action);
    }
    
    /**
     * Gets the tick actions
     */
    public List<CommandItemAction> getTickActions() {
        return tickActions;
    }
    
    /**
     * Adds a use action
     */
    public void addUseAction(CommandItemAction action) {
        useActions.add(action);
    }
    
    /**
     * Gets the use actions
     */
    public List<CommandItemAction> getUseActions() {
        return useActions;
    }
    
    /**
     * Adds an action for when the item is finished being used
     */
    public void addFinishUseAction(CommandItemAction action) {
        finishUseActions.add(action);
    }
    
    /**
     * Gets the actions for when the item is finished being used
     */
    public List<CommandItemAction> getFinishUseActions() {
        return finishUseActions;
    }
    
    /**
     * Adds an action for when the item is used on a block
     */
    public void addUseOnAction(CommandItemAction action) {
        useOnActions.add(action);
    }
    
    /**
     * Gets the actions for when the item is used on a block
     */
    public List<CommandItemAction> getUseOnActions() {
        return useOnActions;
    }
    
    /**
     * Adds an action for when an entity is hit with the item
     */
    public void addHitEntityAction(CommandItemAction action) {
        hitEntityActions.add(action);
    }
    
    /**
     * Gets the actions for when an entity is hit with the item
     */
    public List<CommandItemAction> getHitEntityActions() {
        return hitEntityActions;
    }
    
    /**
     * Adds an action for when the item is swung
     */
    public void addSwingAction(CommandItemAction action) {
        swingActions.add(action);
    }
    
    /**
     * Gets the actions for when the item is swung
     */
    public List<CommandItemAction> getSwingActions() {
        return swingActions;
    }
    
    /**
     * Adds an action for when the item is dropped
     */
    public void addDropAction(CommandItemAction action) {
        dropActions.add(action);
    }
    
    /**
     * Gets the actions for when the item is dropped
     */
    public List<CommandItemAction> getDropActions() {
        return dropActions;
    }
    
    /**
     * Adds an action for when the item is released before use duration ends
     */
    public void addReleaseUsingAction(CommandItemAction action) {
        releaseUsingActions.add(action);
    }
    
    /**
     * Gets the actions for when the item is released before use duration ends
     */
    public List<CommandItemAction> getReleaseUsingActions() {
        return releaseUsingActions;
    }
    
    /**
     * Adds a stage condition
     */
    public void addStage(StageCondition stage) {
        stages.add(stage);
    }
    
    /**
     * Adds a stage condition with the specified parameters
     */
    public void addStage(String stageType, String stage, boolean shouldBeSet) {
        stages.add(new StageCondition(stageType, stage, shouldBeSet));
    }
    
    /**
     * Gets the stage conditions
     */
    public List<StageCondition> getStages() {
        return stages;
    }
    
    /**
     * Sets the stage evaluation logic (AND, OR, or DEF)
     */
    public void setStagesLogic(StagesLogic logic) {
        this.stagesLogic = logic;
    }
    
    /**
     * Gets the stage evaluation logic
     */
    public StagesLogic getStagesLogic() {
        return stagesLogic;
    }
    
    /**
     * Checks if all stage requirements are met
     * 
     * @param player The player to check stages for
     * @return true if all stage requirements are met, false otherwise
     */
    public boolean checkAllStages(net.minecraft.server.level.ServerPlayer player) {
        // If using DEF logic, stages are checked per action, not here
        if (stagesLogic == StagesLogic.DEF) {
            return true;
        }
        
        // If no stages, always active
        if (stages.isEmpty()) {
            return true;
        }
        
        // Check stages according to logic (AND or OR)
        if (stagesLogic == StagesLogic.AND) {
            // All stages must match
            for (StageCondition stage : stages) {
                if (!checkSingleStage(player, stage)) {
                    return false;
                }
            }
            return true;
        } else { // OR logic
            // At least one stage must match
            for (StageCondition stage : stages) {
                if (checkSingleStage(player, stage)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * Checks if a specific stage condition is met
     * 
     * @param player The player to check the stage for
     * @param condition The stage condition to check
     * @return true if the condition is met, false otherwise
     */
    public boolean checkSingleStage(net.minecraft.server.level.ServerPlayer player, StageCondition condition) {
        String stageId = condition.getStage();
        boolean shouldBeSet = condition.shouldBeSet();
        
        // Get the stage registry from the server
        net.unfamily.iskautils.stage.StageRegistry registry = 
            net.unfamily.iskautils.stage.StageRegistry.getInstance(player.getServer());
        
        switch (condition.getStageType().toLowerCase()) {
            case "player":
                // Check if the player has the stage
                boolean hasPlayerStage = registry.hasPlayerStage(player, stageId);
                return hasPlayerStage == shouldBeSet;
            
            case "world":
                // Check if the world has the stage
                boolean hasWorldStage = registry.hasWorldStage(stageId);
                return hasWorldStage == shouldBeSet;
                
            case "dimension":
                // Check if the player is in the specified dimension
                String dimensionKey = player.level().dimension().location().toString();
                return dimensionKey.equals(stageId) == shouldBeSet;
                
            default:
                return false;
        }
    }
    
    /**
     * Condition for a specific stage
     */
    public static class StageCondition {
        private String stageType;
        private String stage;
        private boolean shouldBeSet;
        
        public StageCondition(String stageType, String stage, boolean shouldBeSet) {
            this.stageType = stageType;
            this.stage = stage;
            this.shouldBeSet = shouldBeSet;
        }
        
        public String getStageType() {
            return stageType;
        }
        
        public String getStage() {
            return stage;
        }
        
        public boolean shouldBeSet() {
            return shouldBeSet;
        }
    }
} 