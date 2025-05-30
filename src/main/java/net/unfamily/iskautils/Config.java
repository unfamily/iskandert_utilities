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
            
    // Portable Dislocator resource priority configuration
    private static final ModConfigSpec.BooleanValue PORTABLE_DISLOCATOR_PRIORITIZE_ENERGY = BUILDER
            .comment("If true, energy will be consumed before XP when both are available",
                    "If both this and prioritizeXp are true, both resources will be consumed")
            .define("102_portableDislocatorPrioritizeEnergy", true);
            
    private static final ModConfigSpec.BooleanValue PORTABLE_DISLOCATOR_PRIORITIZE_XP = BUILDER
            .comment("If true, XP will be consumed before energy when both are available",
                    "If both this and prioritizeEnergy are true, both resources will be consumed")
            .define("103_portableDislocatorPrioritizeXp", false);

    static {
        BUILDER.pop(); // End of general_utilities category
        
        // Category for Development/Advanced Configuration
        BUILDER.comment("Development and Advanced Configuration").push("dev");
    }

    // Dynamic Potion Plates configuration
    private static final ModConfigSpec.ConfigValue<String> EXTERNAL_SCRIPTS_PATH = BUILDER
            .comment("Path to the external scripts directory for custom potion plates and stage items",
                    "Default: 'kubejs/external_scripts'",
                    "The system will look for any scripts of this mod: <path>/potion_plates/")
            .define("000_external_scripts_path", "kubejs/external_scripts");

    static {
        BUILDER.pop(); // End of dev category
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
    }
}
