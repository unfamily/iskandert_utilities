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
    
    /**
     * Called when the curio is equipped.
     * Can be called through reflection from Curios APIs.
     */
    public static void onEquip(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player) {
            LOGGER.debug("Portable Dislocator equipped by player {}", player.getName().getString());
        }
    }
    
    /**
     * Called when the curio is unequipped.
     * Can be called through reflection from Curios APIs.
     */
    public static void onUnequip(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player) {
            LOGGER.debug("Portable Dislocator unequipped by player {}", player.getName().getString());
        }
    }
    
    /**
     * Called every tick when the curio is equipped.
     * Can be called through reflection from Curios APIs.
     */
    public static void curioTick(ItemStack stack, LivingEntity entity) {
        // Semplicemente controlla se è un giocatore e poi gestisce l'attivazione
        if (entity instanceof Player player) {
            // Gestione lato client - verifica della pressione del tasto
            if (entity.level().isClientSide() && KeyBindings.PORTABLE_DISLOCATOR_KEY.consumeClick()) {
                LOGGER.info("Portable Dislocator key pressed from Curios slot");
                // Attiva direttamente il dislocator senza passare per la reflection
                activateDislocatorFromCurio(stack, player);
            }
            
            // Gestione lato server - processa le teleport pending
            if (!entity.level().isClientSide()) {
                // Gestisce eventuali teleport pending
                PortableDislocatorItem.handlePendingTeleportation(player, entity.level());
                PortableDislocatorItem.checkForTeleportRequest(player, entity.level());
            }
        }
    }
    
    /**
     * Attiva il Portable Dislocator quando è equipaggiato come Curio
     * Questo metodo è una versione semplificata di handleDislocatorActivation
     * che viene chiamata direttamente quando il keybind è premuto
     */
    private static void activateDislocatorFromCurio(ItemStack stack, Player player) {
        if (player.level().isClientSide()) {
            // Lato client, crea una richiesta che poi verrà gestita dal server
            LOGGER.info("Creating teleport request from curios slot on client side");
            // Notifica all'utente che il Dislocator è stato attivato
            PortableDislocatorItem.handleDislocatorActivation(player, "curios");
        }
    }
} 