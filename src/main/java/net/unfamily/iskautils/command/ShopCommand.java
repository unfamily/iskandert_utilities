package net.unfamily.iskautils.command;

import net.unfamily.iskautils.util.ModLogger;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.load.IskaUtilsLoadReloadEffects;
import net.unfamily.iskautils.shop.ShopLoader;
import net.unfamily.iskautils.shop.ShopCategory;
import net.unfamily.iskautils.shop.ShopCurrency;
import net.unfamily.iskautils.shop.ShopEntry;
import net.unfamily.iskautils.shop.ShopTransactionManager;

import java.util.Map;

/**
 * Command to test the shop system
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class ShopCommand {
    private static final ModLogger LOGGER = ModLogger.of(ShopCommand.class);
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {

        register(event.getDispatcher());
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
                        IskaUtilsLoadReloadEffects.sendReloadNotice(source);
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
            .then(Commands.literal("edit")
                .executes(context -> openShopEditor(context.getSource())))
        );
    }

    private static int openShopEditor(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        if (!net.unfamily.iskautils.shop.edit.ShopEditSession.tryAcquire(player)) {
            source.sendFailure(net.unfamily.iskautils.shop.edit.ShopEditSession.occupiedMessage());
            return 0;
        }
        player.openMenu(new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.iska_utils.shop_edit.title");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player p) {
                return new net.unfamily.iskautils.client.gui.ShopEditMenu(id, inv, true);
            }
        });
        if (player.containerMenu instanceof net.unfamily.iskautils.client.gui.ShopEditMenu) {
            var data = net.unfamily.iskautils.shop.edit.ShopEditSession.getData();
            if (data != null) {
                net.unfamily.iskautils.network.packet.ShopEditSyncS2CPacket.sendTo(player, data);
            }
        }
        return 1;
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
    
    /**
     * Notifies client GUIs of reload (executed on client).
     * Public for use by iska_lib_debug reload.
     */
    public static void notifyClientGUIReload() {
        net.unfamily.iskautils.util.ClientRuntimeAccess.runOnClientThread(
                net.unfamily.iskautils.util.ClientGuiAccess::notifyShopReload);
    }
} 