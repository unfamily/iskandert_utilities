package net.unfamily.iskautils.util;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for the mod
 */
public class ModUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModUtils.class);
    
    /**
     * Curios mod ID for integration
     */
    private static final String CURIOS_MOD_ID = "curios";
    
    /**
     * Nature's Compass mod ID for integration
     */
    private static final String NATURES_COMPASS_MOD_ID = "naturescompass";
    
    /**
     * Explorer's Compass mod ID for integration
     */
    private static final String EXPLORERS_COMPASS_MOD_ID = "explorerscompass";
    
    /**
     * Checks if the Curios mod is loaded
     * @return true if Curios is loaded
     */
    public static boolean isCuriosLoaded() {
        boolean loaded = ModList.get().isLoaded(CURIOS_MOD_ID);
        if (loaded) {
            // LOGGER.debug("Curios mod is loaded");
        } else {
            // LOGGER.debug("Curios mod is not loaded");
        }
        return loaded;
    }
    
    /**
     * Checks if the Nature's Compass mod is loaded
     * @return true if Nature's Compass is loaded
     */
    public static boolean isNaturesCompassLoaded() {
        boolean loaded = ModList.get().isLoaded(NATURES_COMPASS_MOD_ID);
        if (loaded) {
            // LOGGER.debug("Nature's Compass mod is loaded");
        } else {
            // LOGGER.debug("Nature's Compass mod is not loaded");
        }
        return loaded;
    }
    
    /**
     * Checks if the Explorer's Compass mod is loaded
     * @return true if Explorer's Compass is loaded
     */
    public static boolean isExplorersCompassLoaded() {
        boolean loaded = ModList.get().isLoaded(EXPLORERS_COMPASS_MOD_ID);
        if (loaded) {
            // LOGGER.debug("Explorer's Compass mod is loaded");
        } else {
            // LOGGER.debug("Explorer's Compass mod is not loaded");
        }
        return loaded;
    }
    
    /**
     * Converts ticks to seconds (approximately).
     * @param ticks Number of ticks
     * @return Seconds (approximately)
     */
    public static float ticksToSeconds(int ticks) {
        return ticks / 20f;
    }
    
    /**
     * Converts seconds to ticks.
     * @param seconds Seconds
     * @return Equivalent number of ticks
     */
    public static int secondsToTicks(float seconds) {
        return (int) (seconds * 20);
    }
} 