package net.unfamily.iskautils.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.item.custom.NecroticCrystalHeartCurioHandler;
import net.unfamily.iskautils.item.custom.NecroticCrystalHeartItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler per intercettare gli eventi di danno e gestire il Necrotic Crystal Heart.
 */
@EventBusSubscriber
public class NecroticEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NecroticEventHandler.class);

    /**
     * Intercetta l'evento di danno in arrivo per un'entità.
     * Se l'entità ha equipaggiato un Necrotic Crystal Heart come curio,
     * modifica il danno secondo la logica definita nell'item.
     *
     * @param event L'evento di danno in arrivo
     */
    @SubscribeEvent
    public static void onEntityAttacked(LivingIncomingDamageEvent event) {
        // Ottiene l'entità che subisce danno
        LivingEntity entity = event.getEntity();
        
        // Controlla se l'entità ha il Necrotic Crystal Heart equipaggiato
        if (NecroticCrystalHeartCurioHandler.isNecroticHeartEquipped(entity)) {
            // Ottiene la quantità di danno originale
            float originalDamage = event.getAmount();
            
            // Azzera completamente il danno (sarà 0.0f come modificato nel NecroticCrystalHeartItem)
            event.setAmount(0.0f);
            
            LOGGER.debug("Danno annullato dal Necrotic Crystal Heart: {} -> 0.0", originalDamage);
        }
    }
} 