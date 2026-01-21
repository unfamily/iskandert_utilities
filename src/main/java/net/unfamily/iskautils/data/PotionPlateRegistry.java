package net.unfamily.iskautils.data;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.PotionPlateBlock;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for dynamically created potion plate blocks and items
 * Now uses configurations discovered during mod initialization
 */
public class PotionPlateRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // DeferredRegister for dynamic blocks and items
    public static final DeferredRegister.Blocks POTION_PLATES = DeferredRegister.createBlocks(IskaUtils.MOD_ID);
    public static final DeferredRegister.Items POTION_PLATE_ITEMS = DeferredRegister.createItems(IskaUtils.MOD_ID);
    
    // Maps to store dynamically registered blocks and items
    private static final Map<String, DeferredHolder<Block, PotionPlateBlock>> REGISTERED_BLOCKS = new HashMap<>();
    private static final Map<String, DeferredHolder<Item, BlockItem>> REGISTERED_ITEMS = new HashMap<>();
    
    // Maps for virtual blocks (loaded from external datapacks like KubeJS)
    private static final Map<String, PotionPlateConfig> VIRTUAL_BLOCKS = new HashMap<>();
    
    // Common properties for all potion plate blocks
    private static final BlockBehaviour.Properties POTION_PLATE_PROPERTIES = BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(0.3f, 1.0f)
            .sound(SoundType.DEEPSLATE)
            .noOcclusion()
            .noCollission()
            .isRedstoneConductor((state, level, pos) -> false)
            .pushReaction(PushReaction.DESTROY)
            .isViewBlocking((state, level, pos) -> false)
            .lightLevel((state) -> 0);
    
    // Common properties for all potion plate items
    private static final Item.Properties POTION_PLATE_ITEM_PROPERTIES = new Item.Properties();
    
    /**
     * Registers dynamic blocks and items based on discovered configurations
     * This is called during mod initialization, before RegisterEvent
     */
    public static void registerDiscoveredBlocks() {
        Map<String, PotionPlateConfig> configs = DynamicPotionPlateScanner.getDiscoveredConfigs();
        
        if (configs.isEmpty()) {
            LOGGER.info("No potion plate configurations discovered, skipping dynamic registration");
            return;
        }
        
        LOGGER.info("Registering {} dynamic potion plate blocks and items", configs.size());
        
        for (Map.Entry<String, PotionPlateConfig> entry : configs.entrySet()) {
            String plateId = entry.getKey();
            PotionPlateConfig config = entry.getValue();
            
            try {
                registerPotionPlate(plateId, config);
            } catch (Exception e) {
                LOGGER.error("Failed to register potion plate for config {}: {}", plateId, e.getMessage());
            }
        }
        
        LOGGER.info("Dynamic potion plate registration completed. Registered {} blocks and {} items", 
                   REGISTERED_BLOCKS.size(), REGISTERED_ITEMS.size());
    }
    
    /**
     * Registers a single potion plate block and item
     */
    private static void registerPotionPlate(String plateId, PotionPlateConfig config) {
        String blockName = config.getRegistryBlockName(); // Use registry-safe name (converts - to _)
        
        // Check if already registered
        if (REGISTERED_BLOCKS.containsKey(plateId)) {
            LOGGER.warn("Potion plate {} is already registered, skipping", plateId);
            return;
        }
        
        // Register the block
        DeferredHolder<Block, PotionPlateBlock> blockHolder = POTION_PLATES.register(blockName, 
            () -> new PotionPlateBlock(POTION_PLATE_PROPERTIES, config));
        
        // Decide whether to register the item in the creative tabs
        Item.Properties itemProperties;
        if (config.isCreativeTabVisible()) {
            // Use default properties (visible in creative tabs)
            itemProperties = POTION_PLATE_ITEM_PROPERTIES;
        } else {
            // Create new properties instance without any creative tab registration
            itemProperties = new Item.Properties();
        }
        
        // Register the item with appropriate properties
        DeferredHolder<Item, BlockItem> itemHolder = POTION_PLATE_ITEMS.register(blockName,
            () -> new net.unfamily.iskautils.item.custom.PotionPlateBlockItem(blockHolder.get(), itemProperties));
        
        REGISTERED_BLOCKS.put(plateId, blockHolder);
        REGISTERED_ITEMS.put(plateId, itemHolder);
        
        LOGGER.debug("Registered potion plate: {} -> {} (block and item, creative tab: {})", 
            plateId, blockName, config.isCreativeTabVisible());
    }
    
    /**
     * Gets a registered potion plate block by plate ID
     */
    public static DeferredHolder<Block, PotionPlateBlock> getBlock(String plateId) {
        return REGISTERED_BLOCKS.get(plateId);
    }
    
    /**
     * Gets a registered potion plate item by plate ID
     */
    public static DeferredHolder<Item, BlockItem> getItem(String plateId) {
        return REGISTERED_ITEMS.get(plateId);
    }
    
    /**
     * Gets all registered potion plate blocks
     */
    public static Map<String, DeferredHolder<Block, PotionPlateBlock>> getAllBlocks() {
        return new HashMap<>(REGISTERED_BLOCKS);
    }
    
    /**
     * Gets all registered potion plate items
     */
    public static Map<String, DeferredHolder<Item, BlockItem>> getAllItems() {
        return new HashMap<>(REGISTERED_ITEMS);
    }
    
    /**
     * Checks if a potion plate is registered (either real or virtual)
     */
    public static boolean isRegistered(String plateId) {
        return REGISTERED_BLOCKS.containsKey(plateId) || VIRTUAL_BLOCKS.containsKey(plateId);
    }
    
    /**
     * Checks if a potion plate is registered as a real block
     */
    public static boolean isRealBlock(String plateId) {
        return REGISTERED_BLOCKS.containsKey(plateId);
    }
    
    /**
     * Checks if a potion plate is registered as a virtual block
     */
    public static boolean isVirtualBlock(String plateId) {
        return VIRTUAL_BLOCKS.containsKey(plateId);
    }
    
    /**
     * Gets a virtual potion plate configuration by plate ID
     */
    public static PotionPlateConfig getVirtualConfig(String plateId) {
        return VIRTUAL_BLOCKS.get(plateId);
    }
    
    /**
     * Gets all virtual potion plate configurations
     */
    public static Map<String, PotionPlateConfig> getAllVirtualBlocks() {
        return new HashMap<>(VIRTUAL_BLOCKS);
    }
    
    /**
     * Registers a potion plate dynamically at runtime (for KubeJS support)
     * Since NeoForge registries are frozen at runtime, we use virtual blocks
     */
    public static void registerPotionPlateDynamic(String plateId, PotionPlateConfig config) {
        // Check if already registered as real block
        if (REGISTERED_BLOCKS.containsKey(plateId)) {
            LOGGER.warn("Potion plate {} is already registered as real block, skipping virtual registration", plateId);
            return;
        }
        
        // Check if already registered as virtual block
        if (VIRTUAL_BLOCKS.containsKey(plateId)) {
            LOGGER.warn("Virtual potion plate {} is already registered, updating configuration", plateId);
        }
        
        // Register as virtual block (configuration only)
        VIRTUAL_BLOCKS.put(plateId, config);
        LOGGER.info("Registered virtual potion plate: {} (configuration only, no physical block)", plateId);
        LOGGER.info("Virtual plates can be used by other systems but won't appear as placeable blocks");
    }
    
    /**
     * Gets the number of registered potion plates (real + virtual)
     */
    public static int getRegisteredCount() {
        return REGISTERED_BLOCKS.size() + VIRTUAL_BLOCKS.size();
    }
    
    /**
     * Gets the number of real registered potion plates
     */
    public static int getRealBlockCount() {
        return REGISTERED_BLOCKS.size();
    }
    
    /**
     * Gets the number of virtual registered potion plates
     */
    public static int getVirtualBlockCount() {
        return VIRTUAL_BLOCKS.size();
    }
} 