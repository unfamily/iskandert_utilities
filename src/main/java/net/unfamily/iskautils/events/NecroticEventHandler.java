package net.unfamily.iskautils.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.unfamily.iskautils.item.custom.NecroticCrystalHeartCurioHandler;
import net.unfamily.iskautils.item.custom.NecroticCrystalHeartItem;
import net.unfamily.iskautils.stage.StageRegistry;
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
     * annulla completamente il danno.
     *
     * @param event L'evento di danno in arrivo
     */
    @SubscribeEvent
    public static void onEntityAttacked(LivingIncomingDamageEvent event) {
        // Ottiene l'entità che subisce danno
        LivingEntity entity = event.getEntity();
        
        // Verifica se l'entità è un giocatore
        if (entity instanceof Player player && !entity.level().isClientSide()) {
            // Controlla se il giocatore ha lo stage necro_crystal_equip impostato a true
            if (StageRegistry.playerHasStage(player, NecroticCrystalHeartCurioHandler.STAGE_NECRO_CRYSTAL_EQUIP)) {
                // Annulla completamente il danno
                event.setCanceled(true);
                // In alternativa, imposta il danno a 0
                event.setAmount(0.0f);
                
                // Rimuove lo stage per il prossimo danno
                StageRegistry.removePlayerStage(player, NecroticCrystalHeartCurioHandler.STAGE_NECRO_CRYSTAL_EQUIP);
                
                // Debug log
                LOGGER.debug("Danno annullato per il giocatore {} grazie al Necrotic Crystal Heart", 
                        player.getName().getString());
            }
        }
    }
} 