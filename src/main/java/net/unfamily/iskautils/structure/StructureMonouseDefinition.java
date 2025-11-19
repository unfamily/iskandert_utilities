package net.unfamily.iskautils.structure;

import java.util.ArrayList;
import java.util.List;

/**
 * Definition of a Structure Monouse item generated dynamically from JSON
 */
public class StructureMonouseDefinition {
    private String id;
    private String structureId;
    private String placeName;
    private List<GiveItem> giveItems = new ArrayList<>();
    private boolean aggressive = false;
    
    /**
     * Represents an item to give to the player after placement
     */
    public static class GiveItem {
        private String item;
        private int count;
        
        public GiveItem(String item, int count) {
            this.item = item;
            this.count = count;
        }
        
        public String getItem() { return item; }
        public void setItem(String item) { this.item = item; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
    
    /**
     * Creates a new monouse item definition
     */
    public StructureMonouseDefinition(String id) {
        this.id = id;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getStructureId() { return structureId; }
    public void setStructureId(String structureId) { this.structureId = structureId; }
    
    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }
    
    public List<GiveItem> getGiveItems() { return giveItems; }
    public void setGiveItems(List<GiveItem> giveItems) { this.giveItems = giveItems; }
    public void addGiveItem(GiveItem giveItem) { this.giveItems.add(giveItem); }

    public boolean isAggressive() { return aggressive; }
    public void setAggressive(boolean aggressive) { this.aggressive = aggressive; }
} 