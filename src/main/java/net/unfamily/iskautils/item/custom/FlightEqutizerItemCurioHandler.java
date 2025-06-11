package net.unfamily.iskautils.item.custom;

import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that handles Curios integration for the Flight Equitizer.
 * This is a proxy that avoids direct dependencies on Curios, so it's safe to load
 * even when Curios is not present.
 */
public class FlightEqutizerItemCurioHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlightEqutizerItemCurioHandler.class);
   
    /**
     * Registers the Flight Equitizer as a curio.
     * Called during mod initialization if Curios is present.
     */
    public static void register() {
        if (!ModUtils.isCuriosLoaded()) return;
        
        try {
            // Registration happens through Curios APIs, but is handled
            // automatically by JSON tags, so nothing special needs to be done here
        } catch (Exception e) {
            LOGGER.error("Could not register Necrotic Crystal Heart as Curio", e);
        }
    }
} 