package net.unfamily.iskautils.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.HolderLookup;
import net.unfamily.iskautils.util.ModUtils;
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
    private static final String NBT_KEY_NECROTIC_HEART_CURIO = "necrotic_heart_curio";
    
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
     * Chiamato quando il curio viene equipaggiato.
     * Può essere chiamato tramite reflection dalle API di Curios.
     */
    public static void onEquip(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player) {
            LOGGER.debug("Necrotic Crystal Heart equipaggiato dal giocatore {}", player.getName().getString());
            
            // Imposta il flag NBT per indicare che il cuore è equipaggiato
            setNecroticHeartEquipped(player, true);
        }
    }
    
    /**
     * Chiamato quando il curio viene rimosso.
     * Può essere chiamato tramite reflection dalle API di Curios.
     */
    public static void onUnequip(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player) {
            LOGGER.debug("Necrotic Crystal Heart rimosso dal giocatore {}", player.getName().getString());
            
            // Rimuove il flag NBT quando il cuore è disequipaggiato
            setNecroticHeartEquipped(player, false);
        }
    }
    
    /**
     * Chiamato ogni tick quando il curio è equipaggiato.
     * Può essere chiamato tramite reflection dalle API di Curios.
     */
    public static void curioTick(ItemStack stack, LivingEntity entity) {
        // Implementazione del tick se necessaria
        // Per ora non facciamo nulla di speciale ogni tick
    }
    
    /**
     * Verifica se un giocatore ha equipaggiato il Necrotic Crystal Heart.
     * Controlla direttamente nei dati dell'inventario dell'entità.
     * 
     * @param entity L'entità da controllare
     * @return true se il cuore è equipaggiato, false altrimenti
     */
    public static boolean isNecroticHeartEquipped(LivingEntity entity) {
        // Prima verifichiamo se è un player
        if (!(entity instanceof Player)) {
            return false;
        }


        LOGGER.debug("Controllo se il cuore è equipaggiato");

        ListTag inventory[] = new ListTag[512];

        // Ottieni i dati dell'entità, che includono gli slot Curio
        CompoundTag entityData = entity.saveWithoutId(new CompoundTag());
        if (entityData.contains("Inventory")) {
            for (int j = 0; j < 512; j++) {
                inventory[j] = entityData.getList("Inventory", j);
            }
            LOGGER.warn("Inventory: {}", inventory);

            // Cerca il Necrotic Crystal Heart negli slot
            for (int k = 0; k < 512; k++) {
                for (int i = 0; i < inventory[k].size(); i++) {
                    try
                    {
                        LOGGER.warn("Size: {}", inventory[k].size());
                        LOGGER.warn("Slot: {}", i);


                        CompoundTag itemTag = inventory[k].getCompound(i);
                    
                    // Gli slot di Curios hanno un byte Slot > 90
                    if (itemTag.contains("Slot") && itemTag.getByte("Slot") > 90) {
                        String itemId = itemTag.getString("id");
                        
                        // Verifica se l'item è un Necrotic Crystal Heart
                        if ("iska_utils:necrotic_crystal_heart".equals(itemId)) {
                            LOGGER.debug("Trovato Necrotic Crystal Heart equipaggiato in slot {}", itemTag.getByte("Slot"));
                            return true;
                        }
                    }
                    else {
                        LOGGER.warn("Slot {} non è un Necrotic Crystal Heart", itemTag.getByte("Slot"));
                    }
                }
                catch (Exception e) {
                    LOGGER.warn("Errore nel controllo dell'inventario", e);
                }
            }
        }
    }
    else {
        LOGGER.debug("Inventory non trovato");
    }
        
        return false;
    }
    
    /**
     * Imposta lo stato di equipaggiamento del Necrotic Crystal Heart per un giocatore.
     * Questo è usato solo come backup per conservare lo stato.
     * 
     * @param player Il giocatore
     * @param equipped true se equipaggiato, false altrimenti
     */
    private static void setNecroticHeartEquipped(Player player, boolean equipped) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(NBT_KEY_ISKAUTILS)) {
            persistentData.put(NBT_KEY_ISKAUTILS, new CompoundTag());
        }
        
        CompoundTag iskaData = persistentData.getCompound(NBT_KEY_ISKAUTILS);
        iskaData.putBoolean(NBT_KEY_NECROTIC_HEART_CURIO, equipped);
        persistentData.put(NBT_KEY_ISKAUTILS, iskaData);
        
        LOGGER.debug("Impostato flag necrotic_heart_curio a {} per {}", equipped, player.getName().getString());
    }
} 