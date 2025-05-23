package net.unfamily.iskautils.network;

import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe che gestisce i messaggi di rete della mod
 * Versione semplificata per evitare problemi di compatibilità con NeoForge
 */
public class ModMessages {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModMessages.class);

    /**
     * Registra tutti i messaggi di rete
     */
    public static void register() {
        LOGGER.info("Registering network messages for {}", IskaUtils.MOD_ID);
        // Per implementare in seguito quando sarà necessario il networking
    }

    /**
     * Invia un pacchetto al server (stub per quando sarà implementato)
     */
    public static <MSG> void sendToServer(MSG message) {
        // Metodo stub per l'invio di pacchetti al server
        LOGGER.debug("Sending packet to server: {}", message);
    }
} 