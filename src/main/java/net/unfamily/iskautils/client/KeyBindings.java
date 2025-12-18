package net.unfamily.iskautils.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.VectorCharmData;
import net.unfamily.iskautils.data.VectorFactorType;
import net.unfamily.iskautils.network.ModMessages;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages keybindings for the mod
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class KeyBindings {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyBindings.class);

    private static final String KEY_CATEGORY_ISKA_UTILS = "key.categories.iska_utils";
    private static final String KEY_VECTOR_VERTICAL = "key.iska_utils.vector_vertical";
    private static final String KEY_VECTOR_HORIZONTAL = "key.iska_utils.vector_horizontal";
    private static final String KEY_VECTOR_HOVER = "key.iska_utils.vector_hover";
    private static final String KEY_VECTOR_VERTICAL_DECREASE = "key.iska_utils.vector_vertical_decrease";
    private static final String KEY_VECTOR_HORIZONTAL_DECREASE = "key.iska_utils.vector_horizontal_decrease";
    private static final String KEY_PORTABLE_DISLOCATOR = "key.iska_utils.portable_dislocator";
    private static final String KEY_STRUCTURE_UNDO = "key.iska_utils.structure_undo";
    private static final String KEY_BURNING_BRAZIER_TOGGLE = "key.iska_utils.burning_brazier_toggle";
    private static final String KEY_GHOST_BRAZIER_TOGGLE = "key.iska_utils.ghost_brazier_toggle";

    // Vector Charm keys
    public static final KeyMapping VECTOR_VERTICAL_KEY = new KeyMapping(
            KEY_VECTOR_VERTICAL,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,  // Default: C for climb/vertical
            KEY_CATEGORY_ISKA_UTILS
    );

    public static final KeyMapping VECTOR_HORIZONTAL_KEY = new KeyMapping(
            KEY_VECTOR_HORIZONTAL,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,  // Default: V for velocity/horizontal
            KEY_CATEGORY_ISKA_UTILS
    );

    public static final KeyMapping VECTOR_HOVER_KEY = new KeyMapping(
            KEY_VECTOR_HOVER,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,  // Default: H for hover
            KEY_CATEGORY_ISKA_UTILS
    );

    public static final KeyMapping VECTOR_VERTICAL_DECREASE_KEY = new KeyMapping(
            KEY_VECTOR_VERTICAL_DECREASE,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,  // Z key for decreasing vertical force
            KEY_CATEGORY_ISKA_UTILS
    );

    public static final KeyMapping VECTOR_HORIZONTAL_DECREASE_KEY = new KeyMapping(
            KEY_VECTOR_HORIZONTAL_DECREASE,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,  // X key for decreasing horizontal force
            KEY_CATEGORY_ISKA_UTILS
    );

    // Portable Dislocator key
    public static final KeyMapping PORTABLE_DISLOCATOR_KEY = new KeyMapping(
            KEY_PORTABLE_DISLOCATOR,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,  // K key for dislocator
            KEY_CATEGORY_ISKA_UTILS
    );

    // Structure Undo key
    public static final KeyMapping STRUCTURE_UNDO_KEY = new KeyMapping(
            KEY_STRUCTURE_UNDO,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,  // U key for undo
            KEY_CATEGORY_ISKA_UTILS
    );

    // Burning Brazier auto-placement toggle key
    public static final KeyMapping BURNING_BRAZIER_TOGGLE_KEY = new KeyMapping(
            KEY_BURNING_BRAZIER_TOGGLE,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,  // B key for brazier
            KEY_CATEGORY_ISKA_UTILS
    );

    // Ghost Brazier toggle key
    public static final KeyMapping GHOST_BRAZIER_TOGGLE_KEY = new KeyMapping(
            KEY_GHOST_BRAZIER_TOGGLE,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,  // G key for ghost
            KEY_CATEGORY_ISKA_UTILS
    );

    /**
     * Registers key bindings
     */
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(VECTOR_VERTICAL_KEY);
        event.register(VECTOR_HORIZONTAL_KEY);
        event.register(VECTOR_HOVER_KEY);
        event.register(VECTOR_VERTICAL_DECREASE_KEY);
        event.register(VECTOR_HORIZONTAL_DECREASE_KEY);
        event.register(PORTABLE_DISLOCATOR_KEY);
        event.register(STRUCTURE_UNDO_KEY);
        event.register(BURNING_BRAZIER_TOGGLE_KEY);
        event.register(GHOST_BRAZIER_TOGGLE_KEY);
        LOGGER.info("Registered all key mappings");
    }

    /**
     * Handles Vector Charm key events and displays messages to the client
     * Called from ClientEvents.checkKeysInClientThread()
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
                    
                    // Send message to player
                    player.displayClientMessage(Component.translatable("message.iska_utils.vector_hover_mode.off"), true);
                } else {
                    // Otherwise, activate hover mode
                    VectorCharmData.setVerticalFactorToPlayer(player, hoverFactor);
                    
                    // Send message to player
                    player.displayClientMessage(Component.translatable("message.iska_utils.vector_hover_mode.hover"), true);
                }
            }
            
            // Key for vertical adjustment decrease
            if (VECTOR_VERTICAL_DECREASE_KEY.consumeClick()) {
                // Use persistent player data instead of singleton
                byte currentFactor = VectorCharmData.getVerticalFactorFromPlayer(player);
                byte nextFactor = (byte) ((currentFactor - 1 + 6) % 6); // 0-5 (None, Slow, Moderate, Fast, Extreme, Ultra)
                VectorCharmData.setVerticalFactorToPlayer(player, nextFactor);
                VectorFactorType newFactor = VectorFactorType.fromByte(nextFactor);
                
                // Send message to player
                player.displayClientMessage(Component.translatable("message.iska_utils.vector_vertical_factor", 
                                     Component.translatable("vectorcharm.factor." + newFactor.getName())), true);
            }

            // Key for horizontal adjustment decrease
            if (VECTOR_HORIZONTAL_DECREASE_KEY.consumeClick()) {
                // Use persistent player data instead of singleton
                byte currentFactor = VectorCharmData.getHorizontalFactorFromPlayer(player);
                byte nextFactor = (byte) ((currentFactor - 1 + 6) % 6); // 0-5 (None, Slow, Moderate, Fast, Extreme, Ultra)
                VectorCharmData.setHorizontalFactorToPlayer(player, nextFactor);
                VectorFactorType newFactor = VectorFactorType.fromByte(nextFactor);
                
                // Send message to player
                player.displayClientMessage(Component.translatable("message.iska_utils.vector_horizontal_factor",
                                     Component.translatable("vectorcharm.factor." + newFactor.getName())), true);
            }

            // Key for Burning Brazier auto-placement toggle
            if (BURNING_BRAZIER_TOGGLE_KEY.consumeClick()) {
                // Send toggle request to server
                ModMessages.sendBurningBrazierTogglePacket();
            }

            // Ghost Brazier keybind is handled in the item's inventoryTick method
            // The item checks if the keybind was pressed when it ticks
        }
    }
    
    /**
     * Checks if the structure undo key has been pressed and consumes the event
     * @return true if the key was pressed, false otherwise
     */
    public static boolean consumeStructureUndoKeyClick() {
        return STRUCTURE_UNDO_KEY.consumeClick();
    }
    
    /**
     * Checks if the dislocator key has been pressed and consumes the event
     * @return true if the key was pressed, false otherwise
     */
    public static boolean consumeDislocatorKeyClick() {
        return PORTABLE_DISLOCATOR_KEY.consumeClick();
    }

    /**
     * Checks if the burning brazier toggle key has been pressed and consumes the event
     * @return true if the key was pressed, false otherwise
     */
    public static boolean consumeBurningBrazierToggleKeyClick() {
        return BURNING_BRAZIER_TOGGLE_KEY.consumeClick();
    }

    /**
     * Checks if the ghost brazier toggle key has been pressed and consumes the event
     * @return true if the key was pressed, false otherwise
     */
    public static boolean consumeGhostBrazierToggleKeyClick() {
        return GHOST_BRAZIER_TOGGLE_KEY.consumeClick();
    }
} 