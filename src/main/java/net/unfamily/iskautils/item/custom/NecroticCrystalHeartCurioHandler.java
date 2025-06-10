package net.unfamily.iskautils.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.unfamily.iskautils.util.ModUtils;
import net.unfamily.iskautils.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe che gestisce l'integrazione Curios per il Necrotic Crystal Heart.
 * Questo è un proxy che evita dipendenze dirette a Curios, quindi è sicuro da caricare
 * anche quando Curios non è presente.
 */
public class NecroticCrystalHeartCurioHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NecroticCrystalHeartCurioHandler.class);
    
    // Nome dello stage per controllare se il cristallo necrotico è equipaggiato
    public static final String STAGE_NECRO_CRYSTAL_EQUIP = "necro_crystal_equip";
    
    // Nome dello stage per l'hex del cristallo necrotico (valore float salvato come flag)
    public static final String STAGE_NECRO_CRYSTAL_HEX = "necro_crystal_hex";
    
    /**
     * Registra il Necrotic Crystal Heart come curio.
     * Chiamato durante l'inizializzazione della mod se Curios è presente.
     */
    public static void register() {
        if (!ModUtils.isCuriosLoaded()) return;
        
        try {
            // La registrazione avviene attraverso le API di Curios, ma è gestita
            // automaticamente dai tag JSON, quindi non è necessario fare nulla di speciale qui
            LOGGER.info("Necrotic Crystal Heart registrato come Curio");
        } catch (Exception e) {
            LOGGER.error("Impossibile registrare il Necrotic Crystal Heart come Curio", e);
        }
    }
    
    /**
     * Chiamato ogni tick quando il curio è equipaggiato.
     * Può essere chiamato tramite reflection dalle API di Curios.
     */
    public static void curioTick(ItemStack stack, LivingEntity entity) {
        // Verifica se l'entità è un giocatore
        if (entity instanceof Player player && !entity.level().isClientSide()) {
            // Imposta lo stage di equipaggiamento su true
            StageRegistry.addPlayerStage(player, STAGE_NECRO_CRYSTAL_EQUIP);
        }
    }
    
} 