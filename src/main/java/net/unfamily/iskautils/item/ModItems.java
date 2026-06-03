package net.unfamily.iskautils.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.fluid.ModFluids;
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
import net.unfamily.iskautils.item.custom.TranslatedTooltipBlockItem;
import net.unfamily.iskautils.item.custom.FanBlockItem;
import net.unfamily.iskautils.item.custom.ShopBlockItem;
import net.unfamily.iskautils.item.custom.HellfireIgniterBlockItem;
import net.unfamily.iskautils.item.custom.RubberSapExtractorBlockItem;
import net.unfamily.iskautils.item.custom.StructurePlacerMachineBlockItem;
import net.unfamily.iskautils.item.custom.StructureSaverMachineBlockItem;
import net.unfamily.iskautils.item.custom.RangeModuleItem;
import net.unfamily.iskautils.item.custom.NormalDamageModuleItem;
import net.unfamily.iskautils.item.custom.LethalDamageModuleItem;
import net.unfamily.iskautils.item.custom.EnchantModuleItem;
import net.unfamily.iskautils.item.custom.BeheadingModuleItem;
import net.unfamily.iskautils.item.custom.LuckModuleItem;
import net.unfamily.iskautils.item.custom.ExperienceModuleItem;
import net.unfamily.iskautils.item.custom.GhostModuleItem;
import net.unfamily.iskautils.item.custom.SlowModuleItem;
import net.unfamily.iskautils.item.custom.ModerateModuleItem;
import net.unfamily.iskautils.item.custom.FastModuleItem;
import net.unfamily.iskautils.item.custom.ExtremeModuleItem;
import net.unfamily.iskautils.item.custom.UltraModuleItem;
import net.unfamily.iskautils.item.custom.LogicModuleItem;
import net.unfamily.iskautils.item.custom.ProductionModuleItem;
import net.unfamily.iskautils.item.custom.SacredRubberSaplingBlockItem;
import net.unfamily.iskautils.item.custom.AncientTabletItem;
import net.unfamily.iskautils.item.custom.UnstableEntropyCatalystItem;
import net.unfamily.iskautils.item.custom.SuspiciousDeliveryItem;
import net.unfamily.iskautils.item.custom.artifact.AncientStarItem;
import net.unfamily.iskautils.item.custom.artifact.ArcaneDictionaryItem;
import net.unfamily.iskautils.item.custom.artifact.ChosenCheeseItem;
import net.unfamily.iskautils.item.custom.artifact.CoarselyForgedRingItem;
import net.unfamily.iskautils.item.custom.artifact.CursedCandleItem;
import net.unfamily.iskautils.item.custom.artifact.CursedArtifactItem;
import net.unfamily.iskautils.item.custom.artifact.EntropicClockItem;
import net.unfamily.iskautils.item.custom.artifact.EntropicRingItem;
import net.unfamily.iskautils.item.custom.artifact.RunicDiceItem;
import net.unfamily.iskautils.item.custom.artifact.IceDiamondItem;
import net.unfamily.iskautils.item.custom.artifact.OldBrickItem;
import net.unfamily.iskautils.item.custom.artifact.SharpenedBoneItem;
import net.unfamily.iskautils.item.custom.artifact.TheRootsItem;

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
    private static final Item.Properties REAPER_STACKABLE_MODULE = new Item.Properties().stacksTo(64);

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
            () -> new SlowModuleItem(ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> MODERATE_MODULE = ITEMS.register("moderate_module",
            () -> new ModerateModuleItem(ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> FAST_MODULE = ITEMS.register("fast_module",
            () -> new FastModuleItem(ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> EXTREME_MODULE = ITEMS.register("extreme_module",
            () -> new ExtremeModuleItem(ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> ULTRA_MODULE = ITEMS.register("ultra_module",
            () -> new UltraModuleItem(ITEM_PROPERTIES));
            
    // Vector Charm - Custom item with special functionality
    // Registered as a Curio charm when Curios is available
    public static final DeferredItem<Item> VECTOR_CHARM = ITEMS.register("vector_charm",
            () -> new VectorCharmItem(new Item.Properties().stacksTo(1)));
    
    // Fanpack - Extension of Vector Charm with creative flight
    public static final DeferredItem<Item> FANPACK = ITEMS.register("fanpack",
            () -> new net.unfamily.iskautils.item.custom.FanpackItem(new Item.Properties().stacksTo(1)));

    // Portable Dislocator - Custom item with dislocator functionality
    // Registered as a Curio when Curios is available
    public static final DeferredItem<Item> PORTABLE_DISLOCATOR = ITEMS.register("portable_dislocator",
            () -> new PortableDislocatorItem(new Item.Properties().stacksTo(1)));
    
    // Gauntlet of Climbing - Custom item that allows wall climbing when in inventory
    public static final DeferredItem<Item> GAUNTLET_OF_CLIMBING = ITEMS.register("gauntlet_of_climbing",
            () -> new GauntletOfClimbingItem(new Item.Properties().stacksTo(1)));
            
    
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
    
    // Creative Dolly - Indestructible tool for picking up and moving ANY blocks
    // Infinite durability, no restrictions, can move even indestructible blocks
    public static final DeferredItem<Item> DOLLY_CREATIVE = ITEMS.register("dolly_creative",
            () -> new net.unfamily.iskautils.item.custom.CreativeDollyItem(new Item.Properties().stacksTo(1).fireResistant()));
            
    // ===== RUBBER TREE ITEMS =====
    
    // Sap item dropped from rubber logs
    public static final DeferredItem<Item> SAP = ITEMS.register("sap",
            () -> new Item(ITEM_PROPERTIES));

    // Dye bush items
    public static final DeferredItem<Item> DYE_BERRY = ITEMS.register("dye_berry",
            () -> new net.unfamily.iskautils.item.custom.DyeBerryItem(new Item.Properties().food(ModFoodProperties.DYE_BERRY)));
    public static final DeferredItem<Item> GREEN_SLUDGE = ITEMS.register("green_sludge",
            () -> new Item(ITEM_PROPERTIES));

    // ===== NEW ARTIFACTS (obtaining) =====
    public static final DeferredItem<Item> SUSPICIOUS_DELIVERY = ITEMS.register("suspicious_delivery",
            () -> new SuspiciousDeliveryItem(new Item.Properties()));

    /** Placeholder shown in JEI for masked loot entries; not in creative tab. */
    public static final DeferredItem<Item> SUSPICIOUS_DELIVERY_UNDEFINED = ITEMS.register(
            "suspicious_delivery_undefined",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> DROP_OF_ENTROPY = ITEMS.register("drop_of_entropy",
            () -> new net.unfamily.iskautils.item.custom.DropOfEntropyItem(ITEM_PROPERTIES));

    public static final DeferredItem<Item> CURSED_KEY = ITEMS.register("cursed_key",
            () -> new net.unfamily.iskautils.item.custom.CursedKeyItem(ITEM_PROPERTIES));

    public static final DeferredItem<Item> ENTROPIC_AGGLOMERATION = ITEMS.register("entropic_agglomeration",
            () -> new net.unfamily.iskautils.item.custom.EntropicAgglomerationItem(ITEM_PROPERTIES));

    public static final DeferredItem<Item> DRUIDIC_AGGLOMERATION = ITEMS.register("druidic_agglomeration",
            () -> new net.unfamily.iskautils.item.custom.DruidicAgglomerationItem(ITEM_PROPERTIES));

    public static final DeferredItem<Item> ANCIENT_TABLET = ITEMS.register("ancient_tablet",
            () -> new AncientTabletItem(new Item.Properties()));

    public static final DeferredItem<Item> UNSTABLE_ENTROPY_CATALYST = ITEMS.register("unstable_entropy_catalyst",
            () -> new UnstableEntropyCatalystItem(ITEM_PROPERTIES));

    public static final DeferredItem<Item> ENTROPY_CRYSTAL = ITEMS.register("entropy_crystal",
            () -> new Item(ITEM_PROPERTIES));

    // ===== ENTROPIC GEAR (indestructible) =====
    public static final DeferredItem<Item> ENTROPIC_SMITHING_TEMPLATE = ITEMS.register("entropic_smithing_template",
            () -> new net.unfamily.iskautils.item.entropic.EntropicSmithingTemplateItem(
                    net.unfamily.iskautils.item.entropic.EntropicSmithingTemplateItem.defaultProperties()));

    public static final DeferredItem<Item> ENTROPIC_SWORD = ITEMS.register("entropic_sword",
            () -> new net.unfamily.iskautils.item.entropic.EntropicSwordItem(net.unfamily.iskautils.item.entropic.EntropicGear.swordProperties()));

    public static final DeferredItem<Item> ENTROPIC_PICKAXE = ITEMS.register("entropic_pickaxe",
            () -> new net.unfamily.iskautils.item.entropic.EntropicPickaxeItem(net.unfamily.iskautils.item.entropic.EntropicGear.pickaxeProperties()));

    public static final DeferredItem<Item> ENTROPIC_AXE = ITEMS.register("entropic_axe",
            () -> new net.unfamily.iskautils.item.entropic.EntropicAxeItem(net.unfamily.iskautils.item.entropic.EntropicGear.axeProperties()));

    public static final DeferredItem<Item> ENTROPIC_SHOVEL = ITEMS.register("entropic_shovel",
            () -> new net.unfamily.iskautils.item.entropic.EntropicShovelItem(net.unfamily.iskautils.item.entropic.EntropicGear.shovelProperties()));

    public static final DeferredItem<Item> ENTROPIC_HOE = ITEMS.register("entropic_hoe",
            () -> new net.unfamily.iskautils.item.entropic.EntropicHoeItem(net.unfamily.iskautils.item.entropic.EntropicGear.hoeProperties()));

    public static final DeferredItem<Item> ENTROPIC_PAXEL = ITEMS.register("entropic_paxel",
            () -> new net.unfamily.iskautils.item.entropic.EntropicPaxelItem(net.unfamily.iskautils.item.entropic.EntropicGear.baseProperties()));

    public static final DeferredItem<Item> DURABLE_SHEARS = ITEMS.register("durable_shears",
            () -> new net.unfamily.iskautils.item.custom.DurableShearsItem(
                    new Item.Properties().stacksTo(1).durability(net.unfamily.iskautils.item.custom.DurableShearsItem.MAX_DURABILITY)));

    public static final DeferredItem<Item> ENTROPIC_HELMET = ITEMS.register("entropic_helmet",
            () -> new net.unfamily.iskautils.item.entropic.EntropicArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET, net.unfamily.iskautils.item.entropic.EntropicGear.armorProperties()));

    public static final DeferredItem<Item> ENTROPIC_CHESTPLATE = ITEMS.register("entropic_chestplate",
            () -> new net.unfamily.iskautils.item.entropic.EntropicArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, net.unfamily.iskautils.item.entropic.EntropicGear.armorProperties()));

    public static final DeferredItem<Item> ENTROPIC_LEGGINGS = ITEMS.register("entropic_leggings",
            () -> new net.unfamily.iskautils.item.entropic.EntropicArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, net.unfamily.iskautils.item.entropic.EntropicGear.armorProperties()));

    public static final DeferredItem<Item> ENTROPIC_BOOTS = ITEMS.register("entropic_boots",
            () -> new net.unfamily.iskautils.item.entropic.EntropicArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS, net.unfamily.iskautils.item.entropic.EntropicGear.armorProperties()));

    // ===== RELICS (reliquie) =====
    public static final DeferredItem<Item> OLD_BRICK = ITEMS.register("old_brick",
            () -> new OldBrickItem(new Item.Properties()));
    public static final DeferredItem<Item> CHOSEN_CHEESE = ITEMS.register("chosen_cheese",
            () -> new ChosenCheeseItem(new Item.Properties()));
    public static final DeferredItem<Item> ICE_DIAMOND = ITEMS.register("ice_diamond",
            () -> new IceDiamondItem(new Item.Properties()));
    public static final DeferredItem<Item> SHARPENED_BONE = ITEMS.register("sharpened_bone",
            () -> new SharpenedBoneItem(new Item.Properties()));
    public static final DeferredItem<Item> THE_ROOTS = ITEMS.register("the_roots",
            () -> new TheRootsItem(new Item.Properties()));
    public static final DeferredItem<Item> COARSELY_FORGED_RING = ITEMS.register("coarsely_forged_ring",
            () -> new CoarselyForgedRingItem(new Item.Properties()));
    public static final DeferredItem<Item> ENTROPIC_RING = ITEMS.register("entropic_ring",
            () -> new EntropicRingItem(new Item.Properties()));
    public static final DeferredItem<Item> ENTROPIC_CLOCK = ITEMS.register("entropic_clock",
            () -> new EntropicClockItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> ANCIENT_STAR = ITEMS.register("ancient_star",
            () -> new AncientStarItem(new Item.Properties()));
    public static final DeferredItem<Item> RUNIC_DICE = ITEMS.register("runic_dice",
            () -> new RunicDiceItem(new Item.Properties()));

    // Cursed artifact set
    public static final DeferredItem<Item> TOTEM_OF_PAIN = ITEMS.register("totem_of_pain",
            () -> new CursedArtifactItem(new Item.Properties()));
    public static final DeferredItem<Item> CURSED_CANDLE = ITEMS.register("cursed_candle",
            () -> new CursedCandleItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BUSTED_CROWN = ITEMS.register("busted_crown",
            () -> new CursedArtifactItem(new Item.Properties()));
    public static final DeferredItem<Item> RITUAL_GAUNTLET = ITEMS.register("ritual_gauntlet",
            () -> new CursedArtifactItem(new Item.Properties()));
    public static final DeferredItem<Item> THE_DECEPTION = ITEMS.register("the_deception",
            () -> new net.unfamily.iskautils.item.custom.artifact.TheDeceptionItem(
                    ModBlocks.THE_DECEPTION.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> ARCANE_DICTIONARY = ITEMS.register("arcane_dictionary",
            () -> new ArcaneDictionaryItem(new Item.Properties()));

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

    public static final DeferredItem<Item> DYE_BUSH_EMPTY = ITEMS.register("dye_bush_empty",
            () -> new BlockItem(ModBlocks.DYE_BUSH_EMPTY.get(), new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DYE_BUSH_FILLED = ITEMS.register("dye_bush_filled",
            () -> new BlockItem(ModBlocks.DYE_BUSH_FILLED.get(), new Item.Properties().stacksTo(64)));

    // Dye Extractor machine block item
    public static final DeferredItem<Item> FACTORY = ITEMS.register("factory",
            () -> new BlockItem(ModBlocks.FACTORY.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> ANCIENT_TABLE = ITEMS.register("ancient_table",
            () -> new BlockItem(ModBlocks.ANCIENT_TABLE.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> RUBBER_SAPLING = ITEMS.register("rubber_sapling",
            () -> new BlockItem(ModBlocks.RUBBER_SAPLING.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> SACRED_RUBBER_SAPLING = ITEMS.register("sacred_rubber_sapling",
            () -> new SacredRubberSaplingBlockItem(ModBlocks.SACRED_RUBBER_SAPLING.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> SACRED_RUBBER_ROOT = ITEMS.register("sacred_rubber_root",
            () -> new BlockItem(ModBlocks.SACRED_RUBBER_ROOT.get(), ITEM_PROPERTIES));
            // Not added to creative tab - hidden item
            
    public static final DeferredItem<Item> RUBBER_LOG_SACRED = ITEMS.register("rubber_log_sacred",
            () -> new BlockItem(ModBlocks.RUBBER_LOG_SACRED.get(), ITEM_PROPERTIES));
            // Not added to creative tab - hidden item

    public static final DeferredItem<Item> RUBBER_BLOCK = ITEMS.register("rubber_block",
            () -> new BlockItem(ModBlocks.RUBBER_BLOCK.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> PLASTIC_BLOCK = ITEMS.register("plastic_block",
            () -> new BlockItem(ModBlocks.PLASTIC_BLOCK.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> PLASTIC_BRICKS = ITEMS.register("plastic_bricks",
            () -> new BlockItem(ModBlocks.PLASTIC_BRICKS.get(), ITEM_PROPERTIES));
            
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
            () -> new HellfireIgniterBlockItem(ModBlocks.HELLFIRE_IGNITER.get(), ITEM_PROPERTIES));
    
    // Item for the Fan
    public static final DeferredItem<Item> FAN = ITEMS.register("fan",
            () -> new FanBlockItem(ModBlocks.FAN.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> MOB_REAPER = ITEMS.register("mob_reaper",
            () -> new BlockItem(ModBlocks.MOB_REAPER.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> COLLECTING_CRATE = ITEMS.register("collecting_crate",
            () -> new net.unfamily.iskautils.item.custom.CollectingCrateBlockItem(
                    ModBlocks.COLLECTING_CRATE.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> CONDENSED_KNOWLEDGE_BUCKET = ITEMS.register("condensed_knowledge_bucket",
            () -> new BucketItem(ModFluids.CONDENSED_KNOWLEDGE_SOURCE.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // Fan upgrade modules
    public static final DeferredItem<Item> RANGE_MODULE = ITEMS.register("range_module",
            () -> new RangeModuleItem(ITEM_PROPERTIES));

    public static final DeferredItem<Item> GHOST_MODULE = ITEMS.register("ghost_module",
            () -> new GhostModuleItem(ITEM_PROPERTIES));

    public static final DeferredItem<Item> LOGIC_MODULE = ITEMS.register("logic_module",
            () -> new LogicModuleItem(ITEM_PROPERTIES));

    public static final DeferredItem<Item> PRODUCTION_MODULE = ITEMS.register("production_module",
            () -> new ProductionModuleItem(ITEM_PROPERTIES));

    public static final DeferredItem<Item> CAPACITOR_MODULE = ITEMS.register("capacitor_module",
            () -> new Item(ITEM_PROPERTIES));

    // Mob Reaper modules
    public static final DeferredItem<Item> NORMAL_DAMAGE_MODULE = ITEMS.register("normal_damage_module",
            () -> new NormalDamageModuleItem(REAPER_STACKABLE_MODULE));
    public static final DeferredItem<Item> LETHAL_DAMAGE_MODULE = ITEMS.register("lethal_damage_module",
            () -> new LethalDamageModuleItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> ENCHANT_MODULE = ITEMS.register("enchant_module",
            () -> new EnchantModuleItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BEHEADING_MODULE = ITEMS.register("beheading_module",
            () -> new BeheadingModuleItem(REAPER_STACKABLE_MODULE));
    public static final DeferredItem<Item> LUCK_MODULE = ITEMS.register("luck_module",
            () -> new LuckModuleItem(REAPER_STACKABLE_MODULE));
    public static final DeferredItem<Item> EXPERIENCE_MODULE = ITEMS.register("experience_module",
            () -> new ExperienceModuleItem(REAPER_STACKABLE_MODULE));

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
    
    // ===== SAP =====
    public static final DeferredItem<Item> SAP_BLOCK = ITEMS.register("sap_block",
            () -> new BlockItem(ModBlocks.SAP_BLOCK.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> TAR_SLIMEBALL = ITEMS.register("tar_slimeball",
            () -> new Item(ITEM_PROPERTIES));

    // ===== RUBBER ARMOR =====
    public static final DeferredItem<Item> RUBBER_BOOTS = ITEMS.register("rubber_boots",
            () -> new RubberBootsItem(new Item.Properties().durability(256)));

            
    // ===== UTILITY BLOCKS =====
    public static final DeferredItem<Item> RUBBER_SAP_EXTRACTOR = ITEMS.register("rubber_sap_extractor", 
            () -> new RubberSapExtractorBlockItem(ModBlocks.RUBBER_SAP_EXTRACTOR.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> WEATHER_DETECTOR = ITEMS.register("weather_detector",
            () -> new BlockItem(ModBlocks.WEATHER_DETECTOR.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> SOUND_MUFFLER = ITEMS.register("sound_muffler",
            () -> new BlockItem(ModBlocks.SOUND_MUFFLER.get(), ITEM_PROPERTIES));
            
    public static final DeferredItem<Item> WEATHER_ALTERER = ITEMS.register("weather_alterer",
            () -> new BlockItem(ModBlocks.WEATHER_ALTERER.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> TIME_ALTERER = ITEMS.register("time_alterer",
            () -> new BlockItem(ModBlocks.TIME_ALTERER.get(), ITEM_PROPERTIES));
    
    // Temporal Overclocker
    public static final DeferredItem<Item> TEMPORAL_OVERCLOCKER = ITEMS.register("temporal_overclocker",
            () -> new TemporalOverclockerBlockItem(ModBlocks.TEMPORAL_OVERCLOCKER.get(), new Item.Properties().stacksTo(64)));
    
    // Temporal Overclocker Chipset
    public static final DeferredItem<Item> TEMPORAL_OVERCLOCKER_CHIPSET = ITEMS.register("temporal_overclocker_chip",
            () -> new TemporalOverclockerChipsetItem(new Item.Properties().stacksTo(1)));

    // ===== ANGEL BLOCK =====
    public static final DeferredItem<Item> ANGEL_BLOCK = ITEMS.register("angel_block",
            () -> new AngelBlockItem(ModBlocks.ANGEL_BLOCK.get(), new Item.Properties().stacksTo(64)));

    // ===== STRUCTURE SYSTEM =====
    public static final DeferredItem<Item> STRUCTURE_PLACER_MACHINE = ITEMS.register("structure_placer_machine",
            () -> new StructurePlacerMachineBlockItem(ModBlocks.STRUCTURE_PLACER_MACHINE.get(), ITEM_PROPERTIES));
    
    public static final DeferredItem<Item> STRUCTURE_SAVER_MACHINE = ITEMS.register("structure_saver_machine",
            () -> new StructureSaverMachineBlockItem(ModBlocks.STRUCTURE_SAVER_MACHINE.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> SHOP = ITEMS.register("shop",
            () -> new ShopBlockItem(ModBlocks.SHOP.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> AUTO_SHOP = ITEMS.register("auto_shop",
            () -> new AutoShopItem(ModBlocks.AUTO_SHOP.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> SMART_TIMER = ITEMS.register("smart_timer",
            () -> new BlockItem(ModBlocks.SMART_TIMER.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> STRUCTURE_PLACER = ITEMS.register("structure_placer",
            () -> new StructurePlacerItem(new Item.Properties().stacksTo(1)));
            
    // ===== DEV ITEMS =====
    
    // Blueprint - Strumento per salvare coordinate di vertici
    public static final DeferredItem<Item> BLUEPRINT = ITEMS.register("blueprint",
            () -> new BlueprintItem(new Item.Properties().stacksTo(1)));

    // Entropy TNT Block
    public static final DeferredItem<Item> ENTROPY_TNT = ITEMS.register("entropy_tnt",
            () -> new BlockItem(ModBlocks.ENTROPY_TNT.get(), ITEM_PROPERTIES));

    public static final DeferredItem<Item> ENTROPIC_SOIL = ITEMS.register("entropic_soil",
            () -> new TranslatedTooltipBlockItem(
                    ModBlocks.ENTROPIC_SOIL.get(),
                    ITEM_PROPERTIES,
                    "tooltip.iska_utils.entropic_soil.desc0",
                    "tooltip.iska_utils.entropic_soil.desc1"));

    public static final DeferredItem<Item> ENTROPIC_DIRT = ITEMS.register("entropic_dirt",
            () -> new TranslatedTooltipBlockItem(
                    ModBlocks.ENTROPIC_DIRT.get(),
                    ITEM_PROPERTIES,
                    "tooltip.iska_utils.entropic_dirt.desc0",
                    "tooltip.iska_utils.entropic_dirt.desc1"));

    public static final DeferredItem<Item> GRAVEYARD_SOIL = ITEMS.register("graveyard_soil",
            () -> new TranslatedTooltipBlockItem(
                    ModBlocks.GRAVEYARD_SOIL.get(),
                    ITEM_PROPERTIES,
                    "tooltip.iska_utils.graveyard_soil.desc0"));

    public static final DeferredItem<Item> DRUIDIC_PODZOL = ITEMS.register("druidic_podzol",
            () -> new TranslatedTooltipBlockItem(
                    ModBlocks.DRUIDIC_PODZOL.get(),
                    ITEM_PROPERTIES,
                    "tooltip.iska_utils.druidic_podzol.desc0",
                    "tooltip.iska_utils.druidic_podzol.desc1"));

    // ===== BURNING BRAZIER ITEM =====

    // Burning Brazier (places burning flame blocks when light level is low)
    public static final DeferredItem<Item> BURNING_BRAZIER = ITEMS.register("burning_brazier",
            () -> new BurningBrazierItem(new Item.Properties().stacksTo(1).durability(BurningBrazierItem.MAX_DURABILITY)));

    public static final DeferredItem<Item> BLAZING_ALTAR = ITEMS.register("blazing_altar",
            () -> new net.unfamily.iskautils.item.custom.BlazingAltarBlockItem(ModBlocks.BLAZING_ALTAR.get(), ITEM_PROPERTIES));
    
    public static final DeferredItem<Item> REDSTONE_ACTIVATOR = ITEMS.register("redstone_activator",
            () -> new RedstoneSignalItem(new Item.Properties().stacksTo(1)));
    
    // Redstone Activator Signal Block Item (not indexed in creative tab)
    public static final DeferredItem<Item> REDSTONE_ACTIVATOR_SIGNAL = ITEMS.register("redstone_activator_signal",
            () -> new BlockItem(ModBlocks.REDSTONE_ACTIVATOR_SIGNAL.get(), ITEM_PROPERTIES));

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
    
    public static final DeferredItem<Item> DEEP_DRAWER_INTERFACE = ITEMS.register("deep_drawer_interface",
            () -> new BlockItem(ModBlocks.DEEP_DRAWER_INTERFACE.get(), ITEM_PROPERTIES));
    
    public static final DeferredItem<Item> DEEP_DRAWER_EXTENDER = ITEMS.register("deep_drawer_extender",
            () -> new BlockItem(ModBlocks.DEEP_DRAWER_EXTENDER.get(), ITEM_PROPERTIES));

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