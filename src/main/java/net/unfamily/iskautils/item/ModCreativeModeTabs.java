package net.unfamily.iskautils.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.ModBlocks;

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
                                pOutput.accept(ModItems.VECTOR_CHARM.get());
                                pOutput.accept(ModItems.BASE_MODULE.get());
                                pOutput.accept(ModItems.SLOW_MODULE.get());
                                pOutput.accept(ModItems.MODERATE_MODULE.get());
                                pOutput.accept(ModItems.FAST_MODULE.get());
                                pOutput.accept(ModItems.EXTREME_MODULE.get());
                                pOutput.accept(ModItems.ULTRA_MODULE.get());
                                
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
                                
                                // Utility Blocks
                                pOutput.accept(ModItems.HELLFIRE_IGNITER.get());
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE.get());
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE_SLAB.get());
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE_STAIRS.get());
                                pOutput.accept(ModItems.SMOOTH_BLACKSTONE_WALL.get());
                                pOutput.accept(ModItems.PLATE_BASE_BLOCK.get());
                            }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
} 