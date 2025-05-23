package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that manages client-specific events
 */
public class ClientEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEvents.class);
    
    /**
     * Flag to check if the thread is active
     */
    private static volatile boolean threadActive = false;

    /**
     * Registers the client tick event
     */
    public static void init() {
        // Avoid initializing multiple times
        if (threadActive) {
            return;
        }
        
        threadActive = true;
        
        // Create a dedicated thread for key checking
        Thread keyCheckThread = new Thread(() -> {
            while (threadActive) {
                try {
                    // Check keys every 100ms
                    Thread.sleep(100);
                    
                    // Execute key checking only in the client thread
                    if (Minecraft.getInstance() != null) {
                        Minecraft.getInstance().execute(ClientEvents::checkKeysInClientThread);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    // Continue running despite errors
                }
            }
        }, "VectorCharmKeyChecker");
        
        // Set the thread as daemon so it stops when the game is closed
        keyCheckThread.setDaemon(true);
        keyCheckThread.start();
    }
    
    /**
     * Method to stop the key checking thread
     */
    public static void shutdown() {
        threadActive = false;
    }

    /**
     * Check keys in the client thread
     */
    private static void checkKeysInClientThread() {
        // Check keys only if there is no GUI open
        if (Minecraft.getInstance().screen == null && Minecraft.getInstance().player != null) {
            KeyBindings.checkKeys();
        }
        
        // We no longer apply movement here, as it's done directly by the item tick methods
    }
} 