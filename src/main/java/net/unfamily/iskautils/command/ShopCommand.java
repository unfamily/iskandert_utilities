package net.unfamily.iskautils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopTransactionManager;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Command to test the shop system
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class ShopCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {

        register(event.getDispatcher());
        
        // Register team commands

        ShopTeamCommand.register(event.getDispatcher());
    }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("iska_utils_shop")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("reload")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    source.sendSuccess(() -> Component.literal("Reloading shop system..."), false);
                    
                    try {
                        ShopLoader.reloadAllConfigurations();
                        
                        // Notify client GUIs of the reload
                        notifyClientGUIReload();
                        
                        source.sendSuccess(() -> Component.literal("Shop system reloaded successfully!"), false);
                        return 1;
                    } catch (Exception e) {
                        LOGGER.error("Error during shop system reload: {}", e.getMessage());
                        source.sendFailure(Component.literal("Error during shop system reload: " + e.getMessage()));
                        return 0;
                    }
                }))
            .then(Commands.literal("info")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    showShopInfo(source);
                    return 1;
                }))
            .then(Commands.literal("currencies")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    showValutes(source);
                    return 1;
                }))
            .then(Commands.literal("valutes") // legacy command
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    showValutes(source);
                    return 1;
                }))
            .then(Commands.literal("categories")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    showCategories(source);
                    return 1;
                }))
            .then(Commands.literal("entries")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    showEntries(source);
                    return 1;
                })
                .then(Commands.argument("category", StringArgumentType.string())
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        String category = StringArgumentType.getString(context, "category");
                        showEntriesInCategory(source, category);
                        return 1;
                    })))
            .then(Commands.literal("balance")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (source.getPlayer() != null) {
                        ShopTransactionManager.showTeamBalance(source.getPlayer());
                    } else {
                        source.sendFailure(Component.literal("This command can only be used by players"));
                    }
                    return 1;
                }))
        );
    }
    
    private static void showShopInfo(CommandSourceStack source) {
        Map<String, ShopCurrency> currencies = ShopLoader.getCurrencies();
        Map<String, ShopCategory> categories = ShopLoader.getCategories();
        Map<String, ShopEntry> entries = ShopLoader.getEntries();
        
        source.sendSuccess(() -> Component.literal("=== Shop System Info ==="), false);
        source.sendSuccess(() -> Component.literal("Loaded currencies: " + currencies.size()), false);
        source.sendSuccess(() -> Component.literal("Loaded categories: " + categories.size()), false);
        source.sendSuccess(() -> Component.literal("Loaded entries: " + entries.size()), false);
    }
    
    private static void showValutes(CommandSourceStack source) {
        Map<String, ShopCurrency> currencies = ShopLoader.getCurrencies();
        
        source.sendSuccess(() -> Component.literal("=== Available Currencies ==="), false);
        for (ShopCurrency currency : currencies.values()) {
            // Use localized name and format with symbol after
            String localizedName = Component.translatable(currency.name).getString();
            String formattedName = localizedName + " " + currency.charSymbol;
            source.sendSuccess(() -> Component.literal(
                String.format("- %s (%s): %s", currency.id, formattedName, currency.charSymbol)
            ), false);
        }
    }
    
    private static void showCategories(CommandSourceStack source) {
        Map<String, ShopCategory> categories = ShopLoader.getCategories();
        
        source.sendSuccess(() -> Component.literal("=== Available Categories ==="), false);
        for (ShopCategory category : categories.values()) {
            source.sendSuccess(() -> Component.literal(
                String.format("- %s (%s): %s", category.id, category.name, category.description)
            ), false);
        }
    }
    
    private static void showEntries(CommandSourceStack source) {
        Map<String, ShopEntry> entries = ShopLoader.getEntries();
        
        source.sendSuccess(() -> Component.literal("=== All Entries ==="), false);
        for (Map.Entry<String, ShopEntry> entry : entries.entrySet()) {
            ShopEntry shopEntry = entry.getValue();
            String category = shopEntry.inCategory != null ? shopEntry.inCategory : "default";
            String valute = shopEntry.valute != null ? shopEntry.valute : "default";
            
            source.sendSuccess(() -> Component.literal(
                String.format("- %s (Cat: %s, Val: %s, Buy: %.1f, Sell: %.1f)", 
                    shopEntry.item, category, valute, shopEntry.buy, shopEntry.sell)
            ), false);
        }
    }
    
    private static void showEntriesInCategory(CommandSourceStack source, String categoryId) {
        Map<String, ShopEntry> entries = ShopLoader.getEntries();
        
        source.sendSuccess(() -> Component.literal("=== Entries in Category: " + categoryId + " ==="), false);
        
        boolean found = false;
        for (Map.Entry<String, ShopEntry> entry : entries.entrySet()) {
            ShopEntry shopEntry = entry.getValue();
            if (categoryId.equals(shopEntry.inCategory)) {
                String valute = shopEntry.valute != null ? shopEntry.valute : "default";
                source.sendSuccess(() -> Component.literal(
                    String.format("- %s (Val: %s, Buy: %.1f, Sell: %.1f)", 
                        shopEntry.item, valute, shopEntry.buy, shopEntry.sell)
                ), false);
                found = true;
            }
        }
        
        if (!found) {
            source.sendSuccess(() -> Component.literal("No entries found for category: " + categoryId), false);
        }
    }
    
    /**
     * Notifies client GUIs of reload (executed on client)
     */
    private static void notifyClientGUIReload() {
        try {
            // This runs on server, but must notify the client
            // On integrated server (single player), we can call GUIs directly
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                net.unfamily.iskautils.client.gui.ShopScreen.notifyReload();
            });
        } catch (Exception e) {
            // Ignore errors when running on dedicated server
        }
    }
} 