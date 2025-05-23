package net.unfamily.iskautils.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.unfamily.iskautils.IskaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Classe per gestire i dati persistenti del Vector Charm.
 * Attualmente utilizza solo la memoria, il supporto di salvataggio
 * verrà implementato in seguito.
 */
public class VectorCharmData {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmData.class);
    private static final String DATA_NAME = IskaUtils.MOD_ID + "_vector_charm_data";
    
    // Singleton instance
    private static VectorCharmData instance;
    
    // Mappe per memorizzare i fattori per ciascun giocatore
    private final Map<UUID, Byte> verticalFactors = new HashMap<>();
    private final Map<UUID, Byte> horizontalFactors = new HashMap<>();
    
    // Mappa per memorizzare il valore precedente del fattore verticale prima dell'hover
    private final Map<UUID, Byte> previousVerticalFactors = new HashMap<>();
    
    // Valore speciale per la modalità hover
    public static final byte HOVER_MODE_VALUE = 6;
    
    // Flag per tracciare i cambiamenti
    private boolean isDirty = false;

    /**
     * Costruttore privato per il singleton
     */
    private VectorCharmData() {
        // Private constructor
    }
    
    /**
     * Ottiene l'istanza singleton di VectorCharmData
     */
    public static VectorCharmData getInstance() {
        if (instance == null) {
            instance = new VectorCharmData();
            LOGGER.info("Created new Vector Charm data instance");
        }
        return instance;
    }

    /**
     * Imposta il fattore verticale per un giocatore
     * @param player il giocatore
     * @param factor il fattore (0-5, o 6 per hover)
     */
    public void setVerticalFactor(Player player, byte factor) {
        UUID playerId = player.getUUID();
        
        // Se stiamo attivando la modalità hover, salva il valore precedente
        byte currentFactor = verticalFactors.getOrDefault(playerId, (byte) 0);
        if (factor == HOVER_MODE_VALUE && currentFactor != HOVER_MODE_VALUE) {
            previousVerticalFactors.put(playerId, currentFactor);
            LOGGER.debug("Saved previous vertical factor {} before hover mode for player {}", currentFactor, playerId);
        }
        
        verticalFactors.put(playerId, factor);
        isDirty = true;
        LOGGER.debug("Set vertical factor {} for player {}", factor, playerId);
    }

    /**
     * Ottiene il fattore verticale per un giocatore
     * @param player il giocatore
     * @return il fattore (0-5, o 6 per hover)
     */
    public byte getVerticalFactor(Player player) {
        UUID playerId = player.getUUID();
        return verticalFactors.getOrDefault(playerId, (byte) 0);
    }
    
    /**
     * Disattiva la modalità hover ripristinando il valore precedente
     * @param player il giocatore
     * @return il fattore ripristinato
     */
    public byte disableHoverMode(Player player) {
        UUID playerId = player.getUUID();
        
        // Ottieni il valore precedente, o usa 0 come fallback
        byte previousFactor = previousVerticalFactors.getOrDefault(playerId, (byte) 0);
        
        // Imposta il fattore verticale al valore precedente
        verticalFactors.put(playerId, previousFactor);
        isDirty = true;
        
        LOGGER.debug("Disabled hover mode for player {}, restored vertical factor to {}", playerId, previousFactor);
        return previousFactor;
    }

    /**
     * Imposta il fattore orizzontale per un giocatore
     * @param player il giocatore
     * @param factor il fattore (0-5)
     */
    public void setHorizontalFactor(Player player, byte factor) {
        UUID playerId = player.getUUID();
        horizontalFactors.put(playerId, factor);
        isDirty = true;
        LOGGER.debug("Set horizontal factor {} for player {}", factor, playerId);
    }

    /**
     * Ottiene il fattore orizzontale per un giocatore
     * @param player il giocatore
     * @return il fattore (0-5)
     */
    public byte getHorizontalFactor(Player player) {
        UUID playerId = player.getUUID();
        return horizontalFactors.getOrDefault(playerId, (byte) 0);
    }

    /**
     * Verifica se i dati sono stati modificati
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Resetta il flag dirty
     */
    public void resetDirty() {
        isDirty = false;
    }

    /**
     * Salva i dati in NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag verticalTag = new CompoundTag();
        CompoundTag horizontalTag = new CompoundTag();
        CompoundTag previousVerticalTag = new CompoundTag();

        // Salva tutti i fattori verticali
        for (Map.Entry<UUID, Byte> entry : verticalFactors.entrySet()) {
            verticalTag.putByte(entry.getKey().toString(), entry.getValue());
        }
        
        // Salva tutti i fattori orizzontali
        for (Map.Entry<UUID, Byte> entry : horizontalFactors.entrySet()) {
            horizontalTag.putByte(entry.getKey().toString(), entry.getValue());
        }
        
        // Salva tutti i fattori verticali precedenti
        for (Map.Entry<UUID, Byte> entry : previousVerticalFactors.entrySet()) {
            previousVerticalTag.putByte(entry.getKey().toString(), entry.getValue());
        }

        // Aggiunge le mappe al tag principale
        tag.put("vertical_factors", verticalTag);
        tag.put("horizontal_factors", horizontalTag);
        tag.put("previous_vertical_factors", previousVerticalTag);

        return tag;
    }

    /**
     * Carica i dati da NBT
     */
    public void load(CompoundTag tag) {
        // Carica i fattori verticali
        if (tag.contains("vertical_factors")) {
            CompoundTag verticalTag = tag.getCompound("vertical_factors");
            for (String key : verticalTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    verticalFactors.put(playerId, verticalTag.getByte(key));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID in vertical_factors: {}", key);
                }
            }
        }
        
        // Carica i fattori orizzontali
        if (tag.contains("horizontal_factors")) {
            CompoundTag horizontalTag = tag.getCompound("horizontal_factors");
            for (String key : horizontalTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    horizontalFactors.put(playerId, horizontalTag.getByte(key));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID in horizontal_factors: {}", key);
                }
            }
        }
        
        // Carica i fattori verticali precedenti
        if (tag.contains("previous_vertical_factors")) {
            CompoundTag previousVerticalTag = tag.getCompound("previous_vertical_factors");
            for (String key : previousVerticalTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    previousVerticalFactors.put(playerId, previousVerticalTag.getByte(key));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID in previous_vertical_factors: {}", key);
                }
            }
        }
    }

    /**
     * Ottiene l'istanza dei dati, creandola se necessario
     */
    public static VectorCharmData get(ServerLevel level) {
        return getInstance();
    }
} 