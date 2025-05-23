package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe che gestisce gli eventi specifici del client
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEvents.class);
    
    /**
     * Flag per controllare se il thread è attivo
     */
    private static volatile boolean threadActive = false;

    /**
     * Registra l'evento di tick del client
     */
    public static void init() {
        LOGGER.info("Initializing client events for Vector Charm keys");
        
        // Evita di inizializzare più volte
        if (threadActive) {
            LOGGER.warn("Key checking thread already active, skipping initialization");
            return;
        }
        
        threadActive = true;
        
        // Crea un thread dedicato al controllo dei tasti
        Thread keyCheckThread = new Thread(() -> {
            LOGGER.info("Vector Charm key checking thread started");
            
            while (threadActive) {
                try {
                    // Controlla i tasti ogni 100ms
                    Thread.sleep(100);
                    
                    // Esegue il controllo dei tasti solo nel thread del client
                    if (Minecraft.getInstance() != null) {
                        Minecraft.getInstance().execute(ClientEvents::checkKeysInClientThread);
                    }
                } catch (InterruptedException e) {
                    LOGGER.warn("Vector Charm key checking thread interrupted", e);
                    break;
                } catch (Exception e) {
                    LOGGER.error("Error in key checking thread", e);
                    // Continue running despite errors
                }
            }
            
            LOGGER.info("Vector Charm key checking thread stopped");
        }, "VectorCharmKeyChecker");
        
        // Imposta il thread come daemon in modo che si fermi quando il gioco viene chiuso
        keyCheckThread.setDaemon(true);
        keyCheckThread.start();
    }
    
    /**
     * Metodo per arrestare il thread di controllo tasti
     */
    public static void shutdown() {
        threadActive = false;
        LOGGER.info("Shutting down Vector Charm key checking thread");
    }

    /**
     * Verifica i tasti nel thread del client
     */
    private static void checkKeysInClientThread() {
        if (Minecraft.getInstance().screen == null && Minecraft.getInstance().player != null) {
            KeyBindings.checkKeys();
        }
    }
} 