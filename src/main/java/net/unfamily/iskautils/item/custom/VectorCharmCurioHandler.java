package net.unfamily.iskautils.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.client.VectorCharmMovement;
import net.unfamily.iskautils.util.ModUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe che gestisce l'integrazione con Curios per il Vector Charm.
 * Questo è un proxy che evita dipendenze dirette verso Curios, quindi è safe da caricare
 * anche quando Curios non è presente.
 */
public class VectorCharmCurioHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmCurioHandler.class);
    
    /**
     * Registra il Vector Charm come curio tramite reflection.
     * Viene chiamato durante l'inizializzazione della mod se Curios è presente.
     */
    public static void register() {
        if (!ModUtils.isCuriosLoaded()) return;
        
        try {
            LOGGER.info("Registering Vector Charm as Curio");
            
            // La registrazione avviene tramite Curios API, ma è gestita
            // automaticamente dai tag JSON, quindi qui non serve fare nulla di speciale
            
        } catch (Exception e) {
            LOGGER.error("Failed to register Vector Charm as Curio", e);
        }
    }
    
    /**
     * Questo metodo viene chiamato quando il curio è equipaggiato.
     * Può essere chiamato tramite reflection dalla Curios API.
     * @param stack L'item stack del Vector Charm
     * @param entity L'entità che indossa il curio
     */
    public static void onEquip(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && entity.level().isClientSide) {
            LOGGER.debug("Vector Charm equipped as Curio by {}", player.getScoreboardName());
        }
    }
    
    /**
     * Questo metodo viene chiamato quando il curio è rimosso.
     * Può essere chiamato tramite reflection dalla Curios API.
     * @param stack L'item stack del Vector Charm
     * @param entity L'entità che indossava il curio
     */
    public static void onUnequip(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && entity.level().isClientSide) {
            LOGGER.debug("Vector Charm unequipped as Curio by {}", player.getScoreboardName());
        }
    }
    
    /**
     * Questo metodo viene chiamato ad ogni tick quando il curio è equipaggiato.
     * Può essere chiamato tramite reflection dalla Curios API.
     * @param stack L'item stack del Vector Charm
     * @param entity L'entità che indossa il curio
     */
    public static void curioTick(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && entity.level().isClientSide) {
            // Applica il movimento usando la stessa logica del tick client
            VectorCharmMovement.applyMovement(player);
            LOGGER.debug("Vector Charm ticking as Curio for {} - applying movement", player.getScoreboardName());
        }
    }
} 