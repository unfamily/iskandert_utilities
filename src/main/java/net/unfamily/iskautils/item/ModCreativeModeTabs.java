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
                                // Custom Items
                                pOutput.accept(ModItems.SWISS_WRENCH.get());
                                pOutput.accept(ModItems.DOLLY.get());
                                pOutput.accept(ModItems.DOLLY_HARD.get());
                                pOutput.accept(ModItems.DOLLY_CREATIVE.get());
                                pOutput.accept(ModItems.VECTOR_CHARM.get());
                                pOutput.accept(ModItems.PORTABLE_DISLOCATOR.get());
                                pOutput.accept(ModItems.BASE_MODULE.get());
                                pOutput.accept(ModItems.SLOW_MODULE.get());
                                pOutput.accept(ModItems.MODERATE_MODULE.get());
                                pOutput.accept(ModItems.FAST_MODULE.get());
                                pOutput.accept(ModItems.EXTREME_MODULE.get());
                                pOutput.accept(ModItems.ULTRA_MODULE.get());
                                pOutput.accept(ModItems.RUBBER_BOOTS.get());
                                
                                // Rubber Tree Items
                                pOutput.accept(ModItems.SAP.get());
                                pOutput.accept(ModItems.RUBBER_CHUNK.get());
                                pOutput.accept(ModItems.RUBBER.get());
                                pOutput.accept(ModItems.TREE_TAP.get());
                                pOutput.accept(ModItems.ELECTRIC_TREE_TAP.get());
                                pOutput.accept(ModItems.RUBBER_LOG.get());
                                pOutput.accept(ModItems.STRIPPED_RUBBER_LOG.get());
                                pOutput.accept(ModItems.RUBBER_LEAVES.get());
                                pOutput.accept(ModItems.RUBBER_SAPLING.get());
                                pOutput.accept(ModItems.RUBBER_WOOD.get());
                                pOutput.accept(ModItems.STRIPPED_RUBBER_WOOD.get());
                                pOutput.accept(ModItems.RUBBER_PLANKS.get());
                                pOutput.accept(ModItems.RUBBER_BLOCK.get());
                                pOutput.accept(ModItems.RUBBER_SAP_EXTRACTOR.get());
                                pOutput.accept(ModItems.PLASTIC_INGOT.get());

                                // Deep Drawers
                                pOutput.accept(ModItems.DEEP_DRAWERS.get());
                                pOutput.accept(ModItems.DEEP_DRAWER_EXTRACTOR.get());
                                pOutput.accept(ModItems.DEEP_DRAWER_INTERFACE.get());
                                pOutput.accept(ModItems.DEEP_DRAWER_EXTENDER.get());

                                // Shops
                                pOutput.accept(ModItems.SHOP.get());
                                pOutput.accept(ModItems.AUTO_SHOP.get());

                                //Redstone
                                pOutput.accept(ModItems.SMART_TIMER.get());

                                // Rubber Wood Variants
                                pOutput.accept(ModItems.RUBBER_STAIRS.get());
                                pOutput.accept(ModItems.RUBBER_SLAB.get());
                                pOutput.accept(ModItems.RUBBER_FENCE.get());
                                pOutput.accept(ModItems.RUBBER_FENCE_GATE.get());
                                pOutput.accept(ModItems.RUBBER_BUTTON.get());
                                pOutput.accept(ModItems.RUBBER_PRESSURE_PLATE.get());
                                pOutput.accept(ModItems.RUBBER_DOOR.get());
                                pOutput.accept(ModItems.RUBBER_TRAPDOOR.get());

                                //not inxeding this items in the creative tab
                                //pOutput.accept(ModItems.RUBBER_LOG_FILLED.get());
                                //pOutput.accept(ModItems.RUBBER_LOG_EMPTY.get());

                                // Tar
                                pOutput.accept(ModItems.TAR_SLIME_BLOCK.get());
                                pOutput.accept(ModItems.TAR_SLIMEBALL.get());
                                
                                // Command Items
                                // Aggiungiamo tutti gli oggetti di comando visibili al tab principale
                                Map<String, DeferredHolder<Item, CommandItem>> commandItems = 
                                    CommandItemRegistry.getAllItems();
                                for (DeferredHolder<Item, CommandItem> itemHolder : commandItems.values()) {
                                    CommandItem item = itemHolder.get();
                                    if (item.getDefinition().isCreativeTabVisible()) {
                                        pOutput.accept(item);
                                    }
                                }
                                
                                                // Standard Vector Plates
                                pOutput.accept(ModItems.SLOW_VECT.get());
                                pOutput.accept(ModItems.MODERATE_VECT.get());
                                pOutput.accept(ModItems.FAST_VECT.get());
                                pOutput.accept(ModItems.EXTREME_VECT.get());
                                pOutput.accept(ModItems.ULTRA_VECT.get());
                                
                                // Player Vector Plates
                                pOutput.accept(ModItems.PLAYER_SLOW_VECT.get());
                                pOutput.accept(ModItems.PLAYER_MODERATE_VECT.get());
                                pOutput.accept(ModItems.PLAYER_FAST_VECT.get());
                                pOutput.accept(ModItems.PLAYER_EXTREME_VECT.get());
                                pOutput.accept(ModItems.PLAYER_ULTRA_VECT.get());
                                
                                // Dynamic Potion Plates
                                Map<String, DeferredHolder<Item, BlockItem>> potionPlateItems = PotionPlateRegistry.getAllItems();
                                for (DeferredHolder<Item, BlockItem> itemHolder : potionPlateItems.values()) {
                                    pOutput.accept(itemHolder.get());
                                }
                                
                                // Bars
                                pOutput.accept(ModItems.NETHERITE_BARS.get());

                                // Wither Proof Blocks
                                pOutput.accept(ModItems.WITHER_PROOF_BLOCK.get());
                                pOutput.accept(ModItems.WITHER_PROOF_STAIRS.get());
                                pOutput.accept(ModItems.WITHER_PROOF_SLAB.get());
                                pOutput.accept(ModItems.WITHER_PROOF_WALL.get());

                                // Smooth Blackstone Blocks
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE.get());
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE_SLAB.get());
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE_STAIRS.get());
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE_WALL.get());
                                pOutput.accept(ModItems.PLATE_BASE_BLOCK.get());

                                // Utility Blocks
                                pOutput.accept(ModItems.HELLFIRE_IGNITER.get());
                                pOutput.accept(ModItems.RAFT.get());
                                pOutput.accept(ModItems.WEATHER_DETECTOR.get());
                                pOutput.accept(ModItems.WEATHER_ALTERER.get());
                                pOutput.accept(ModItems.TIME_ALTERER.get());
                                pOutput.accept(ModItems.ANGEL_BLOCK.get());


                                // Scanner
                                pOutput.accept(ModItems.SCANNER.get());
                                pOutput.accept(ModItems.SCANNER_CHIP.get());
                                pOutput.accept(ModItems.SCANNER_CHIP_ORES.get());
                                pOutput.accept(ModItems.SCANNER_CHIP_MOBS.get());

                                // Structure System
                                pOutput.accept(ModItems.STRUCTURE_PLACER_MACHINE.get());
                                pOutput.accept(ModItems.STRUCTURE_SAVER_MACHINE.get());
                                pOutput.accept(ModItems.STRUCTURE_PLACER.get());
                                pOutput.accept(ModItems.BLUEPRINT.get());

                                // Structure Monouse Items
                                Map<String, DeferredHolder<Item, net.unfamily.iskautils.item.custom.StructureMonouseItem>> monouseItems =
                                    net.unfamily.iskautils.structure.StructureMonouseRegistry.getAllItems();
                                for (DeferredHolder<Item, net.unfamily.iskautils.item.custom.StructureMonouseItem> itemHolder : monouseItems.values()) {
                                    pOutput.accept(itemHolder.get());
                                }

                                // Food Items
                                pOutput.accept(ModItems.LAPIS_ICE_CREAM.get());

                                // Artifacts
                                pOutput.accept(ModItems.NECROTIC_CRYSTAL_HEART.get());
                                pOutput.accept(ModItems.MINING_EQUITIZER.get());
                                pOutput.accept(ModItems.BURNING_BRAZIER.get());
                                pOutput.accept(ModItems.GHOST_BRAZIER.get());


                            }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
} 