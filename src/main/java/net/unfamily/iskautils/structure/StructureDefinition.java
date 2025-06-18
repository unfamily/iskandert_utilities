package net.unfamily.iskautils.structure;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Definition of a structure loadable from the scripting system
 */
public class StructureDefinition {
    private String id;
    private String name;
    private String description;
    private IconDefinition icon;
    private String[][][][] pattern; // [Y][X][Z][characters]
    private Map<String, List<BlockDefinition>> key;
    private List<String> canReplace;
    private boolean canForce;
    private boolean overwritable;
    private boolean slower = false; // If true, apply delay to each individual block instead of layers
    private boolean placeAsPlayer = false; // If true, place blocks as if done by a player
    private List<String> stages;

    /**
     * Definition of an icon for the structure
     */
    public static class IconDefinition {
        private String item;
        private int count = 1;
        private CompoundTag nbt;
        
        // Getters and setters
        public String getItem() { return item; }
        public void setItem(String item) { this.item = item; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public CompoundTag getNbt() { return nbt; }
        public void setNbt(CompoundTag nbt) { this.nbt = nbt; }
    }

    /**
     * Definition of a block in the structure pattern
     */
    public static class BlockDefinition {
        private String block;
        private String display;
        private Map<String, String> properties;
        private CompoundTag nbt;
        private boolean ignorePlacement = false;
        
        // Getters and setters
        public String getBlock() { return block; }
        public void setBlock(String block) { this.block = block; }
        public String getDisplay() { return display; }
        public void setDisplay(String display) { this.display = display; }
        public Map<String, String> getProperties() { return properties; }
        public void setProperties(Map<String, String> properties) { this.properties = properties; }
        public CompoundTag getNbt() { return nbt; }
        public void setNbt(CompoundTag nbt) { this.nbt = nbt; }
        public boolean isIgnorePlacement() { return ignorePlacement; }
        public void setIgnorePlacement(boolean ignorePlacement) { this.ignorePlacement = ignorePlacement; }
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public IconDefinition getIcon() { return icon; }
    public void setIcon(IconDefinition icon) { this.icon = icon; }
    public String[][][][] getPattern() { return pattern; }
    public void setPattern(String[][][][] pattern) { this.pattern = pattern; }
    public Map<String, List<BlockDefinition>> getKey() { return key; }
    public void setKey(Map<String, List<BlockDefinition>> key) { this.key = key; }
    public List<String> getCanReplace() { return canReplace; }
    public void setCanReplace(List<String> canReplace) { this.canReplace = canReplace; }
    public boolean isCanForce() { return canForce; }
    public void setCanForce(boolean canForce) { this.canForce = canForce; }
    public boolean isOverwritable() { return overwritable; }
    public void setOverwritable(boolean overwritable) { this.overwritable = overwritable; }
    public boolean isSlower() { return slower; }
    public void setSlower(boolean slower) { this.slower = slower; }
    public boolean isPlaceAsPlayer() { return placeAsPlayer; }
    public void setPlaceAsPlayer(boolean placeAsPlayer) { this.placeAsPlayer = placeAsPlayer; }
    public List<String> getStages() { return stages; }
    public void setStages(List<String> stages) { this.stages = stages; }

    /**
     * Finds the position of the structure's center (character '@')
     * Returns the relative position within the structure
     */
    public BlockPos findCenter() {
        if (pattern == null) return null;
        
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length; x++) {
                for (int z = 0; z < pattern[y][x].length; z++) {
                    String[] cellChars = pattern[y][x][z];
                    if (cellChars != null) {
                        for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                            if ("@".equals(cellChars[charIndex])) {
                                // Calculate effective Z coordinate
                                int effectiveZ = z * cellChars.length + charIndex;
                                // Return center position: (X, Y, effective_Z)
                                return new BlockPos(x, y, effectiveZ);
                            }
                        }
                    }
                }
            }
        }
        
        // Default to structure center if @ not found
        if (pattern.length > 0 && pattern[0].length > 0 && pattern[0][0].length > 0) {
            int centerX = pattern[0].length / 2;
            int centerY = pattern.length / 2;
            int centerZ = pattern[0][0].length / 2;
            return new BlockPos(centerX, centerY, centerZ);
        }
        
        return null;
    }

    /**
     * Gets the structure dimensions [width, height, depth]
     */
    public int[] getDimensions() {
        if (pattern == null || pattern.length == 0) return new int[]{0, 0, 0};
        
        int height = pattern.length;
        int width = pattern[0].length;
        int depth = pattern[0][0].length;
        
        return new int[]{width, height, depth};
    }

    /**
     * Verifies if this structure can be placed based on stages
     */
    public boolean canBePlaced(net.minecraft.world.entity.player.Player player) {
        if (stages == null || stages.isEmpty()) return true;
        
        // For now always returns true, complete implementation will be added later
        return true;
    }
} 