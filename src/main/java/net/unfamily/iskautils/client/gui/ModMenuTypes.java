package net.unfamily.iskautils.client.gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.client.gui.ShopMenu;
import net.unfamily.iskautils.client.gui.AutoShopMenu;

/**
 * Registrazione dei MenuType per le GUI personalizzate
 */
public class ModMenuTypes {
    
    public static final DeferredRegister<MenuType<?>> MENUS = 
        DeferredRegister.create(Registries.MENU, IskaUtils.MOD_ID);
    
    // Structure Placer menu
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<StructurePlacerMenu>> STRUCTURE_PLACER_MENU = 
        MENUS.register("structure_placer", () -> 
            new MenuType<>(StructurePlacerMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    
    // Structure Placer Machine menu  
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<StructurePlacerMachineMenu>> STRUCTURE_PLACER_MACHINE_MENU =
            MENUS.register("structure_placer_machine", () -> 
                new MenuType<>(StructurePlacerMachineMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    
    // Structure Selection menu (without slots)
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<StructureSelectionMenu>> STRUCTURE_SELECTION_MENU = 
        MENUS.register("structure_selection", () -> 
            new MenuType<>(StructureSelectionMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    
    // Structure Saver Machine menu
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<StructureSaverMachineMenu>> STRUCTURE_SAVER_MACHINE_MENU =
            MENUS.register("structure_saver_machine", () -> 
                new MenuType<>(StructureSaverMachineMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<ShopMenu>> SHOP_MENU =
        MENUS.register("shop_menu", () -> new MenuType<>(ShopMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<AutoShopMenu>> AUTO_SHOP_MENU =
        MENUS.register("auto_shop_menu", () -> new MenuType<>(AutoShopMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    
} 