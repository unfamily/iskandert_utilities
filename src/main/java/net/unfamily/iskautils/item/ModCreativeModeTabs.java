package net.unfamily.iskautils.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.PotionPlateRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.unfamily.iskautils.item.ModItems;

import java.util.Map;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IskaUtils.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ISKA_UTIL_TAB = 
            CREATIVE_MODE_TABS.register("iska_utils_tab", 
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.iska_utils.tab"))
                            .icon(() -> new ItemStack(ModItems.VECTOR_CHARM.get()))
                            .displayItems((pParameters, pOutput) -> {
                                
                                // 1. Decorative Blocks
                                pOutput.accept(ModItems.WITHER_PROOF_BLOCK.get());
                                pOutput.accept(ModItems.WITHER_PROOF_STAIRS.get());
                                pOutput.accept(ModItems.WITHER_PROOF_SLAB.get());
                                pOutput.accept(ModItems.WITHER_PROOF_WALL.get());
                                pOutput.accept(ModItems.NETHERITE_BARS.get());
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE.get());
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE_SLAB.get());
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE_STAIRS.get());
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE_WALL.get());
                                
                                // 2. Plates of various types
                                pOutput.accept(ModItems.PLATE_BASE_BLOCK.get());
                                // Vector mobfarm (standard vector plates)
                                pOutput.accept(ModItems.SLOW_VECT.get());
                                pOutput.accept(ModItems.MODERATE_VECT.get());
                                pOutput.accept(ModItems.FAST_VECT.get());
                                pOutput.accept(ModItems.EXTREME_VECT.get());
                                pOutput.accept(ModItems.ULTRA_VECT.get());
                                // Vector player
                                pOutput.accept(ModItems.PLAYER_SLOW_VECT.get());
                                pOutput.accept(ModItems.PLAYER_MODERATE_VECT.get());
                                pOutput.accept(ModItems.PLAYER_FAST_VECT.get());
                                pOutput.accept(ModItems.PLAYER_EXTREME_VECT.get());
                                pOutput.accept(ModItems.PLAYER_ULTRA_VECT.get());
                                // Trap plates (potion plates)
                                Map<String, DeferredHolder<Item, BlockItem>> potionPlateItems = PotionPlateRegistry.getAllItems();
                                for (DeferredHolder<Item, BlockItem> itemHolder : potionPlateItems.values()) {
                                    pOutput.accept(itemHolder.get());
                                }
                                // Base module
                                pOutput.accept(ModItems.BASE_MODULE.get());
                                // Fan modules
                                pOutput.accept(ModItems.RANGE_MODULE.get());
                                pOutput.accept(ModItems.GHOST_MODULE.get());
                                // Modules (follow vector tier)
                                pOutput.accept(ModItems.SLOW_MODULE.get());
                                pOutput.accept(ModItems.MODERATE_MODULE.get());
                                pOutput.accept(ModItems.FAST_MODULE.get());
                                pOutput.accept(ModItems.EXTREME_MODULE.get());
                                pOutput.accept(ModItems.ULTRA_MODULE.get());
                                // Vector Charm
                                pOutput.accept(ModItems.VECTOR_CHARM.get());
                                
                                // 3. Rubber and related items
                                pOutput.accept(ModItems.SAP.get());
                                pOutput.accept(ModItems.RUBBER.get());
                                pOutput.accept(ModItems.RUBBER_CHUNK.get());
                                pOutput.accept(ModItems.RUBBER_BLOCK.get());
                                pOutput.accept(ModItems.PLASTIC_INGOT.get());
                                pOutput.accept(ModItems.TAR_SLIME_BLOCK.get());
                                pOutput.accept(ModItems.TAR_SLIMEBALL.get());
                                pOutput.accept(ModItems.TREE_TAP.get());
                                pOutput.accept(ModItems.ELECTRIC_TREE_TAP.get());
                                // Rubber wood, leaves, sapling
                                pOutput.accept(ModItems.RUBBER_LOG.get());
                                pOutput.accept(ModItems.STRIPPED_RUBBER_LOG.get());
                                pOutput.accept(ModItems.RUBBER_WOOD.get());
                                pOutput.accept(ModItems.STRIPPED_RUBBER_WOOD.get());
                                pOutput.accept(ModItems.RUBBER_PLANKS.get());
                                pOutput.accept(ModItems.RUBBER_LEAVES.get());
                                pOutput.accept(ModItems.RUBBER_SAPLING.get());
                                // Rubber wood variants
                                pOutput.accept(ModItems.RUBBER_STAIRS.get());
                                pOutput.accept(ModItems.RUBBER_SLAB.get());
                                pOutput.accept(ModItems.RUBBER_FENCE.get());
                                pOutput.accept(ModItems.RUBBER_FENCE_GATE.get());
                                pOutput.accept(ModItems.RUBBER_BUTTON.get());
                                pOutput.accept(ModItems.RUBBER_PRESSURE_PLATE.get());
                                pOutput.accept(ModItems.RUBBER_DOOR.get());
                                pOutput.accept(ModItems.RUBBER_TRAPDOOR.get());
                                // Not including rubber_log_empty/filled (hidden)
                                
                                // 4. Machines
                                pOutput.accept(ModItems.RUBBER_SAP_EXTRACTOR.get());
                                pOutput.accept(ModItems.HELLFIRE_IGNITER.get());
                                pOutput.accept(ModItems.SMART_TIMER.get());
                                pOutput.accept(ModItems.FAN.get());
                                pOutput.accept(ModItems.WEATHER_DETECTOR.get());
                                pOutput.accept(ModItems.WEATHER_ALTERER.get());
                                pOutput.accept(ModItems.TIME_ALTERER.get());
                                pOutput.accept(ModItems.TEMPORAL_OVERCLOCKER.get());
                                pOutput.accept(ModItems.TEMPORAL_OVERCLOCKER_CHIPSET.get());
                                // Deep drawer (all components)
                                pOutput.accept(ModItems.DEEP_DRAWERS.get());
                                pOutput.accept(ModItems.DEEP_DRAWER_EXTRACTOR.get());
                                pOutput.accept(ModItems.DEEP_DRAWER_INTERFACE.get());
                                pOutput.accept(ModItems.DEEP_DRAWER_EXTENDER.get());
                                
                                // 5. Structure managers
                                pOutput.accept(ModItems.STRUCTURE_PLACER_MACHINE.get());
                                pOutput.accept(ModItems.STRUCTURE_PLACER.get());
                                pOutput.accept(ModItems.STRUCTURE_SAVER_MACHINE.get());
                                pOutput.accept(ModItems.BLUEPRINT.get());
                                // Single-use structure items
                                Map<String, DeferredHolder<Item, net.unfamily.iskautils.item.custom.StructureMonouseItem>> monouseItems =
                                    net.unfamily.iskautils.structure.StructureMonouseRegistry.getAllItems();
                                for (DeferredHolder<Item, net.unfamily.iskautils.item.custom.StructureMonouseItem> itemHolder : monouseItems.values()) {
                                    pOutput.accept(itemHolder.get());
                                }
                                
                                // 6. General utility items
                                pOutput.accept(ModItems.ANGEL_BLOCK.get());
                                pOutput.accept(ModItems.RAFT.get());
                                pOutput.accept(ModItems.SCANNER.get());
                                pOutput.accept(ModItems.SCANNER_CHIP.get());
                                pOutput.accept(ModItems.SCANNER_CHIP_ORES.get());
                                pOutput.accept(ModItems.SCANNER_CHIP_MOBS.get());
                                pOutput.accept(ModItems.SHOP.get());
                                pOutput.accept(ModItems.AUTO_SHOP.get());
                                pOutput.accept(ModItems.RUBBER_BOOTS.get());
                                pOutput.accept(ModItems.SWISS_WRENCH.get());
                                pOutput.accept(ModItems.PORTABLE_DISLOCATOR.get());
                                pOutput.accept(ModItems.DOLLY.get());
                                pOutput.accept(ModItems.DOLLY_HARD.get());
                                pOutput.accept(ModItems.DOLLY_CREATIVE.get());
                                
                                // Command Items
                                Map<String, DeferredHolder<Item, CommandItem>> commandItems = 
                                    CommandItemRegistry.getAllItems();
                                for (DeferredHolder<Item, CommandItem> itemHolder : commandItems.values()) {
                                    CommandItem item = itemHolder.get();
                                    if (item.getDefinition().isCreativeTabVisible()) {
                                        pOutput.accept(item);
                                    }
                                }
                                
                                // 7. Food
                                pOutput.accept(ModItems.LAPIS_ICE_CREAM.get());
                                
                                // 8. Artifacts
                                pOutput.accept(ModItems.NECROTIC_CRYSTAL_HEART.get());
                                pOutput.accept(ModItems.MINING_EQUITIZER.get());
                                pOutput.accept(ModItems.BURNING_BRAZIER.get());
                                pOutput.accept(ModItems.GHOST_BRAZIER.get());

                            }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
