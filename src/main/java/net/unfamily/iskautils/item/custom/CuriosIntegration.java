package net.unfamily.iskautils.item.custom;

import net.neoforged.bus.api.IEventBus;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe per gestire l'integrazione con Curios API.
 * Questo codice verrà eseguito solo quando Curios è presente.
 * Implementazione semplificata per evitare problemi di compatibilità.
 */
public class CuriosIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(CuriosIntegration.class);

    /**
     * Registra gli eventi necessari per l'integrazione con Curios
     * @param eventBus Bus degli eventi della mod
     */
    public static void register(IEventBus eventBus) {
        // Registra questa classe solo se Curios è caricato
        if (ModUtils.isCuriosLoaded()) {
            LOGGER.info("Registrando l'integrazione con Curios");
            eventBus.addListener(CuriosIntegration::setupCurios);
        }
    }

    /**
     * Metodo chiamato durante il setup per integrare con Curios
     */
    private static void setupCurios(Object event) {
        // L'evento specifico dipende dalla versione di Curios
        LOGGER.info("Setup dell'integrazione con Curios in corso...");
        LOGGER.info("Vector Charm sarà disponibile come ciondolo quando Curios è presente");
    }
} 