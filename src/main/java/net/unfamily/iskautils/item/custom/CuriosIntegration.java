package net.unfamily.iskautils.item.custom;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle integration with Curios API.
 * This code will only run when Curios is present.
 * Simplified implementation to avoid compatibility issues.
 */
public class CuriosIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(CuriosIntegration.class);

    /**
     * Registers the necessary events for Curios integration
     * @param eventBus Mod event bus
     */
    public static void register(IEventBus eventBus) {
        // Register this class only if Curios is loaded
        if (ModUtils.isCuriosLoaded()) {
            // LOGGER.info("Registering Curios integration");
            eventBus.addListener(CuriosIntegration::setupCurios);
        }
    }

    /**
     * Method called during setup to integrate with Curios
     * Uses FMLCommonSetupEvent which is a valid event for the mod bus
     */
    private static void setupCurios(FMLCommonSetupEvent event) {
        // Register Vector Charm as curio
        VectorCharmCurioHandler.register();
        
        // Register Portable Dislocator as curio
        PortableDislocatorCurioHandler.register();
        
    }
} 