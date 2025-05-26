package net.unfamily.iskautils.data;

/**
 * Enum defining the different types of potion plates
 */
public enum PotionPlateType {
    /**
     * Standard potion effect plate
     */
    EFFECT("effect"),
    
    /**
     * Damage dealing plate
     */
    DAMAGE("damage"),
    
    /**
     * Special plates (fire, freeze, etc.)
     */
    SPECIAL("special");
    
    private final String typeName;
    
    PotionPlateType(String typeName) {
        this.typeName = typeName;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    /**
     * Gets a PotionPlateType from its string name
     */
    public static PotionPlateType fromString(String typeName) {
        for (PotionPlateType type : values()) {
            if (type.typeName.equals(typeName)) {
                return type;
            }
        }
        return EFFECT; // Default fallback
    }
    
    @Override
    public String toString() {
        return typeName;
    }
} 