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
     * @param factor il fattore (0-5)
     */
    public void setVerticalFactor(Player player, byte factor) {
        UUID playerId = player.getUUID();
        verticalFactors.put(playerId, factor);
        isDirty = true;
        LOGGER.debug("Set vertical factor {} for player {}", factor, playerId);
    }

    /**
     * Ottiene il fattore verticale per un giocatore
     * @param player il giocatore
     * @return il fattore (0-5)
     */
    public byte getVerticalFactor(Player player) {
        UUID playerId = player.getUUID();
        return verticalFactors.getOrDefault(playerId, (byte) 0);
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

        // Salva tutti i fattori verticali
        for (Map.Entry<UUID, Byte> entry : verticalFactors.entrySet()) {
            verticalTag.putByte(entry.getKey().toString(), entry.getValue());
        }
        
        // Salva tutti i fattori orizzontali
        for (Map.Entry<UUID, Byte> entry : horizontalFactors.entrySet()) {
            horizontalTag.putByte(entry.getKey().toString(), entry.getValue());
        }

        // Aggiunge le due mappe al tag principale
        tag.put("vertical_factors", verticalTag);
        tag.put("horizontal_factors", horizontalTag);

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
    }

    /**
     * Ottiene l'istanza dei dati, creandola se necessario
     */
    public static VectorCharmData get(ServerLevel level) {
        return getInstance();
    }
} 