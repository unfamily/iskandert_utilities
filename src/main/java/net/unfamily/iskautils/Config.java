package net.unfamily.iskautils;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = IskaUtils.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Category for Vector Plates
    static {
        BUILDER.comment("Vector Things Configuration").push("vector_plates");
    }

    // Vector plates speed configuration, ordered from slowest to fastest
    private static final ModConfigSpec.DoubleValue SLOW_VECTOR_SPEED = BUILDER
            .comment("Speed of the Slow Vector Plate")
            .defineInRange("000_slowVectorSpeed", 0.1D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue MODERATE_VECTOR_SPEED = BUILDER
            .comment("Speed of the Moderate Vector Plate")
            .defineInRange("001_moderateVectorSpeed", 0.5D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue FAST_VECTOR_SPEED = BUILDER
            .comment("Speed of the Fast Vector Plate")
            .defineInRange("002_fastVectorSpeed", 1.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue EXTREME_VECTOR_SPEED = BUILDER
            .comment("Speed of the Extreme Vector Plate")
            .defineInRange("003_extremeVectorSpeed", 5.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ULTRA_VECTOR_SPEED = BUILDER
            .comment("Speed of the Ultra Vector Plate")
            .defineInRange("004_ultraVectorSpeed", 15.0D, 0.0D, 100.0D);

    // Vector plate vertical boost factors
    private static final ModConfigSpec.DoubleValue VERTICAL_BOOST_FACTOR = BUILDER
            .comment("Vertical boost factor for players on vertical vector plates")
            .defineInRange("005_verticalBoostPlayer", 0.5D, -100.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ENTITY_VERTICAL_BOOST_FACTOR = BUILDER
            .comment("Vertical boost factor for mobs/entities on vertical vector plates (higher values help heavier mobs overcome gravity)")
            .defineInRange("006_verticalBoostEntity", 1.2D, -100.0D, 100.0D);

    private static final ModConfigSpec.BooleanValue VERTICAL_CONVEYOR_ENABLED = BUILDER
            .comment("Enable vertical conveyor belts")
            .define("007_verticalConveyorEnabled", true);

    // Vector Charm configurazione
    private static final ModConfigSpec.BooleanValue VERTICAL_CHARM_ENABLED = BUILDER
            .comment("Enable vertical vector charm")
            .define("100_verticalCharmEnabled", true);

    private static final ModConfigSpec.BooleanValue HORIZONTAL_CHARM_ENABLED = BUILDER
            .comment("Enable horizontal vector charm")
            .define("101_horizontalCharmEnabled", true);

    // Vector Charm energy configuration
    private static final ModConfigSpec.IntValue VECTOR_CHARM_ENERGY_CAPACITY = BUILDER
            .comment("Energy capacity of the Vector Charm in RF/FE",
                    "Recommended value: 1000000, but set to 0 to disable energy consumption")
            .defineInRange("102_vectorCharmEnergyCapacity", 0, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> VECTOR_CHARM_ENERGY_CONSUME = BUILDER
            .comment("Energy consumed per tick by the Vector Charm when active in RF/FE",
                     "Array with 7 values for: [none, slow, moderate, fast, extreme, ultra, hover]",
                     "Recommended values: [0, 5, 15, 30, 50, 100, 3], but set to 0 to disable energy consumption")
            .defineList("103_vectorCharmEnergyConsume", 
                       java.util.Arrays.asList(0, 0, 0, 0, 0, 0, 0), 
                       obj -> obj instanceof Integer && (Integer) obj >= 0);

    static {
        BUILDER.pop(); // End of vector_plates category
        
        // Category for General Utilities
        BUILDER.comment("General Utilities Configuration").push("general_utilities");
    }

    private static final ModConfigSpec.IntValue HELLFIRE_IGNITER_CONSUME = BUILDER
            .comment("Amount of energy consumed by the Hellfire Igniter",
                    "Recommended value: 10, but set to 0 to disable energy consumption")
            .defineInRange("000_hellfireIgniterConsume", 0, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue HELLFIRE_IGNITER_BUFFER = BUILDER
            .comment("Quantity of energy the Hellfire Igniter can be stored",
                    "Recommended value: 1000, but set to 0 to disable energy consumption")
            .defineInRange("001_hellfireIgniterBuffer", 0, 0, Integer.MAX_VALUE);

    // Portable Dislocator energy configuration
    private static final ModConfigSpec.IntValue PORTABLE_DISLOCATOR_ENERGY_CAPACITY = BUILDER
            .comment("Energy capacity of the Portable Dislocator in RF/FE",
                    "Recommended value: 5000, but set to 0 to disable energy consumption")
            .defineInRange("100_portableDislocatorEnergyCapacity", 5000, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue PORTABLE_DISLOCATOR_ENERGY_CONSUME = BUILDER
            .comment("Energy consumed per teleportation by the Portable Dislocator in RF/FE",
                    "Recommended value: 1000 or 100, but set to 0 to disable energy consumption")
            .defineInRange("100_portableDislocatorEnergyConsume", 100, 0, Integer.MAX_VALUE);
            
    // Portable Dislocator experience configuration
    private static final ModConfigSpec.IntValue PORTABLE_DISLOCATOR_XP_CONSUME = BUILDER
            .comment("Experience points consumed per teleportation by the Portable Dislocator when energy is not available",
                    "Set to 0 to disable experience consumption")
            .defineInRange("101_portableDislocatorXpConsume", 10, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue WEATHER_ALTERER_ENERGY_BUFFER = BUILDER
            .comment("Energy capacity of the Weather Alterer in RF/FE")
            .comment("Recommended value: 100000, but set to 0 to disable energy consumption")
            .defineInRange("200_weatherAltererEnergyBuffer", 0, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue WEATHER_ALTERER_ENERGY_CONSUME = BUILDER
            .comment("Energy consumed per tick by the Weather Alterer in RF/FE")
            .comment("Recommended value: 5000, but set to 0 to disable energy consumption")
            .defineInRange("201_weatherAltererEnergyConsume", 0, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue TIME_ALTERER_ENERGY_BUFFER = BUILDER
            .comment("Energy capacity of the Time Alterer in RF/FE")
            .comment("Recommended value: 100000, but set to 0 to disable energy consumption")
            .defineInRange("202_timeAltererEnergyBuffer", 0, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue TIME_ALTERER_ENERGY_CONSUME = BUILDER
            .comment("Energy consumed per tick by the Time Alterer in RF/FE")
            .comment("Recommended value: 5000, but set to 0 to disable energy consumption")
            .defineInRange("203_timeAltererEnergyConsume", 0, 0, Integer.MAX_VALUE);
            
    // Portable Dislocator resource priority configuration
    private static final ModConfigSpec.BooleanValue PORTABLE_DISLOCATOR_PRIORITIZE_ENERGY = BUILDER
            .comment("If true, energy will be consumed before XP when both are available",
                    "If both this and prioritizeXp are true, both resources will be consumed")
            .define("102_portableDislocatorPrioritizeEnergy", true);
            
    private static final ModConfigSpec.BooleanValue PORTABLE_DISLOCATOR_PRIORITIZE_XP = BUILDER
            .comment("If true, XP will be consumed before energy when both are available",
                    "If both this and prioritizeEnergy are true, both resources will be consumed")
            .define("103_portableDislocatorPrioritizeXp", false);

    private static final ModConfigSpec.IntValue RUBBER_SAP_EXTRACTOR_SPEED = BUILDER
            .comment("Speed of the Rubber Sap Extractor in ticks (lower is faster)")
            .defineInRange("200_rubberSapExtractorSpeed", 10, 1, Integer.MAX_VALUE);
    
    // Structure Placer Machine configuration
    private static final ModConfigSpec.IntValue STRUCTURE_PLACER_MACHINE_ENERGY_CONSUME = BUILDER
            .comment("Energy consumed per block placed by the Structure Placer Machine in RF/FE")
            .defineInRange("300_structurePlacerMachineEnergyConsume", 50, 0, Integer.MAX_VALUE);
    
    private static final ModConfigSpec.IntValue STRUCTURE_PLACER_MACHINE_ENERGY_BUFFER = BUILDER
            .comment("Energy capacity of the Structure Placer Machine in RF/FE")
            .defineInRange("301_structurePlacerMachineEnergyBuffer", 10000, 0, Integer.MAX_VALUE);
    
    // Deep Drawers Configuration (in general_utilities category)
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DEEP_DRAWERS_ALLOWED_TAGS = BUILDER
            .comment("List of item tags that can be stored in the Deep Drawers",
                    "Tags starting with # are item tags (e.g. #c:enchantables)",
                    "Items without # are item IDs (e.g. apotheosis:gem, minecraft:enchanted_book)",
                    "Only items matching these tags/IDs will be accepted")
            .defineList("400_deep_drawers_allowed_tags", 
                       java.util.Arrays.asList("#c:enchantables", "apotheosis:gem", "minecraft:enchanted_book", "minecraft:potion"), 
                       obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DEEP_DRAWERS_BLACKLIST = BUILDER
            .comment("List of item tags/IDs that are explicitly forbidden in the Deep Drawers",
                    "This blacklist overrides the allowed tags list",
                    "Tags starting with # are item tags, items without # are item IDs",
                    "Default includes minecraft:book for safety reasons")
            .defineList("401_deep_drawers_blacklist",
                    java.util.Arrays.asList("minecraft:book"),
                    obj -> obj instanceof String);

    private static final ModConfigSpec.IntValue DEEP_DRAWERS_SLOT_COUNT = BUILDER
            .comment("Number of slots in the Deep Drawers storage",
                    "Default: 49995 (9 x 5555, close to 50000)",
                    "Higher values may impact performance")
            .defineInRange("402_deep_drawers_slot_count", 49995, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue DEEP_DRAWERS_GUI_ENABLED = BUILDER
            .comment("Enable the Deep Drawers GUI (not perfect, use at your own risk)",
                    "Default: false (disabled)",
                    "WARNING: The GUI is not perfect and its use is discouraged")
            .define("403_deep_drawers_gui_enabled", false);

    // Deep Drawer Extractor Configuration (starts at 410)
    private static final ModConfigSpec.IntValue DEEP_DRAWER_EXTRACTOR_INTERVAL = BUILDER
            .comment("Extraction interval in ticks for the Deep Drawer Extractor (lower is faster)",
                    "Default: 1 (extracts every tick, maximum speed)",
                    "1 tick = 0.05 seconds")
            .defineInRange("410_deep_drawer_extractor_interval", 1, 1, Integer.MAX_VALUE);
    
    // Dolly Configuration (in general_utilities category)
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DOLLY_WHITELIST = BUILDER
            .comment("List of block tags/IDs that can be picked up by the Dolly",
                    "Tags starting with # are block tags (e.g. #minecraft:mineable/pickaxe)",
                    "Blocks without # are block IDs (e.g. minecraft:chest, minecraft:furnace)",
                    "Empty list means all blocks are allowed (subject to blacklist and hardness/tool requirements)",
                    "If whitelist is not empty, only blocks matching these tags/IDs will be allowed")
            .defineList("500_dolly_whitelist", 
                       java.util.Collections.emptyList(), 
                       obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DOLLY_BLACKLIST = BUILDER
            .comment("List of block tags/IDs that are explicitly forbidden from being picked up by the Dolly",
                    "This blacklist ALWAYS takes priority over the whitelist",
                    "Tags starting with # are block tags, blocks without # are block IDs",
                    "Examples: minecraft:bedrock, minecraft:end_portal_frame, #c:ores")
            .defineList("501_dolly_blacklist",
                    java.util.Collections.emptyList(),
                    obj -> obj instanceof String);
    
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DOLLY_ALLOWED_MINING_LEVEL_TAGS = BUILDER
            .comment("List of block tags that represent allowed mining levels for the Dolly",
                    "Only blocks matching these mining level tags can be picked up",
                    "Default includes wooden, stone, and iron tool levels",
                    "Tags must start with #",
                    "Empty list means no mining level check (only hardness < 0 blocks are rejected)",
                    "Examples: #minecraft:mineable/pickaxe, #minecraft:needs_stone_tool, #minecraft:needs_iron_tool")
            .defineList("502_dolly_allowed_mining_level_tags",
                    java.util.Arrays.asList(
                            "#minecraft:mineable/axe",
                            "#minecraft:mineable/pickaxe",
                            "#minecraft:mineable/shovel",
                            "#minecraft:mineable/hoe",
                            "#minecraft:needs_stone_tool",
                            "#minecraft:needs_iron_tool"
                    ),
                    obj -> obj instanceof String);
    
    // Hard Dolly Configuration (in general_utilities category)
    // Hard Dolly has no limits - can pick up any block except indestructible ones (hardness < 0)
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> HARD_DOLLY_WHITELIST = BUILDER
            .comment("List of block tags/IDs that can be picked up by the Hard Dolly",
                    "Tags starting with # are block tags (e.g. #minecraft:mineable/pickaxe)",
                    "Blocks without # are block IDs (e.g. minecraft:chest, minecraft:furnace)",
                    "Empty list (default) means ALL blocks are allowed (except indestructible ones with hardness < 0)",
                    "If whitelist is not empty, only blocks matching these tags/IDs will be allowed")
            .defineList("510_hard_dolly_whitelist", 
                       java.util.Collections.emptyList(), 
                       obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> HARD_DOLLY_BLACKLIST = BUILDER
            .comment("List of block tags/IDs that are explicitly forbidden from being picked up by the Hard Dolly",
                    "This blacklist ALWAYS takes priority over the whitelist",
                    "Tags starting with # are block tags, blocks without # are block IDs",
                    "Empty list (default) means no blocks are blacklisted",
                    "Examples: minecraft:bedrock, minecraft:end_portal_frame, #c:ores")
            .defineList("511_hard_dolly_blacklist",
                    java.util.Collections.emptyList(),
                    obj -> obj instanceof String);
    
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> HARD_DOLLY_ALLOWED_MINING_LEVEL_TAGS = BUILDER
            .comment("List of block tags that represent allowed mining levels for the Hard Dolly",
                    "Empty list (default) means NO mining level check - all blocks allowed except indestructible ones (hardness < 0)",
                    "Tags must start with #",
                    "If not empty, only blocks matching these mining level tags can be picked up",
                    "Examples: #minecraft:mineable/pickaxe, #minecraft:needs_stone_tool, #minecraft:needs_iron_tool")
            .defineList("512_hard_dolly_allowed_mining_level_tags",
                    java.util.Collections.emptyList(),
                    obj -> obj instanceof String);
            
    static {
        BUILDER.pop(); // End of general_utilities category
        
        // === Rubber Tree Configuration ===
        BUILDER.comment("Rubber Tree Configuration").push("rubber_tree");
    }
    
    public static final ModConfigSpec.IntValue MIN_SAP_REFILL_TIME = BUILDER
            .comment("Minimum time in ticks for a rubber log to refill with sap (1 tick = 1/20 second)")
            .defineInRange("000_minSapRefillTime", 600, 0, Integer.MAX_VALUE);
            
    public static final ModConfigSpec.IntValue MAX_SAP_REFILL_TIME = BUILDER
            .comment("Maximum time in ticks for a rubber log to refill with sap (1 tick = 1/20 second)")
            .defineInRange("001_maxSapRefillTime", 1800, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ELECTRIC_TREETAP_ENERGY_CONSUME = BUILDER
            .comment("Amount of energy consumed by the Electric Treetap")
            .defineInRange("002_electricTreetapEnergyConsume", 10, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue ELECTRIC_TREETAP_ENERGY_BUFFER = BUILDER
            .comment("Amount of energy the Electric Treetap can be stored")
            .defineInRange("003_electricTreetapEnergyBuffer", 1000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue RUBBER_SAP_EXTRACTOR_ENERGY_CONSUME = BUILDER
            .comment("Amount of energy consumed by the Rubber Sap Extractor per operation")
            .defineInRange("004_rubberSapExtractorEnergyConsume", 10, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue RUBBER_SAP_EXTRACTOR_ENERGY_BUFFER = BUILDER
            .comment("Amount of energy the Rubber Sap Extractor can store")
            .defineInRange("005_rubberSapExtractorEnergyBuffer", 10000, 0, Integer.MAX_VALUE);


    static {
        BUILDER.pop(); // End of rubber_tree category
        
        // Category for Development/Advanced Configuration
        BUILDER.comment("Tweaks Configuration").push("tweaks");
        }
 
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>>  sticky_fluids = BUILDER
            .comment("List of fluids that should be sticky")
            .defineList("000_sticky_fluids", java.util.Arrays.asList("#c:oil"), obj -> obj instanceof String);

    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>>  crude_oils = BUILDER
            .comment("List of fluids that should be crude oil")
            .defineList("001_crude_oils", java.util.Arrays.asList("#c:oil"), obj -> obj instanceof String);

            
    static {
        BUILDER.pop(); // pop rubber_sap category
        
        // Category for scanner
        BUILDER.comment("Scanner Configuration").push("scanner");
    }

    //removed TTL from scanner config is hardcoded now

    private static final ModConfigSpec.IntValue SCANNER_SCAN_RANGE = BUILDER
            .comment("Maximum scan range in blocks")
            .defineInRange("000_scannerScanRange", 64, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue SCANNER_SCAN_DURATION = BUILDER
            .comment("Duration in ticks needed to hold the scanner for scanning (1 second = 20 ticks)")
            .defineInRange("001_scannerScanDuration", 60, 1, 200);

    private static final ModConfigSpec.IntValue SCANNER_MAX_BLOCKS = BUILDER
            .comment("Maximum number of blocks that can be scanned at once")
            .defineInRange("002_scannerMaxBlocks", 8192, 1, Integer.MAX_VALUE);

    // Scanner energy configuration
    private static final ModConfigSpec.IntValue SCANNER_ENERGY_CONSUME = BUILDER
            .comment("Amount of energy consumed per scan operation by the Scanner")
            .defineInRange("003_scannerEnergyConsume", 50, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue SCANNER_ENERGY_BUFFER = BUILDER
            .comment("Energy capacity of the Scanner in RF/FE")
            .defineInRange("004_scannerEnergyBuffer", 10000, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue SCANNER_MARKER_TTL = BUILDER
            .comment("Time to live for the scanner markers in ticks (1 second = 20 ticks)")
            .defineInRange("005_scannerMarkerTTL", 600, 1, Integer.MAX_VALUE);


    private static final ModConfigSpec.ConfigValue<String> SCANNER_DEFAULT_ALPHA = BUILDER
    .comment("Default alpha value for scanner markers (in hexadecimal)")
    .define("006_scannerDefaultAlpha", "80");

    private static final ModConfigSpec.ConfigValue<String> SCANNER_DEFAULT_ORE_COLOR = BUILDER
    .comment("Default color for ore markers (in hexadecimal RGB)")
    .define("007_scannerDefaultOreColor", "00BFFF");

    private static final ModConfigSpec.ConfigValue<String> SCANNER_DEFAULT_MOB_COLOR = BUILDER
    .comment("Default color for mob markers (in hexadecimal RGB)")
    .define("008_scannerDefaultMobColor", "EB3480");


    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SCANNER_ORE_ENTRIES = BUILDER
            .comment("List of ore entries that can be scanned with their colors",
                    "Format: 'ore_name;RRGGBB' where:",
                    "- ore_name is a special prefix that will match all blocks containing this name",
                    "- RRGGBB is the hexadecimal RGB color",
                    "Example: 'gold;FFD700' will match all gold ores with gold color")
            .defineList("100_scanner_ore_entries", 
                    java.util.Arrays.asList(
                        "gold;FFD700",     // Gold ore - Gold color
                        "iron;C0C0C0",     // Iron ore - Silver color
                        "diamond;00FFFF",  // Diamond ore - White-Cyan color
                        "emerald;00FF00",  // Emerald ore - Green color
                        "coal;333333",     // Coal ore - Dark gray
                        "lapis;0000FF",    // Lapis ore - Blue color
                        "redstone;FF0000", // Redstone ore - Red color
                        "copper;D2691E",   // Copper ore - Copper color
                        "quartz;FFFAFA",   // Quartz ore - White color
                        "netherite;4B0082", // Ancient Debris - Indigo color
                        "ancient_debris;4B0082", // Ancient Debris - Indigo color
                        "amethyst;9966CC", // Amethyst ore - Purple color
                        "certus;BCF7F7", // Certus Quartz ore - White-Lightblue color
                        "uraninite;00FF00", // Uraninite ore - Green color
                        "uranium;365226", // Uranium ore - Olive Green color
                        "fluorite;FC79CF", // Fluorite ore - Pink color
                        "tin;BCF7F7", // Tin ore - White-Lightblue color
                        "lead;381C80", // Lead ore - Blue-Purple color
                        "silver;B6B6B8", // Silver ore - White-Gray color
                        "platinum;A1D1CC", // Platinum ore - Light-Blue color
                        "osmium;A1D1CC", // Platinum ore - Light-Blue color
                        "nickel;CED1A1", // Nickel ore - White-Yellow color
                        "aluminum;B6B6B8", // Aluminum ore - White-Gray color
                        "zinc;00FF00", // Zinc ore - White-Green color
                        "allthemodium;C78938", // Allthemodium ore - Orange color
                        "vibranium;40BBBF", // Vibranium ore - Blue-Green color
                        "unubtanium;993BAC" // Unubtanium ore - Purple color
                    ), 
                    obj -> obj instanceof String && ((String)obj).contains(";"));

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SCANNER_MOB_ENTRIES = BUILDER
            .comment("List of mob entries that can be scanned with their colors",
                    "Format: '$mob_name;RRGGBB' for prefix matching or 'modid:exact_mob;RRGGBB' for exact match",
                    "- $mob_name will match all entities containing this name",
                    "- RRGGBB is the hexadecimal RGB color",
                    "Example: '$zombie;00AA00' will match all zombie variants")
            .defineList("101_scanner_mob_entries", 
                    java.util.Arrays.asList(
                        "$creeper;00FF00",   // Creeper - Green
                        "$zombie;00AA00",    // Zombie variants - Dark green
                        "$skeleton;CCCCCC",  // Skeleton variants - Light gray
                        "$spider;222222",    // Spider variants - Black
                        "$enderman;AA00AA",  // Enderman - Purple
                        "$slime;55FF55",     // Slime - Light green
                        "$witch;AA00FF",     // Witch - Purple/Magenta
                        "$blaze;FF6600",     // Blaze - Orange
                        "$ghast;FFFFFF",     // Ghast - White
                        "$wither;333333",    // Wither - Black
                        "$dragon;AA00AA",    // Ender Dragon - Purple
                        "minecraft:evoker;0000FF", // Evoker - Blue (exact match)
                        "$villager;AA8866",  // Villager - Brown
                        "$golem;CCCCCC",     // Iron Golem - Light gray
                        "$axolotl;FF99FF",   // Axolotl - Pink
                        "$allay;99CCFF",     // Allay - Light blue
                        "$cow;AA5522",       // Cow - Brown
                        "$sheep;FFFFFF",     // Sheep - White
                        "$chicken;FFFF00",    // Chicken - Yellow
                        "$mimic;4A2150"     // Mimic - Brown
                    ), 
                    obj -> obj instanceof String && ((String)obj).contains(";"));


    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SCANNER_ORE_TAGS = BUILDER
            .comment("List of ore tags that should be scanned when using the ore scanner chip",
                    "These tags will be used to identify blocks as ores",
                    "You can use * as a wildcard at the end of a tag name")
            .defineList("102_scanner_ore_tags", 
                    java.util.Arrays.asList(
                        "c:ores",
                        "c:raw_materials",
                        "c:storage_blocks",
                        "c:budding_blocks"
                    ), 
                    obj -> obj instanceof String);

    static {
        BUILDER.pop(); // End of scanner category
        BUILDER.comment("Development and Advanced Configuration").push("dev");
    }

    // External Scripts configuration
    private static final ModConfigSpec.ConfigValue<String> EXTERNAL_SCRIPTS_PATH = BUILDER
            .comment("Path to the external scripts directory for custom potion plates and stage items",
                    "Default: 'kubejs/external_scripts'",
                    "The system will look for any scripts of this mod: <path>/potion_plates/")
            .define("000_external_scripts_path", "kubejs/external_scripts");

    private static final ModConfigSpec.ConfigValue<String> CLIENT_STRUCTURE_PATH = BUILDER
            .comment("Path to the external scripts directory for custom structures",
                    "Default: 'iska_utils_client/structures'",
                    "The system will look for any scripts of this mod: <path>/iska_utils_structures/")
            .define("001_client_structure_path", "iska_utils_client/structures");

    private static final ModConfigSpec.BooleanValue ACCEPT_CLIENT_STRUCTURE = BUILDER
            .comment("If true, the client will be able to use the structures saved by the server",
                    "Default: true")
            .define("002_accept_client_structure", true);

    public static final ModConfigSpec.BooleanValue ALLOW_CLIENT_STRUCTURE_PLAYER_LIKE = BUILDER
            .comment("If true, the client structure are placed like players",
                    "Default: true")
            .define("003_allow_client_structure_player_like", true);

    private static final ModConfigSpec.BooleanValue ARTIFACTS_INFO= BUILDER
            .comment("If false not desplay where obtain the artifacts or mod dependecy required for obtain it (only for lootable artifacts)")
            .define("100_artifacts_info", true);

    // Category for Artifacts Settings (under dev)
    static {
        BUILDER.comment("Artifacts Settings").push("artifacts_settings");
    }

    private static final ModConfigSpec.DoubleValue GREEDY_SHIELD_BLOCK_CHANCE = BUILDER
            .comment("Chance for Greedy Shield to completely block damage (0.0 to 1.0)",
                    "Default: 0.3 (30%)")
            .defineInRange("000_greedy_shield_block_chance", 0.3D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue GREEDY_SHIELD_REDUCE_CHANCE = BUILDER
            .comment("Chance for Greedy Shield to reduce damage by 80% if block fails (0.0 to 1.0)",
                    "Default: 0.3 (30%)")
            .defineInRange("001_greedy_shield_reduce_chance", 0.3D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue GREEDY_SHIELD_REDUCE_AMOUNT = BUILDER
            .comment("Damage remaining amount when Greedy Shield reduces damage (0.0 to 1.0)",
                    "Default: 0.8 (blocks 20% of damage, so 80% of original damage remains)")
            .defineInRange("002_greedy_shield_reduce_amount", 0.8D, 0.0D, 1.0D);

    private static final ModConfigSpec.BooleanValue GREEDY_SHIELD_INFO = BUILDER
            .comment("If false, hide where to obtain the Greedy Shield (only for lootable artifacts)")
            .define("003_greedy_shield_info", true);

    static {
        BUILDER.pop(); // End of artifacts_settings category
        BUILDER.pop(); // End of dev category

        // Category for Evil Things (fun but fiery!)
        BUILDER.comment("Evil Things - Fun but Fiery!").push("evil_things");
    }

    private static final ModConfigSpec.BooleanValue BURNING_BRAZIER_SUPER_HOT = BUILDER
            .comment("If true, makes the Burning Brazier extra hot - when placing a flame, set on fire the player!",
                    "Also activates automatically if player has stage 'iska_utils_internal-flame_curse'")
            .define("000_burning_brazier_super_hot", false);

    private static final ModConfigSpec.BooleanValue BURNING_FLAME_SUPER_HOT = BUILDER
            .comment("If true, makes Burning Flame blocks extra hot - mobs & players touching them will catch fire!",
                    "Also activates automatically if world has stage 'iska_utils_internal-flame_curse'")
            .define("001_burning_flame_super_hot", false);

    private static final ModConfigSpec.BooleanValue GIFT_PLACE_HARD_ICE = BUILDER
            .comment("If true, breaking a Gift block will place a Hard Ice block after 3 seconds",
                    "Hard Ice is indestructible and cannot be broken")
            .define("100_gift_place_hard_ice", true);

    static {
        BUILDER.pop(); // End of evil_things category
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    // Public variables to access configuration values
    public static double slowVectorSpeed;
    public static double moderateVectorSpeed;
    public static double fastVectorSpeed;
    public static double extremeVectorSpeed;
    public static double ultraVectorSpeed;
    public static double verticalBoostFactor;
    public static double entityVerticalBoostFactor;
    public static boolean verticalConveyorEnabled;
    public static int hellfireIgniterConsume;
    public static int hellfireIgniterBuffer;
    public static boolean verticalCharmEnabled;
    public static boolean horizontalCharmEnabled;
    public static int vectorCharmEnergyCapacity;
    public static java.util.List<Integer> vectorCharmEnergyConsume;
    public static int portableDislocatorEnergyCapacity;
    public static int portableDislocatorEnergyConsume;
    public static int portableDislocatorXpConsume;
    public static boolean portableDislocatorPrioritizeEnergy;
    public static boolean portableDislocatorPrioritizeXp;
    public static String externalScriptsPath;
    public static java.util.List<String> stickyFluids;
    public static int electricTreetapEnergyConsume;
    public static int electricTreetapEnergyBuffer;
    public static int rubberSapExtractorEnergyConsume;
    public static int rubberSapExtractorEnergyBuffer;
    public static int rubberSapExtractorSpeed;
    public static java.util.List<String> crudeOils;
    public static int scannerScanRange;
    public static int scannerScanDuration;
    public static int scannerMaxBlocks;
    public static int scannerEnergyConsume;
    public static int scannerEnergyBuffer;
    public static int weatherAltererEnergyBuffer;
    public static int weatherAltererEnergyConsume;
    public static int timeAltererEnergyBuffer;
    public static int timeAltererEnergyConsume;
    public static boolean artifactsInfo;
    public static int scannerMarkerTTL;
    public static java.util.List<String> scannerOreEntries;
    public static java.util.List<String> scannerMobEntries;
    public static int scannerDefaultAlpha;
    public static int scannerDefaultOreColor;
    public static int scannerDefaultMobColor;
    public static java.util.List<String> scannerOreTags;
    public static int structurePlacerMachineEnergyConsume;
    public static int structurePlacerMachineEnergyBuffer;
    public static String clientStructurePath;
    public static boolean acceptClientStructure;
    public static boolean allowClientStructurePlayerLike;
    public static boolean burningBrazierSuperHot;
    public static double greedyShieldBlockChance;
    public static double greedyShieldReduceChance;
    public static double greedyShieldReduceAmount;
    public static boolean greedyShieldInfo;
    public static boolean burningFlameSuperHot;
    public static boolean giftPlaceHardIce;
    public static java.util.List<String> deepDrawersAllowedTags;
    public static java.util.List<String> deepDrawersBlacklist;
    public static int deepDrawersSlotCount;
    public static boolean deepDrawersGuiEnabled;
    public static int deepDrawerExtractorInterval;
    
    public static java.util.List<String> dollyWhitelist;
    public static java.util.List<String> dollyBlacklist;
    public static java.util.List<String> dollyAllowedMiningLevelTags;
    public static java.util.List<String> hardDollyWhitelist;
    public static java.util.List<String> hardDollyBlacklist;
    public static java.util.List<String> hardDollyAllowedMiningLevelTags;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        slowVectorSpeed = SLOW_VECTOR_SPEED.get();
        moderateVectorSpeed = MODERATE_VECTOR_SPEED.get();
        fastVectorSpeed = FAST_VECTOR_SPEED.get();
        extremeVectorSpeed = EXTREME_VECTOR_SPEED.get();
        ultraVectorSpeed = ULTRA_VECTOR_SPEED.get();
        verticalBoostFactor = VERTICAL_BOOST_FACTOR.get();
        entityVerticalBoostFactor = ENTITY_VERTICAL_BOOST_FACTOR.get();
        verticalConveyorEnabled = VERTICAL_CONVEYOR_ENABLED.get();
        verticalCharmEnabled = VERTICAL_CHARM_ENABLED.get();
        horizontalCharmEnabled = HORIZONTAL_CHARM_ENABLED.get();
        vectorCharmEnergyCapacity = VECTOR_CHARM_ENERGY_CAPACITY.get();
        vectorCharmEnergyConsume = new java.util.ArrayList<>(VECTOR_CHARM_ENERGY_CONSUME.get());
        hellfireIgniterConsume = HELLFIRE_IGNITER_CONSUME.get();
        hellfireIgniterBuffer = HELLFIRE_IGNITER_BUFFER.get();
        portableDislocatorEnergyCapacity = PORTABLE_DISLOCATOR_ENERGY_CAPACITY.get();
        portableDislocatorEnergyConsume = PORTABLE_DISLOCATOR_ENERGY_CONSUME.get();
        portableDislocatorXpConsume = PORTABLE_DISLOCATOR_XP_CONSUME.get();
        portableDislocatorPrioritizeEnergy = PORTABLE_DISLOCATOR_PRIORITIZE_ENERGY.get();
        portableDislocatorPrioritizeXp = PORTABLE_DISLOCATOR_PRIORITIZE_XP.get();
        externalScriptsPath = EXTERNAL_SCRIPTS_PATH.get();
        stickyFluids = new java.util.ArrayList<>(sticky_fluids.get());
        crudeOils = new java.util.ArrayList<>(crude_oils.get());
        // Electric Treetap logic
        electricTreetapEnergyConsume = ELECTRIC_TREETAP_ENERGY_CONSUME.get();
        electricTreetapEnergyBuffer = ELECTRIC_TREETAP_ENERGY_BUFFER.get();
        weatherAltererEnergyBuffer = WEATHER_ALTERER_ENERGY_BUFFER.get();
        weatherAltererEnergyConsume = WEATHER_ALTERER_ENERGY_CONSUME.get();
        timeAltererEnergyBuffer = TIME_ALTERER_ENERGY_BUFFER.get();
        timeAltererEnergyConsume = TIME_ALTERER_ENERGY_CONSUME.get();
        artifactsInfo = ARTIFACTS_INFO.get();
        scannerMarkerTTL = SCANNER_MARKER_TTL.get();
        scannerOreEntries = new java.util.ArrayList<>(SCANNER_ORE_ENTRIES.get());
        scannerMobEntries = new java.util.ArrayList<>(SCANNER_MOB_ENTRIES.get());
        scannerOreTags = new java.util.ArrayList<>(SCANNER_ORE_TAGS.get());
                
        // Default values for scanner colors
        scannerDefaultAlpha = Integer.parseInt(SCANNER_DEFAULT_ALPHA.get(), 16);
        scannerDefaultOreColor = Integer.parseInt(SCANNER_DEFAULT_ORE_COLOR.get(), 16);
        scannerDefaultMobColor = Integer.parseInt(SCANNER_DEFAULT_MOB_COLOR.get(), 16);
        // Structure Placer Machine logic
        structurePlacerMachineEnergyConsume = STRUCTURE_PLACER_MACHINE_ENERGY_CONSUME.get();
        structurePlacerMachineEnergyBuffer = STRUCTURE_PLACER_MACHINE_ENERGY_BUFFER.get();

        // Client Structure Path logic
        clientStructurePath = CLIENT_STRUCTURE_PATH.get();
        acceptClientStructure = ACCEPT_CLIENT_STRUCTURE.get();
        allowClientStructurePlayerLike = ALLOW_CLIENT_STRUCTURE_PLAYER_LIKE.get();

        // Evil Things configuration
        burningBrazierSuperHot = BURNING_BRAZIER_SUPER_HOT.get();
        greedyShieldBlockChance = GREEDY_SHIELD_BLOCK_CHANCE.get();
        greedyShieldReduceChance = GREEDY_SHIELD_REDUCE_CHANCE.get();
        greedyShieldReduceAmount = GREEDY_SHIELD_REDUCE_AMOUNT.get();
        greedyShieldInfo = GREEDY_SHIELD_INFO.get();
        burningFlameSuperHot = BURNING_FLAME_SUPER_HOT.get();
        giftPlaceHardIce = GIFT_PLACE_HARD_ICE.get();
        
        // Deep Drawers configuration
        deepDrawersAllowedTags = new java.util.ArrayList<>(DEEP_DRAWERS_ALLOWED_TAGS.get());
        deepDrawersBlacklist = new java.util.ArrayList<>(DEEP_DRAWERS_BLACKLIST.get());
        deepDrawersSlotCount = DEEP_DRAWERS_SLOT_COUNT.get();
        deepDrawersGuiEnabled = DEEP_DRAWERS_GUI_ENABLED.get();
        deepDrawerExtractorInterval = DEEP_DRAWER_EXTRACTOR_INTERVAL.get();
        
        // Dolly configuration
        dollyWhitelist = new java.util.ArrayList<>(DOLLY_WHITELIST.get());
        dollyBlacklist = new java.util.ArrayList<>(DOLLY_BLACKLIST.get());
        dollyAllowedMiningLevelTags = new java.util.ArrayList<>(DOLLY_ALLOWED_MINING_LEVEL_TAGS.get());
        
        // Hard Dolly configuration
        hardDollyWhitelist = new java.util.ArrayList<>(HARD_DOLLY_WHITELIST.get());
        hardDollyBlacklist = new java.util.ArrayList<>(HARD_DOLLY_BLACKLIST.get());
        hardDollyAllowedMiningLevelTags = new java.util.ArrayList<>(HARD_DOLLY_ALLOWED_MINING_LEVEL_TAGS.get());

        // If the energy required is 0, the energy stored is 0 automatically
        if (electricTreetapEnergyConsume <= 0) {
            electricTreetapEnergyBuffer = 0;
        }
        
        // If the energy stored is 0, the energy consumption is disabled
        if (electricTreetapEnergyBuffer <= 0) {
            electricTreetapEnergyConsume = 0;
        }

        // If the energy buffer is less than the energy consume, set the energy consume to the energy buffer
        if(electricTreetapEnergyBuffer < electricTreetapEnergyConsume) {
            electricTreetapEnergyConsume = electricTreetapEnergyBuffer;
        }
        
        // Rubber Sap Extractor logic
        rubberSapExtractorEnergyConsume = RUBBER_SAP_EXTRACTOR_ENERGY_CONSUME.get();
        rubberSapExtractorEnergyBuffer = RUBBER_SAP_EXTRACTOR_ENERGY_BUFFER.get();
        
        // If the energy required is 0, the energy stored is 0 automatically
        if (rubberSapExtractorEnergyConsume <= 0) {
            rubberSapExtractorEnergyBuffer = 0;
        }
        
        // If the energy stored is 0, the energy consumption is disabled
        if (rubberSapExtractorEnergyBuffer <= 0) {
            rubberSapExtractorEnergyConsume = 0;
        }

        // If the energy buffer is less than the energy consume, set the energy consume to the energy buffer
        if(rubberSapExtractorEnergyBuffer < rubberSapExtractorEnergyConsume) {
            rubberSapExtractorEnergyConsume = rubberSapExtractorEnergyBuffer;
        }

        
        rubberSapExtractorSpeed = RUBBER_SAP_EXTRACTOR_SPEED.get();
        
        scannerScanRange = SCANNER_SCAN_RANGE.get();
        scannerScanDuration = SCANNER_SCAN_DURATION.get();
        scannerMaxBlocks = SCANNER_MAX_BLOCKS.get();
        scannerEnergyConsume = SCANNER_ENERGY_CONSUME.get();
        scannerEnergyBuffer = SCANNER_ENERGY_BUFFER.get();
        
        // Scanner energy logic
        if (scannerEnergyConsume <= 0) {
            scannerEnergyBuffer = 0;
        }
        
        if (scannerEnergyBuffer <= 0) {
            scannerEnergyConsume = 0;
        }
        
        // If the energy buffer is less than the energy consume, set the energy consume to the energy buffer
        if(scannerEnergyBuffer < scannerEnergyConsume) {
            scannerEnergyConsume = scannerEnergyBuffer;
        }

        // Weather Alterer logic
        weatherAltererEnergyBuffer = WEATHER_ALTERER_ENERGY_BUFFER.get();
        weatherAltererEnergyConsume = WEATHER_ALTERER_ENERGY_CONSUME.get();

        // If the energy required is 0, the energy stored is 0 automatically
        if (weatherAltererEnergyConsume <= 0) {
            weatherAltererEnergyBuffer = 0;
        }
        
        // If the energy stored is 0, the energy consumption is disabled
        if (weatherAltererEnergyBuffer <= 0) {
            weatherAltererEnergyConsume = 0;
        }
        
        // If the energy buffer is less than the energy consume, set the energy consume to the energy buffer
        if(weatherAltererEnergyBuffer < weatherAltererEnergyConsume) {
            weatherAltererEnergyConsume = weatherAltererEnergyBuffer;
        }
        
        // Time Alterer logic
        timeAltererEnergyBuffer = TIME_ALTERER_ENERGY_BUFFER.get();
        timeAltererEnergyConsume = TIME_ALTERER_ENERGY_CONSUME.get();

        // If the energy required is 0, the energy stored is 0 automatically
        if (timeAltererEnergyConsume <= 0) {
            timeAltererEnergyBuffer = 0;
        }
        
        // If the energy stored is 0, the energy consumption is disabled
        if (timeAltererEnergyBuffer <= 0) {
            timeAltererEnergyConsume = 0;
        }

        // If the energy buffer is less than the energy consume, set the energy consume to the energy buffer
        if(timeAltererEnergyBuffer < timeAltererEnergyConsume) {
            timeAltererEnergyConsume = timeAltererEnergyBuffer;
        }
        
        // Structure Placer Machine logic
        if(structurePlacerMachineEnergyConsume <= 0) {
            structurePlacerMachineEnergyBuffer = 0;
        }
        
        if(structurePlacerMachineEnergyBuffer <= 0) {
            structurePlacerMachineEnergyConsume = 0;    
        }

        // If the energy buffer is less than the energy consume, set the energy consume to the energy buffer
        if(structurePlacerMachineEnergyBuffer < structurePlacerMachineEnergyConsume) {
            structurePlacerMachineEnergyConsume = structurePlacerMachineEnergyBuffer;
        }
        
    }
    
    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        onLoad(event);
    }
}
