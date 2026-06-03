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

    private static final ModConfigSpec.BooleanValue HELLFIRE_IGNITER_VANILLA_LIKE = BUILDER
            .comment("If true, Hellfire Igniter uses vanilla-like behavior (always PULSE mode, no mode switching)",
                    "When enabled, shift+click will always set to PULSE mode and no mode change message is shown",
                    "Also disables inverted placement (Shift when placing the block has no effect on direction)")
            .define("002_hellfireIgniterVanillaLike", false);

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
            .defineInRange("210_weatherAltererEnergyBuffer", 0, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue WEATHER_ALTERER_ENERGY_CONSUME = BUILDER
            .comment("Energy consumed per tick by the Weather Alterer in RF/FE")
            .comment("Recommended value: 5000, but set to 0 to disable energy consumption")
            .defineInRange("211_weatherAltererEnergyConsume", 0, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue TIME_ALTERER_ENERGY_BUFFER = BUILDER
            .comment("Energy capacity of the Time Alterer in RF/FE")
            .comment("Recommended value: 100000, but set to 0 to disable energy consumption")
            .defineInRange("220_timeAltererEnergyBuffer", 0, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue TIME_ALTERER_ENERGY_CONSUME = BUILDER
            .comment("Energy consumed per tick by the Time Alterer in RF/FE")
            .comment("Recommended value: 5000, but set to 0 to disable energy consumption")
            .defineInRange("221_timeAltererEnergyConsume", 0, 0, Integer.MAX_VALUE);
            
    // Temporal Overclocker configuration
    private static final ModConfigSpec.IntValue TEMPORAL_OVERCLOCKER_ENERGY_BUFFER = BUILDER
            .comment("Energy capacity of the Temporal Overclocker in RF/FE")
            .comment("Recommended value: 5000000, but set to 0 to disable energy consumption")
            .defineInRange("230_temporalOverclockerEnergyBuffer", 5000000, 0, Integer.MAX_VALUE);
    
    private static final ModConfigSpec.IntValue TEMPORAL_OVERCLOCKER_ENERGY_PER_ACCELERATION = BUILDER
            .comment("Energy consumed per acceleration degree per linked block per tick by the Temporal Overclocker in RF/FE")
            .comment("Formula: consumption = energyPerAcceleration * accelerationFactor * linkedBlocks")
            .comment("Example: with energyPerAcceleration=250, accelerationFactor=2, 1 block = 500 RF/tick")
            .comment("Example: with energyPerAcceleration=250, accelerationFactor=20, 1 block = 5000 RF/tick")
            .comment("Example: with energyPerAcceleration=250, accelerationFactor=40, 1 block = 10000 RF/tick")
            .defineInRange("231_temporalOverclockerEnergyPerAcceleration", 250, 0, Integer.MAX_VALUE);
    
    private static final ModConfigSpec.IntValue TEMPORAL_OVERCLOCKER_MAX_LINKS = BUILDER
            .comment("Maximum number of blocks that can be linked to a Temporal Overclocker")
            .defineInRange("232_temporalOverclockerMaxLinks", 5, 1, Integer.MAX_VALUE);
    
    private static final ModConfigSpec.IntValue TEMPORAL_OVERCLOCKER_ACCELERATION_FACTOR_MIN = BUILDER
            .comment("Minimum acceleration factor for Temporal Overclocker")
            .comment("Default: 2, range: 2 to Integer.MAX_VALUE")
            .defineInRange("233_temporalOverclockerAccelerationFactorMin", 2, 2, Integer.MAX_VALUE);
    
    private static final ModConfigSpec.IntValue TEMPORAL_OVERCLOCKER_ACCELERATION_FACTOR_MAX = BUILDER
            .comment("Maximum acceleration factor for Temporal Overclocker")
            .comment("Default: 40, range: 2 to Integer.MAX_VALUE")
            .defineInRange("234_temporalOverclockerAccelerationFactorMax", 40, 2, Integer.MAX_VALUE);
    
    private static final ModConfigSpec.IntValue TEMPORAL_OVERCLOCKER_ACCELERATION_FACTOR = BUILDER
            .comment("Default acceleration factor for Temporal Overclocker")
            .comment("Default: 20, range: 2 to Integer.MAX_VALUE")
            .defineInRange("235_temporalOverclockerAccelerationFactor", 20, 2, Integer.MAX_VALUE);
    
    private static final ModConfigSpec.IntValue TEMPORAL_OVERCLOCKER_LINK_RANGE = BUILDER
            .comment("Maximum distance in blocks for linking blocks to the Temporal Overclocker")
            .comment("Blocks beyond this range cannot be linked")
            .defineInRange("236_temporalOverclockerLinkRange", 16, 1, 256);

    private static final ModConfigSpec.DoubleValue ENTROPIC_CLOCK_MAX_FACTOR_MULTIPLIER = BUILDER
            .comment("Max acceleration factor multiplier when an Entropic Clock upgrade is installed")
            .defineInRange("237_entropicClockMaxFactorMultiplier", 2.0D, 1.0D, 10.0D);

    private static final ModConfigSpec.IntValue ENTROPIC_CLOCK_ENTROPY_PER_TICK = BUILDER
            .comment("Entropy consumed per acceleration tick while the Entropic Clock upgrade is active")
            .defineInRange("238_entropicClockEntropyPerTick", 10, 0, 100000);

    private static final ModConfigSpec.IntValue ENTROPIC_CLOCK_MAX_STORED = BUILDER
            .comment("Maximum stored entropy buffer for the Entropic Clock upgrade (0 uses Ancient Table max)")
            .defineInRange("239_entropicClockMaxStored", 0, 0, 1000000);
            
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

    private static final ModConfigSpec.IntValue FACTORY_ENERGY_BUFFER = BUILDER
            .comment("Energy capacity of the Factory in RF/FE (0 disables energy storage and consumption)",
                    "Per-operation cost is set per recipe in data (energy_per_operation); default 1 RF when omitted.")
            .defineInRange("303_factoryEnergyBuffer", 10000, 0, Integer.MAX_VALUE);

    static {
        BUILDER.comment("Entropy internal charges (Drop of Entropy)").push("entropy");
    }

    private static final ModConfigSpec.IntValue ENTROPY_CHARGE_PER_DROP = BUILDER
            .comment("Internal entropy charges per Drop of Entropy (Ancient Table and Arcane Dictionary)")
            .defineInRange("000_charge_per_drop", 1000, 1, Integer.MAX_VALUE);

    static {
        BUILDER.comment("Ancient Table").push("ancient_table");
    }

    private static final ModConfigSpec.IntValue ENTROPY_ANCIENT_TABLE_MAX_STORED = BUILDER
            .comment("Maximum internal entropy stored by the Ancient Table fuel buffer")
            .defineInRange("100_max_stored", 10000, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue ANCIENT_TABLE_CRAFT_TICKS = BUILDER
            .comment("Ticks per Ancient Table craft operation")
            .defineInRange("101_craft_ticks", 10, 1, 72000);

    private static final ModConfigSpec.IntValue ANCIENT_TABLE_IO_SLOTS = BUILDER
            .comment("Logical input and output slot count per side on the Ancient Table")
            .defineInRange("102_io_slots", 63, 9, 63);

    static {
        BUILDER.pop();
        BUILDER.comment("Arcane Dictionary — 100–199: item/reroll; 200+: per-trait tuning").push("arcane_dictionary");
    }

    private static final ModConfigSpec.IntValue ARCANE_DICTIONARY_MAX_STORED = BUILDER
            .comment("Maximum internal entropy stored on an Arcane Dictionary stack (default one order of magnitude below Ancient Table)")
            .defineInRange("100_max_stored", 500000, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue ARCANE_DICTIONARY_MIN_TRAITS = BUILDER
            .defineInRange("101_min_traits", 1, 1, 5);

    private static final ModConfigSpec.IntValue ARCANE_DICTIONARY_MAX_TRAITS = BUILDER
            .defineInRange("102_max_traits", 5, 1, 5);

    private static final ModConfigSpec.IntValue ARCANE_DICTIONARY_MIN_LEVEL = BUILDER
            .defineInRange("103_min_level", 1, 1, 5);

    private static final ModConfigSpec.IntValue ARCANE_DICTIONARY_MAX_LEVEL = BUILDER
            .defineInRange("104_max_level", 5, 1, 5);

    private static final ModConfigSpec.IntValue ARCANE_DICTIONARY_MAX_ROLL_LEVELS = BUILDER
            .comment("Maximum player levels worth of XP consumed per reroll (vanilla curve; excess XP is kept)")
            .defineInRange("105_max_roll_levels", 100, 1, 1000);

    private static final ModConfigSpec.DoubleValue ARCANE_DICTIONARY_ROLL_COUNT_EXPONENT = BUILDER
            .comment("Trait count ceiling curve: lower exponent fills max traits faster (default 0.4)")
            .defineInRange("106_roll_count_exponent", 0.4D, 0.05D, 5.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_DICTIONARY_ROLL_LEVEL_EXPONENT = BUILDER
            .comment("Trait level ceiling curve: higher exponent makes high levels harder (default 1.8)")
            .defineInRange("107_roll_level_exponent", 1.8D, 0.05D, 10.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_GLASS_SKIN_REFLECT_MULT = BUILDER
            .comment("Glass Skin: magic damage dealt back to attacker per level")
            .defineInRange("200_glass_skin_reflect_mult_per_level", 2.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_GLASS_SKIN_SELF_DAMAGE = BUILDER
            .comment("Glass Skin: self damage per level when hit")
            .defineInRange("201_glass_skin_self_damage_per_level", 1.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_STONE_SKIN_ARMOR = BUILDER
            .comment("Stone Skin: flat armor bonus per level")
            .defineInRange("202_stone_skin_armor_per_level", 1.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_ENTROPY_SHELL_TOUGHNESS = BUILDER
            .comment("Entropy Shell: armor toughness bonus per level")
            .defineInRange("203_entropy_shell_toughness_per_level", 1.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_ENTROPY_SHELL_HP_PENALTY = BUILDER
            .comment("Entropy Shell: flat max health penalty (stored positive, applied as negative; not per trait level)")
            .defineInRange("204_entropy_shell_hp_penalty", 2.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_AGILITY_SPEED_MULT = BUILDER
            .comment("Agility: movement speed multiplier added per level (0.05 = +5% per level)")
            .defineInRange("205_agility_speed_mult_per_level", 0.05D, 0.0D, 5.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_BLOOD_LEDGER_LOW_HP_RATIO = BUILDER
            .comment("Blood Ledger: max health ratio below which Regeneration is applied (0.30 = 30%)")
            .defineInRange("206_blood_ledger_low_hp_ratio", 0.30D, 0.01D, 1.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_EXECUTION_LINE_HP_THRESHOLD = BUILDER
            .comment("Execution Line: target max HP ratio threshold for bonus damage (0.20 = 20%)")
            .defineInRange("207_execution_line_hp_threshold", 0.20D, 0.01D, 1.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_EXECUTION_LINE_BONUS_MULT = BUILDER
            .comment("Execution Line: bonus damage fraction of hit damage per level (0.05 = +5% per level)")
            .defineInRange("208_execution_line_bonus_damage_mult", 0.05D, 0.0D, 5.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_ENTROPY_BLADE_BYPASS = BUILDER
            .comment("Entropy Blade: armor bypass fraction per level on your hits")
            .defineInRange("209_entropy_blade_armor_bypass_per_level", 0.05D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_MARTYR_SCRIPT_DEALT = BUILDER
            .comment("Martyr Script: extra dealt damage multiplier per level (0.15 = +15% per level)")
            .defineInRange("210_martyr_script_dealt_mult_per_level", 0.15D, 0.0D, 5.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_MARTYR_SCRIPT_TAKEN = BUILDER
            .comment("Martyr Script: extra taken damage multiplier per level (0.10 = +10% per level)")
            .defineInRange("211_martyr_script_taken_mult_per_level", 0.10D, 0.0D, 5.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_RECALL_XP_MULT = BUILDER
            .comment("Recall of Knowledge: bonus XP multiplier per level from orbs (0.05 = +5% per level)")
            .defineInRange("212_recall_of_knowledge_xp_mult_per_level", 0.05D, 0.0D, 5.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_PHASE_MISMATCH_DODGE = BUILDER
            .comment("Phase Mismatch: chance per level to negate incoming hit (0.05 = 5% per level)")
            .defineInRange("213_phase_mismatch_dodge_chance_per_level", 0.05D, 0.0D, 1.0D);

    private static final ModConfigSpec.IntValue ARCANE_SHIFTING_POWER_MIMIC_SECONDS = BUILDER
            .comment("Shifting Power: seconds each mimicked trait lasts before rotating")
            .defineInRange("214_shifting_power_mimic_duration_seconds", 30, 1, 600);

    private static final ModConfigSpec.DoubleValue ARCANE_ENTROPY_OVERFLOW_CONSUME_REDUCTION = BUILDER
            .comment("Entropy Overflow: entropy consume reduction per level on other traits (0.10 = 10% per level)")
            .defineInRange("215_entropy_overflow_consume_reduction_per_level", 0.10D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_ENTROPY_OVERFLOW_HP_PENALTY = BUILDER
            .comment("Entropy Overflow: max health penalty per level (stored positive, applied as negative)")
            .defineInRange("216_entropy_overflow_hp_penalty_per_level", 1.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_VOID_THORNS_DAMAGE = BUILDER
            .comment("Void Thorns: magic damage dealt to attacker per level when you are hit")
            .defineInRange("217_void_thorns_damage_per_level", 1.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_LIFE_SIPHON_HEAL_FRACTION = BUILDER
            .comment("Life Siphon: fraction of dealt damage healed per level (0.05 = 5% per level)")
            .defineInRange("218_life_siphon_heal_fraction_per_level", 0.05D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_LIFE_SIPHON_HEAL_CAP = BUILDER
            .comment("Life Siphon: maximum HP healed per hit")
            .defineInRange("219_life_siphon_heal_cap", 4.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_IRON_ROOT_KNOCKBACK_RESIST = BUILDER
            .comment("Iron Root: knockback resistance added per level (0.10 = +10% per level)")
            .defineInRange("220_iron_root_knockback_resist_per_level", 0.10D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_QUICK_HANDS_ATTACK_SPEED = BUILDER
            .comment("Quick Hands: attack speed multiplier added per level (0.05 = +5% per level)")
            .defineInRange("221_quick_hands_attack_speed_per_level", 0.05D, 0.0D, 5.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_ENTROPY_FUNNEL_BONUS_CHARGES = BUILDER
            .comment("Entropy Funnel: extra entropy charges absorbed per drop per level (beyond the base drop charge)")
            .defineInRange("222_entropy_funnel_bonus_charges_per_level", 1.0D, 0.0D, 1000.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_LAST_STAND_HP_RATIO = BUILDER
            .comment("Last Stand: max HP ratio at or below which Resistance is applied (0.30 = 30%)")
            .defineInRange("223_last_stand_hp_ratio", 0.30D, 0.01D, 1.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_GRAVE_DEBT_HIGH_HP_RATIO = BUILDER
            .comment("Grave Debt: max HP ratio above which Slowness is applied (0.70 = 70%)")
            .defineInRange("224_grave_debt_high_hp_ratio", 0.70D, 0.01D, 1.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_GRAVE_DEBT_LOW_HP_RATIO = BUILDER
            .comment("Grave Debt: max HP ratio below which bonus movement speed is applied (0.30 = 30%)")
            .defineInRange("225_grave_debt_low_hp_ratio", 0.30D, 0.01D, 1.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_GRAVE_DEBT_SPEED_MULT = BUILDER
            .comment("Grave Debt: movement speed multiplier added per level when below low HP ratio")
            .defineInRange("226_grave_debt_speed_mult_per_level", 0.03D, 0.0D, 5.0D);

    private static final ModConfigSpec.DoubleValue ARCANE_TIER_RESONANCE_CONSUME_REDUCTION = BUILDER
            .comment("Tier Resonance (Apotheosis): entropy consume reduction per world tier step per level (0.02 = 2% per tier per level)")
            .defineInRange("227_tier_resonance_consume_reduction_per_tier_per_level", 0.02D, 0.0D, 1.0D);

    static {
        BUILDER.pop(); // arcane_dictionary
        BUILDER.comment("Entropic Gear").push("entropic_gear");
    }

    private static final ModConfigSpec.DoubleValue ENTROPIC_HELMET_BASE_HP = BUILDER
            .comment("Base max health bonus while wearing entropic helmet.")
            .defineInRange("000_helmet_base_hp", 4.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ENTROPIC_HELMET_HP_PER_PIECE = BUILDER
            .comment("Extra max HP per other entropic armor piece worn (excludes helmet itself).")
            .defineInRange("001_helmet_hp_per_entropic_piece", 2.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ENTROPIC_CHEST_MISSING_HP_PER_STEP = BUILDER
            .comment("Missing health points required per toughness bonus step (e.g. 50 missing HP / 7 -> 7 steps).")
            .defineInRange("010_chestplate_missing_hp_per_step", 7.0D, 1.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ENTROPIC_CHEST_TOUGHNESS_BONUS_PER_STEP = BUILDER
            .comment("Armor toughness added per step while wearing entropic chestplate.")
            .defineInRange("011_chestplate_toughness_bonus_per_step", 1.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ENTROPIC_LEGGINGS_MISSING_HP_PER_STEP = BUILDER
            .comment("Missing health points required per armor bonus step (e.g. 50 missing HP / 3 -> 16 steps).")
            .defineInRange("020_leggings_missing_hp_per_step", 3.0D, 1.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ENTROPIC_LEGGINGS_ARMOR_BONUS_PER_STEP = BUILDER
            .comment("Armor added per step while wearing entropic leggings.")
            .defineInRange("021_leggings_armor_bonus_per_step", 1.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.BooleanValue ENTROPIC_BOOTS_NEGATE_FALL = BUILDER
            .comment("Negate fall damage while wearing entropic boots.")
            .define("030_boots_negate_fall_damage", true);

    private static final ModConfigSpec.IntValue ENTROPIC_PICKAXE_BONUS_FORTUNE_LEVELS = BUILDER
            .comment("Extra Fortune levels on block drops for entropic pickaxe and paxel (added on top of Fortune enchant). Disabled while Silk Touch is on the tool. Set 0 to turn off.")
            .defineInRange("040_pickaxe_bonus_fortune_levels", 1, 0, 10);

    private static final ModConfigSpec.DoubleValue ENTROPIC_ARMOR_PEN_CHANCE = BUILDER
            .comment("Chance for entropic sword/axe/spear to partially ignore armor and toughness.")
            .defineInRange("050_armor_pen_chance", 0.3D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue ENTROPIC_ARMOR_PEN_IGNORE = BUILDER
            .comment("Fraction of armor and toughness ignored on proc (0.8 = 80%). Power off if chance or this is 0.")
            .defineInRange("051_armor_pen_ignore_fraction", 0.8D, 0.0D, 1.0D);

    private static final ModConfigSpec.BooleanValue ENTROPIC_HOE_CROP_ENABLED = BUILDER
            .comment("Enable crop reset power on entropic hoe.")
            .define("060_hoe_crop_power_enabled", true);

    private static final ModConfigSpec.BooleanValue ENTROPIC_HOE_REQUIRE_MATURE = BUILDER
            .comment("If false, entropic hoe resets immature crops to age 0 (works alongside harvest mods). If true, only mature crops.")
            .define("061_hoe_require_mature_crop", false);

    private static final ModConfigSpec.BooleanValue ENTROPIC_AXE_STRIP_ENABLED = BUILDER
            .comment("Enable log stripping on entropic axe and paxel (off-hand strip, sneak + inventory strip).")
            .define("070_axe_strip_enabled", true);

    private static final ModConfigSpec.BooleanValue ENTROPIC_SHOVEL_BRUSH_ENABLED = BUILDER
            .comment("Enable instant brush on entropic shovel and paxel.")
            .define("071_shovel_brush_enabled", true);

    private static final ModConfigSpec.IntValue ENTROPIC_SHOVEL_BRUSH_TICKS = BUILDER
            .comment("Brush simulation ticks per use (higher = faster completion on tough blocks).")
            .defineInRange("072_shovel_brush_ticks", 12, 1, 100);

    static {
        BUILDER.pop(); // entropic_gear
        BUILDER.comment("Unstable Entropy Catalyst").push("unstable_entropy_catalyst");
    }

    private static final ModConfigSpec.BooleanValue UNSTABLE_ENTROPY_CATALYST_DECAY_KILLS_PLAYER = BUILDER
            .comment("If true, the player dies when an unstable_entropy_catalyst fully decays in their inventory.")
            .define("200_unstable_entropy_catalyst_decay_kills_player", false);

    private static final ModConfigSpec.IntValue UNSTABLE_ENTROPY_CATALYST_DECAY_TICKS = BUILDER
            .comment("Ticks until unstable_entropy_catalyst decays in player inventory (0 = disabled). Default 600 = 30 seconds.")
            .defineInRange("110_unstable_entropy_catalyst_decay_ticks", 600, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.ConfigValue<String> UNSTABLE_ENTROPY_CATALYST_DECAY_TINTS = BUILDER
            .comment("Color ramp used by unstable_entropy_catalyst as it decays (semicolon-separated).",
                    "Format: #RRGGBB;#RRGGBB;... (spaces allowed).",
                    "Default: pink -> red.")
            .define("111_unstable_entropy_catalyst_decay_tints", "#FFFFFF;#FF77AA;#FF3355;#FF0000");

    static {
        BUILDER.pop(); // unstable_entropy_catalyst
        BUILDER.pop(); // entropy
    }

    private static final ModConfigSpec.IntValue SOUND_MUFFLER_RANGE_MAX = BUILDER
            .comment("Maximum range (blocks) for Sound Muffler effect. Minimum is always 8. Allowed values in GUI: 8, 16, 32, and up to this max (default 500).")
            .defineInRange("302_soundMufflerRangeMax", 16, 8, 1024);

    // Deep Drawers Configuration (in general_utilities category)
    // Renamed from 400_deep_drawers_allowed_tags / 401_deep_drawers_blacklist in 3.4.0.0.0
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DEEP_DRAWERS_ALLOW = BUILDER
            .comment("Extra allow overrides for Deep Drawers (bypass adaptive rules).",
                    "Required for stackable items (maxStackSize > 1); non-stackable items are accepted automatically.",
                    "Tags starting with # are item tags, items without # are item IDs (e.g. apotheosis:gem, minecraft:potion)")
            .defineList("400_deep_drawers_allow",
                    java.util.Arrays.asList("apotheosis:gem", "minecraft:potion", "minecraft:enchanted_book", "irons_spellbooks:scroll"),
                    obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DEEP_DRAWERS_DENY = BUILDER
            .comment("Extra deny overrides for Deep Drawers (always blocked, wins over allow and adaptive).",
                    "Tags starting with # are item tags, items without # are item IDs",
                    "Default includes minecraft:book for safety reasons")
            .defineList("401_deep_drawers_deny",
                    java.util.Arrays.asList("minecraft:book", "#c:skulls", "#minecraft:skulls"),
                    obj -> obj instanceof String);

    // Deep Drawer Extractor Configuration (starts at 410)
    private static final ModConfigSpec.IntValue DEEP_DRAWER_EXTRACTOR_INTERVAL = BUILDER
            .comment("Extraction interval in ticks for the Deep Drawer Extractor (lower is faster)",
                    "Default: 1 (extracts every tick, maximum speed)",
                    "1 tick = 0.05 seconds")
            .defineInRange("410_deep_drawer_extractor_interval", 1, 1, Integer.MAX_VALUE);
    
    private static final ModConfigSpec.IntValue DEEP_DRAWER_EXTRACTOR_MAX_FILTERS = BUILDER
            .comment("Maximum number of filter fields in the Deep Drawer Extractor GUI",
                    "Default: 50",
                    "All EditBoxes are always created, even if empty")
            .defineInRange("411_deep_drawer_extractor_max_filters", 50, 1, Integer.MAX_VALUE);
    
    // Category for Dolly Configuration (under general_utilities)
    static {
        BUILDER.comment("Dolly Configuration").push("dolly");
    }
    
    // Dolly Configuration
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DOLLY_WHITELIST = BUILDER
            .comment("List of block tags/IDs that can be picked up by the Dolly",
                    "Tags starting with # are block tags (e.g. #minecraft:mineable/pickaxe)",
                    "Blocks without # are block IDs (e.g. minecraft:chest, minecraft:furnace)",
                    "Empty list means all blocks are allowed (subject to blacklist and hardness/tool requirements)",
                    "If whitelist is not empty, only blocks matching these tags/IDs will be allowed")
            .defineList("000_dolly_whitelist", 
                       java.util.Collections.emptyList(), 
                       obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DOLLY_BLACKLIST = BUILDER
            .comment("List of block tags/IDs that are explicitly forbidden from being picked up by the Dolly",
                    "This blacklist ALWAYS takes priority over the whitelist",
                    "Tags starting with # are block tags, blocks without # are block IDs",
                    "Examples: minecraft:bedrock, minecraft:end_portal_frame, #c:ores")
            .defineList("001_dolly_blacklist",
                    java.util.Arrays.asList("minecraft:spawner", "minecraft:trial_spawner"),
                    obj -> obj instanceof String);
    
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DOLLY_ALLOWED_MINING_LEVEL_TAGS = BUILDER
            .comment("List of block tags that represent allowed mining levels for the Dolly",
                    "Only blocks matching these mining level tags can be picked up",
                    "Default includes wooden, stone, and iron tool levels",
                    "Tags must start with #",
                    "Empty list means no mining level check (only hardness < 0 blocks are rejected)",
                    "Examples: #minecraft:mineable/pickaxe, #minecraft:needs_stone_tool, #minecraft:needs_iron_tool")
            .defineList("002_dolly_allowed_mining_level_tags",
                    java.util.Arrays.asList(
                            "#minecraft:mineable/axe",
                            "#minecraft:mineable/pickaxe",
                            "#minecraft:mineable/shovel",
                            "#minecraft:mineable/hoe",
                            "#minecraft:needs_stone_tool",
                            "#minecraft:needs_iron_tool"
                    ),
                    obj -> obj instanceof String);
    
    private static final ModConfigSpec.BooleanValue DOLLY_CAN_MOVE_ALL_UNBREAKABLE = BUILDER
            .comment("If true, allows the Dolly to move all unbreakable blocks (hardness < 0)",
                    "When enabled, unbreakable blocks are checked against unbreakable whitelist/blacklist",
                    "Default: false")
            .define("003_dolly_can_move_all_unbreakable", false);
    
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DOLLY_UNBREAKABLE_WHITELIST = BUILDER
            .comment("List of unbreakable block IDs that can be picked up by the Dolly",
                    "Only used when can_move_all_unbreakable is true",
                    "Blocks without # are block IDs (e.g. iska_utils:hard_ice)",
                    "Empty list means no unbreakable blocks allowed (except blacklisted ones are always rejected)",
                    "If whitelist has entries, only those unbreakable blocks allowed")
            .defineList("004_dolly_unbreakable_whitelist",
                    java.util.Arrays.asList("iska_utils:hard_ice"),
                    obj -> obj instanceof String);
    
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DOLLY_UNBREAKABLE_BLACKLIST = BUILDER
            .comment("List of unbreakable block IDs that are explicitly forbidden from being picked up by the Dolly",
                    "Only used when can_move_all_unbreakable is true",
                    "This blacklist ALWAYS takes priority over the unbreakable whitelist",
                    "Blocks without # are block IDs (e.g. minecraft:bedrock, minecraft:end_portal_frame)")
            .defineList("005_dolly_unbreakable_blacklist",
                    java.util.Collections.emptyList(),
                    obj -> obj instanceof String);
    
    // Hard Dolly Configuration
    // Hard Dolly has no limits - can pick up any block except indestructible ones (hardness < 0)
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> HARD_DOLLY_WHITELIST = BUILDER
            .comment("List of block tags/IDs that can be picked up by the Hard Dolly",
                    "Tags starting with # are block tags (e.g. #minecraft:mineable/pickaxe)",
                    "Blocks without # are block IDs (e.g. minecraft:chest, minecraft:furnace)",
                    "Empty list (default) means ALL blocks are allowed (except indestructible ones with hardness < 0)",
                    "If whitelist is not empty, only blocks matching these tags/IDs will be allowed")
            .defineList("010_hard_dolly_whitelist", 
                       java.util.Collections.emptyList(), 
                       obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> HARD_DOLLY_BLACKLIST = BUILDER
            .comment("List of block tags/IDs that are explicitly forbidden from being picked up by the Hard Dolly",
                    "This blacklist ALWAYS takes priority over the whitelist",
                    "Tags starting with # are block tags, blocks without # are block IDs",
                    "Empty list (default) means no blocks are blacklisted",
                    "Examples: minecraft:bedrock, minecraft:end_portal_frame, #c:ores")
            .defineList("011_hard_dolly_blacklist",
                    java.util.Collections.emptyList(),
                    obj -> obj instanceof String);
    
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> HARD_DOLLY_ALLOWED_MINING_LEVEL_TAGS = BUILDER
            .comment("List of block tags that represent allowed mining levels for the Hard Dolly",
                    "Empty list (default) means NO mining level check - all blocks allowed except indestructible ones (hardness < 0)",
                    "Tags must start with #",
                    "If not empty, only blocks matching these mining level tags can be picked up",
                    "Examples: #minecraft:mineable/pickaxe, #minecraft:needs_stone_tool, #minecraft:needs_iron_tool")
            .defineList("012_hard_dolly_allowed_mining_level_tags",
                    java.util.Collections.emptyList(),
                    obj -> obj instanceof String);
    
    private static final ModConfigSpec.BooleanValue HARD_DOLLY_CAN_MOVE_ALL_UNBREAKABLE = BUILDER
            .comment("If true, allows the Hard Dolly to move all unbreakable blocks (hardness < 0)",
                    "When enabled, unbreakable blocks are checked against unbreakable whitelist/blacklist",
                    "Default: false")
            .define("013_hard_dolly_can_move_all_unbreakable", false);
    
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> HARD_DOLLY_UNBREAKABLE_WHITELIST = BUILDER
            .comment("List of unbreakable block IDs that can be picked up by the Hard Dolly",
                    "Only used when can_move_all_unbreakable is true",
                    "Blocks without # are block IDs (e.g. iska_utils:hard_ice)",
                    "Empty list means no unbreakable blocks allowed (except blacklisted ones are always rejected)",
                    "If whitelist has entries, only those unbreakable blocks allowed")
            .defineList("014_hard_dolly_unbreakable_whitelist",
                    java.util.Arrays.asList("iska_utils:hard_ice"),
                    obj -> obj instanceof String);
    
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> HARD_DOLLY_UNBREAKABLE_BLACKLIST = BUILDER
            .comment("List of unbreakable block IDs that are explicitly forbidden from being picked up by the Hard Dolly",
                    "Only used when can_move_all_unbreakable is true",
                    "This blacklist ALWAYS takes priority over the unbreakable whitelist",
                    "Blocks without # are block IDs (e.g. minecraft:bedrock, minecraft:end_portal_frame)")
            .defineList("015_hard_dolly_unbreakable_blacklist",
                    java.util.Collections.emptyList(),
                    obj -> obj instanceof String);
    
    // Creative Dolly Configuration
    // Creative Dolly has infinite durability and can move ANY block including indestructible ones
    private static final ModConfigSpec.BooleanValue CREATIVE_DOLLY_CAN_MOVE_ALL_UNBREAKABLE = BUILDER
            .comment("If true, allows the Creative Dolly to move all unbreakable blocks (hardness < 0)",
                    "When enabled, unbreakable blocks are checked against unbreakable whitelist/blacklist",
                    "Default: true (Creative Dolly can move everything by default)")
            .define("020_creative_dolly_can_move_all_unbreakable", true);
    
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> CREATIVE_DOLLY_UNBREAKABLE_WHITELIST = BUILDER
            .comment("List of unbreakable block IDs that can be picked up by the Creative Dolly",
                    "Only used when can_move_all_unbreakable is true",
                    "Blocks without # are block IDs (e.g. iska_utils:hard_ice)",
                    "Empty list (default) means all unbreakable blocks allowed (except blacklisted ones are always rejected)",
                    "If whitelist has entries, only those unbreakable blocks allowed")
            .defineList("021_creative_dolly_unbreakable_whitelist",
                    java.util.Collections.emptyList(),
                    obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> CREATIVE_DOLLY_UNBREAKABLE_BLACKLIST = BUILDER
            .comment("List of unbreakable block IDs that are explicitly forbidden from being picked up by the Creative Dolly",
                    "Only used when can_move_all_unbreakable is true",
                    "This blacklist ALWAYS takes priority over the unbreakable whitelist",
                    "Blocks without # are block IDs (e.g. minecraft:bedrock, minecraft:end_portal_frame)",
                    "Empty list (default) means no unbreakable blocks are blacklisted")
            .defineList("022_creative_dolly_unbreakable_blacklist",
                    java.util.Collections.emptyList(),
                    obj -> obj instanceof String);
    
    // Dolly Info Lines Configuration
    private static final ModConfigSpec.IntValue DOLLY_INFO_LINES = BUILDER
            .comment("Number of info tooltip lines to display for the Dolly",
                    "Each line uses tooltip.iska_utils.dolly.info0, info1, info2, etc.",
                    "Default: 1 (shows only info0)")
            .defineInRange("100_dolly_info_lines", 1, 0, Integer.MAX_VALUE);
    
    private static final ModConfigSpec.IntValue HARD_DOLLY_INFO_LINES = BUILDER
            .comment("Number of info tooltip lines to display for the Hard Dolly",
                    "Each line uses tooltip.iska_utils.dolly_hard.info0, info1, info2, etc.",
                    "Default: 1 (shows only info0)")
            .defineInRange("101_hard_dolly_info_lines", 1, 0, Integer.MAX_VALUE);
    
    private static final ModConfigSpec.IntValue CREATIVE_DOLLY_INFO_LINES = BUILDER
            .comment("Number of info tooltip lines to display for the Creative Dolly",
                    "Each line uses tooltip.iska_utils.dolly_creative.info0, info1, info2, etc.",
                    "Default: 1 (shows only info0)")
            .defineInRange("102_creative_dolly_info_lines", 1, 0, Integer.MAX_VALUE);
    
    static {
        BUILDER.pop(); // End of dolly category
        
        // Dye Bush Configuration (under general_utilities)
        BUILDER.comment("Dye Bush Configuration").push("dye_bush");
    }

    public static final ModConfigSpec.IntValue MIN_DYE_BUSH_REFILL_TIME = BUILDER
            .comment("Minimum time in ticks for a dye bush to refill with berries (1 tick = 1/20 second, default 30 seconds)")
            .defineInRange("000_minDyeBushRefillTime", 600, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_DYE_BUSH_REFILL_TIME = BUILDER
            .comment("Maximum time in ticks for a dye bush to refill with berries (1 tick = 1/20 second, default 1 minute)")
            .defineInRange("001_maxDyeBushRefillTime", 1200, 0, Integer.MAX_VALUE);

    static {
        BUILDER.pop(); // End of dye_bush category

        BUILDER.comment("Entropic & Graveyard Soils").push("soils");
    }

    private static final ModConfigSpec.BooleanValue ENTROPIC_SOIL_SPAWN_ENABLED = BUILDER
            .comment("If true, entropic soil networks can spawn mobs in darkness.")
            .define("000_entropic_soil_spawn_enabled", true);

    private static final ModConfigSpec.IntValue ENTROPIC_SOIL_SPAWN_INTERVAL_MIN = BUILDER
            .comment("Minimum ticks between entropic soil spawn attempts (random range).")
            .defineInRange("001_entropic_soil_spawn_interval_min_ticks", 300, 1, 720000);

    private static final ModConfigSpec.IntValue ENTROPIC_SOIL_SPAWN_INTERVAL_MAX = BUILDER
            .comment("Maximum ticks between entropic soil spawn attempts (random range).")
            .defineInRange("002_entropic_soil_spawn_interval_max_ticks", 600, 1, 720000);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> ENTROPIC_SOIL_SPAWN_ALLOW = BUILDER
            .comment("Extra allow entries merged with biome spawn tables (does not restrict other biome mobs).",
                    "Deny list always wins. Format: biome_or_#tag;entity_id",
                    "Example: #minecraft:is_overworld;minecraft:slime")
            .defineList("003_entropic_soil_spawn_allow",
                    java.util.Arrays.asList(
                            "#minecraft:is_overworld;minecraft:slime",
                            "#minecraft:is_overworld;minecraft:phantom"),
                    obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> ENTROPIC_SOIL_SPAWN_DENY = BUILDER
            .comment("Deny list for entropic soil spawns (wins over allow). Same format as allow.")
            .defineList("004_entropic_soil_spawn_deny",
                    java.util.Collections.singletonList("#minecraft:is_overworld;mowziesmobs:grottol"),
                    obj -> obj instanceof String);

    private static final ModConfigSpec.BooleanValue ENTROPIC_SOIL_REDSTONE_ACCEL_ENABLED = BUILDER
            .comment("If true, redstone on the network uses fast per-block spawn waves (Mob Grinding Utils dreadful dirt style).")
            .define("005_entropic_soil_redstone_accel_enabled", true);

    private static final ModConfigSpec.IntValue ENTROPIC_SOIL_REDSTONE_ACCEL_MIN = BUILDER
            .comment("Minimum ticks between accelerated spawn waves while redstone is active (random range).")
            .defineInRange("006_entropic_soil_redstone_accel_min_ticks", 20, 1, 720000);

    private static final ModConfigSpec.IntValue ENTROPIC_SOIL_REDSTONE_ACCEL_MAX = BUILDER
            .comment("Maximum ticks between accelerated spawn waves while redstone is active (random range).")
            .defineInRange("007_entropic_soil_redstone_accel_max_ticks", 60, 1, 720000);

    private static final ModConfigSpec.IntValue ENTROPIC_SOIL_ACCEL_MOB_CAP = BUILDER
            .comment("Max hostile mobs within 5 blocks (horizontal) and 2 vertical of each soil before that block skips a spawn try.")
            .defineInRange("008_entropic_soil_accel_mob_cap", 8, 0, 64);

    private static final ModConfigSpec.IntValue ENTROPIC_SOIL_SPAWN_MAX_HEALTH = BUILDER
            .comment("Maximum max-health (HP) for mobs spawned on entropic soil. 0 disables the cap.")
            .defineInRange("009_entropic_soil_spawn_max_health", 60, 0, 10000);

    private static final ModConfigSpec.DoubleValue ENTROPIC_EMPOWERMENT_DAMAGE_BONUS = BUILDER
            .comment("Bonus outgoing damage fraction for mobs with Entropic Empowerment (0.25 = +25%).")
            .defineInRange("010_entropic_empowerment_damage_bonus", 0.25D, 0.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue ENTROPIC_EMPOWERMENT_DAMAGE_REDUCTION = BUILDER
            .comment("Incoming damage reduction fraction for empowered mobs (0.20 = -20% damage taken).")
            .defineInRange("011_entropic_empowerment_damage_reduction", 0.20D, 0.0D, 1.0D);

    private static final ModConfigSpec.IntValue ENTROPIC_SOIL_SLOW_SPREAD_CHANCE = BUILDER
            .comment("Random-tick spread denominator onto vanilla grass/dirt only (1/N per random tick). Not used by agglomeration.")
            .defineInRange("020_entropic_soil_slow_spread_chance", 24, 1, 1000);

    private static final ModConfigSpec.IntValue ENTROPIC_SOIL_EDGE_SPREAD_CHANCE = BUILDER
            .comment("Random-tick spread denominator when target grass/dirt is adjacent to entropic soil (faster border creep). Not used by agglomeration.")
            .defineInRange("021_entropic_soil_edge_spread_chance", 8, 1, 1000);

    private static final ModConfigSpec.IntValue ENTROPIC_SOIL_DIRT_SPREAD_CHANCE = BUILDER
            .comment("Random-tick spread denominator when reclaiming adjacent Entropic Dirt back to Entropic Soil (1/N per random tick; lower = faster). Not used by agglomeration.")
            .defineInRange("021a_entropic_soil_dirt_spread_chance", 3, 1, 1000);

    private static final ModConfigSpec.IntValue ENTROPIC_SOIL_TO_DIRT_MIN = BUILDER
            .comment("Minimum ticks in light before entropic soil turns into entropic dirt (random per block, 20 = 1s).")
            .defineInRange("022_entropic_soil_to_dirt_min_ticks", 20, 1, 720000);

    private static final ModConfigSpec.IntValue ENTROPIC_SOIL_TO_DIRT_MAX = BUILDER
            .comment("Maximum ticks in light before entropic soil turns into entropic dirt (random per block, 100 = 5s).")
            .defineInRange("023_entropic_soil_to_dirt_max_ticks", 100, 1, 720000);

    private static final ModConfigSpec.IntValue ENTROPIC_DIRT_TO_VANILLA_MIN = BUILDER
            .comment("Minimum ticks in light before entropic dirt reverts to vanilla dirt (random per block, 1200 = 1 min).")
            .defineInRange("025_entropic_dirt_to_vanilla_min_ticks", 1200, 1, 720000);

    private static final ModConfigSpec.IntValue ENTROPIC_DIRT_TO_VANILLA_MAX = BUILDER
            .comment("Maximum ticks in light before entropic dirt reverts to vanilla dirt (random per block, 6000 = 5 min).")
            .defineInRange("026_entropic_dirt_to_vanilla_max_ticks", 6000, 1, 720000);

    private static final ModConfigSpec.IntValue ENTROPIC_AGGLOMERATION_SPREAD_INTERVAL = BUILDER
            .comment("Ticks between each circular ring when spreading entropic agglomeration.")
            .defineInRange("024_entropic_agglomeration_spread_ring_ticks", 15, 1, 720000);

    private static final ModConfigSpec.IntValue GRAVEYARD_SOIL_HEAL_INTERVAL_MIN = BUILDER
            .comment("Minimum ticks between graveyard soil undead heals (random range).")
            .defineInRange("100_graveyard_soil_heal_interval_min_ticks", 20, 1, 720000);

    private static final ModConfigSpec.IntValue GRAVEYARD_SOIL_HEAL_INTERVAL_MAX = BUILDER
            .comment("Maximum ticks between graveyard soil undead heals (random range).")
            .defineInRange("101_graveyard_soil_heal_interval_max_ticks", 40, 1, 720000);

    private static final ModConfigSpec.DoubleValue GRAVEYARD_SOIL_HEAL_AMOUNT = BUILDER
            .comment("HP healed per graveyard soil tick cycle for undead standing on the block.")
            .defineInRange("102_graveyard_soil_heal_amount", 3.0D, 0.0D, 1000.0D);

    private static final ModConfigSpec.BooleanValue DRUIDIC_PODZOL_SPAWN_ENABLED = BUILDER
            .comment("If true, druidic podzol networks can spawn biome animals in light.")
            .define("150_druidic_podzol_spawn_enabled", true);

    private static final ModConfigSpec.IntValue DRUIDIC_PODZOL_SPAWN_INTERVAL_MIN = BUILDER
            .comment("Minimum ticks between druidic podzol spawn attempts (random range).")
            .defineInRange("151_druidic_podzol_spawn_interval_min_ticks", 300, 1, 720000);

    private static final ModConfigSpec.IntValue DRUIDIC_PODZOL_SPAWN_INTERVAL_MAX = BUILDER
            .comment("Maximum ticks between druidic podzol spawn attempts (random range).")
            .defineInRange("152_druidic_podzol_spawn_interval_max_ticks", 600, 1, 720000);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DRUIDIC_PODZOL_SPAWN_ALLOW = BUILDER
            .comment("Extra allow entries merged with biome animal spawn tables. Deny list always wins.",
                    "Format: biome_or_#tag;entity_id")
            .defineList("153_druidic_podzol_spawn_allow",
                    java.util.Arrays.asList(
                            "#minecraft:is_overworld;minecraft:cow",
                            "#minecraft:is_overworld;minecraft:pig",
                            "#minecraft:is_overworld;minecraft:sheep",
                            "#minecraft:is_overworld;minecraft:chicken"),
                    obj -> obj instanceof String);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DRUIDIC_PODZOL_SPAWN_DENY = BUILDER
            .comment("Deny list for druidic podzol spawns (wins over allow). Same format as allow.")
            .defineList("154_druidic_podzol_spawn_deny", java.util.Collections.emptyList(), obj -> obj instanceof String);

    private static final ModConfigSpec.BooleanValue DRUIDIC_PODZOL_REDSTONE_ACCEL_ENABLED = BUILDER
            .comment("If true, redstone on the network uses fast per-block spawn waves (Mob Grinding Utils delightful dirt style).")
            .define("155_druidic_podzol_redstone_accel_enabled", true);

    private static final ModConfigSpec.IntValue DRUIDIC_PODZOL_REDSTONE_ACCEL_MIN = BUILDER
            .comment("Minimum ticks between accelerated spawn waves while redstone is active (random range).")
            .defineInRange("156_druidic_podzol_redstone_accel_min_ticks", 20, 1, 720000);

    private static final ModConfigSpec.IntValue DRUIDIC_PODZOL_REDSTONE_ACCEL_MAX = BUILDER
            .comment("Maximum ticks between accelerated spawn waves while redstone is active (random range).")
            .defineInRange("157_druidic_podzol_redstone_accel_max_ticks", 60, 1, 720000);

    private static final ModConfigSpec.IntValue DRUIDIC_PODZOL_ACCEL_MOB_CAP = BUILDER
            .comment("Max passive animals within 5 blocks (horizontal) and 2 vertical of each podzol before that block skips a spawn try.")
            .defineInRange("158_druidic_podzol_accel_mob_cap", 8, 0, 64);

    private static final ModConfigSpec.IntValue DRUIDIC_PODZOL_SPAWN_MAX_HEALTH = BUILDER
            .comment("Maximum max-health (HP) for animals spawned on druidic podzol. 0 disables the cap.")
            .defineInRange("159_druidic_podzol_spawn_max_health", 60, 0, 10000);

    private static final ModConfigSpec.IntValue DRUIDIC_PODZOL_SLOW_SPREAD_CHANCE = BUILDER
            .comment("Random-tick spread denominator onto vanilla dirt/podzol only (1/N per random tick). Not used by agglomeration.")
            .defineInRange("170_druidic_podzol_slow_spread_chance", 24, 1, 1000);

    private static final ModConfigSpec.IntValue DRUIDIC_AGGLOMERATION_SPREAD_INTERVAL = BUILDER
            .comment("Ticks between each circular ring when spreading druidic agglomeration.")
            .defineInRange("171_druidic_agglomeration_spread_ring_ticks", 15, 1, 720000);

    static {
        BUILDER.pop(); // End of soils category
        
        // Category for Artifacts Settings (under general_utilities)
        BUILDER.comment("Artifacts Settings").push("artifacts_settings");
    }
    
    private static final ModConfigSpec.BooleanValue ARTIFACTS_INFO= BUILDER
            .comment("If false not desplay where obtain the artifacts or mod dependecy required for obtain it (only for lootable artifacts)")
            .define("900_artifacts_info", true);

    // Artifacts configuration (starts at 500)
    private static final ModConfigSpec.IntValue CHOSEN_CHEESE_MAX = BUILDER
            .comment("Maximum cheese level (X in tooltip Y/X). Effective bonus uses min(Y, X) levels.")
            .defineInRange("500_chosen_cheese_max", 10, 0, 200);

    private static final ModConfigSpec.DoubleValue CHOSEN_CHEESE_HP_PER_LEVEL = BUILDER
            .comment("Max health bonus per cheese level (2.0 = one heart).")
            .defineInRange("501_chosen_cheese_hp_per_level", 2.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue OLD_BRICK_ARMOR_BONUS = BUILDER
            .comment("Armor bonus from Old Brick while equipped in Curios.")
            .defineInRange("502_old_brick_armor_bonus", 2.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue SHARPENED_BONE_ARMOR_IGNORE_CHANCE = BUILDER
            .comment("Chance (0.0–1.0) to fully ignore target armor on hit (toughness still applies).")
            .defineInRange("503_sharpened_bone_armor_ignore_chance", 0.25D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue THE_ROOTS_BREAK_SPEED_MIN_MULTIPLIER = BUILDER
            .comment("Minimum break-speed multiplier for The Roots (1.0 = no bonus).")
            .defineInRange("506_the_roots_break_speed_min_multiplier", 1.0D, 0.1D, 10.0D);

    private static final ModConfigSpec.DoubleValue THE_ROOTS_BREAK_SPEED_MAX_BONUS = BUILDER
            .comment("Random extra break-speed multiplier added on top of the minimum (final = min + random(0..max)).")
            .defineInRange("507_the_roots_break_speed_max_bonus", 1.0D, 0.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue TOTEM_OF_PAIN_PROC_CHANCE = BUILDER
            .comment("Chance (0.0–1.0) to apply Curse of Pain on hit with Totem of Pain.")
            .defineInRange("508_totem_of_pain_proc_chance", 0.25D, 0.0D, 1.0D);

    private static final ModConfigSpec.IntValue TOTEM_OF_PAIN_CURSE_DURATION_SECONDS = BUILDER
            .comment("Duration in seconds for Curse of Pain applied by Totem of Pain.")
            .defineInRange("509_totem_of_pain_curse_duration_seconds", 30, 1, 3600);

    private static final ModConfigSpec.DoubleValue RITUAL_GAUNTLET_CRIT_CHANCE = BUILDER
            .comment("Chance (0.0–1.0) for Ritual Gauntlet critical damage bonus.")
            .defineInRange("510_ritual_gauntlet_crit_chance", 0.15D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue RITUAL_GAUNTLET_CRIT_DAMAGE_BENEFICIAL_NEUTRAL = BUILDER
            .comment("Damage multiplier on Ritual Gauntlet crit with a beneficial or neutral effect (1.15 = +15% damage).")
            .defineInRange("511_ritual_gauntlet_crit_damage_beneficial_neutral", 1.15D, 1.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue RITUAL_GAUNTLET_CRIT_DAMAGE_HARMFUL = BUILDER
            .comment("Damage multiplier on Ritual Gauntlet crit with a harmful effect (1.30 = +30% damage).")
            .defineInRange("515_ritual_gauntlet_crit_damage_harmful", 1.30D, 1.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue BUSTED_CROWN_HP_PER_CURSED_ARTIFACT = BUILDER
            .comment("Max health bonus per cursed artifact worn with Busted Crown.")
            .defineInRange("512_busted_crown_hp_per_cursed_artifact", 2.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue CURSE_OF_PAIN_DAMAGE_PER_LEVEL = BUILDER
            .comment("Incoming damage multiplier per Curse of Pain level (0.10 = +10% per level).")
            .defineInRange("513_curse_of_pain_damage_per_level", 0.10D, 0.0D, 10.0D);

    private static final ModConfigSpec.IntValue THE_DECEPTION_ABSORPTION_DURATION_SECONDS = BUILDER
            .comment("Deceived effect duration in seconds after eating with The Deception equipped.")
            .defineInRange("514_the_deception_absorption_duration_seconds", 30, 1, 3600);

    private static final ModConfigSpec.DoubleValue NECROTIC_CRYSTAL_HEART_HP_COST_PER_SAVE = BUILDER
            .comment("Max health removed each time Necrotic Crystal Heart prevents lethal damage (2.0 = one heart).")
            .defineInRange("515_necrotic_crystal_heart_hp_cost_per_save", 2.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue NECROTIC_CRYSTAL_HEART_MIN_MAX_HEALTH = BUILDER
            .comment("Minimum max health before Necrotic Crystal Heart stops saving the player.")
            .defineInRange("516_necrotic_crystal_heart_min_max_health", 2.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue NECROTIC_CRYSTAL_HEART_BASE_HEALTH = BUILDER
            .comment("Base max health used by Necrotic Crystal Heart hex tracking.")
            .defineInRange("517_necrotic_crystal_heart_base_health", 20.0D, 1.0D, 1000.0D);

    private static final ModConfigSpec.IntValue ICE_DIAMOND_REPAIR_INTERVAL_TICKS = BUILDER
            .comment("Ticks between Ice Diamond repair attempts while equipped.")
            .defineInRange("518_ice_diamond_repair_interval_ticks", 20, 1, 1200);

    private static final ModConfigSpec.DoubleValue ICE_DIAMOND_COLD_BIOME_MAX_TEMP = BUILDER
            .comment("Biomes at or below this temperature cost no Ice Diamond durability per repair.")
            .defineInRange("519_ice_diamond_cold_biome_max_temp", 0.15D, -1.0D, 2.0D);

    private static final ModConfigSpec.DoubleValue ICE_DIAMOND_HOT_BIOME_MIN_TEMP = BUILDER
            .comment("Biomes at or above this temperature use the hot repair durability cost.")
            .defineInRange("520_ice_diamond_hot_biome_min_temp", 1.5D, -1.0D, 3.0D);

    private static final ModConfigSpec.IntValue ICE_DIAMOND_COLD_REPAIR_COST = BUILDER
            .comment("Ice Diamond durability cost per repair in cold biomes.")
            .defineInRange("521_ice_diamond_cold_repair_cost", 0, 0, 100);

    private static final ModConfigSpec.IntValue ICE_DIAMOND_TEMPERATE_REPAIR_COST = BUILDER
            .comment("Ice Diamond durability cost per repair in temperate biomes.")
            .defineInRange("522_ice_diamond_temperate_repair_cost", 1, 0, 100);

    private static final ModConfigSpec.IntValue ICE_DIAMOND_HOT_REPAIR_COST = BUILDER
            .comment("Ice Diamond durability cost per repair in hot biomes.")
            .defineInRange("523_ice_diamond_hot_repair_cost", 5, 0, 100);

    private static final ModConfigSpec.DoubleValue ENTROPIC_RING_DAMAGE_PER_100_HP = BUILDER
            .comment("Flat outgoing damage bonus per 100 HP the target has above the attacker (Entropic Ring).")
            .defineInRange("524_entropic_ring_damage_per_100_hp", 5.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ANCIENT_STAR_ARMOR_BONUS = BUILDER
            .comment("Armor bonus while Ancient Star is equipped in Curios.")
            .defineInRange("525_ancient_star_armor_bonus", 5.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ANCIENT_STAR_DAMAGE_BONUS = BUILDER
            .comment("Flat outgoing damage bonus while attacker HP is at or above the high threshold.")
            .defineInRange("526_ancient_star_damage_bonus", 5.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ANCIENT_STAR_TOUGHNESS_BONUS = BUILDER
            .comment("Armor toughness bonus while attacker HP is below the low threshold.")
            .defineInRange("527_ancient_star_toughness_bonus", 5.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ANCIENT_STAR_HIGH_HP_RATIO = BUILDER
            .comment("HP ratio (0–1) at or above which Ancient Star grants bonus damage.")
            .defineInRange("528_ancient_star_high_hp_ratio", 0.91D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue ANCIENT_STAR_LOW_HP_RATIO = BUILDER
            .comment("HP ratio (0–1) below which Ancient Star grants bonus toughness.")
            .defineInRange("529_ancient_star_low_hp_ratio", 0.50D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue RUNIC_DICE_ATTACK_SPEED_MIN = BUILDER
            .comment("Minimum attack-speed bonus fraction for Runic Dice (0.30 = +30%).")
            .defineInRange("540_runic_dice_attack_speed_min", 0.30D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue RUNIC_DICE_ATTACK_SPEED_MAX = BUILDER
            .comment("Maximum attack-speed bonus fraction for Runic Dice (0.50 = +50%).")
            .defineInRange("541_runic_dice_attack_speed_max", 0.50D, 0.0D, 1.0D);

    private static final ModConfigSpec.IntValue RUNIC_DICE_REROLL_TICKS = BUILDER
            .comment("Ticks between Runic Dice attack-speed re-rolls while equipped.")
            .defineInRange("542_runic_dice_reroll_ticks", 60, 1, 72000);

    private static final ModConfigSpec.DoubleValue ENTROPIC_RING_APOTHEOSIS_HAVEN_MULT = BUILDER
            .comment("Entropic Ring damage multiplier at Apotheosis WorldTier Haven.")
            .defineInRange("530_entropic_ring_apotheosis_haven_mult", 1.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ENTROPIC_RING_APOTHEOSIS_FRONTIER_MULT = BUILDER
            .comment("Entropic Ring damage multiplier at Apotheosis WorldTier Frontier.")
            .defineInRange("531_entropic_ring_apotheosis_frontier_mult", 2.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ENTROPIC_RING_APOTHEOSIS_ASCENT_MULT = BUILDER
            .comment("Entropic Ring damage multiplier at Apotheosis WorldTier Ascent.")
            .defineInRange("532_entropic_ring_apotheosis_ascent_mult", 3.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ENTROPIC_RING_APOTHEOSIS_SUMMIT_MULT = BUILDER
            .comment("Entropic Ring damage multiplier at Apotheosis WorldTier Summit.")
            .defineInRange("533_entropic_ring_apotheosis_summit_mult", 4.0D, 0.0D, 100.0D);

    private static final ModConfigSpec.DoubleValue ENTROPIC_RING_APOTHEOSIS_PINNACLE_MULT = BUILDER
            .comment("Entropic Ring damage multiplier at Apotheosis WorldTier Pinnacle.")
            .defineInRange("534_entropic_ring_apotheosis_pinnacle_mult", 5.0D, 0.0D, 100.0D);

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

    private static final ModConfigSpec.DoubleValue GAUNTLET_CLIMBING_SPEED = BUILDER
            .comment("Speed of the Gauntlet of Climbing (0.0 to 10.0)",
                    "Default: 0.15 (0.288 is like a ladder speed)")
            .defineInRange("010_gauntlet_climbing_speed", 0.15D, 0.0D, 10.0D);

    static {
        BUILDER.pop(); // End of artifacts_settings category

        // Category for Mob Reaper Configuration (under general_utilities)
        BUILDER.comment("Mob Reaper Configuration").push("reaper");
    }

    private static final ModConfigSpec.DoubleValue REAPER_DEFAULT_DAMAGE = BUILDER
            .comment("Base damage dealt per attack tick cycle",
                    "NOTE: This parameter is WITHOUT upgrades")
            .defineInRange("000_reaperDefaultDamage", 10.0D, 0.0D, 10000.0D);

    private static final ModConfigSpec.IntValue REAPER_ATTACK_INTERVAL_TICKS = BUILDER
            .comment("Ticks between damage applications")
            .defineInRange("001_reaperAttackIntervalTicks", 10, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue REAPER_HIT_DEPTH = BUILDER
            .comment("Extended hit depth in blocks along FACING only; other valid faces hit the adjacent block")
            .defineInRange("002_reaperHitDepth", 5, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue REAPER_NORMAL_UPGRADE_MAX = BUILDER
            .comment("Maximum normal damage modules in slot 0")
            .defineInRange("100_reaperNormalUpgradeMax", 10, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue REAPER_NORMAL_BONUS_PER_MODULE = BUILDER
            .comment("Bonus damage per normal module stack")
            .defineInRange("101_reaperNormalBonusPerModule", 5.0D, 0.0D, 10000.0D);

    private static final ModConfigSpec.IntValue REAPER_LETHAL_UPGRADE_MAX = BUILDER
            .comment("Maximum lethal damage modules in slot 0 (mutually exclusive with normal)")
            .defineInRange("102_reaperLethalUpgradeMax", 1, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue REAPER_LETHAL_DAMAGE = BUILDER
            .comment("Fixed damage when lethal module is installed (overrides normal calculation)")
            .defineInRange("103_reaperLethalDamage", 500.0D, 0.0D, 100000.0D);

    private static final ModConfigSpec.IntValue REAPER_ENCHANT_UPGRADE_MAX = BUILDER
            .comment("Maximum enchant modules in slot 1")
            .defineInRange("104_reaperEnchantUpgradeMax", 1, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue REAPER_BEHEADING_UPGRADE_MAX = BUILDER
            .comment("Maximum beheading modules in slot 2")
            .defineInRange("105_reaperBeheadingUpgradeMax", 10, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue REAPER_BEHEADING_CHANCE_PER_LEVEL = BUILDER
            .comment("Skull drop chance added per beheading module stack (0.10 = 10%)")
            .defineInRange("106_reaperBeheadingChancePerLevel", 0.10D, 0.0D, 1.0D);

    private static final ModConfigSpec.IntValue REAPER_LUCK_UPGRADE_MAX = BUILDER
            .comment("Maximum luck modules in slot 3")
            .defineInRange("107_reaperLuckUpgradeMax", 3, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue REAPER_EXPERIENCE_UPGRADE_MAX = BUILDER
            .comment("Maximum experience modules in slot 4")
            .defineInRange("108_reaperExperienceUpgradeMax", 10, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue REAPER_EXPERIENCE_BONUS_PER_LEVEL = BUILDER
            .comment("Experience multiplier bonus per experience module stack (0.10 = +10%)")
            .defineInRange("109_reaperExperienceBonusPerLevel", 0.10D, 0.0D, 10.0D);

    private static final ModConfigSpec.DoubleValue REAPER_BLADE_MAX_DEG_PER_TICK = BUILDER
            .comment("Client blade rotation speed in degrees per tick (used by BER)")
            .defineInRange("003_reaperBladeMaxDegPerTick", 12.0D, 0.0D, 360.0D);

    static {
        BUILDER.pop(); // End of reaper category

        BUILDER.comment("Collecting Crate").push("collecting_crate");
    }

    private static final ModConfigSpec.IntValue COLLECTING_CRATE_XP_CAPACITY_LEVELS = BUILDER
            .comment("Internal XP tank capacity expressed in experience levels (converted to mB at runtime)")
            .defineInRange("000_collectingCrateXpCapacityLevels", 5000, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue COLLECTING_CRATE_XP_MB_PER_POINT = BUILDER
            .comment("Millibuckets of liquid experience per vanilla XP point (standard modpack value: 20)")
            .defineInRange("001_collectingCrateXpMbPerPoint", 20, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue COLLECTING_CRATE_BASE_RANGE = BUILDER
            .comment("Base cubic collection radius in blocks without range modules")
            .defineInRange("002_collectingCrateBaseRange", 5, 1, 64);

    private static final ModConfigSpec.IntValue COLLECTING_CRATE_MAX_RANGE = BUILDER
            .comment("Maximum cubic collection radius with range modules installed")
            .defineInRange("003_collectingCrateMaxRange", 16, 1, 64);

    private static final ModConfigSpec.IntValue COLLECTING_CRATE_MAX_INSERTIONS_PER_TICK = BUILDER
            .comment("Maximum item entities absorbed per collection tick (0 = unlimited)")
            .defineInRange("004_collectingCrateMaxInsertionsPerTick", 20, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue COLLECTING_CRATE_COLLECTION_INTERVAL_TICKS = BUILDER
            .comment("Server ticks between collection scans")
            .defineInRange("005_collectingCrateCollectionIntervalTicks", 1, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue COLLECTING_CRATE_STORAGE_SLOTS = BUILDER
            .comment("Internal item storage slot count")
            .defineInRange("006_collectingCrateStorageSlots", 27, 1, 256);

    private static final ModConfigSpec.IntValue COLLECTING_CRATE_RANGE_UPGRADE_MAX = BUILDER
            .comment("Maximum stack size of range modules in the upgrade slot")
            .defineInRange("105_collectingCrateRangeUpgradeMax", 8, 1, 64);

    static {
        BUILDER.pop(); // End of collecting_crate category

        BUILDER.comment("Blazing Altar — area flames and natural spawn control").push("blazing_altar");
    }

    private static final ModConfigSpec.IntValue BLAZING_ALTAR_MAX_CHUNK_RADIUS = BUILDER
            .comment("Maximum chunk radius selectable in the GUI (Chebyshev, 1–4 typical)")
            .defineInRange("000_max_chunk_radius", 4, 1, 16);

    private static final ModConfigSpec.IntValue BLAZING_ALTAR_TICK_INTERVAL = BUILDER
            .comment("Server ticks between flame placement cycles per altar")
            .defineInRange("001_tick_interval", 20, 1, 200);

    private static final ModConfigSpec.IntValue BLAZING_ALTAR_PLACEMENTS_PER_TICK = BUILDER
            .comment("Max flame placement attempts per altar per cycle")
            .defineInRange("002_placements_per_tick", 6, 1, 64);

    private static final ModConfigSpec.IntValue BLAZING_ALTAR_EXCLUSION_RADIUS = BUILDER
            .comment("Block radius to check excluded blocks near a flame candidate")
            .defineInRange("003_exclusion_radius", 3, 0, 16);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BLAZING_ALTAR_LIGHT_SENSITIVE_ENTRIES = BUILDER
            .comment("Blocks/tags that must not be lit by altar flames. Prefix # for tags, otherwise block id (namespace:path).",
                    "Default: mushrooms, entropic soil, dreadful dirt.")
            .defineList("004_light_sensitive_entries",
                    java.util.List.of("#c:mushrooms", "iska_utils:entropic_soil", "mob_grinding_utils:dreadful_dirt"),
                    obj -> obj instanceof String);

    private static final ModConfigSpec.IntValue BLAZING_ALTAR_LIGHT_SENSITIVE_MAX_BLOCK_LIGHT = BUILDER
            .comment("Maximum block light allowed on light-sensitive blocks and the space above them (simulated after placing a flame).",
                    "0 = strict darkness (default); placement is blocked if block light would be 1 or higher.")
            .defineInRange("005_light_sensitive_max_block_light", 0, 0, 15);

    private static final ModConfigSpec.IntValue BLAZING_ALTAR_RANGE_MODULE_CHUNK_BONUS = BUILDER
            .comment("Extra chunk radius (Chebyshev) per range module installed in the altar")
            .defineInRange("006_range_module_chunk_bonus", 3, 1, 16);

    private static final ModConfigSpec.IntValue BLAZING_ALTAR_RANGE_UPGRADE_MAX = BUILDER
            .comment("Maximum stack size of range modules in the altar upgrade slot")
            .defineInRange("007_range_upgrade_max", 4, 1, 64);

    private static final ModConfigSpec.IntValue BLAZING_ALTAR_EXTINGUISH_COLUMNS_PER_TICK = BUILDER
            .comment("Chunk columns (16x1 block strips) processed per server tick when removing flames on break or GUI extinguish")
            .defineInRange("008_extinguish_columns_per_tick", 48, 1, 4096);

    static {
        BUILDER.pop(); // End of blazing_altar category
        BUILDER.pop(); // End of general_utilities category

        // Category for Fan Configuration
        BUILDER.comment("Fan Configuration").push("fan");
    }

    // Fan Configuration
    // NOTE: These parameters are WITHOUT upgrades. Upgrades will be added starting from 100.
    private static final ModConfigSpec.IntValue FAN_RANGE_HORIZONTAL_MAX = BUILDER
            .comment("Maximum horizontal range (left/right) for the fan (in blocks)",
                    "This is the maximum value that can be set for range_left and range_right",
                    "NOTE: This parameter is WITHOUT upgrades")
            .defineInRange("000_fanRangeHorizontalMax", 2, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue FAN_RANGE_VERTICAL_MAX = BUILDER
            .comment("Maximum vertical range (up/down) for the fan (in blocks)",
                    "This is the maximum value that can be set for range_up and range_down",
                    "NOTE: This parameter is WITHOUT upgrades")
            .defineInRange("001_fanRangeVerticalMax", 2, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue FAN_RANGE_FRONT_MAX = BUILDER
            .comment("Maximum front range for the fan (in blocks)",
                    "This is automatically calculated as horizontal range * 2 to maintain a cube shape",
                    "Default: 5 (horizontal 2 * 2) for a 5x5 cube (2+1+2)",
                    "NOTE: This parameter is WITHOUT upgrades and should be horizontal * 2")
            .defineInRange("002_fanRangeFrontMax", 5, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue FAN_DEFAULT_POWER = BUILDER
            .comment("Default power/force with which the fan pushes entities",
                    "Default: 0.3 (midway between slow and moderate vector plates)",
                    "NOTE: This parameter is WITHOUT upgrades")
            .defineInRange("003_fanDefaultPower", 0.3D, 0.0D, 100.0D);

    private static final ModConfigSpec.IntValue FAN_RANGE_UPGRADE_MAX = BUILDER
            .comment("Maximum number of range module upgrades that can be installed",
                    "Each range module increases the maximum range by 1",
                    "Default: 5")
            .defineInRange("100_fanRangeUpgradeMax", 5, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue FAN_ACCELERATION_UPGRADE_MAX = BUILDER
            .comment("Maximum number of acceleration module upgrades (slow, moderate, fast, extreme, ultra) that can be installed",
                    "Each acceleration module increases the fan's power",
                    "Default: 3")
            .defineInRange("101_fanAccelerationUpgradeMax", 3, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends Double>> FAN_ACCELERATION_MODULE_POWERS = BUILDER
            .comment("Power values for acceleration modules (slow, moderate, fast, extreme, ultra)",
                    "Each module adds this power value to the fan's base power",
                    "Order: [slow, moderate, fast, extreme, ultra]",
                    "Defaults match vector plate speeds: [0.1, 0.5, 1.0, 5.0, 15.0]")
            .defineList("102_fanAccelerationModulePowers", 
                    java.util.List.of(0.1D, 0.5D, 1.0D, 5.0D, 15.0D),
                    o -> o instanceof Double && (Double) o >= 0.0D && (Double) o <= 100.0D);
    
    private static final ModConfigSpec.BooleanValue FAN_GHOST_MODULE_BYPASS_UNBREAKABLE = BUILDER
            .comment("If true, the Ghost Module allows airflow to pass through unbreakable blocks (hardness < 0)",
                    "When false (default), unbreakable blocks still block airflow even with Ghost Module")
            .define("103_fanGhostModuleBypassUnbreakable", false);
    
    // Fanpack Configuration (starts at 200)
    private static final ModConfigSpec.IntValue FANPACK_ENERGY_CAPACITY = BUILDER
            .comment("Energy capacity of the Fanpack in RF/FE",
                    "Recommended value: same as Vector Charm, but set to 0 to disable energy consumption")
            .defineInRange("200_fanpackEnergyCapacity", 500000, 0, Integer.MAX_VALUE);
    
    private static final ModConfigSpec.IntValue FANPACK_FLIGHT_ENERGY_CONSUME = BUILDER
            .comment("Energy consumed per tick for creative flight",
                    "Default: 5 RF/tick",
                    "Set to 0 to disable energy consumption for flight")
            .defineInRange("201_fanpackFlightEnergyConsume", 5, 0, Integer.MAX_VALUE);

    static {
        BUILDER.pop(); // End of fan category
        
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

        BUILDER.comment("Worldgen Configuration").push("worldgen");
    }

    public static final ModConfigSpec.BooleanValue GENERATE_RUBBER_TREES = BUILDER
            .comment("When true, rubber trees are injected into overworld generation (vegetal_decoration step).",
                    "Set to false to disable worldgen rubber trees (saplings and structures are unaffected).")
            .define("000_generate_rubber_trees", true);

    static {
        BUILDER.pop(); // End of worldgen category

        BUILDER.comment("Tweaks Configuration").push("tweaks");
    }

    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>>  sticky_fluids = BUILDER
            .comment("List of fluids that should be sticky")
            .defineList("000_sticky_fluids", java.util.Arrays.asList("#c:oil"), obj -> obj instanceof String);

    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>>  crude_oils = BUILDER
            .comment("List of fluids that should be crude oil")
            .defineList("001_crude_oils", java.util.Arrays.asList("#c:oil"), obj -> obj instanceof String);

            
    static {
        BUILDER.pop(); // End of tweaks category

        // Category for scanner
        BUILDER.comment("Scanner Configuration").push("scanner");
    }

    // Scanner field order matches legacy iska_utils-common.toml (100 first, then 001/000/200/…)

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
                        "boro;2f4751", // Boron ore - Dark blue-gray color
                        "chromo;31a929", // Chromium ore - Green color
                        "allthemodium;C78938", // Allthemodium ore - Orange color
                        "vibranium;40BBBF", // Vibranium ore - Blue-Green color
                        "unubtanium;993BAC", // Unubtanium ore - Purple color
                        "draconium;9933FF" // Draconium Evolution / common mod ore - Purple
                    ), 
                    obj -> obj instanceof String && ((String)obj).contains(";"));

    private static final ModConfigSpec.IntValue SCANNER_SCAN_DURATION = BUILDER
            .comment("Duration in ticks needed to hold the scanner for scanning (1 second = 20 ticks)")
            .defineInRange("001_scannerScanDuration", 40, 1, 200);

    private static final ModConfigSpec.IntValue SCANNER_SCAN_RANGE = BUILDER
            .comment("DEPRECATED: This parameter has been moved to 200_scannerRangeOptions.",
                    "The maximum scan range is now determined by the highest value in the scannerRangeOptions array.",
                    "This parameter is kept for backward compatibility but is no longer used.")
            .translation("iska_utils.config.deprecated_scanner_scan_range")
            .defineInRange("000_scannerScanRange", 64, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> SCANNER_RANGE_OPTIONS = BUILDER
            .comment("Available scan range options that can be cycled with keybind")
            .defineList("200_scannerRangeOptions",
                    java.util.Arrays.asList(16, 24, 32, 64, 96),
                    obj -> obj instanceof Integer);

    private static final ModConfigSpec.IntValue SCANNER_DEFAULT_RANGE = BUILDER
            .comment("Default scan range value (must be one of the values in scannerRangeOptions)")
            .defineInRange("201_scannerDefaultRange", 32, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue SCANNER_MAX_BLOCKS = BUILDER
            .comment("Maximum number of blocks that can be scanned at once")
            .defineInRange("002_scannerMaxBlocks", 8192, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue SCANNER_BLOCKS_PER_TICK = BUILDER
            .comment("Block positions evaluated per server tick during an active scanner job (spreads large scans over multiple ticks)")
            .defineInRange("002b_scannerBlocksPerTick", 2048, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue SCANNER_MARKERS_PER_TICK = BUILDER
            .comment("Maximum highlight packets sent per server tick while a scanner job is running")
            .defineInRange("002c_scannerMarkersPerTick", 32, 1, 10000);

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

    private static final ModConfigSpec.ConfigValue<String> CLIENT_STRUCTURE_PATH = BUILDER
            .comment("Path to the client structures directory",
                    "Default: 'iska_utils_client/structures'",
                    "This folder is not a datapack; it is read from the filesystem on client side.")
            .define("001_client_structure_path", "iska_utils_client/structures");

    private static final ModConfigSpec.BooleanValue ACCEPT_CLIENT_STRUCTURE = BUILDER
            .comment("If true, the client will be able to use the structures saved by the server",
                    "Default: true")
            .define("002_accept_client_structure", true);

    public static final ModConfigSpec.BooleanValue ALLOW_CLIENT_STRUCTURE_PLAYER_LIKE = BUILDER
            .comment("If true, the client structure are placed like players",
                    "Default: true")
            .define("003_allow_client_structure_player_like", true);

    private static final ModConfigSpec.BooleanValue FTB_TEAMS_SYNC_ENABLED = BUILDER
            .comment("If true, IskaUtils shop teams are synchronized with FTB Teams when available",
                    "Default: true")
            .define("010_ftb_teams_sync_enabled", true);

    private static final ModConfigSpec.BooleanValue FACTORY_STONECUTTER_ENABLED = BUILDER
            .comment("If true, the Factory can process vanilla stonecutter recipes when no iska_utils:factory mapping exists.",
                    "Stonecutter recipes are not added to JEI Factory category.",
                    "Default: true")
            .define("004_factory_stonecutter_enabled", true);

    private static final ModConfigSpec.IntValue FACTORY_STONECUTTER_ENERGY_PER_OPERATION = BUILDER
            .comment("RF/FE consumed per stonecutter-style Factory operation (only when factory energy buffer is enabled).",
                    "Default: 5")
            .defineInRange("005_factory_stonecutter_energy_per_operation", 5, 0, Integer.MAX_VALUE);

    static {
        BUILDER.pop(); // End of dev category

        // Category for Evil Things (fun but fiery!)
        BUILDER.comment("Evil Things - Fun but Fiery!").push("evil_things");
    }

    private static final ModConfigSpec.BooleanValue BURNING_BRAZIER_SUPER_HOT = BUILDER
            .comment("If true, makes the Burning Brazier extra hot - when placing a flame, set on fire the player!",
                    "Also activates automatically if player has stage 'iska_utils_internal-curse_flame'")
            .define("000_burning_brazier_super_hot", false);

    private static final ModConfigSpec.BooleanValue BURNING_FLAME_SUPER_HOT = BUILDER
            .comment("If true, makes Burning Flame blocks extra hot - mobs & players touching them will catch fire!",
                    "Also activates automatically if world has stage 'iska_utils_internal-curse_flame'")
            .define("001_burning_flame_super_hot", false);

    private static final ModConfigSpec.BooleanValue GIFT_PLACE_HARD_ICE = BUILDER
            .comment("If true, breaking a Gift block will place a Hard Ice block after 3 seconds",
                    "Hard Ice is indestructible and cannot be broken")
            .define("100_gift_place_hard_ice", true);

    private static final ModConfigSpec.BooleanValue ENTROPIC_EGG_ALWAYS_CONSUME = BUILDER
            .comment("If true, using an Entropic Egg on a mob without a vanilla spawn egg still consumes one from the stack (no reward or buff). Valid mobs always consume one egg on use.")
            .define("200_000_always_consume_egg", false);

    private static final ModConfigSpec.BooleanValue ENTROPIC_EGG_APPLY_BUFF = BUILDER
            .comment("If true, a successful Entropic Egg use applies Entropic Empowerment to the mob.")
            .define("200_001_apply_entropic_buff", true);

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
    public static boolean hellfireIgniterVanillaLike;
    public static boolean verticalCharmEnabled;
    public static boolean horizontalCharmEnabled;
    public static int vectorCharmEnergyCapacity;
    public static java.util.List<Integer> vectorCharmEnergyConsume;
    public static int portableDislocatorEnergyCapacity;
    public static int portableDislocatorEnergyConsume;
    public static int portableDislocatorXpConsume;
    public static boolean portableDislocatorPrioritizeEnergy;
    public static boolean portableDislocatorPrioritizeXp;
    public static java.util.List<String> stickyFluids;
    public static int electricTreetapEnergyConsume;
    public static int electricTreetapEnergyBuffer;
    public static int rubberSapExtractorEnergyConsume;
    public static int rubberSapExtractorEnergyBuffer;
    public static int rubberSapExtractorSpeed;
    public static boolean generateRubberTrees;
    public static java.util.List<String> crudeOils;
    public static int scannerScanRange;
    public static java.util.List<Integer> scannerRangeOptions;
    public static int scannerDefaultRange;
    public static int scannerScanDuration;
    public static int scannerMaxBlocks;
    public static int scannerBlocksPerTick;
    public static int scannerMarkersPerTick;
    public static int scannerEnergyConsume;
    public static int scannerEnergyBuffer;
    public static int weatherAltererEnergyBuffer;
    public static int weatherAltererEnergyConsume;
    public static int timeAltererEnergyBuffer;
    public static int timeAltererEnergyConsume;
    public static int temporalOverclockerEnergyBuffer;
    public static int temporalOverclockerEnergyPerAcceleration;
    public static int temporalOverclockerMaxLinks;
    public static int temporalOverclockerAccelerationFactorMin;
    public static int temporalOverclockerAccelerationFactorMax;
    public static int temporalOverclockerAccelerationFactor;
    public static int temporalOverclockerLinkRange;
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
    public static int factoryEnergyBuffer;
    public static int entropyChargePerDrop;
    public static int entropyAncientTableMaxStored;
    public static int ancientTableMaxFuel;
    public static int ancientTableFuelPerDrop;
    public static int ancientTableCraftTicks;
    public static int ancientTableIoSlots;
    public static int arcaneDictionaryMaxStored;
    public static int arcaneDictionaryMinTraits;
    public static int arcaneDictionaryMaxTraits;
    public static int arcaneDictionaryMinLevel;
    public static int arcaneDictionaryMaxLevel;
    public static int arcaneDictionaryMaxRollLevels;
    public static double arcaneDictionaryRollCountExponent;
    public static double arcaneDictionaryRollLevelExponent;
    public static double arcaneGlassSkinReflectMultPerLevel;
    public static double arcaneGlassSkinSelfDamagePerLevel;
    public static double arcaneStoneSkinArmorPerLevel;
    public static double arcaneEntropyShellToughnessPerLevel;
    public static double arcaneEntropyShellHpPenalty;
    public static double arcaneAgilitySpeedMultPerLevel;
    public static double arcaneBloodLedgerLowHpRatio;
    public static double arcaneExecutionLineHpThreshold;
    public static double arcaneExecutionLineBonusDamageMult;
    public static double arcaneEntropyBladeArmorBypassPerLevel;
    public static double arcaneMartyrScriptDealtMultPerLevel;
    public static double arcaneMartyrScriptTakenMultPerLevel;
    public static double arcaneRecallOfKnowledgeXpMultPerLevel;
    public static double arcanePhaseMismatchDodgeChancePerLevel;
    public static int arcaneShiftingPowerMimicDurationSeconds;
    public static double arcaneEntropyOverflowConsumeReductionPerLevel;
    public static double arcaneEntropyOverflowHpPenaltyPerLevel;
    public static double arcaneVoidThornsDamagePerLevel;
    public static double arcaneLifeSiphonHealFractionPerLevel;
    public static double arcaneLifeSiphonHealCap;
    public static double arcaneIronRootKnockbackResistPerLevel;
    public static double arcaneQuickHandsAttackSpeedPerLevel;
    public static double arcaneEntropyFunnelBonusChargesPerLevel;
    public static double arcaneLastStandHpRatio;
    public static double arcaneGraveDebtHighHpRatio;
    public static double arcaneGraveDebtLowHpRatio;
    public static double arcaneGraveDebtSpeedMultPerLevel;
    public static double arcaneTierResonanceConsumeReductionPerTierPerLevel;
    public static boolean factoryStonecutterEnabled;
    public static int factoryStonecutterEnergyPerOp;
    public static int soundMufflerRangeMax;
    public static String clientStructurePath;
    public static boolean acceptClientStructure;
    public static boolean ftbTeamsSyncEnabled;
    public static boolean allowClientStructurePlayerLike;
    public static int blazingAltarMaxChunkRadius;
    public static int blazingAltarTickInterval;
    public static int blazingAltarPlacementsPerTick;
    public static int blazingAltarExclusionRadius;
    public static java.util.List<String> blazingAltarLightSensitiveEntries;
    public static int blazingAltarLightSensitiveMaxBlockLight;
    public static int blazingAltarRangeModuleChunkBonus;
    public static int blazingAltarRangeUpgradeMax;
    public static int blazingAltarExtinguishColumnsPerTick;

    public static boolean burningBrazierSuperHot;
    public static double greedyShieldBlockChance;
    public static double greedyShieldReduceChance;
    public static double greedyShieldReduceAmount;
    public static boolean greedyShieldInfo;
    public static boolean burningFlameSuperHot;
    public static boolean giftPlaceHardIce;
    public static boolean entropicEggAlwaysConsume;
    public static boolean entropicEggApplyBuff;
    public static int unstableEntropyCatalystDecayTicks;
    public static boolean unstableEntropyCatalystDecayKillsPlayer;
    public static java.util.List<Integer> unstableEntropyCatalystDecayTintColors;
    public static double gauntletClimbingSpeed;
    public static int chosenCheeseMax;
    public static double chosenCheeseHpPerLevel;
    public static double oldBrickArmorBonus;
    public static double sharpenedBoneArmorIgnoreChance;
    public static double entropicHelmetBaseHp;
    public static double entropicHelmetHpPerEntropicPiece;
    public static double entropicChestplateMissingHpPerStep;
    public static double entropicChestplateToughnessBonusPerStep;
    public static double entropicLeggingsMissingHpPerStep;
    public static double entropicLeggingsArmorBonusPerStep;
    public static boolean entropicBootsNegateFallDamage;
    public static int entropicPickaxeBonusFortuneLevels;
    public static double entropicArmorPenChance;
    public static double entropicArmorPenIgnoreFraction;
    public static boolean entropicHoeCropPowerEnabled;
    public static boolean entropicHoeRequireMatureCrop;
    public static boolean entropicAxeStripEnabled;
    public static boolean entropicShovelBrushEnabled;
    public static int entropicShovelBrushTicks;
    public static double theRootsBreakSpeedMinMultiplier;
    public static double theRootsBreakSpeedMaxBonus;
    public static double totemOfPainProcChance;
    public static int totemOfPainCurseDurationSeconds;
    public static double ritualGauntletCritChance;
    public static double ritualGauntletCritDamageBeneficialNeutral;
    public static double ritualGauntletCritDamageHarmful;
    public static double bustedCrownHpPerCursedArtifact;
    public static double curseOfPainDamagePerLevel;
    public static int theDeceptionAbsorptionDurationSeconds;
    public static double necroticCrystalHeartHpCostPerSave;
    public static double necroticCrystalHeartMinMaxHealth;
    public static double necroticCrystalHeartBaseHealth;
    public static int iceDiamondRepairIntervalTicks;
    public static double iceDiamondColdBiomeMaxTemp;
    public static double iceDiamondHotBiomeMinTemp;
    public static int iceDiamondColdRepairCost;
    public static int iceDiamondTemperateRepairCost;
    public static int iceDiamondHotRepairCost;
    public static double entropicRingDamagePer100Hp;
    public static double entropicRingApotheosisHavenMult;
    public static double entropicRingApotheosisFrontierMult;
    public static double entropicRingApotheosisAscentMult;
    public static double entropicRingApotheosisSummitMult;
    public static double entropicRingApotheosisPinnacleMult;
    public static double ancientStarArmorBonus;
    public static double ancientStarDamageBonus;
    public static double ancientStarToughnessBonus;
    public static double ancientStarHighHpRatio;
    public static double ancientStarLowHpRatio;
    public static double runicDiceAttackSpeedMin;
    public static double runicDiceAttackSpeedMax;
    public static int runicDiceRerollTicks;
    public static double entropicClockMaxFactorMultiplier;
    public static int entropicClockEntropyPerTick;
    public static int entropicClockMaxStored;

    // Entropic & graveyard soils
    public static boolean entropicSoilSpawnEnabled;
    public static int entropicSoilSpawnIntervalMinTicks;
    public static int entropicSoilSpawnIntervalMaxTicks;
    public static java.util.List<String> entropicSoilSpawnAllow;
    public static java.util.List<String> entropicSoilSpawnDeny;
    public static boolean entropicSoilRedstoneAccelEnabled;
    public static int entropicSoilRedstoneAccelMinTicks;
    public static int entropicSoilRedstoneAccelMaxTicks;
    public static int entropicSoilAccelMobCap;
    public static int entropicSoilSpawnMaxHealth;
    public static double entropicEmpowermentDamageBonus;
    public static double entropicEmpowermentDamageReduction;
    public static int entropicSoilSlowSpreadChance;
    public static int entropicSoilEdgeSpreadChance;
    public static int entropicSoilDirtSpreadChance;
    public static int entropicSoilToDirtMinTicks;
    public static int entropicSoilToDirtMaxTicks;
    public static int entropicDirtToVanillaMinTicks;
    public static int entropicDirtToVanillaMaxTicks;
    public static int entropicAgglomerationSpreadIntervalTicks;
    public static int graveyardSoilHealIntervalMinTicks;
    public static int graveyardSoilHealIntervalMaxTicks;
    public static double graveyardSoilHealAmount;

    public static boolean druidicPodzolSpawnEnabled;
    public static int druidicPodzolSpawnIntervalMinTicks;
    public static int druidicPodzolSpawnIntervalMaxTicks;
    public static java.util.List<String> druidicPodzolSpawnAllow;
    public static java.util.List<String> druidicPodzolSpawnDeny;
    public static boolean druidicPodzolRedstoneAccelEnabled;
    public static int druidicPodzolRedstoneAccelMinTicks;
    public static int druidicPodzolRedstoneAccelMaxTicks;
    public static int druidicPodzolAccelMobCap;
    public static int druidicPodzolSpawnMaxHealth;
    public static int druidicPodzolSlowSpreadChance;
    public static int druidicAgglomerationSpreadIntervalTicks;

    // Fan configuration
    public static int fanRangeHorizontalMax;
    public static int fanRangeVerticalMax;
    public static int fanRangeFrontMax;
    public static double fanDefaultPower;
    public static int fanRangeUpgradeMax;
    public static int fanAccelerationUpgradeMax;
    public static java.util.List<Double> fanAccelerationModulePowers;
    public static boolean fanGhostModuleBypassUnbreakable;
    public static int fanpackEnergyCapacity;
    public static int fanpackFlightEnergyConsume;

    // Mob Reaper configuration
    public static double reaperDefaultDamage;
    public static int reaperAttackIntervalTicks;
    public static int reaperHitDepth;
    public static int reaperNormalUpgradeMax;
    public static double reaperNormalBonusPerModule;
    public static int reaperLethalUpgradeMax;
    public static double reaperLethalDamage;
    public static int reaperEnchantUpgradeMax;
    public static int reaperBeheadingUpgradeMax;
    public static double reaperBeheadingChancePerLevel;
    public static int reaperLuckUpgradeMax;
    public static int reaperExperienceUpgradeMax;
    public static double reaperExperienceBonusPerLevel;
    public static double reaperBladeMaxDegPerTick;

    public static int collectingCrateXpCapacityLevels;
    public static int collectingCrateXpMbPerPoint;
    public static int collectingCrateBaseRange;
    public static int collectingCrateMaxRange;
    public static int collectingCrateMaxInsertionsPerTick;
    public static int collectingCrateCollectionIntervalTicks;
    public static int collectingCrateStorageSlots;
    public static int collectingCrateRangeUpgradeMax;
    
    public static java.util.List<String> deepDrawersAllow;
    public static java.util.List<String> deepDrawersDeny;
    public static int deepDrawerExtractorInterval;
    public static int deepDrawerExtractorMaxFilters;
    
    public static java.util.List<String> dollyWhitelist;
    public static java.util.List<String> dollyBlacklist;
    public static java.util.List<String> dollyAllowedMiningLevelTags;
    public static boolean dollyCanMoveAllUnbreakable;
    public static java.util.List<String> dollyUnbreakableWhitelist;
    public static java.util.List<String> dollyUnbreakableBlacklist;
    public static java.util.List<String> hardDollyWhitelist;
    public static java.util.List<String> hardDollyBlacklist;
    public static java.util.List<String> hardDollyAllowedMiningLevelTags;
    public static boolean hardDollyCanMoveAllUnbreakable;
    public static java.util.List<String> hardDollyUnbreakableWhitelist;
    public static java.util.List<String> hardDollyUnbreakableBlacklist;
    
    public static boolean creativeDollyCanMoveAllUnbreakable;
    public static java.util.List<String> creativeDollyUnbreakableWhitelist;
    public static java.util.List<String> creativeDollyUnbreakableBlacklist;
    public static int dollyInfoLines;
    public static int hardDollyInfoLines;
    public static int creativeDollyInfoLines;

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
        hellfireIgniterVanillaLike = HELLFIRE_IGNITER_VANILLA_LIKE.get();
        portableDislocatorEnergyCapacity = PORTABLE_DISLOCATOR_ENERGY_CAPACITY.get();
        portableDislocatorEnergyConsume = PORTABLE_DISLOCATOR_ENERGY_CONSUME.get();
        portableDislocatorXpConsume = PORTABLE_DISLOCATOR_XP_CONSUME.get();
        portableDislocatorPrioritizeEnergy = PORTABLE_DISLOCATOR_PRIORITIZE_ENERGY.get();
        portableDislocatorPrioritizeXp = PORTABLE_DISLOCATOR_PRIORITIZE_XP.get();
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
        factoryEnergyBuffer = FACTORY_ENERGY_BUFFER.get();
        entropyChargePerDrop = ENTROPY_CHARGE_PER_DROP.get();
        entropyAncientTableMaxStored = ENTROPY_ANCIENT_TABLE_MAX_STORED.get();
        ancientTableMaxFuel = entropyAncientTableMaxStored;
        ancientTableFuelPerDrop = entropyChargePerDrop;
        ancientTableCraftTicks = ANCIENT_TABLE_CRAFT_TICKS.get();
        ancientTableIoSlots = ANCIENT_TABLE_IO_SLOTS.get();
        arcaneDictionaryMaxStored = ARCANE_DICTIONARY_MAX_STORED.get();
        arcaneDictionaryMinTraits = ARCANE_DICTIONARY_MIN_TRAITS.get();
        arcaneDictionaryMaxTraits = ARCANE_DICTIONARY_MAX_TRAITS.get();
        arcaneDictionaryMinLevel = ARCANE_DICTIONARY_MIN_LEVEL.get();
        arcaneDictionaryMaxLevel = ARCANE_DICTIONARY_MAX_LEVEL.get();
        arcaneDictionaryMaxRollLevels = ARCANE_DICTIONARY_MAX_ROLL_LEVELS.get();
        arcaneDictionaryRollCountExponent = ARCANE_DICTIONARY_ROLL_COUNT_EXPONENT.get();
        arcaneDictionaryRollLevelExponent = ARCANE_DICTIONARY_ROLL_LEVEL_EXPONENT.get();
        arcaneGlassSkinReflectMultPerLevel = ARCANE_GLASS_SKIN_REFLECT_MULT.get();
        arcaneGlassSkinSelfDamagePerLevel = ARCANE_GLASS_SKIN_SELF_DAMAGE.get();
        arcaneStoneSkinArmorPerLevel = ARCANE_STONE_SKIN_ARMOR.get();
        arcaneEntropyShellToughnessPerLevel = ARCANE_ENTROPY_SHELL_TOUGHNESS.get();
        arcaneEntropyShellHpPenalty = ARCANE_ENTROPY_SHELL_HP_PENALTY.get();
        arcaneAgilitySpeedMultPerLevel = ARCANE_AGILITY_SPEED_MULT.get();
        arcaneBloodLedgerLowHpRatio = ARCANE_BLOOD_LEDGER_LOW_HP_RATIO.get();
        arcaneExecutionLineHpThreshold = ARCANE_EXECUTION_LINE_HP_THRESHOLD.get();
        arcaneExecutionLineBonusDamageMult = ARCANE_EXECUTION_LINE_BONUS_MULT.get();
        arcaneEntropyBladeArmorBypassPerLevel = ARCANE_ENTROPY_BLADE_BYPASS.get();
        arcaneMartyrScriptDealtMultPerLevel = ARCANE_MARTYR_SCRIPT_DEALT.get();
        arcaneMartyrScriptTakenMultPerLevel = ARCANE_MARTYR_SCRIPT_TAKEN.get();
        arcaneRecallOfKnowledgeXpMultPerLevel = ARCANE_RECALL_XP_MULT.get();
        arcanePhaseMismatchDodgeChancePerLevel = ARCANE_PHASE_MISMATCH_DODGE.get();
        arcaneShiftingPowerMimicDurationSeconds = ARCANE_SHIFTING_POWER_MIMIC_SECONDS.get();
        arcaneEntropyOverflowConsumeReductionPerLevel = ARCANE_ENTROPY_OVERFLOW_CONSUME_REDUCTION.get();
        arcaneEntropyOverflowHpPenaltyPerLevel = ARCANE_ENTROPY_OVERFLOW_HP_PENALTY.get();
        arcaneVoidThornsDamagePerLevel = ARCANE_VOID_THORNS_DAMAGE.get();
        arcaneLifeSiphonHealFractionPerLevel = ARCANE_LIFE_SIPHON_HEAL_FRACTION.get();
        arcaneLifeSiphonHealCap = ARCANE_LIFE_SIPHON_HEAL_CAP.get();
        arcaneIronRootKnockbackResistPerLevel = ARCANE_IRON_ROOT_KNOCKBACK_RESIST.get();
        arcaneQuickHandsAttackSpeedPerLevel = ARCANE_QUICK_HANDS_ATTACK_SPEED.get();
        arcaneEntropyFunnelBonusChargesPerLevel = ARCANE_ENTROPY_FUNNEL_BONUS_CHARGES.get();
        arcaneLastStandHpRatio = ARCANE_LAST_STAND_HP_RATIO.get();
        arcaneGraveDebtHighHpRatio = ARCANE_GRAVE_DEBT_HIGH_HP_RATIO.get();
        arcaneGraveDebtLowHpRatio = ARCANE_GRAVE_DEBT_LOW_HP_RATIO.get();
        arcaneGraveDebtSpeedMultPerLevel = ARCANE_GRAVE_DEBT_SPEED_MULT.get();
        arcaneTierResonanceConsumeReductionPerTierPerLevel = ARCANE_TIER_RESONANCE_CONSUME_REDUCTION.get();
        factoryStonecutterEnabled = FACTORY_STONECUTTER_ENABLED.get();
        factoryStonecutterEnergyPerOp = FACTORY_STONECUTTER_ENERGY_PER_OPERATION.get();
        soundMufflerRangeMax = SOUND_MUFFLER_RANGE_MAX.get();

        // Client Structure Path logic
        clientStructurePath = CLIENT_STRUCTURE_PATH.get();
        acceptClientStructure = ACCEPT_CLIENT_STRUCTURE.get();
        ftbTeamsSyncEnabled = FTB_TEAMS_SYNC_ENABLED.get();
        allowClientStructurePlayerLike = ALLOW_CLIENT_STRUCTURE_PLAYER_LIKE.get();

        blazingAltarMaxChunkRadius = BLAZING_ALTAR_MAX_CHUNK_RADIUS.get();
        blazingAltarTickInterval = BLAZING_ALTAR_TICK_INTERVAL.get();
        blazingAltarPlacementsPerTick = BLAZING_ALTAR_PLACEMENTS_PER_TICK.get();
        blazingAltarExclusionRadius = BLAZING_ALTAR_EXCLUSION_RADIUS.get();
        blazingAltarLightSensitiveEntries = new java.util.ArrayList<>(BLAZING_ALTAR_LIGHT_SENSITIVE_ENTRIES.get());
        blazingAltarLightSensitiveMaxBlockLight = BLAZING_ALTAR_LIGHT_SENSITIVE_MAX_BLOCK_LIGHT.get();
        blazingAltarRangeModuleChunkBonus = BLAZING_ALTAR_RANGE_MODULE_CHUNK_BONUS.get();
        blazingAltarRangeUpgradeMax = BLAZING_ALTAR_RANGE_UPGRADE_MAX.get();
        blazingAltarExtinguishColumnsPerTick = BLAZING_ALTAR_EXTINGUISH_COLUMNS_PER_TICK.get();

        // Evil Things configuration
        burningBrazierSuperHot = BURNING_BRAZIER_SUPER_HOT.get();
        greedyShieldBlockChance = GREEDY_SHIELD_BLOCK_CHANCE.get();
        greedyShieldReduceChance = GREEDY_SHIELD_REDUCE_CHANCE.get();
        greedyShieldReduceAmount = GREEDY_SHIELD_REDUCE_AMOUNT.get();
        greedyShieldInfo = GREEDY_SHIELD_INFO.get();
        burningFlameSuperHot = BURNING_FLAME_SUPER_HOT.get();
        giftPlaceHardIce = GIFT_PLACE_HARD_ICE.get();
        entropicEggAlwaysConsume = ENTROPIC_EGG_ALWAYS_CONSUME.get();
        entropicEggApplyBuff = ENTROPIC_EGG_APPLY_BUFF.get();
        unstableEntropyCatalystDecayTicks = UNSTABLE_ENTROPY_CATALYST_DECAY_TICKS.get();
        unstableEntropyCatalystDecayTintColors = parseHexRgbList(UNSTABLE_ENTROPY_CATALYST_DECAY_TINTS.get(), java.util.List.of(0xFF0000));
        unstableEntropyCatalystDecayKillsPlayer = UNSTABLE_ENTROPY_CATALYST_DECAY_KILLS_PLAYER.get();

        // Fan configuration
        fanRangeHorizontalMax = FAN_RANGE_HORIZONTAL_MAX.get();
        fanRangeVerticalMax = FAN_RANGE_VERTICAL_MAX.get();
        fanRangeFrontMax = FAN_RANGE_FRONT_MAX.get();
        fanDefaultPower = FAN_DEFAULT_POWER.get();
        fanRangeUpgradeMax = FAN_RANGE_UPGRADE_MAX.get();
        fanAccelerationUpgradeMax = FAN_ACCELERATION_UPGRADE_MAX.get();
        fanAccelerationModulePowers = new java.util.ArrayList<>(FAN_ACCELERATION_MODULE_POWERS.get().stream()
                .map(Double::doubleValue)
                .toList());
        fanGhostModuleBypassUnbreakable = FAN_GHOST_MODULE_BYPASS_UNBREAKABLE.get();
        
        // Fanpack configuration
        fanpackEnergyCapacity = FANPACK_ENERGY_CAPACITY.get();
        fanpackFlightEnergyConsume = FANPACK_FLIGHT_ENERGY_CONSUME.get();

        // Mob Reaper configuration
        reaperDefaultDamage = REAPER_DEFAULT_DAMAGE.get();
        reaperAttackIntervalTicks = REAPER_ATTACK_INTERVAL_TICKS.get();
        reaperHitDepth = REAPER_HIT_DEPTH.get();
        reaperNormalUpgradeMax = REAPER_NORMAL_UPGRADE_MAX.get();
        reaperNormalBonusPerModule = REAPER_NORMAL_BONUS_PER_MODULE.get();
        reaperLethalUpgradeMax = REAPER_LETHAL_UPGRADE_MAX.get();
        reaperLethalDamage = REAPER_LETHAL_DAMAGE.get();
        reaperEnchantUpgradeMax = REAPER_ENCHANT_UPGRADE_MAX.get();
        reaperBeheadingUpgradeMax = REAPER_BEHEADING_UPGRADE_MAX.get();
        reaperBeheadingChancePerLevel = REAPER_BEHEADING_CHANCE_PER_LEVEL.get();
        reaperLuckUpgradeMax = REAPER_LUCK_UPGRADE_MAX.get();
        reaperExperienceUpgradeMax = REAPER_EXPERIENCE_UPGRADE_MAX.get();
        reaperExperienceBonusPerLevel = REAPER_EXPERIENCE_BONUS_PER_LEVEL.get();
        reaperBladeMaxDegPerTick = REAPER_BLADE_MAX_DEG_PER_TICK.get();

        collectingCrateXpCapacityLevels = COLLECTING_CRATE_XP_CAPACITY_LEVELS.get();
        collectingCrateXpMbPerPoint = COLLECTING_CRATE_XP_MB_PER_POINT.get();
        collectingCrateBaseRange = COLLECTING_CRATE_BASE_RANGE.get();
        collectingCrateMaxRange = COLLECTING_CRATE_MAX_RANGE.get();
        collectingCrateMaxInsertionsPerTick = COLLECTING_CRATE_MAX_INSERTIONS_PER_TICK.get();
        collectingCrateCollectionIntervalTicks = COLLECTING_CRATE_COLLECTION_INTERVAL_TICKS.get();
        collectingCrateStorageSlots = COLLECTING_CRATE_STORAGE_SLOTS.get();
        collectingCrateRangeUpgradeMax = COLLECTING_CRATE_RANGE_UPGRADE_MAX.get();
        
        // Deep Drawers configuration
        deepDrawersAllow = new java.util.ArrayList<>(DEEP_DRAWERS_ALLOW.get());
        deepDrawersDeny = new java.util.ArrayList<>(DEEP_DRAWERS_DENY.get());
        chosenCheeseMax = CHOSEN_CHEESE_MAX.get();
        chosenCheeseHpPerLevel = CHOSEN_CHEESE_HP_PER_LEVEL.get();
        oldBrickArmorBonus = OLD_BRICK_ARMOR_BONUS.get();
        sharpenedBoneArmorIgnoreChance = SHARPENED_BONE_ARMOR_IGNORE_CHANCE.get();
        entropicHelmetBaseHp = ENTROPIC_HELMET_BASE_HP.get();
        entropicHelmetHpPerEntropicPiece = ENTROPIC_HELMET_HP_PER_PIECE.get();
        entropicChestplateMissingHpPerStep = ENTROPIC_CHEST_MISSING_HP_PER_STEP.get();
        entropicChestplateToughnessBonusPerStep = ENTROPIC_CHEST_TOUGHNESS_BONUS_PER_STEP.get();
        entropicLeggingsMissingHpPerStep = ENTROPIC_LEGGINGS_MISSING_HP_PER_STEP.get();
        entropicLeggingsArmorBonusPerStep = ENTROPIC_LEGGINGS_ARMOR_BONUS_PER_STEP.get();
        entropicBootsNegateFallDamage = ENTROPIC_BOOTS_NEGATE_FALL.get();
        entropicPickaxeBonusFortuneLevels = ENTROPIC_PICKAXE_BONUS_FORTUNE_LEVELS.get();
        entropicArmorPenChance = ENTROPIC_ARMOR_PEN_CHANCE.get();
        entropicArmorPenIgnoreFraction = ENTROPIC_ARMOR_PEN_IGNORE.get();
        entropicHoeCropPowerEnabled = ENTROPIC_HOE_CROP_ENABLED.get();
        entropicHoeRequireMatureCrop = ENTROPIC_HOE_REQUIRE_MATURE.get();
        entropicAxeStripEnabled = ENTROPIC_AXE_STRIP_ENABLED.get();
        entropicShovelBrushEnabled = ENTROPIC_SHOVEL_BRUSH_ENABLED.get();
        entropicShovelBrushTicks = ENTROPIC_SHOVEL_BRUSH_TICKS.get();
        theRootsBreakSpeedMinMultiplier = THE_ROOTS_BREAK_SPEED_MIN_MULTIPLIER.get();
        theRootsBreakSpeedMaxBonus = THE_ROOTS_BREAK_SPEED_MAX_BONUS.get();
        totemOfPainProcChance = TOTEM_OF_PAIN_PROC_CHANCE.get();
        totemOfPainCurseDurationSeconds = TOTEM_OF_PAIN_CURSE_DURATION_SECONDS.get();
        ritualGauntletCritChance = RITUAL_GAUNTLET_CRIT_CHANCE.get();
        ritualGauntletCritDamageBeneficialNeutral = RITUAL_GAUNTLET_CRIT_DAMAGE_BENEFICIAL_NEUTRAL.get();
        ritualGauntletCritDamageHarmful = RITUAL_GAUNTLET_CRIT_DAMAGE_HARMFUL.get();
        bustedCrownHpPerCursedArtifact = BUSTED_CROWN_HP_PER_CURSED_ARTIFACT.get();
        curseOfPainDamagePerLevel = CURSE_OF_PAIN_DAMAGE_PER_LEVEL.get();
        theDeceptionAbsorptionDurationSeconds = THE_DECEPTION_ABSORPTION_DURATION_SECONDS.get();
        necroticCrystalHeartHpCostPerSave = NECROTIC_CRYSTAL_HEART_HP_COST_PER_SAVE.get();
        necroticCrystalHeartMinMaxHealth = NECROTIC_CRYSTAL_HEART_MIN_MAX_HEALTH.get();
        necroticCrystalHeartBaseHealth = NECROTIC_CRYSTAL_HEART_BASE_HEALTH.get();
        iceDiamondRepairIntervalTicks = ICE_DIAMOND_REPAIR_INTERVAL_TICKS.get();
        iceDiamondColdBiomeMaxTemp = ICE_DIAMOND_COLD_BIOME_MAX_TEMP.get();
        iceDiamondHotBiomeMinTemp = ICE_DIAMOND_HOT_BIOME_MIN_TEMP.get();
        iceDiamondColdRepairCost = ICE_DIAMOND_COLD_REPAIR_COST.get();
        iceDiamondTemperateRepairCost = ICE_DIAMOND_TEMPERATE_REPAIR_COST.get();
        iceDiamondHotRepairCost = ICE_DIAMOND_HOT_REPAIR_COST.get();
        entropicRingDamagePer100Hp = ENTROPIC_RING_DAMAGE_PER_100_HP.get();
        entropicRingApotheosisHavenMult = ENTROPIC_RING_APOTHEOSIS_HAVEN_MULT.get();
        entropicRingApotheosisFrontierMult = ENTROPIC_RING_APOTHEOSIS_FRONTIER_MULT.get();
        entropicRingApotheosisAscentMult = ENTROPIC_RING_APOTHEOSIS_ASCENT_MULT.get();
        entropicRingApotheosisSummitMult = ENTROPIC_RING_APOTHEOSIS_SUMMIT_MULT.get();
        entropicRingApotheosisPinnacleMult = ENTROPIC_RING_APOTHEOSIS_PINNACLE_MULT.get();
        ancientStarArmorBonus = ANCIENT_STAR_ARMOR_BONUS.get();
        ancientStarDamageBonus = ANCIENT_STAR_DAMAGE_BONUS.get();
        ancientStarToughnessBonus = ANCIENT_STAR_TOUGHNESS_BONUS.get();
        ancientStarHighHpRatio = ANCIENT_STAR_HIGH_HP_RATIO.get();
        ancientStarLowHpRatio = ANCIENT_STAR_LOW_HP_RATIO.get();
        runicDiceAttackSpeedMin = RUNIC_DICE_ATTACK_SPEED_MIN.get();
        runicDiceAttackSpeedMax = RUNIC_DICE_ATTACK_SPEED_MAX.get();
        runicDiceRerollTicks = RUNIC_DICE_REROLL_TICKS.get();
        deepDrawerExtractorInterval = DEEP_DRAWER_EXTRACTOR_INTERVAL.get();
        deepDrawerExtractorMaxFilters = DEEP_DRAWER_EXTRACTOR_MAX_FILTERS.get();
        
        // Dolly configuration
        dollyWhitelist = new java.util.ArrayList<>(DOLLY_WHITELIST.get());
        dollyBlacklist = new java.util.ArrayList<>(DOLLY_BLACKLIST.get());
        dollyAllowedMiningLevelTags = new java.util.ArrayList<>(DOLLY_ALLOWED_MINING_LEVEL_TAGS.get());
        dollyCanMoveAllUnbreakable = DOLLY_CAN_MOVE_ALL_UNBREAKABLE.get();
        dollyUnbreakableWhitelist = new java.util.ArrayList<>(DOLLY_UNBREAKABLE_WHITELIST.get());
        dollyUnbreakableBlacklist = new java.util.ArrayList<>(DOLLY_UNBREAKABLE_BLACKLIST.get());
        
        // Hard Dolly configuration
        hardDollyWhitelist = new java.util.ArrayList<>(HARD_DOLLY_WHITELIST.get());
        hardDollyBlacklist = new java.util.ArrayList<>(HARD_DOLLY_BLACKLIST.get());
        hardDollyAllowedMiningLevelTags = new java.util.ArrayList<>(HARD_DOLLY_ALLOWED_MINING_LEVEL_TAGS.get());
        hardDollyCanMoveAllUnbreakable = HARD_DOLLY_CAN_MOVE_ALL_UNBREAKABLE.get();
        hardDollyUnbreakableWhitelist = new java.util.ArrayList<>(HARD_DOLLY_UNBREAKABLE_WHITELIST.get());
        hardDollyUnbreakableBlacklist = new java.util.ArrayList<>(HARD_DOLLY_UNBREAKABLE_BLACKLIST.get());
        
        // Creative Dolly configuration
        creativeDollyCanMoveAllUnbreakable = CREATIVE_DOLLY_CAN_MOVE_ALL_UNBREAKABLE.get();
        creativeDollyUnbreakableWhitelist = new java.util.ArrayList<>(CREATIVE_DOLLY_UNBREAKABLE_WHITELIST.get());
        creativeDollyUnbreakableBlacklist = new java.util.ArrayList<>(CREATIVE_DOLLY_UNBREAKABLE_BLACKLIST.get());
        
        // Dolly info lines configuration
        dollyInfoLines = DOLLY_INFO_LINES.get();
        hardDollyInfoLines = HARD_DOLLY_INFO_LINES.get();
        creativeDollyInfoLines = CREATIVE_DOLLY_INFO_LINES.get();

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
        generateRubberTrees = GENERATE_RUBBER_TREES.get();
        
        scannerScanRange = SCANNER_SCAN_RANGE.get(); // Deprecated, kept for backward compatibility
        scannerRangeOptions = new java.util.ArrayList<>(SCANNER_RANGE_OPTIONS.get());
        scannerDefaultRange = SCANNER_DEFAULT_RANGE.get();
        // Ensure default range is in the options array
        if (scannerRangeOptions != null && !scannerRangeOptions.isEmpty()) {
            if (!scannerRangeOptions.contains(scannerDefaultRange)) {
                // If default is not in options, use the first option
                scannerDefaultRange = scannerRangeOptions.get(0);
            }
        }
        scannerScanDuration = SCANNER_SCAN_DURATION.get();
        scannerMaxBlocks = SCANNER_MAX_BLOCKS.get();
        scannerBlocksPerTick = SCANNER_BLOCKS_PER_TICK.get();
        scannerMarkersPerTick = SCANNER_MARKERS_PER_TICK.get();
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
        
        // Temporal Overclocker logic
        temporalOverclockerEnergyBuffer = TEMPORAL_OVERCLOCKER_ENERGY_BUFFER.get();
        temporalOverclockerEnergyPerAcceleration = TEMPORAL_OVERCLOCKER_ENERGY_PER_ACCELERATION.get();
        temporalOverclockerMaxLinks = TEMPORAL_OVERCLOCKER_MAX_LINKS.get();
        temporalOverclockerAccelerationFactorMin = TEMPORAL_OVERCLOCKER_ACCELERATION_FACTOR_MIN.get();
        temporalOverclockerAccelerationFactorMax = TEMPORAL_OVERCLOCKER_ACCELERATION_FACTOR_MAX.get();
        temporalOverclockerAccelerationFactor = TEMPORAL_OVERCLOCKER_ACCELERATION_FACTOR.get();
        temporalOverclockerLinkRange = TEMPORAL_OVERCLOCKER_LINK_RANGE.get();
        entropicClockMaxFactorMultiplier = ENTROPIC_CLOCK_MAX_FACTOR_MULTIPLIER.get();
        entropicClockEntropyPerTick = ENTROPIC_CLOCK_ENTROPY_PER_TICK.get();
        entropicClockMaxStored = ENTROPIC_CLOCK_MAX_STORED.get();
        if (entropicClockMaxStored <= 0) {
            entropicClockMaxStored = entropyAncientTableMaxStored;
        }

        entropicSoilSpawnEnabled = ENTROPIC_SOIL_SPAWN_ENABLED.get();
        entropicSoilSpawnIntervalMinTicks = ENTROPIC_SOIL_SPAWN_INTERVAL_MIN.get();
        entropicSoilSpawnIntervalMaxTicks = ENTROPIC_SOIL_SPAWN_INTERVAL_MAX.get();
        if (entropicSoilSpawnIntervalMaxTicks < entropicSoilSpawnIntervalMinTicks) {
            entropicSoilSpawnIntervalMaxTicks = entropicSoilSpawnIntervalMinTicks;
        }
        entropicSoilSpawnAllow = new java.util.ArrayList<>(ENTROPIC_SOIL_SPAWN_ALLOW.get());
        entropicSoilSpawnDeny = new java.util.ArrayList<>(ENTROPIC_SOIL_SPAWN_DENY.get());
        entropicSoilRedstoneAccelEnabled = ENTROPIC_SOIL_REDSTONE_ACCEL_ENABLED.get();
        entropicSoilRedstoneAccelMinTicks = ENTROPIC_SOIL_REDSTONE_ACCEL_MIN.get();
        entropicSoilRedstoneAccelMaxTicks = ENTROPIC_SOIL_REDSTONE_ACCEL_MAX.get();
        if (entropicSoilRedstoneAccelMaxTicks < entropicSoilRedstoneAccelMinTicks) {
            entropicSoilRedstoneAccelMaxTicks = entropicSoilRedstoneAccelMinTicks;
        }
        entropicSoilAccelMobCap = ENTROPIC_SOIL_ACCEL_MOB_CAP.get();
        entropicSoilSpawnMaxHealth = ENTROPIC_SOIL_SPAWN_MAX_HEALTH.get();
        entropicEmpowermentDamageBonus = ENTROPIC_EMPOWERMENT_DAMAGE_BONUS.get();
        entropicEmpowermentDamageReduction = ENTROPIC_EMPOWERMENT_DAMAGE_REDUCTION.get();
        entropicSoilSlowSpreadChance = ENTROPIC_SOIL_SLOW_SPREAD_CHANCE.get();
        entropicSoilEdgeSpreadChance = ENTROPIC_SOIL_EDGE_SPREAD_CHANCE.get();
        entropicSoilDirtSpreadChance = ENTROPIC_SOIL_DIRT_SPREAD_CHANCE.get();
        entropicSoilToDirtMinTicks = ENTROPIC_SOIL_TO_DIRT_MIN.get();
        entropicSoilToDirtMaxTicks = ENTROPIC_SOIL_TO_DIRT_MAX.get();
        if (entropicSoilToDirtMaxTicks < entropicSoilToDirtMinTicks) {
            entropicSoilToDirtMaxTicks = entropicSoilToDirtMinTicks;
        }
        entropicDirtToVanillaMinTicks = ENTROPIC_DIRT_TO_VANILLA_MIN.get();
        entropicDirtToVanillaMaxTicks = ENTROPIC_DIRT_TO_VANILLA_MAX.get();
        if (entropicDirtToVanillaMaxTicks < entropicDirtToVanillaMinTicks) {
            entropicDirtToVanillaMaxTicks = entropicDirtToVanillaMinTicks;
        }
        entropicAgglomerationSpreadIntervalTicks = ENTROPIC_AGGLOMERATION_SPREAD_INTERVAL.get();
        graveyardSoilHealIntervalMinTicks = GRAVEYARD_SOIL_HEAL_INTERVAL_MIN.get();
        graveyardSoilHealIntervalMaxTicks = GRAVEYARD_SOIL_HEAL_INTERVAL_MAX.get();
        if (graveyardSoilHealIntervalMaxTicks < graveyardSoilHealIntervalMinTicks) {
            graveyardSoilHealIntervalMaxTicks = graveyardSoilHealIntervalMinTicks;
        }
        graveyardSoilHealAmount = GRAVEYARD_SOIL_HEAL_AMOUNT.get();

        druidicPodzolSpawnEnabled = DRUIDIC_PODZOL_SPAWN_ENABLED.get();
        druidicPodzolSpawnIntervalMinTicks = DRUIDIC_PODZOL_SPAWN_INTERVAL_MIN.get();
        druidicPodzolSpawnIntervalMaxTicks = DRUIDIC_PODZOL_SPAWN_INTERVAL_MAX.get();
        if (druidicPodzolSpawnIntervalMaxTicks < druidicPodzolSpawnIntervalMinTicks) {
            druidicPodzolSpawnIntervalMaxTicks = druidicPodzolSpawnIntervalMinTicks;
        }
        druidicPodzolSpawnAllow = new java.util.ArrayList<>(DRUIDIC_PODZOL_SPAWN_ALLOW.get());
        druidicPodzolSpawnDeny = new java.util.ArrayList<>(DRUIDIC_PODZOL_SPAWN_DENY.get());
        druidicPodzolRedstoneAccelEnabled = DRUIDIC_PODZOL_REDSTONE_ACCEL_ENABLED.get();
        druidicPodzolRedstoneAccelMinTicks = DRUIDIC_PODZOL_REDSTONE_ACCEL_MIN.get();
        druidicPodzolRedstoneAccelMaxTicks = DRUIDIC_PODZOL_REDSTONE_ACCEL_MAX.get();
        if (druidicPodzolRedstoneAccelMaxTicks < druidicPodzolRedstoneAccelMinTicks) {
            druidicPodzolRedstoneAccelMaxTicks = druidicPodzolRedstoneAccelMinTicks;
        }
        druidicPodzolAccelMobCap = DRUIDIC_PODZOL_ACCEL_MOB_CAP.get();
        druidicPodzolSpawnMaxHealth = DRUIDIC_PODZOL_SPAWN_MAX_HEALTH.get();
        druidicPodzolSlowSpreadChance = DRUIDIC_PODZOL_SLOW_SPREAD_CHANCE.get();
        druidicAgglomerationSpreadIntervalTicks = DRUIDIC_AGGLOMERATION_SPREAD_INTERVAL.get();

        // If the energy per acceleration is 0, the energy stored is 0 automatically
        if (temporalOverclockerEnergyPerAcceleration <= 0) {
            temporalOverclockerEnergyBuffer = 0;
        }
        
        // If the energy stored is 0, the energy consumption is disabled
        if (temporalOverclockerEnergyBuffer <= 0) {
            temporalOverclockerEnergyPerAcceleration = 0;
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
        
        // Gauntlet of Climbing logic
        gauntletClimbingSpeed = GAUNTLET_CLIMBING_SPEED.get();
    }

    private static int parseHexRgb(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        String s = hex.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        if (s.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseUnsignedInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static java.util.List<Integer> parseHexRgbList(String raw, java.util.List<Integer> fallback) {
        if (raw == null) {
            return fallback;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return fallback;
        }
        java.util.ArrayList<Integer> out = new java.util.ArrayList<>();
        for (String part : s.split(";")) {
            if (part == null) {
                continue;
            }
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            int rgb = parseHexRgb(p, -1);
            if (rgb != -1) {
                out.add(rgb & 0xFFFFFF);
            }
        }
        return out.isEmpty() ? fallback : java.util.Collections.unmodifiableList(out);
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        onLoad(event);
    }
}

