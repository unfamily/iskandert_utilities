package net.unfamily.iskautils.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.custom.VectorCharmItem;
import net.unfamily.iskautils.item.custom.PortableDislocatorItem;
import net.unfamily.iskautils.item.custom.TreeTapItem;
import net.unfamily.iskautils.item.custom.ElectricTreeTapItem;
import net.unfamily.iskautils.item.custom.ScannerItem;
import net.unfamily.iskautils.item.custom.ScannerChipItem;
import net.unfamily.iskautils.item.custom.NecroticCrystalHeartItem;
import net.unfamily.iskautils.item.custom.SwissWrenchItem;
import net.unfamily.iskautils.item.custom.AngelBlockItem;
import net.unfamily.iskautils.item.custom.StructurePlacerItem;
import net.unfamily.iskautils.item.custom.BurningBrazierItem;
import net.unfamily.iskautils.item.custom.GhostBrazierItem;
import net.unfamily.iskautils.item.custom.GreedyShieldItem;
import net.unfamily.iskautils.item.custom.BlueprintItem;
import net.unfamily.iskautils.item.custom.AutoShopItem;
import net.unfamily.iskautils.item.custom.GiftItem;

import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.unfamily.iskautils.item.custom.RubberBootsItem;
import net.unfamily.iskautils.item.custom.MiningEquitizer;



public class ModItems {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModItems.class);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IskaUtils.MOD_ID);

    // Common properties for all items
    private static final Item.Properties ITEM_PROPERTIES = new Item.Properties();

    // ===== WITHER PROOF BLOCKS =====
    
    // Wither Proof Block
    public static final DeferredItem<Item> WITHER_PROOF_BLOCK = ITEMS.register("wither_proof_block",
            () -> new BlockItem(ModBlocks.WITHER_PROOF_BLOCK.get(), ITEM_PROPERTIES));
            
    // Wither Proof Stairs
    public static final DeferredItem<Item> WITHER_PROOF_STAIRS = ITEMS.register("wither_proof_stairs",
            () -> new BlockItem(ModBlocks.WITHER_PROOF_STAIRS.get(), ITEM_PROPERTIES));
            
    // Wither Proof Slab
    public static final DeferredItem<Item> WITHER_PROOF_SLAB = ITEMS.register("wither_proof_slab",
            () -> new BlockItem(ModBlocks.WITHER_PROOF_SLAB.get(), ITEM_PROPERTIES));

    // Wither Proof Wall
    public static final DeferredItem<Item> WITHER_PROOF_WALL = ITEMS.register("wither_proof_wall",
            () -> new BlockItem(ModBlocks.WITHER_PROOF_WALL.get(), ITEM_PROPERTIES));
            
    // Netherite Bars
    public static final DeferredItem<Item> NETHERITE_BARS = ITEMS.register("netherite_bars",
            () -> new BlockItem(ModBlocks.NETHERITE_BARS.get(), ITEM_PROPERTIES));

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
            
    
    // Scanner - Custom item for scanning specific blocks in an area
    public static final DeferredItem<Item> SCANNER = ITEMS.register("scanner",
            () -> {
                ScannerItem scanner = new ScannerItem();
                return scanner;
            });

    // Scanner Chip - Custom item to store scanner targets
    public static final DeferredItem<Item> SCANNER_CHIP = ITEMS.register("scanner_chip",
            () -> new ScannerChipItem());
            
    // Scanner Chip for Ores - Specialized chip for scanning all ores
    public static final DeferredItem<Item> SCANNER_CHIP_ORES = ITEMS.register("scanner_chip_ores",
            () -> new ScannerChipItem() {
                @Override
                public ItemStack getDefaultInstance() {
                    // Initialize with "ores" target
                    ItemStack stack = super.getDefaultInstance();
                    this.setGenericTarget(stack, "ores");
                    return stack;
                }
            });
            
    // Scanner Chip for Mobs - Specialized chip for scanning all mobs
    public static final DeferredItem<Item> SCANNER_CHIP_MOBS = ITEMS.register("scanner_chip_mobs",
            () -> new ScannerChipItem() {
                @Override
                public ItemStack getDefaultInstance() {
                    // Initialize with "mobs" target
                    ItemStack stack = super.getDefaultInstance();
                    this.setGenericTarget(stack, "mobs");
                    return stack;
                }
            });
    
    // Necrotic Crystal Heart - Custom item that modifies incoming damage
    // Registered as a Curio when Curios is available
    public static final DeferredItem<Item> NECROTIC_CRYSTAL_HEART = ITEMS.register("necrotic_crystal_heart",
            () -> new NecroticCrystalHeartItem(new Item.Properties().stacksTo(1)));
            
    // Swiss Wrench - Custom item that can be used to repair vector plates
    public static final DeferredItem<Item> SWISS_WRENCH = ITEMS.register("swiss_wrench",
            () -> new SwissWrenchItem(new Item.Properties().stacksTo(1)));
            
    // Mining Equitizer - Custom item that negates flying mining speed penalty
    public static final DeferredItem<Item> MINING_EQUITIZER = ITEMS.register("mining_equitizer",
            () -> new MiningEquitizer(new Item.Properties().stacksTo(1)));
    
    // Dolly - Tool for picking up and moving blocks with their contents
    // Has 512 durability, works on blocks up to iron mining level
    public static final DeferredItem<Item> DOLLY = ITEMS.register("dolly",
            () -> new net.unfamily.iskautils.item.custom.DollyItem(new Item.Properties().stacksTo(1)));
    
    // Hard Dolly - Tool for picking up and moving blocks with their contents
    // Has 4096 durability, works on blocks up to iron mining level, separate config from regular dolly
    public static final DeferredItem<Item> DOLLY_HARD = ITEMS.register("dolly_hard",
            () -> new net.unfamily.iskautils.item.custom.HardDollyItem(new Item.Properties().stacksTo(1)));
            
    // ===== RUBBER TREE ITEMS =====
    
    // Sap item dropped from rubber logs
    public static final DeferredItem<Item> SAP = ITEMS.register("sap",
            () -> new Item(ITEM_PROPERTIES));
            
    // Rubber chunk item created from rubber
    public static final DeferredItem<Item> RUBBER_CHUNK = ITEMS.register("rubber_chunk",
            () -> new Item(ITEM_PROPERTIES));
            
    // Plastic sheet item created from rubber
    public static final DeferredItem<Item> PLASTIC_INGOT = ITEMS.register("plastic_ingot",
            () -> new Item(ITEM_PROPERTIES));

    // Rubber item created from sap
    public static final DeferredItem<Item> RUBBER = ITEMS.register("rubber",
            () -> new Item(ITEM_PROPERTIES));
            
    // Tree tap for collecting sap
    public static final DeferredItem<Item> TREE_TAP = ITEMS.register("treetap",
            () -> new TreeTapItem(new Item.Properties().durability(64)));
            
    // Electric tree tap for collecting sap (no durability)
    public static final DeferredItem<Item> ELECTRIC_TREE_TAP = ITEMS.register("electric_treetap",
            () -> new ElectricTreeTapItem(new Item.Properties().stacksTo(1)));
            
    // Rubber tree blocks
    public static final DeferredItem<Item> RUBBER_LOG = ITEMS.register("rubber_log",
            () -> new BlockItem(ModBlocks.RUBBER_LOG.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> STRIPPED_RUBBER_LOG = ITEMS.register("stripped_rubber_log",
            () -> new BlockItem(ModBlocks.STRIPPED_RUBBER_LOG.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_WOOD = ITEMS.register("rubber_wood",
            () -> new BlockItem(ModBlocks.RUBBER_WOOD.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> STRIPPED_RUBBER_WOOD = ITEMS.register("stripped_rubber_wood",
            () -> new BlockItem(ModBlocks.STRIPPED_RUBBER_WOOD.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_PLANKS = ITEMS.register("rubber_planks",
            () -> new BlockItem(ModBlocks.RUBBER_PLANKS.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_LOG_FILLED = ITEMS.register("rubber_log_filled",
            () -> new BlockItem(ModBlocks.RUBBER_LOG_FILLED.get(), new Item.Properties().stacksTo(64)));
            
    public static final DeferredItem<Item> RUBBER_LOG_EMPTY = ITEMS.register("rubber_log_empty",
            () -> new BlockItem(ModBlocks.RUBBER_LOG_EMPTY.get(), new Item.Properties().stacksTo(64)));
            
    public static final DeferredItem<Item> RUBBER_LEAVES = ITEMS.register("rubber_leaves",
            () -> new BlockItem(ModBlocks.RUBBER_LEAVES.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_SAPLING = ITEMS.register("rubber_sapling",
            () -> new BlockItem(ModBlocks.RUBBER_SAPLING.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> RUBBER_BLOCK = ITEMS.register("rubber_block",
            () -> new BlockItem(ModBlocks.RUBBER_BLOCK.get(), ITEM_PROPERTIES));
            
    // Rubber block variants
    public static final DeferredItem<Item> RUBBER_STAIRS = ITEMS.register("rubber_stairs",
            () -> new BlockItem(ModBlocks.RUBBER_STAIRS.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_SLAB = ITEMS.register("rubber_slab",
            () -> new BlockItem(ModBlocks.RUBBER_SLAB.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_FENCE = ITEMS.register("rubber_fence",
            () -> new BlockItem(ModBlocks.RUBBER_FENCE.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_FENCE_GATE = ITEMS.register("rubber_fence_gate",
            () -> new BlockItem(ModBlocks.RUBBER_FENCE_GATE.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_BUTTON = ITEMS.register("rubber_button",
            () -> new BlockItem(ModBlocks.RUBBER_BUTTON.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_PRESSURE_PLATE = ITEMS.register("rubber_pressure_plate",
            () -> new BlockItem(ModBlocks.RUBBER_PRESSURE_PLATE.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_DOOR = ITEMS.register("rubber_door",
            () -> new BlockItem(ModBlocks.RUBBER_DOOR.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_TRAPDOOR = ITEMS.register("rubber_trapdoor",
            () -> new BlockItem(ModBlocks.RUBBER_TRAPDOOR.get(), ITEM_PROPERTIES));

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
    public static final DeferredItem<Item> RAFT_NO_DROP = ITEMS.register("raft_no_drop",
            () -> new BlockItem(ModBlocks.RAFT_NO_DROP.get(), ITEM_PROPERTIES));

    // ===== TAR =====
    public static final DeferredItem<Item> TAR_SLIME_BLOCK = ITEMS.register("tar_slime_block",
            () -> new BlockItem(ModBlocks.TAR_SLIME_BLOCK.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> TAR_SLIMEBALL = ITEMS.register("tar_slimeball",
            () -> new Item(ITEM_PROPERTIES));

    // ===== RUBBER ARMOR =====
    public static final DeferredItem<Item> RUBBER_BOOTS = ITEMS.register("rubber_boots",
            () -> new RubberBootsItem(new Item.Properties().durability(256)));

            
    // ===== UTILITY BLOCKS =====
    public static final DeferredItem<Item> RUBBER_SAP_EXTRACTOR = ITEMS.register("rubber_sap_extractor", 
            () -> new BlockItem(ModBlocks.RUBBER_SAP_EXTRACTOR.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> WEATHER_DETECTOR = ITEMS.register("weather_detector",
            () -> new BlockItem(ModBlocks.WEATHER_DETECTOR.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> WEATHER_ALTERER = ITEMS.register("weather_alterer",
            () -> new BlockItem(ModBlocks.WEATHER_ALTERER.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> TIME_ALTERER = ITEMS.register("time_alterer",
            () -> new BlockItem(ModBlocks.TIME_ALTERER.get(), ITEM_PROPERTIES));

    // ===== ANGEL BLOCK =====
    public static final DeferredItem<Item> ANGEL_BLOCK = ITEMS.register("angel_block",
            () -> new AngelBlockItem(ModBlocks.ANGEL_BLOCK.get(), new Item.Properties().stacksTo(64)));

    // ===== STRUCTURE SYSTEM =====
    public static final DeferredItem<Item> STRUCTURE_PLACER_MACHINE = ITEMS.register("structure_placer_machine",
            () -> new BlockItem(ModBlocks.STRUCTURE_PLACER_MACHINE.get(), ITEM_PROPERTIES));
    
    public static final DeferredItem<Item> STRUCTURE_SAVER_MACHINE = ITEMS.register("structure_saver_machine",
            () -> new BlockItem(ModBlocks.STRUCTURE_SAVER_MACHINE.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> SHOP = ITEMS.register("shop",
            () -> new BlockItem(ModBlocks.SHOP.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> AUTO_SHOP = ITEMS.register("auto_shop",
            () -> new AutoShopItem(ModBlocks.AUTO_SHOP.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> STRUCTURE_PLACER = ITEMS.register("structure_placer",
            () -> new StructurePlacerItem(new Item.Properties().stacksTo(1)));
            
    // ===== DEV ITEMS =====
    
    // Blueprint - Strumento per salvare coordinate di vertici
    public static final DeferredItem<Item> BLUEPRINT = ITEMS.register("blueprint",
            () -> new BlueprintItem(new Item.Properties().stacksTo(1)));

    // Chaotic TNT Block
    public static final DeferredItem<Item> CHAOTIC_TNT = ITEMS.register("chaotic_tnt",
            () -> new BlockItem(ModBlocks.CHAOTIC_TNT.get(), ITEM_PROPERTIES));

    // ===== BURNING BRAZIER ITEM =====

    // Burning Brazier (places burning flame blocks when light level is low)
    public static final DeferredItem<Item> BURNING_BRAZIER = ITEMS.register("burning_brazier",
            () -> new BurningBrazierItem(new Item.Properties().stacksTo(1)));

    // Burning Flame Block Item (not indexed in creative tab)
    public static final DeferredItem<Item> BURNING_FLAME = ITEMS.register("burning_flame",
            () -> new BlockItem(ModBlocks.BURNING_FLAME.get(), ITEM_PROPERTIES));

    // ===== GHOST BRAZIER ITEM =====

    // Ghost Brazier (allows toggling between Survival and Spectator mode)
    public static final DeferredItem<Item> GHOST_BRAZIER = ITEMS.register("ghost_brazier",
            () -> new GhostBrazierItem(new Item.Properties().stacksTo(1)));

    // ===== GREEDY SHIELD ITEM =====

    // Greedy Shield (chance to block or reduce incoming damage)
    public static final DeferredItem<Item> GREEDY_SHIELD = ITEMS.register("greedy_shield",
            () -> new GreedyShieldItem(new Item.Properties().stacksTo(1)));

    // ===== DEEP DRAWERS =====
    public static final DeferredItem<Item> DEEP_DRAWERS = ITEMS.register("deep_drawers",
            () -> new net.unfamily.iskautils.item.custom.DeepDrawersBlockItem(ModBlocks.DEEP_DRAWERS.get(), ITEM_PROPERTIES));
    
    public static final DeferredItem<Item> DEEP_DRAWER_EXTRACTOR = ITEMS.register("deep_drawer_extractor",
            () -> new BlockItem(ModBlocks.DEEP_DRAWER_EXTRACTOR.get(), ITEM_PROPERTIES));

    // ===== GIFT BLOCK =====
    // Hidden block (not in creative tab)
    public static final DeferredItem<Item> GIFT = ITEMS.register("gift",
            () -> new GiftItem(ModBlocks.GIFT.get(), ITEM_PROPERTIES));

    // ===== HARD ICE BLOCK =====
    // Hidden block (not in creative tab) - indestructible, placed by gift
    public static final DeferredItem<Item> HARD_ICE = ITEMS.register("hard_ice",
            () -> new BlockItem(ModBlocks.HARD_ICE.get(), ITEM_PROPERTIES));

    // ===== FOOD ITEMS =====
    // Lapis Ice Cream - Food item with 8 nutrition and 1.0f saturation modifier
    public static final DeferredItem<Item> LAPIS_ICE_CREAM = ITEMS.register("lapis_ice_cream",
            () -> new Item(new Item.Properties().food(ModFoodProperties.LAPIS_ICE_CREAM)));

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