package net.unfamily.iskautils.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IskaUtils.MOD_ID);

    // Common properties for all items
    private static final Item.Properties ITEM_PROPERTIES = new Item.Properties();

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
    
    // Item per l'Hellfire Igniter
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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
} 