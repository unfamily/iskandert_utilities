package net.unfamily.iskautils.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.VectorCharmData;
import net.unfamily.iskautils.data.VectorFactorType;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages keybindings for the mod
 */
public class KeyBindings {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyBindings.class);
    
    public static final String KEY_CATEGORY_ISKA_UTILS = "key.category.iska_utils";
    public static final String KEY_VECTOR_VERTICAL = "key.iska_utils.vector_vertical";
    public static final String KEY_VECTOR_HORIZONTAL = "key.iska_utils.vector_horizontal";
    public static final String KEY_VECTOR_HOVER = "key.iska_utils.vector_hover";
    public static final String KEY_PORTABLE_DISLOCATOR = "key.iska_utils.portable_dislocator";

    public static final KeyMapping VECTOR_VERTICAL_KEY = new KeyMapping(
            KEY_VECTOR_VERTICAL,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,  // V key for vertical adjustment
            KEY_CATEGORY_ISKA_UTILS
    );

    public static final KeyMapping VECTOR_HORIZONTAL_KEY = new KeyMapping(
            KEY_VECTOR_HORIZONTAL,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,  // B key for horizontal adjustment
            KEY_CATEGORY_ISKA_UTILS
    );

    public static final KeyMapping VECTOR_HOVER_KEY = new KeyMapping(
            KEY_VECTOR_HOVER,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,  // H key for hover
            KEY_CATEGORY_ISKA_UTILS
    );

    public static final KeyMapping PORTABLE_DISLOCATOR_KEY = new KeyMapping(
            KEY_PORTABLE_DISLOCATOR,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,  // G key for dislocator
            KEY_CATEGORY_ISKA_UTILS
    );

    /**
     * Registers key bindings
     */
    @EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class KeybindHandler {
        @SubscribeEvent
        public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
            event.register(VECTOR_VERTICAL_KEY);
            event.register(VECTOR_HORIZONTAL_KEY);
            event.register(VECTOR_HOVER_KEY);
            event.register(PORTABLE_DISLOCATOR_KEY);
        }
    }

    /**
     * Handles key presses and displays messages to the client
     */
    public static void checkKeys() {
        // Check if the player has pressed Vector Charm keys
        if (Minecraft.getInstance().player != null) {
            Player player = Minecraft.getInstance().player;
            
            // Key for vertical adjustment
            if (VECTOR_VERTICAL_KEY.consumeClick()) {
                // Use persistent player data instead of singleton
                byte currentFactor = VectorCharmData.getVerticalFactorFromPlayer(player);
                byte nextFactor = (byte) ((currentFactor + 1) % 6); // 0-5 (None, Slow, Moderate, Fast, Extreme, Ultra)
                VectorCharmData.setVerticalFactorToPlayer(player, nextFactor);
                VectorFactorType newFactor = VectorFactorType.fromByte(nextFactor);
                
                // Singleton no longer needed - using persistent data only
                
                // Send message to player
                player.displayClientMessage(Component.translatable("message.iska_utils.vector_vertical_factor", 
                                     Component.translatable("vectorcharm.factor." + newFactor.getName())), true);
            }

            // Key for horizontal adjustment
            if (VECTOR_HORIZONTAL_KEY.consumeClick()) {
                // Use persistent player data instead of singleton
                byte currentFactor = VectorCharmData.getHorizontalFactorFromPlayer(player);
                byte nextFactor = (byte) ((currentFactor + 1) % 6); // 0-5 (None, Slow, Moderate, Fast, Extreme, Ultra)
                VectorCharmData.setHorizontalFactorToPlayer(player, nextFactor);
                VectorFactorType newFactor = VectorFactorType.fromByte(nextFactor);
                
                // Singleton no longer needed - using persistent data only
                
                // Send message to player
                player.displayClientMessage(Component.translatable("message.iska_utils.vector_horizontal_factor", 
                                     Component.translatable("vectorcharm.factor." + newFactor.getName())), true);
            }

            // Key for hover mode toggle
            if (VECTOR_HOVER_KEY.consumeClick()) {
                // Use persistent player data instead of singleton
                byte currentFactor = VectorCharmData.getVerticalFactorFromPlayer(player);
                
                // Special value for hover mode is 6
                byte hoverFactor = VectorCharmData.HOVER_MODE_VALUE;
                
                // Simple toggle: normal mode <-> hover mode
                if (currentFactor == hoverFactor) {
                    // If in hover mode, disable it and restore previous value
                    byte restoredFactor = VectorCharmData.disableHoverModeFromPlayer(player);
                    
                    // Singleton no longer needed - using persistent data only
                    
                    // Send message to player
                    player.displayClientMessage(Component.translatable("message.iska_utils.vector_hover_mode.off"), true);
                } else {
                    // Otherwise, activate hover mode
                    VectorCharmData.setVerticalFactorToPlayer(player, hoverFactor);
                    
                    // Singleton no longer needed - using persistent data only
                    
                    // Send message to player
                    player.displayClientMessage(Component.translatable("message.iska_utils.vector_hover_mode.hover"), true);
                }
            }

            // Key for Portable Dislocator
            // Note: Now handled in PortableDislocatorItem tick methods
            // if (PORTABLE_DISLOCATOR_KEY.consumeClick()) {
            //     handlePortableDislocatorActivation(player);
            // }
        }
    }
} 