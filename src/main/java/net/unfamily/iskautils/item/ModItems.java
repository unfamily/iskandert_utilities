package net.unfamily.iskautils.item;

import java.util.function.UnaryOperator;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.item.custom.VectorCharmItem;
import net.unfamily.iskautils.item.custom.PortableDislocatorItem;
import net.unfamily.iskautils.item.custom.GauntletOfClimbingItem;
import net.unfamily.iskautils.item.custom.TreeTapItem;
import net.unfamily.iskautils.item.custom.ElectricTreeTapItem;
import net.unfamily.iskautils.item.custom.ScannerItem;
import net.unfamily.iskautils.item.custom.ScannerChipItem;
import net.unfamily.iskautils.item.custom.NecroticCrystalHeartItem;
import net.unfamily.iskautils.item.custom.SwissWrenchItem;
import net.unfamily.iskautils.item.custom.AngelBlockItem;
import net.unfamily.iskautils.item.custom.StructurePlacerItem;
import net.unfamily.iskautils.item.custom.BurningBrazierItem;
import net.unfamily.iskautils.item.custom.RedstoneSignalItem;
import net.unfamily.iskautils.item.custom.GhostBrazierItem;
import net.unfamily.iskautils.item.custom.GreedyShieldItem;
import net.unfamily.iskautils.item.custom.BlueprintItem;
import net.unfamily.iskautils.item.custom.AutoShopItem;
import net.unfamily.iskautils.item.custom.GiftItem;
import net.unfamily.iskautils.item.custom.TemporalOverclockerChipsetItem;
import net.unfamily.iskautils.item.custom.TemporalOverclockerBlockItem;
import net.unfamily.iskautils.item.custom.FanBlockItem;
import net.unfamily.iskautils.item.custom.ShopBlockItem;
import net.unfamily.iskautils.item.custom.HellfireIgniterBlockItem;
import net.unfamily.iskautils.item.custom.RubberSapExtractorBlockItem;
import net.unfamily.iskautils.item.custom.StructurePlacerMachineBlockItem;
import net.unfamily.iskautils.item.custom.StructureSaverMachineBlockItem;
import net.unfamily.iskautils.item.custom.RangeModuleItem;
import net.unfamily.iskautils.item.custom.GhostModuleItem;
import net.unfamily.iskautils.item.custom.SlowModuleItem;
import net.unfamily.iskautils.item.custom.ModerateModuleItem;
import net.unfamily.iskautils.item.custom.FastModuleItem;
import net.unfamily.iskautils.item.custom.ExtremeModuleItem;
import net.unfamily.iskautils.item.custom.UltraModuleItem;
import net.unfamily.iskautils.item.custom.LogicModuleItem;
import net.unfamily.iskautils.item.custom.SacredRubberSaplingBlockItem;

import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.unfamily.iskautils.item.custom.RubberBootsItem;
import net.unfamily.iskautils.item.custom.MiningEquitizer;



public class ModItems {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModItems.class);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IskaUtils.MOD_ID);

    // ===== WITHER PROOF BLOCKS =====

    public static final DeferredItem<BlockItem> WITHER_PROOF_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.WITHER_PROOF_BLOCK);

    public static final DeferredItem<BlockItem> WITHER_PROOF_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.WITHER_PROOF_STAIRS);

    public static final DeferredItem<BlockItem> WITHER_PROOF_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.WITHER_PROOF_SLAB);

    public static final DeferredItem<BlockItem> WITHER_PROOF_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.WITHER_PROOF_WALL);

    public static final DeferredItem<BlockItem> NETHERITE_BARS = ITEMS.registerSimpleBlockItem(ModBlocks.NETHERITE_BARS);

    // ===== CUSTOM ITEMS =====

    public static final DeferredItem<Item> BASE_MODULE = ITEMS.registerSimpleItem("base_module");

    public static final DeferredItem<Item> SLOW_MODULE = ITEMS.registerItem("slow_module", SlowModuleItem::new, UnaryOperator.identity());

    public static final DeferredItem<Item> MODERATE_MODULE = ITEMS.registerItem("moderate_module", ModerateModuleItem::new, UnaryOperator.identity());

    public static final DeferredItem<Item> FAST_MODULE = ITEMS.registerItem("fast_module", FastModuleItem::new, UnaryOperator.identity());

    public static final DeferredItem<Item> EXTREME_MODULE = ITEMS.registerItem("extreme_module", ExtremeModuleItem::new, UnaryOperator.identity());

    public static final DeferredItem<Item> ULTRA_MODULE = ITEMS.registerItem("ultra_module", UltraModuleItem::new, UnaryOperator.identity());

    public static final DeferredItem<Item> VECTOR_CHARM = ITEMS.registerItem("vector_charm", VectorCharmItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> FANPACK = ITEMS.registerItem("fanpack",
            net.unfamily.iskautils.item.custom.FanpackItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> PORTABLE_DISLOCATOR = ITEMS.registerItem("portable_dislocator", PortableDislocatorItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> GAUNTLET_OF_CLIMBING = ITEMS.registerItem("gauntlet_of_climbing", GauntletOfClimbingItem::new, p -> p.stacksTo(1));
            
    
    public static final DeferredItem<Item> SCANNER = ITEMS.registerItem("scanner", ScannerItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());

    public static final DeferredItem<Item> SCANNER_CHIP = ITEMS.registerItem("scanner_chip", ScannerChipItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<Item> SCANNER_CHIP_ORES = ITEMS.registerItem("scanner_chip_ores",
            props -> new ScannerChipItem(props) {
                @Override
                public ItemStack getDefaultInstance() {
                    ItemStack stack = super.getDefaultInstance();
                    setGenericTarget(stack, "ores");
                    return stack;
                }
            }, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<Item> SCANNER_CHIP_MOBS = ITEMS.registerItem("scanner_chip_mobs",
            props -> new ScannerChipItem(props) {
                @Override
                public ItemStack getDefaultInstance() {
                    ItemStack stack = super.getDefaultInstance();
                    setGenericTarget(stack, "mobs");
                    return stack;
                }
            }, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    
    // Necrotic Crystal Heart - Custom item that modifies incoming damage
    // Registered as a Curio when Curios is available
    public static final DeferredItem<Item> NECROTIC_CRYSTAL_HEART = ITEMS.registerItem("necrotic_crystal_heart", NecroticCrystalHeartItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> SWISS_WRENCH = ITEMS.registerItem("swiss_wrench", SwissWrenchItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> MINING_EQUITIZER = ITEMS.registerItem("mining_equitizer", MiningEquitizer::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> DOLLY = ITEMS.registerItem("dolly", net.unfamily.iskautils.item.custom.DollyItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> DOLLY_HARD = ITEMS.registerItem("dolly_hard", net.unfamily.iskautils.item.custom.HardDollyItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> DOLLY_CREATIVE = ITEMS.registerItem("dolly_creative",
            net.unfamily.iskautils.item.custom.CreativeDollyItem::new, p -> p.stacksTo(1).fireResistant());
            
    // ===== RUBBER TREE ITEMS =====
    
    // Sap item dropped from rubber logs
    public static final DeferredItem<Item> SAP = ITEMS.registerSimpleItem("sap");

    public static final DeferredItem<Item> DYE_BERRY = ITEMS.registerItem("dye_berry",
            net.unfamily.iskautils.item.custom.DyeBerryItem::new, p -> p.food(ModFoodProperties.DYE_BERRY));
    public static final DeferredItem<Item> GREEN_SLUDGE = ITEMS.registerSimpleItem("green_sludge");

    public static final DeferredItem<Item> RUBBER_CHUNK = ITEMS.registerSimpleItem("rubber_chunk");

    public static final DeferredItem<Item> PLASTIC_INGOT = ITEMS.registerSimpleItem("plastic_ingot");

    public static final DeferredItem<Item> RUBBER = ITEMS.registerSimpleItem("rubber");

    public static final DeferredItem<Item> TREE_TAP = ITEMS.registerItem("treetap", TreeTapItem::new, p -> p.durability(64));

    public static final DeferredItem<Item> ELECTRIC_TREE_TAP = ITEMS.registerItem("electric_treetap", ElectricTreeTapItem::new, p -> p.stacksTo(1));
            
    public static final DeferredItem<BlockItem> RUBBER_LOG = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_LOG);

    public static final DeferredItem<BlockItem> STRIPPED_RUBBER_LOG = ITEMS.registerSimpleBlockItem(ModBlocks.STRIPPED_RUBBER_LOG);

    public static final DeferredItem<BlockItem> RUBBER_WOOD = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_WOOD);

    public static final DeferredItem<BlockItem> STRIPPED_RUBBER_WOOD = ITEMS.registerSimpleBlockItem(ModBlocks.STRIPPED_RUBBER_WOOD);

    public static final DeferredItem<BlockItem> RUBBER_PLANKS = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_PLANKS);

    public static final DeferredItem<BlockItem> RUBBER_LOG_FILLED = ITEMS.registerSimpleBlockItem("rubber_log_filled", ModBlocks.RUBBER_LOG_FILLED::get, p -> p.stacksTo(64));

    public static final DeferredItem<BlockItem> RUBBER_LOG_EMPTY = ITEMS.registerSimpleBlockItem("rubber_log_empty", ModBlocks.RUBBER_LOG_EMPTY::get, p -> p.stacksTo(64));

    public static final DeferredItem<BlockItem> RUBBER_LEAVES = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_LEAVES);

    public static final DeferredItem<BlockItem> DYE_BUSH_EMPTY = ITEMS.registerSimpleBlockItem("dye_bush_empty", ModBlocks.DYE_BUSH_EMPTY::get, p -> p.stacksTo(64));
    public static final DeferredItem<BlockItem> DYE_BUSH_FILLED = ITEMS.registerSimpleBlockItem("dye_bush_filled", ModBlocks.DYE_BUSH_FILLED::get, p -> p.stacksTo(64));

    public static final DeferredItem<BlockItem> RUBBER_SAPLING = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_SAPLING);

    public static final DeferredItem<Item> SACRED_RUBBER_SAPLING = ITEMS.registerItem("sacred_rubber_sapling",
            props -> new SacredRubberSaplingBlockItem(ModBlocks.SACRED_RUBBER_SAPLING.get(), props), UnaryOperator.identity());

    public static final DeferredItem<BlockItem> SACRED_RUBBER_ROOT = ITEMS.registerSimpleBlockItem(ModBlocks.SACRED_RUBBER_ROOT);

    public static final DeferredItem<BlockItem> RUBBER_LOG_SACRED = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_LOG_SACRED);

    public static final DeferredItem<BlockItem> RUBBER_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_BLOCK);

    public static final DeferredItem<BlockItem> RUBBER_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_STAIRS);

    public static final DeferredItem<BlockItem> RUBBER_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_SLAB);

    public static final DeferredItem<BlockItem> RUBBER_FENCE = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_FENCE);

    public static final DeferredItem<BlockItem> RUBBER_FENCE_GATE = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_FENCE_GATE);

    public static final DeferredItem<BlockItem> RUBBER_BUTTON = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_BUTTON);

    public static final DeferredItem<BlockItem> RUBBER_PRESSURE_PLATE = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_PRESSURE_PLATE);

    public static final DeferredItem<BlockItem> RUBBER_DOOR = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_DOOR);

    public static final DeferredItem<BlockItem> RUBBER_TRAPDOOR = ITEMS.registerSimpleBlockItem(ModBlocks.RUBBER_TRAPDOOR);

    public static final DeferredItem<BlockItem> SLOW_VECT = ITEMS.registerSimpleBlockItem(ModBlocks.SLOW_VECT);

    public static final DeferredItem<BlockItem> MODERATE_VECT = ITEMS.registerSimpleBlockItem(ModBlocks.MODERATE_VECT);

    public static final DeferredItem<BlockItem> FAST_VECT = ITEMS.registerSimpleBlockItem(ModBlocks.FAST_VECT);

    public static final DeferredItem<BlockItem> EXTREME_VECT = ITEMS.registerSimpleBlockItem(ModBlocks.EXTREME_VECT);

    public static final DeferredItem<BlockItem> ULTRA_VECT = ITEMS.registerSimpleBlockItem(ModBlocks.ULTRA_VECT);

    public static final DeferredItem<BlockItem> PLAYER_SLOW_VECT = ITEMS.registerSimpleBlockItem(ModBlocks.PLAYER_SLOW_VECT);

    public static final DeferredItem<BlockItem> PLAYER_MODERATE_VECT = ITEMS.registerSimpleBlockItem(ModBlocks.PLAYER_MODERATE_VECT);

    public static final DeferredItem<BlockItem> PLAYER_FAST_VECT = ITEMS.registerSimpleBlockItem(ModBlocks.PLAYER_FAST_VECT);

    public static final DeferredItem<BlockItem> PLAYER_EXTREME_VECT = ITEMS.registerSimpleBlockItem(ModBlocks.PLAYER_EXTREME_VECT);

    public static final DeferredItem<BlockItem> PLAYER_ULTRA_VECT = ITEMS.registerSimpleBlockItem(ModBlocks.PLAYER_ULTRA_VECT);
            
    // ===== UTILITY ITEMS =====
    
    public static final DeferredItem<Item> HELLFIRE_IGNITER = ITEMS.registerItem("hellfire_igniter",
            props -> new HellfireIgniterBlockItem(ModBlocks.HELLFIRE_IGNITER.get(), props), UnaryOperator.identity());

    public static final DeferredItem<Item> FAN = ITEMS.registerItem("fan",
            props -> new FanBlockItem(ModBlocks.FAN.get(), props), UnaryOperator.identity());

    public static final DeferredItem<Item> RANGE_MODULE = ITEMS.registerItem("range_module", RangeModuleItem::new, UnaryOperator.identity());

    public static final DeferredItem<Item> GHOST_MODULE = ITEMS.registerItem("ghost_module", GhostModuleItem::new, UnaryOperator.identity());

    public static final DeferredItem<Item> LOGIC_MODULE = ITEMS.registerItem("logic_module", LogicModuleItem::new, UnaryOperator.identity());

    public static final DeferredItem<Item> CAPACITOR_MODULE = ITEMS.registerSimpleItem("capacitor_module");

    public static final DeferredItem<BlockItem> SMOOTH_BLACKSTONE = ITEMS.registerSimpleBlockItem(ModBlocks.SMOOTH_BLACKSTONE);
    public static final DeferredItem<BlockItem> SMOOTH_BLACKSTONE_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.SMOOTH_BLACKSTONE_SLAB);
    public static final DeferredItem<BlockItem> SMOOTH_BLACKSTONE_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.SMOOTH_BLACKSTONE_STAIRS);
    public static final DeferredItem<BlockItem> SMOOTH_BLACKSTONE_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.SMOOTH_BLACKSTONE_WALL);
    public static final DeferredItem<BlockItem> PLATE_BASE_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.PLATE_BASE_BLOCK);
    public static final DeferredItem<BlockItem> RAFT = ITEMS.registerSimpleBlockItem(ModBlocks.RAFT);
    public static final DeferredItem<BlockItem> RAFT_NO_DROP = ITEMS.registerSimpleBlockItem(ModBlocks.RAFT_NO_DROP);

    public static final DeferredItem<BlockItem> TAR_SLIME_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.TAR_SLIME_BLOCK);

    public static final DeferredItem<BlockItem> SAP_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.SAP_BLOCK);

    public static final DeferredItem<Item> TAR_SLIMEBALL = ITEMS.registerSimpleItem("tar_slimeball");

    public static final DeferredItem<Item> RUBBER_BOOTS = ITEMS.registerItem("rubber_boots", RubberBootsItem::new, p -> p.durability(256));

    public static final DeferredItem<Item> RUBBER_SAP_EXTRACTOR = ITEMS.registerItem("rubber_sap_extractor",
            props -> new RubberSapExtractorBlockItem(ModBlocks.RUBBER_SAP_EXTRACTOR.get(), props), UnaryOperator.identity());

    public static final DeferredItem<BlockItem> WEATHER_DETECTOR = ITEMS.registerSimpleBlockItem(ModBlocks.WEATHER_DETECTOR);

    public static final DeferredItem<BlockItem> SOUND_MUFFLER = ITEMS.registerSimpleBlockItem(ModBlocks.SOUND_MUFFLER);

    public static final DeferredItem<BlockItem> WEATHER_ALTERER = ITEMS.registerSimpleBlockItem(ModBlocks.WEATHER_ALTERER);

    public static final DeferredItem<BlockItem> TIME_ALTERER = ITEMS.registerSimpleBlockItem(ModBlocks.TIME_ALTERER);

    public static final DeferredItem<Item> TEMPORAL_OVERCLOCKER = ITEMS.registerItem("temporal_overclocker",
            props -> new TemporalOverclockerBlockItem(ModBlocks.TEMPORAL_OVERCLOCKER.get(), props), UnaryOperator.identity());

    public static final DeferredItem<Item> TEMPORAL_OVERCLOCKER_CHIPSET = ITEMS.registerItem("temporal_overclocker_chip",
            TemporalOverclockerChipsetItem::new, UnaryOperator.identity());

    public static final DeferredItem<Item> ANGEL_BLOCK = ITEMS.registerItem("angel_block",
            props -> new AngelBlockItem(ModBlocks.ANGEL_BLOCK.get(), props), p -> p.stacksTo(64));

    public static final DeferredItem<Item> STRUCTURE_PLACER_MACHINE = ITEMS.registerItem("structure_placer_machine",
            props -> new StructurePlacerMachineBlockItem(ModBlocks.STRUCTURE_PLACER_MACHINE.get(), props), UnaryOperator.identity());

    public static final DeferredItem<Item> STRUCTURE_SAVER_MACHINE = ITEMS.registerItem("structure_saver_machine",
            props -> new StructureSaverMachineBlockItem(ModBlocks.STRUCTURE_SAVER_MACHINE.get(), props), UnaryOperator.identity());

    public static final DeferredItem<Item> SHOP = ITEMS.registerItem("shop",
            props -> new ShopBlockItem(ModBlocks.SHOP.get(), props), UnaryOperator.identity());

    public static final DeferredItem<Item> AUTO_SHOP = ITEMS.registerItem("auto_shop",
            props -> new AutoShopItem(ModBlocks.AUTO_SHOP.get(), props), UnaryOperator.identity());

    public static final DeferredItem<BlockItem> SMART_TIMER = ITEMS.registerSimpleBlockItem(ModBlocks.SMART_TIMER);

    public static final DeferredItem<Item> STRUCTURE_PLACER = ITEMS.registerItem("structure_placer", StructurePlacerItem::new, p -> p.stacksTo(1));
            
    // ===== DEV ITEMS =====
    
    // Blueprint - Strumento per salvare coordinate di vertici
    public static final DeferredItem<Item> BLUEPRINT = ITEMS.registerItem("blueprint", BlueprintItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<BlockItem> CHAOTIC_TNT = ITEMS.registerSimpleBlockItem(ModBlocks.CHAOTIC_TNT);

    public static final DeferredItem<Item> BURNING_BRAZIER = ITEMS.registerItem("burning_brazier", BurningBrazierItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> REDSTONE_ACTIVATOR = ITEMS.registerItem("redstone_activator", RedstoneSignalItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<BlockItem> REDSTONE_ACTIVATOR_SIGNAL = ITEMS.registerSimpleBlockItem(ModBlocks.REDSTONE_ACTIVATOR_SIGNAL);

    public static final DeferredItem<BlockItem> BURNING_FLAME = ITEMS.registerSimpleBlockItem(ModBlocks.BURNING_FLAME);

    public static final DeferredItem<Item> GHOST_BRAZIER = ITEMS.registerItem("ghost_brazier", GhostBrazierItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> GREEDY_SHIELD = ITEMS.registerItem("greedy_shield", GreedyShieldItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> DEEP_DRAWERS = ITEMS.registerItem("deep_drawers",
            props -> new net.unfamily.iskautils.item.custom.DeepDrawersBlockItem(ModBlocks.DEEP_DRAWERS.get(), props), UnaryOperator.identity());

    public static final DeferredItem<BlockItem> DEEP_DRAWER_EXTRACTOR = ITEMS.registerSimpleBlockItem(ModBlocks.DEEP_DRAWER_EXTRACTOR);

    public static final DeferredItem<BlockItem> DEEP_DRAWER_INTERFACE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEP_DRAWER_INTERFACE);

    public static final DeferredItem<BlockItem> DEEP_DRAWER_EXTENDER = ITEMS.registerSimpleBlockItem(ModBlocks.DEEP_DRAWER_EXTENDER);

    public static final DeferredItem<Item> GIFT = ITEMS.registerItem("gift",
            props -> new GiftItem(ModBlocks.GIFT.get(), props), UnaryOperator.identity());

    public static final DeferredItem<BlockItem> HARD_ICE = ITEMS.registerSimpleBlockItem(ModBlocks.HARD_ICE);

    public static final DeferredItem<Item> LAPIS_ICE_CREAM = ITEMS.registerItem("lapis_ice_cream", Item::new, p -> p.food(ModFoodProperties.LAPIS_ICE_CREAM));

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