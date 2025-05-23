package net.unfamily.iskautils.util;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe di utilità per la mod
 */
public class ModUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModUtils.class);
    
    /**
     * ID della mod Curios per l'integrazione
     */
    public static final String CURIOS_MOD_ID = "curios";
    
    /**
     * Controlla se la mod Curios è caricata
     * @return true se Curios è caricato
     */
    public static boolean isCuriosLoaded() {
        boolean isLoaded = ModList.get().isLoaded(CURIOS_MOD_ID);
        if (isLoaded) {
            LOGGER.debug("Curios mod is loaded");
        } else {
            LOGGER.debug("Curios mod is not loaded");
        }
        return isLoaded;
    }
    
    /**
     * Converte i tick in secondi (approssimati).
     * @param ticks Numero di tick
     * @return Secondi (approssimativamente)
     */
    public static float ticksToSeconds(int ticks) {
        return ticks / 20f;
    }
    
    /**
     * Converte i secondi in tick.
     * @param seconds Secondi
     * @return Numero di tick equivalenti
     */
    public static int secondsToTicks(float seconds) {
        return (int) (seconds * 20);
    }
} 