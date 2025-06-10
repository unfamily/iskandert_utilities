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
    private static final String NBT_KEY_ISKAUTILS = "iskautils";
    private static final String NBT_KEY_NECROTIC_HEART_CURIO_EQUIP = "necrotic_heart_curio_equip";
    private static final String NBT_KEY_NECROTIC_HEART_ITEM_STACK = "necrotic_heart_item_stack";
    private static final String NBT_KEY_NECROTIC_HEART_ID = "NecroticHeartID";
    
    // Nome dello stage per controllare se il cristallo necrotico è equipaggiato
    public static final String STAGE_NECRO_CRYSTAL_EQUIP = "necro_crystal_equip";
    
    // Nome dello stage per l'hex del cristallo necrotico (valore float salvato come flag)
    public static final String STAGE_NECRO_CRYSTAL_HEX = "necro_crystal_hex";
    
    // Per tenere traccia dell'ultimo log, per evitare spam
    private static long lastLogTime = 0;
    
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
            // Imposta lo stage tramite StageRegistry
            boolean stageSuccess = StageRegistry.addPlayerStage(player, STAGE_NECRO_CRYSTAL_EQUIP);
            
            // Come metodo di backup, imposta anche direttamente tramite NBT
            setNecroticHeartEquipped(player, true);
            
            // Log per debug (limitato per evitare spam)
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLogTime > 5000) {  // Log ogni 5 secondi al massimo
                LOGGER.info("[NecroticCrystalHeart] curioTick chiamato per {} - Stage impostato: {}, NBT impostato: true", 
                        player.getName().getString(), stageSuccess);
                lastLogTime = currentTime;
                
                // Verifica se lo stage è stato effettivamente impostato
                if (StageRegistry.playerHasStage(player, STAGE_NECRO_CRYSTAL_EQUIP)) {
                    LOGGER.info("[NecroticCrystalHeart] Stage {} attivo per il giocatore {}", 
                            STAGE_NECRO_CRYSTAL_EQUIP, player.getName().getString());
                } else {
                    LOGGER.warn("[NecroticCrystalHeart] Stage {} NON attivo nonostante il tentativo di impostarlo!", 
                            STAGE_NECRO_CRYSTAL_EQUIP);
                }
                
                // Verifica anche l'NBT
                LOGGER.info("[NecroticCrystalHeart] NBT {} attivo: {}", 
                        NBT_KEY_NECROTIC_HEART_CURIO_EQUIP, isNecroticHeartEquipped(player));
            }
        }
    }
    
    /**
     * Imposta il flag di equipaggiamento del Necrotic Heart nell'NBT del giocatore
     */
    public static void setNecroticHeartEquipped(Player player, boolean equipped) {
        CompoundTag persistentData = player.getPersistentData();
        
        // Assicurati che la sezione iskautils esista
        if (!persistentData.contains(NBT_KEY_ISKAUTILS)) {
            persistentData.put(NBT_KEY_ISKAUTILS, new CompoundTag());
        }
        
        // Ottieni la sezione iskautils
        CompoundTag iskaData = persistentData.getCompound(NBT_KEY_ISKAUTILS);
        
        // Imposta il flag di equipaggiamento
        iskaData.putBoolean(NBT_KEY_NECROTIC_HEART_CURIO_EQUIP, equipped);
        
        // Aggiorna i dati persistenti
        persistentData.put(NBT_KEY_ISKAUTILS, iskaData);
    }
    
    /**
     * Verifica se il Necrotic Heart è equipaggiato tramite NBT
     */
    public static boolean isNecroticHeartEquipped(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        
        // Verifica se la sezione iskautils esiste
        if (!persistentData.contains(NBT_KEY_ISKAUTILS)) {
            return false;
        }
        
        // Ottieni la sezione iskautils
        CompoundTag iskaData = persistentData.getCompound(NBT_KEY_ISKAUTILS);
        
        // Verifica il flag di equipaggiamento
        return iskaData.getBoolean(NBT_KEY_NECROTIC_HEART_CURIO_EQUIP);
    }
} 