package net.unfamily.iskautils.client.gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;

/**
 * Registrazione dei MenuType per le GUI personalizzate
 */
public class ModMenuTypes {
    
    public static final DeferredRegister<MenuType<?>> MENUS = 
        DeferredRegister.create(Registries.MENU, IskaUtils.MOD_ID);
    
    // Registra il menu del Structure Placer
    public static final DeferredHolder<MenuType<?>, MenuType<StructurePlacerMenu>> STRUCTURE_PLACER_MENU = 
        MENUS.register("structure_placer", () -> 
            new MenuType<>(StructurePlacerMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    
} 