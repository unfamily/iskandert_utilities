package net.unfamily.iskautils.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.custom.VectorCharmItem;
import net.unfamily.iskautils.item.custom.PortableDislocatorItem;
import net.unfamily.iskautils.item.custom.TreeTapItem;  
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModItems {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModItems.class);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IskaUtils.MOD_ID);

    // Common properties for all items
    private static final Item.Properties ITEM_PROPERTIES = new Item.Properties();

    // ===== CUSTOM ITEMS =====
    
    // Vector Modules
    public static final DeferredItem<Item> BASE_MODULE = ITEMS.register("base_module",
            () -> new Item(ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> SLOW_MODULE = ITEMS.register("slow_module",
            () -> new Item(ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> MODERATE_MODULE = ITEMS.register("moderate_module",
            () -> new Item(ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> FAST_MODULE = ITEMS.register("fast_module",
            () -> new Item(ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> EXTREME_MODULE = ITEMS.register("extreme_module",
            () -> new Item(ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> ULTRA_MODULE = ITEMS.register("ultra_module",
            () -> new Item(ITEM_PROPERTIES));
            
    // Vector Charm - Custom item with special functionality
    // Registered as a Curio charm when Curios is available
    public static final DeferredItem<Item> VECTOR_CHARM = ITEMS.register("vector_charm",
            () -> new VectorCharmItem(new Item.Properties().stacksTo(1)));

    // Portable Dislocator - Custom item with dislocator functionality
    // Registered as a Curio when Curios is available
    public static final DeferredItem<Item> PORTABLE_DISLOCATOR = ITEMS.register("portable_dislocator",
            () -> new PortableDislocatorItem(new Item.Properties().stacksTo(1)));
            
    // ===== RUBBER TREE ITEMS =====
    
    // Sap item dropped from rubber logs
    public static final DeferredItem<Item> SAP = ITEMS.register("sap",
            () -> new Item(ITEM_PROPERTIES));
            
    // Rubber item created from sap
    public static final DeferredItem<Item> RUBBER = ITEMS.register("rubber",
            () -> new Item(ITEM_PROPERTIES));
            
    // Tree tap for collecting sap
    public static final DeferredItem<Item> TREE_TAP = ITEMS.register("treetap",
            () -> new TreeTapItem(new Item.Properties().durability(64)));
            
    // Electric tree tap for collecting sap (no durability)
    public static final DeferredItem<Item> ELECTRIC_TREE_TAP = ITEMS.register("electric_treetap",
            () -> new TreeTapItem(new Item.Properties().stacksTo(1)));
            
    // Rubber tree blocks
    public static final DeferredItem<Item> RUBBER_LOG = ITEMS.register("rubber_log",
            () -> new BlockItem(ModBlocks.RUBBER_LOG.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_LOG_FILLED = ITEMS.register("rubber_log_filled",
            () -> new BlockItem(ModBlocks.RUBBER_LOG_FILLED.get(), new Item.Properties().stacksTo(64)));
            
    public static final DeferredItem<Item> RUBBER_LOG_EMPTY = ITEMS.register("rubber_log_empty",
            () -> new BlockItem(ModBlocks.RUBBER_LOG_EMPTY.get(), new Item.Properties().stacksTo(64)));
            
    public static final DeferredItem<Item> RUBBER_LEAVES = ITEMS.register("rubber_leaves",
            () -> new BlockItem(ModBlocks.RUBBER_LEAVES.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_SAPLING = ITEMS.register("rubber_sapling",
            () -> new BlockItem(ModBlocks.RUBBER_SAPLING.get(), ITEM_PROPERTIES));

    // ===== STANDARD VECTOR PLATE ITEMS =====
    
    // Items for Vector Plates
    public static final DeferredItem<Item> SLOW_VECT = ITEMS.register("slow_vect",
            () -> new BlockItem(ModBlocks.SLOW_VECT.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> MODERATE_VECT = ITEMS.register("moderate_vect",
            () -> new BlockItem(ModBlocks.MODERATE_VECT.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> FAST_VECT = ITEMS.register("fast_vect",
            () -> new BlockItem(ModBlocks.FAST_VECT.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> EXTREME_VECT = ITEMS.register("extreme_vect",
            () -> new BlockItem(ModBlocks.EXTREME_VECT.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> ULTRA_VECT = ITEMS.register("ultra_vect",
            () -> new BlockItem(ModBlocks.ULTRA_VECT.get(), ITEM_PROPERTIES));
            
    // ===== PLAYER VECTOR PLATE ITEMS =====
    
    // Items for Player Vector Plates
    public static final DeferredItem<Item> PLAYER_SLOW_VECT = ITEMS.register("player_slow_vect",
            () -> new BlockItem(ModBlocks.PLAYER_SLOW_VECT.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> PLAYER_MODERATE_VECT = ITEMS.register("player_moderate_vect",
            () -> new BlockItem(ModBlocks.PLAYER_MODERATE_VECT.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> PLAYER_FAST_VECT = ITEMS.register("player_fast_vect",
            () -> new BlockItem(ModBlocks.PLAYER_FAST_VECT.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> PLAYER_EXTREME_VECT = ITEMS.register("player_extreme_vect",
            () -> new BlockItem(ModBlocks.PLAYER_EXTREME_VECT.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> PLAYER_ULTRA_VECT = ITEMS.register("player_ultra_vect",
            () -> new BlockItem(ModBlocks.PLAYER_ULTRA_VECT.get(), ITEM_PROPERTIES));
            
    // ===== UTILITY ITEMS =====
    
    // Item for the Hellfire Igniter
    public static final DeferredItem<Item> HELLFIRE_IGNITER = ITEMS.register("hellfire_igniter",
            () -> new BlockItem(ModBlocks.HELLFIRE_IGNITER.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> SMOOTH_BLACKSTONE = ITEMS.register("smooth_blackstone",
            () -> new BlockItem(ModBlocks.SMOOTH_BLACKSTONE.get(), ITEM_PROPERTIES));
    public static final DeferredItem<Item> SMOOTH_BLACKSTONE_SLAB = ITEMS.register("smooth_blackstone_slab",
            () -> new BlockItem(ModBlocks.SMOOTH_BLACKSTONE_SLAB.get(), ITEM_PROPERTIES));
    public static final DeferredItem<Item> SMOOTH_BLACKSTONE_STAIRS = ITEMS.register("smooth_blackstone_stairs",
            () -> new BlockItem(ModBlocks.SMOOTH_BLACKSTONE_STAIRS.get(), ITEM_PROPERTIES));
    public static final DeferredItem<Item> SMOOTH_BLACKSTONE_WALL = ITEMS.register("smooth_blackstone_wall",
            () -> new BlockItem(ModBlocks.SMOOTH_BLACKSTONE_WALL.get(), ITEM_PROPERTIES));
    public static final DeferredItem<Item> PLATE_BASE_BLOCK = ITEMS.register("plate_base_block",
            () -> new BlockItem(ModBlocks.PLATE_BASE_BLOCK.get(), ITEM_PROPERTIES));
    public static final DeferredItem<Item> RAFT = ITEMS.register("raft",
            () -> new BlockItem(ModBlocks.RAFT.get(), ITEM_PROPERTIES));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        
        // Register Vector Charm with Curios integration if available
        if (ModUtils.isCuriosLoaded()) {
            // LOGGER.info("Vector Charm will be registered as a Curio charm");
        } else {
            // LOGGER.info("Vector Charm will be registered as a standard item (Curios not available)");
        }
    }
} 