package net.unfamily.iskautils.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.client.KeyBindings;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe che gestisce l'integrazione Curios per il Portable Dislocator.
 * Questo è un proxy che evita dipendenze dirette a Curios, quindi è sicuro da caricare
 * anche quando Curios non è presente.
 */
public class PortableDislocatorCurioHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortableDislocatorCurioHandler.class);
    
    /**
     * Registers the Portable Dislocator as a curio through reflection.
     * Called during mod initialization if Curios is present.
     */
    public static void register() {
        if (!ModUtils.isCuriosLoaded()) return;
        
        try {
            // Registration happens through Curios APIs, but is handled
            // automatically by JSON tags, so nothing special is needed here
            LOGGER.info("Portable Dislocator registered as Curio");
        } catch (Exception e) {
            LOGGER.error("Failed to register Portable Dislocator as Curio", e);
        }
    }
} 