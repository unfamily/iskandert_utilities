package net.unfamily.iskautils.item.custom;

import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that handles Curios integration for the Vector Charm.
 * This is a proxy that avoids direct dependencies to Curios, so it's safe to load
 * even when Curios is not present.
 */
public class VectorCharmCurioHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmCurioHandler.class);
    
    /**
     * Registers the Vector Charm as a curio through reflection.
     * Called during mod initialization if Curios is present.
     */
    public static void register() {
        if (!ModUtils.isCuriosLoaded()) return;
        
        try {
            // LOGGER.info("Registering Vector Charm as Curio");
            
            // Registration happens through Curios API, but it's handled
            // automatically by JSON tags, so nothing special is needed here
            
        } catch (Exception e) {
            LOGGER.error("Failed to register Vector Charm as Curio", e);
        }
    }
} 