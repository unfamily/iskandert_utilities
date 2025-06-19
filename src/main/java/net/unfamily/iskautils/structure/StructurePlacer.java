package net.unfamily.iskautils.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Class responsible for physical placement of structures in the world
 */
public class StructurePlacer {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Places a structure in the world
     * 
     * @param level The server level where to place the structure
     * @param centerPos The position where to place the structure center
     * @param structure The structure definition to place
     * @param player The player who requested the placement (can be null)
     * @return true if placement succeeded, false otherwise
     */
    public static boolean placeStructure(ServerLevel level, BlockPos centerPos, StructureDefinition structure, ServerPlayer player) {
        try {
            // Verify prerequisites
            if (!canPlaceStructure(level, centerPos, structure, player)) {
                LOGGER.debug("Cannot place structure {} - prerequisites not satisfied", structure.getId());
                return false;
            }

            // Get structure pattern
            String[][][][] pattern = structure.getPattern();
            if (pattern == null || pattern.length == 0) {
                LOGGER.error("Structure {} has empty or null pattern", structure.getId());
                return false;
            }

            // Find relative center position
            BlockPos relativeCenter = structure.findCenter();
            if (relativeCenter == null) {
                relativeCenter = new BlockPos(0, 0, 0);
                LOGGER.warn("Center not found for structure {}, using (0,0,0)", structure.getId());
            }

            // Calculate offset to position structure correctly
            // Subtract relative center position and raise by one block
            BlockPos offsetPos = centerPos.subtract(relativeCenter).above();

            // Place each block in the pattern
            // Pattern converted by loader: [Y][X][Z][characters] 
            // where Y=height, X=east-west, Z=north-south, characters=multiple Z positions from string
            Map<String, List<StructureDefinition.BlockDefinition>> key = structure.getKey();
            
            for (int y = 0; y < pattern.length; y++) {                    // Y (height/layer)
                for (int x = 0; x < pattern[y].length; x++) {             // X (east-west) 
                    for (int z = 0; z < pattern[y][x].length; z++) {      // Z (north-south)
                        String[] cellChars = pattern[y][x][z];
                        
                        if (cellChars != null) {
                            for (int charIndex = 0; charIndex < cellChars.length; charIndex++) {
                                String character = cellChars[charIndex];
                                
                                if (character == null || character.equals(" ")) {
                                    continue; // Skip empty spaces
                                }
                                
                                // If it's @, check if it's defined in the key
                                if (character.equals("@")) {
                                    if (key == null || !key.containsKey("@")) {
                                        // @ is not defined in key, treat as empty space
                                        continue;
                                    }
                                    // If we get here, @ is defined in key, so process as normal block
                                }
                                
                                // Calculate absolute position of this block
                                // Each character in Z string represents an additional Z position
                                int worldX = x;
                                int worldY = y;
                                int worldZ = z * cellChars.length + charIndex;
                                
                                BlockPos blockPos = offsetPos.offset(worldX, worldY, worldZ);
                                
                                // Place block based on key
                                if (!placeBlockFromKey(level, blockPos, character, key, structure)) {
                                    LOGGER.warn("Cannot place block '{}' at position {} for structure {}", 
                                        character, blockPos, structure.getId());
                                    // Continue with other blocks anyway
                                }
                            }
                        }
                    }
                }
            }

            LOGGER.info("Structure {} placed successfully at center {}", structure.getId(), centerPos);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error during structure placement {}: {}", structure.getId(), e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Verifies if a structure can be placed
     */
    private static boolean canPlaceStructure(ServerLevel level, BlockPos centerPos, StructureDefinition structure, ServerPlayer player) {
        // Verify stages if specified
        if (structure.getStages() != null && !structure.getStages().isEmpty()) {
            if (!structure.canBePlaced(player)) {
                LOGGER.debug("Stage not satisfied for structure {}", structure.getId());
                return false;
            }
        }

        // For now always allow placement
        // Later we can add checks for:
        // - Available space verification
        // - Replaceable blocks check
        // - Player inventory verification for required materials
        
        return true;
    }

    /**
     * Places a single block based on key definition
     */
    private static boolean placeBlockFromKey(ServerLevel level, BlockPos pos, String keyCharacter, 
                                           Map<String, List<StructureDefinition.BlockDefinition>> key, 
                                           StructureDefinition structure) {
        
        // Get definitions for this character
        List<StructureDefinition.BlockDefinition> blockDefs = key.get(keyCharacter);
        if (blockDefs == null || blockDefs.isEmpty()) {
            LOGGER.warn("No definition found for character '{}' in structure {}", keyCharacter, structure.getId());
            return false;
        }

        // Use first available definition (alternatives can be implemented later)
        StructureDefinition.BlockDefinition blockDef = blockDefs.get(0);

        try {
            BlockState blockState = getBlockStateFromDefinition(blockDef);
            if (blockState != null) {
                // Verify if we can replace existing block
                if (canReplaceBlock(level, pos, structure)) {
                    level.setBlock(pos, blockState, 3); // Flag 3 = update + notify clients
                    return true;
                } else {
                    LOGGER.debug("Cannot replace block at position {} for structure {}", pos, structure.getId());
                    return false;
                }
            } else {
                LOGGER.warn("Cannot create BlockState from definition for '{}' in structure {}", keyCharacter, structure.getId());
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error placing block '{}' at position {} for structure {}: {}", 
                keyCharacter, pos, structure.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Creates a BlockState from block definition
     */
    private static BlockState getBlockStateFromDefinition(StructureDefinition.BlockDefinition blockDef) {
        try {
            // Handle blocks defined by name
            if (blockDef.getBlock() != null && !blockDef.getBlock().isEmpty()) {
                ResourceLocation blockId = ResourceLocation.parse(blockDef.getBlock());
                Block block = BuiltInRegistries.BLOCK.get(blockId);
                
                if (block == Blocks.AIR && !blockDef.getBlock().equals("minecraft:air")) {
                    LOGGER.warn("Block not found: {}", blockDef.getBlock());
                    return null;
                }
                
                BlockState blockState = block.defaultBlockState();
                
                // Apply properties if specified
                if (blockDef.getProperties() != null && !blockDef.getProperties().isEmpty()) {
                    blockState = applyProperties(blockState, blockDef.getProperties());
                }
                
                return blockState;
            }
            
            // TODO: Handle tags (blocks defined via tags) - for future implementation
            
            LOGGER.warn("Empty or invalid block definition");
            return null;
            
        } catch (Exception e) {
            LOGGER.error("Error creating BlockState: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Applies properties to a BlockState
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState applyProperties(BlockState blockState, Map<String, String> properties) {
        try {
            BlockState result = blockState;
            
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                String propertyValue = entry.getValue();
                
                // Find property in BlockState
                Property<?> property = null;
                for (Property<?> prop : blockState.getProperties()) {
                    if (prop.getName().equals(propertyName)) {
                        property = prop;
                        break;
                    }
                }
                
                if (property == null) {
                    LOGGER.warn("Property '{}' not found for block {}", propertyName, blockState.getBlock());
                    continue;
                }
                
                // Try to apply value
                try {
                    Comparable value = property.getValue(propertyValue).orElse(null);
                    if (value != null) {
                        result = result.setValue((Property) property, value);
                    } else {
                        LOGGER.warn("Invalid value '{}' for property '{}' of block {}", 
                            propertyValue, propertyName, blockState.getBlock());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error applying property '{}={}' to block {}: {}", 
                        propertyName, propertyValue, blockState.getBlock(), e.getMessage());
                }
            }
            
            return result;
        } catch (Exception e) {
            LOGGER.error("Error applying properties: {}", e.getMessage());
            return blockState; // Return original state on error
        }
    }

    /**
     * Verifies if a block can be replaced
     */
    private static boolean canReplaceBlock(ServerLevel level, BlockPos pos, StructureDefinition structure) {
        BlockState existingState = level.getBlockState(pos);
        
        // Air can always be replaced
        if (existingState.isAir()) {
            return true;
        }
        
        // Check structure can_replace list
        List<String> canReplace = structure.getCanReplace();
        if (canReplace != null && !canReplace.isEmpty()) {
            String blockId = BuiltInRegistries.BLOCK.getKey(existingState.getBlock()).toString();
            
            for (String replaceableId : canReplace) {
                if (replaceableId.equals(blockId)) {
                    return true;
                }
                
                // Handle special cases
                if (handleSpecialReplaceableCase(replaceableId, existingState, level, pos)) {
                    return true;
                }
                
                // Handle tags with # prefix
                if (replaceableId.startsWith("#") && handleTagReplacement(replaceableId, existingState)) {
                    return true;
                }
            }
            
            return false; // If there's a can_replace list and block is not in list, it cannot be replaced
        }
        
        // If there's no can_replace list, for safety we only replace air
        // This behavior can be changed in the future
        return existingState.isAir();
    }
    
    /**
     * Handles special replacement cases like $replaceable, $fluids, etc.
     */
    private static boolean handleSpecialReplaceableCase(String replaceableId, BlockState existingState, ServerLevel level, BlockPos pos) {
        return switch (replaceableId.toLowerCase()) {
            case "$replaceable" -> existingState.canBeReplaced();
            case "$fluids", "$fluid" -> existingState.getFluidState().isSource() || !existingState.getFluidState().isEmpty();
            case "$air" -> existingState.isAir();
            case "$water" -> existingState.is(net.minecraft.world.level.block.Blocks.WATER);
            case "$lava" -> existingState.is(net.minecraft.world.level.block.Blocks.LAVA);
            case "$plants", "$plant" -> existingState.is(net.minecraft.tags.BlockTags.REPLACEABLE_BY_TREES) || 
                                      existingState.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS) ||
                                      existingState.is(net.minecraft.tags.BlockTags.TALL_FLOWERS) ||
                                      existingState.is(net.minecraft.tags.BlockTags.SAPLINGS);
            case "$dirt" -> existingState.is(net.minecraft.tags.BlockTags.DIRT);
            case "$logs", "$log" -> existingState.is(net.minecraft.tags.BlockTags.LOGS);
            case "$leaves" -> existingState.is(net.minecraft.tags.BlockTags.LEAVES);
            case "$stone" -> existingState.is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES) ||
                           existingState.is(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES);
            case "$ores", "$ore" -> existingState.is(net.minecraft.tags.BlockTags.COAL_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.IRON_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.GOLD_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.DIAMOND_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.EMERALD_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.LAPIS_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.REDSTONE_ORES) ||
                                  existingState.is(net.minecraft.tags.BlockTags.COPPER_ORES);
            default -> false;
        };
    }
    
    /**
     * Handles tag-based replacement with # prefix
     */
    private static boolean handleTagReplacement(String tagId, BlockState existingState) {
        try {
            // Remove # prefix
            String cleanTagId = tagId.substring(1);
            
            // Parse as ResourceLocation
            net.minecraft.resources.ResourceLocation tagLocation = net.minecraft.resources.ResourceLocation.parse(cleanTagId);
            
            // Get tag from registry
            net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> blockTag = 
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tagLocation);
            
            // Check if block is in the tag
            return existingState.is(blockTag);
            
        } catch (Exception e) {
            LOGGER.warn("Invalid tag format: '{}' - {}", tagId, e.getMessage());
            return false;
        }
    }
} 