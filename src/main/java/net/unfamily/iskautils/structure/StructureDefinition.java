package net.unfamily.iskautils.structure;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Definizione di una struttura caricabile dal sistema di scripting
 */
public class StructureDefinition {
    private String id;
    private String name;
    private boolean canForce = false;
    private List<String> canReplace = new ArrayList<>();
    private IconDefinition icon;
    private List<String> description = new ArrayList<>();
    private String[][][][] pattern; // [layer][row][column][depth]
    private Map<String, List<BlockDefinition>> key = new HashMap<>();
    private List<StageCondition> stages = new ArrayList<>();

    /**
     * Definizione di un'icona per la struttura
     */
    public static class IconDefinition {
        private String type; // "minecraft:item" o "image"
        private String item; // per type="minecraft:item"
        private String image; // per type="image"

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getItem() { return item; }
        public void setItem(String item) { this.item = item; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
    }

    /**
     * Definizione di un blocco nel pattern della struttura
     */
    public static class BlockDefinition {
        private String display; // Nome traducibile per visualizzazione
        private String block;
        private String tag;
        private Map<String, String> properties = new HashMap<>();
        private List<StageCondition> conditions = new ArrayList<>();

        public String getDisplay() { return display; }
        public void setDisplay(String display) { this.display = display; }
        public String getBlock() { return block; }
        public void setBlock(String block) { this.block = block; }
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        public Map<String, String> getProperties() { return properties; }
        public void setProperties(Map<String, String> properties) { this.properties = properties; }
        public List<StageCondition> getConditions() { return conditions; }
        public void setConditions(List<StageCondition> conditions) { this.conditions = conditions; }
    }

    /**
     * Condizione di stage per validare il piazzamento
     */
    public static class StageCondition {
        private String stageType; // "world" o "player"
        private String stage;
        private boolean is;

        public StageCondition(String stageType, String stage, boolean is) {
            this.stageType = stageType;
            this.stage = stage;
            this.is = is;
        }

        public String getStageType() { return stageType; }
        public void setStageType(String stageType) { this.stageType = stageType; }
        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        public boolean isIs() { return is; }
        public void setIs(boolean is) { this.is = is; }
    }

    // Getter e Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public boolean isCanForce() { return canForce; }
    public void setCanForce(boolean canForce) { this.canForce = canForce; }
    
    public List<String> getCanReplace() { return canReplace; }
    public void setCanReplace(List<String> canReplace) { this.canReplace = canReplace; }
    
    public IconDefinition getIcon() { return icon; }
    public void setIcon(IconDefinition icon) { this.icon = icon; }
    
    public List<String> getDescription() { return description; }
    public void setDescription(List<String> description) { this.description = description; }
    
    public String[][][][] getPattern() { return pattern; }
    public void setPattern(String[][][][] pattern) { this.pattern = pattern; }
    
    public Map<String, List<BlockDefinition>> getKey() { return key; }
    public void setKey(Map<String, List<BlockDefinition>> key) { this.key = key; }
    
    public List<StageCondition> getStages() { return stages; }
    public void setStages(List<StageCondition> stages) { this.stages = stages; }

    /**
     * Trova la posizione del centro della struttura (carattere '@')
     * Pattern formato: [Y][X][Z][caratteri]
     * @return BlockPos relativo del centro, o null se non trovato
     */
    public BlockPos findCenter() {
        if (pattern == null) return null;
        
        // Cerca il carattere '@' nel pattern [Y][X][Z][caratteri]
        for (int y = 0; y < pattern.length; y++) {                    // Y (altezza)
            for (int x = 0; x < pattern[y].length; x++) {             // X (est-ovest)
                for (int z = 0; z < pattern[y][x].length; z++) {      // Z (nord-sud)
                    String[] cellChars = pattern[y][x][z];
                    if (cellChars != null) {
                        for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                            if ("@".equals(cellChars[charIndex])) {
                                // Ritorna la posizione del centro: (X, Y, Z_effettivo)
                                int zEffettivo = z * cellChars.length + charIndex;
                                return new BlockPos(x, y, zEffettivo);
                            }
                        }
                    }
                }
            }
        }
        
        // Default al centro della struttura se @ non trovato
        if (pattern.length > 0 && pattern[0].length > 0 && pattern[0][0].length > 0) {
            int centerX = pattern[0].length / 2;
            int centerY = pattern.length / 2;
            int centerZ = pattern[0][0].length / 2;
            return new BlockPos(centerX, centerY, centerZ);
        }
        
        return new BlockPos(0, 0, 0);
    }

    /**
     * Ottiene le dimensioni della struttura [larghezza, altezza, profondità]
     */
    public int[] getDimensions() {
        if (pattern == null || pattern.length == 0) return new int[]{0, 0, 0};
        
        int height = pattern.length;
        int depth = pattern[0].length;
        int width = pattern[0][0].length;
        
        return new int[]{width, height, depth};
    }

    /**
     * Verifica se questa struttura può essere piazzata in base agli stage
     */
    public boolean canPlace(net.minecraft.server.level.ServerPlayer player) {
        if (stages.isEmpty()) return true;
        
        // Implementazione della logica degli stage
        // Per ora ritorna sempre true, l'implementazione completa sarà aggiunta dopo
        return true;
    }
} 