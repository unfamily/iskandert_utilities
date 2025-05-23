package net.unfamily.iskautils.data;

/**
 * Enum per i diversi tipi di fattore del Vector Charm
 */
public enum VectorFactorType {
    NONE(0, "none", 0.0f),
    SLOW(1, "slow", 0.2f),
    MODERATE(2, "moderate", 0.4f),
    FAST(3, "fast", 0.6f),
    EXTREME(4, "extreme", 0.8f),
    ULTRA(5, "ultra", 1.0f),
    UNKNOWN(-1, "unknown", 0.0f);

    private final byte id;
    private final String name;
    private final float factor;

    VectorFactorType(int id, String name, float factor) {
        this.id = (byte) id;
        this.name = name;
        this.factor = factor;
    }

    /**
     * Ottiene l'ID numerico del fattore
     */
    public byte getId() {
        return id;
    }

    /**
     * Ottiene il nome del fattore
     */
    public String getName() {
        return name;
    }

    /**
     * Ottiene il valore del fattore moltiplicativo
     */
    public float getFactor() {
        return factor;
    }

    /**
     * Converte un byte in VectorFactorType
     */
    public static VectorFactorType fromByte(byte id) {
        for (VectorFactorType type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return UNKNOWN;
    }
} 