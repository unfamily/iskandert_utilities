package net.unfamily.iskautils.item.custom;

import net.unfamily.iskautils.util.ModLogger;

import net.unfamily.iskautils.util.ModUtils;

/**
 * Class that handles Curios integration for the Vector Charm.
 * This is a proxy that avoids direct dependencies to Curios, so it's safe to load
 * even when Curios is not present.
 */
public class VectorCharmCurioHandler {
    private static final ModLogger LOGGER = ModLogger.of(VectorCharmCurioHandler.class);
    
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