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
    
    // Per tenere traccia degli ItemStack già processati usiamo una combinazione di UUID del player e hash dello stack
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
    
    // Queue for delayed actions - CopyOnWriteArrayList è intrinsecamente thread-safe
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
     * Genera un hash univoco per l'ItemStack per tener traccia dei processati
     */
    private int getItemStackHash(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        
        // Combinazione di item e count (ignoriamo il tag per semplicità)
        int hash = stack.getItem().hashCode();
        hash = 31 * hash + stack.getCount();
        return hash;
    }
    
    /**
     * Controlla se un ItemStack è già stato processato
     */
    private boolean isItemStackProcessed(UUID playerUuid, ItemStack stack) {
        Set<Integer> processedItems = FIRST_TICK_PROCESSED.get(playerUuid);
        if (processedItems == null) return false;
        
        int stackHash = getItemStackHash(stack);
        return processedItems.contains(stackHash);
    }
    
    /**
     * Marca un ItemStack come processato
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
            // Prima volta che vediamo questo item - eseguiamo le azioni di first tick
            if (!definition.getFirstTickActions().isEmpty()) {
                LOGGER.debug("Executing first tick actions for command item {} for player {}", 
                        definition.getId(), player.getName().getString());
                
                // Esegui le azioni del primo tick
                executeFirstTickActions(player, definition.getFirstTickActions(), stack, slot);
                
                // Se l'item è stato consumato, non continuare
                if (stack.isEmpty()) {
                    return;
                }
                
                // Mark as processed solo se l'item esiste ancora
                markItemStackProcessed(playerUuid, stack);
            } else {
                // Se non ci sono azioni first tick, marca comunque come processato
                markItemStackProcessed(playerUuid, stack);
            }
        } else {
            // Item già processato in tick precedente - controlla se ha lo stage "initialized"
            // Se non ha lo stage, elimina l'item
            if ((definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_AND || 
                definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_OR) &&
                // Non applicare questa logica all'item world_init che ha la sua logica speciale
                !definition.getId().equals("iska_utils-world_init")) {
                boolean hasInitializedStage = false;
                
                // Cerca tra tutti gli stage se esiste uno con "initialized" o simile
                for (CommandItemDefinition.StageCondition stage : definition.getStages()) {
                    if (stage.getStage().equals("initialized") && 
                        stage.shouldBeSet() && 
                        definition.checkSingleStage(player, stage)) {
                        hasInitializedStage = true;
                        break;
                    }
                }
                
                // Se non ha stage "initialized", elimina l'item
                if (!hasInitializedStage) {
                    LOGGER.debug("Item {} non inizializzato, verrà eliminato", definition.getId());
                    // Elimina l'item dall'inventario
                    if (slot >= 0) {
                        player.getInventory().setItem(slot, ItemStack.EMPTY);
                    } else {
                        stack.setCount(0);
                    }
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
     * Esegue solo le azioni del primo tick con attenzione speciale
     */
    private void executeFirstTickActions(ServerPlayer player, List<CommandItemAction> actions, ItemStack stack, int slot) {
        for (CommandItemAction action : actions) {
            if (stack.isEmpty()) {
                // Item consumed in a previous action
                break;
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
            
            // Caso speciale per azioni IF
            if (action.getType() == CommandItemAction.ActionType.IF) {
                // Check if conditions are met based on the indices
                if (action.checkConditionsByIndices(player, definition)) {
                    // Cerchiamo azioni di consumo prioritarie nei sub-actions
                    boolean hasConsumed = false;
                    for (CommandItemAction subAction : action.getSubActions()) {
                        if (subAction.getType() == CommandItemAction.ActionType.ITEM) {
                            CommandItemAction.ItemActionType itemAction = subAction.getItemAction();
                            if (itemAction == CommandItemAction.ItemActionType.DELETE || 
                                itemAction == CommandItemAction.ItemActionType.CONSUME) {
                                LOGGER.debug("First tick IF: eseguo subito l'azione di consumo dell'item");
                                processItemAction(player, itemAction, stack, slot);
                                hasConsumed = true;
                                break;
                            }
                        }
                    }
                    
                    // Se l'item è stato consumato, interrompi ulteriore elaborazione
                    if (hasConsumed && stack.isEmpty()) {
                        return;
                    }
                    
                    // Esegui tutte le altre sub-actions
                    for (CommandItemAction subAction : action.getSubActions()) {
                        if (stack.isEmpty()) {
                            // Item consumed in a previous action
                            break;
                        }
                        
                        // Salta le azioni di consumo che abbiamo già eseguito
                        if (subAction.getType() == CommandItemAction.ActionType.ITEM) {
                            CommandItemAction.ItemActionType itemAction = subAction.getItemAction();
                            if ((itemAction == CommandItemAction.ItemActionType.DELETE || 
                                itemAction == CommandItemAction.ItemActionType.CONSUME) && 
                                hasConsumed) {
                                continue;
                            }
                        }
                        
                        executeAction(player, subAction, stack, slot);
                    }
                    continue; // Abbiamo gestito l'azione IF, passa alla prossima
                }
            }
            
            // Dai precedenza alle azioni di tipo ITEM DELETE/CONSUME
            if (action.getType() == CommandItemAction.ActionType.ITEM) {
                CommandItemAction.ItemActionType itemAction = action.getItemAction();
                if (itemAction == CommandItemAction.ItemActionType.DELETE || 
                    itemAction == CommandItemAction.ItemActionType.CONSUME) {
                    
                    LOGGER.debug("First tick: eseguo subito l'azione di consumo dell'item");
                    processItemAction(player, itemAction, stack, slot);
                    // Se l'item è stato consumato, interrompi ulteriore elaborazione
                    if (stack.isEmpty()) {
                        return;
                    }
                }
            }
            
            // Execute the action (or queue it if it's a delay)
            executeAction(player, action, stack, slot);
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
        
        // CopyOnWriteArrayList permette iterazione sicura anche durante la rimozione
        for (DelayedAction action : actions) {
            if (currentTick >= action.executeAtTick) {
                try {
                    // Esegui l'azione pianificata
                    LOGGER.debug("Executing delayed action for player {} scheduled for tick {} (current tick: {})",
                            player.getName().getString(), action.executeAtTick, currentTick);
                    executeAction(action.player, action.action, action.stack, action.slot);
                } catch (Exception e) {
                    LOGGER.error("Error executing delayed action: {}", e.getMessage());
                } finally {
                    // Aggiungi sempre all'elenco delle azioni da rimuovere, anche in caso di errore
                    actionsToRemove.add(action);
                }
            }
        }
        
        // Rimuove le azioni completate (CopyOnWriteArrayList gestisce la concorrenza)
        if (!actionsToRemove.isEmpty()) {
            actions.removeAll(actionsToRemove);
            
            // Rimuove la lista se vuota
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
            if ((definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_AND || 
                 definition.getStagesLogic() == CommandItemDefinition.StagesLogic.DEF_OR) &&
                !action.getStages().isEmpty() &&
                !action.checkActionStages(player, definition)) {
                // Skip this action if its requirements are not met
                LOGGER.debug("Skipping action due to stage requirements not met");
                continue;
            }
            
            // Execute the action (or queue it if it's a delay)
            executeAction(player, action, stack, slot);
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
                if (action.checkConditionsByIndices(player, definition)) {
                    // Execute all sub-actions if conditions are met, but handle delays specially
                    List<CommandItemAction> subActions = action.getSubActions();
                    if (!subActions.isEmpty()) {
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
     * Esegue le azioni di un blocco IF gestendo correttamente i delay
     * in modo che le azioni successive a un delay vengano eseguite dopo il ritardo
     */
    private void executeIfActionsWithDelays(ServerPlayer player, List<CommandItemAction> actions, ItemStack stack, int slot) {
        // Se non ci sono azioni, esci
        if (actions.isEmpty()) {
            return;
        }
        
        // Inizia con l'esecuzione delle azioni fino al primo delay
        int i = 0;
        for (; i < actions.size(); i++) {
            CommandItemAction currentAction = actions.get(i);
            
            // Se troviamo un delay, interrompiamo il ciclo qui
            if (currentAction.getType() == CommandItemAction.ActionType.DELAY) {
                break;
            }
            
            // Esegui l'azione corrente
            if (stack.isEmpty()) {
                // Item consumed in a previous action
                return;
            }
            executeAction(player, currentAction, stack, slot);
        }
        
        // Se abbiamo finito le azioni o l'item è stato consumato, esci
        if (i >= actions.size() || stack.isEmpty()) {
            return;
        }
        
        // Abbiamo trovato un delay, prendiamo il resto delle azioni
        CommandItemAction delayAction = actions.get(i);
        List<CommandItemAction> remainingActions = actions.subList(i + 1, actions.size());
        
        // Se non ci sono azioni rimanenti dopo il delay, esegui solo il delay
        if (remainingActions.isEmpty()) {
            executeAction(player, delayAction, stack, slot);
            return;
        }
        
        // Creiamo un'azione IF ritardata con le azioni rimanenti
        CommandItemAction delayedIfAction = new CommandItemAction();
        delayedIfAction.setType(CommandItemAction.ActionType.IF);
        
        // Impostiamo le stesse condizioni dell'azione IF originale
        // (per semplicità, assumiamo che sia sempre vera a questo punto)
        List<Integer> emptyConditions = new ArrayList<>();
        delayedIfAction.setConditionIndices(emptyConditions);
        
        // Aggiungiamo tutte le azioni rimanenti come sub-actions
        for (CommandItemAction remainingAction : remainingActions) {
            delayedIfAction.addSubAction(remainingAction);
        }
        
        // Ora eseguiamo l'azione di delay, che metterà in coda l'azione IF ritardata
        int delayTicks = delayAction.getDelay();
        UUID playerUuid = player.getUUID();
        long currentTick = player.level().getGameTime();
        
        // Assicuriamoci che il ritardo sia almeno di 1 tick
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
        
        // Add to queue (CopyOnWriteArrayList è già thread-safe)
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
        
        // Assicuriamoci che il ritardo sia almeno di 1 tick
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
        
        // Add to queue (CopyOnWriteArrayList è già thread-safe)
        CopyOnWriteArrayList<DelayedAction> actions = DELAYED_ACTIONS.computeIfAbsent(
            playerUuid, k -> new CopyOnWriteArrayList<>());
        actions.add(delayedAction);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Queued delayed action for player {} to execute in {} ticks (current: {}, target: {})",
                player.getName().getString(), delayTicks, currentTick, executeAtTick);
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
     * Clears all data associated with a player (called when they disconnect)
     */
    public static void clearPlayerData(UUID playerUuid) {
        if (playerUuid == null) return;
        
        PLAYER_COOLDOWNS.remove(playerUuid);
        DELAYED_ACTIONS.remove(playerUuid);
        FIRST_TICK_PROCESSED.remove(playerUuid);
    }
} 