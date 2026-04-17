package net.unfamily.iskautils.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.custom.StructureMonouseItem;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Registry for dynamically registered Structure Monouse items
 */
public class StructureMonouseRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IskaUtils.MOD_ID);
    
    // Map to store registered monouse items
    private static final Map<String, DeferredHolder<Item, StructureMonouseItem>> REGISTERED_ITEMS = new HashMap<>();
    
    /**
     * Registers this registry with the event bus
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
    
    /**
     * Initializes monouse items based on loaded definitions
     */
    public static void initializeItems() {
        // Definitions are loaded via StructureMonouseLoader.loadAll(...) before this runs
        for (StructureMonouseDefinition definition : StructureMonouseLoader.getAllMonouseItems().values()) {
            registerMonouseItem(definition);
        }
    }
    
    /**
     * Registers a monouse item from its definition
     */
    private static void registerMonouseItem(StructureMonouseDefinition definition) {
        String id = definition.getId();
        String registryName = id.toLowerCase(); // ID is already converted from - to _
        
        DeferredHolder<Item, StructureMonouseItem> registeredItem = ITEMS.registerItem(
                registryName,
                props -> new StructureMonouseItem(props, definition),
                p -> p.stacksTo(1));
        
        // Store in registry
        REGISTERED_ITEMS.put(id, registeredItem);
    }
    
    /**
     * Gets a registered monouse item by ID
     */
    public static DeferredHolder<Item, StructureMonouseItem> getMonouseItem(String id) {
        return REGISTERED_ITEMS.get(id);
    }
    
    /**
     * Checks if a monouse item is registered
     */
    public static boolean isRegistered(String id) {
        return REGISTERED_ITEMS.containsKey(id);
    }
    
    /**
     * Gets the number of registered monouse items
     */
    public static int getRegisteredCount() {
        return REGISTERED_ITEMS.size();
    }
    
    /**
     * Gets all registered monouse items
     */
    public static Map<String, DeferredHolder<Item, StructureMonouseItem>> getAllItems() {
        return new HashMap<>(REGISTERED_ITEMS);
    }
    
    /**
     * Reloads monouse item definitions
     * Note: Items cannot be registered/unregistered dynamically after game startup.
     */
    public static void reloadDefinitions() {
        // Reload definitions from JSON files
        StructureMonouseLoader.reloadAllDefinitions();
    }
} 