package net.unfamily.iskautils.iska_utils_stages;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.IskaUtils;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import org.slf4j.Logger;


/**
 * Manages events and configuration for the item stage system
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class StageItemManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized = false;
    
    /**
     * Initializes the item stage system
     */
    public static void initialize(FMLCommonSetupEvent event) {
        if (initialized) {
            return;
        }
        
        try {
            StageItemHandler.loadAll(null);
            initialized = true;
            LOGGER.info("Item stage system successfully initialized from datapack load");
        } catch (Exception e) {
            LOGGER.error("Error initializing item stage system: {}", e.getMessage());
        }
    }
    
    /**
     * Handles container open events
     */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!initialized) {
            return;
        }
        
        AbstractContainerMenu container = event.getContainer();
        Player player = event.getEntity();
        
        try {
            // Check and apply item restrictions
            StageItemHandler.checkContainer(container, player);
        } catch (Exception e) {
            LOGGER.error("Error checking container: {}", e.getMessage());
        }
    }
    
    /**
     * Handles right-click item interactions
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!initialized || event.getLevel().isClientSide()) {
            return;
        }
        
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        
        try {
            if (StageItemHandler.shouldBlockRightClick(player, itemStack)) {
                event.setCanceled(true);
            }
            
            // Check main/off hand
            checkAndApplyHandRestrictions(player);
        } catch (Exception e) {
            LOGGER.error("Error checking right-click restriction: {}", e.getMessage());
        }
    }
    
    /**
     * Handles right-click block interactions
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!initialized || event.getLevel().isClientSide()) {
            return;
        }
        
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        
        try {
            if (StageItemHandler.shouldBlockRightClick(player, itemStack)) {
                event.setCanceled(true);
            }
            
            // Check main/off hand
            checkAndApplyHandRestrictions(player);
        } catch (Exception e) {
            LOGGER.error("Error checking right-click block restriction: {}", e.getMessage());
        }
    }
    
    /**
     * Handles left-click block interactions
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!initialized || event.getLevel().isClientSide()) {
            return;
        }
        
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        
        try {
            if (StageItemHandler.shouldBlockLeftClick(player, itemStack)) {
                event.setCanceled(true);
                
                // Notify the player
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                        "message.iska_utils.item_restriction.blocked"));
                }
            }
            
            // Check main/off hand
            checkAndApplyHandRestrictions(player);
        } catch (Exception e) {
            LOGGER.error("Error checking left-click restriction: {}", e.getMessage());
        }
    }
    
    /**
     * Handles equipment change events for main/off hand checking
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!initialized || event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        try {
            checkAndApplyHandRestrictions((Player) event.getEntity());
        } catch (Exception e) {
            LOGGER.error("Error checking equipment change: {}", e.getMessage());
        }
    }
    
    /**
     * Handles entity attack (left-click on entity)
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!initialized || event.getEntity().level().isClientSide()) {
            return;
        }
        
        Player player = event.getEntity();
        ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        
        try {
            if (StageItemHandler.shouldBlockLeftClick(player, itemStack)) {
                event.setCanceled(true);
            }
            
            // Check main/off hand
            checkAndApplyHandRestrictions(player);
        } catch (Exception e) {
            LOGGER.error("Error checking attack entity restriction: {}", e.getMessage());
        }
    }
    
    /**
     * Helper method to check and apply restrictions for main/off hand
     */
    private static void checkAndApplyHandRestrictions(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Check main hand
            ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!mainHandItem.isEmpty()) {
                String mainHandConsequence = StageItemHandler.checkMainHandRestriction(player, mainHandItem);
                if (mainHandConsequence != null) {
                    StageItemHandler.applyHandConsequence(serverPlayer, InteractionHand.MAIN_HAND, mainHandConsequence);
                }
            }
            
            // Check off hand
            ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!offHandItem.isEmpty()) {
                String offHandConsequence = StageItemHandler.checkOffHandRestriction(player, offHandItem);
                if (offHandConsequence != null) {
                    StageItemHandler.applyHandConsequence(serverPlayer, InteractionHand.OFF_HAND, offHandConsequence);
                }
            }
        }
    }
    
    /**
     * Reloads item restrictions (useful for admin commands)
     */
    public static void reloadItemRestrictions() {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            StageItemHandler.loadAll(server != null ? server.getResourceManager() : null);
            LOGGER.info("Item restrictions reloaded from datapack");
        } catch (Exception e) {
            LOGGER.error("Error reloading item restrictions: {}", e.getMessage());
        }
    }
}