package net.unfamily.iskautils.network.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.entity.StructureSaverMachineBlockEntity;
import net.unfamily.iskautils.structure.StructureLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Packet to save a structure from the Structure Saver Machine
 */
public class StructureSaverMachineSaveC2SPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSaverMachineSaveC2SPacket.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    private final String structureName;
    private final String structureId;
    private final BlockPos machinePos;
    private final boolean slower;
    private final boolean placeAsPlayer;
    private final String oldStructureId; // For modification - null if it's a new save
    
    /**
     * Creates a new packet to save a structure
     * @param structureName The name of the structure to save
     * @param structureId The ID of the structure to save
     * @param machinePos The position of the machine
     * @param slower If the structure should be placed more slowly
     * @param placeAsPlayer If the structure should be placed as if by a player
     */
    public StructureSaverMachineSaveC2SPacket(String structureName, String structureId, BlockPos machinePos, boolean slower, boolean placeAsPlayer) {
        this(structureName, structureId, machinePos, slower, placeAsPlayer, null);
    }
    
    /**
     * Creates a new packet to save/modify a structure
     * @param structureName The name of the structure to save
     * @param structureId The ID of the structure to save
     * @param machinePos The position of the machine
     * @param slower If the structure should be placed more slowly
     * @param placeAsPlayer If the structure should be placed as if by a player
     * @param oldStructureId The ID of the structure to modify (null for new save)
     */
    public StructureSaverMachineSaveC2SPacket(String structureName, String structureId, BlockPos machinePos, boolean slower, boolean placeAsPlayer, String oldStructureId) {
        this.structureName = structureName;
        this.structureId = structureId;
        this.machinePos = machinePos;
        this.slower = slower;
        this.placeAsPlayer = placeAsPlayer;
        this.oldStructureId = oldStructureId;
    }
    
    /**
     * Handles the packet on the server side
     * @param player The player who sent the packet
     */
    public void handle(ServerPlayer player) {

        
        if (player == null) {
            LOGGER.error("Server player is null while handling StructureSaverMachineSaveC2SPacket");
            return;
        }
        

        
        if (structureName == null || structureName.trim().isEmpty()) {
            player.displayClientMessage(Component.translatable("gui.iska_utils.structure_saver.error.invalid_name"), true);
            return;
        }
        
        if (structureId == null || structureId.trim().isEmpty()) {
            player.displayClientMessage(Component.translatable("gui.iska_utils.structure_saver.error.invalid_id"), true);
            return;
        }
        
        // Check for save/modify operations
        String playerNickname = player.getName().getString();
        String finalStructureId = "client_" + playerNickname + "_" + structureId;
        boolean isModifyOperation = (oldStructureId != null);
        

        
        // Get all loaded client structures to check for duplicates/existence
        var allClientStructures = net.unfamily.iskautils.structure.StructureLoader.getClientStructures();
        
        if (isModifyOperation) {
            // Verify that the structure to modify exists
            if (!allClientStructures.containsKey(oldStructureId)) {
                LOGGER.warn("Attempt to modify non-existent structure: {}", oldStructureId);
                player.displayClientMessage(Component.translatable("gui.iska_utils.structure_saver.error.structure_not_found_for_modify"), true);
                return;
            }
            
            // If the ID changes, verify that the new ID is not already in use (but different from the structure we're modifying)
            if (!oldStructureId.equals(finalStructureId) && allClientStructures.containsKey(finalStructureId)) {
                LOGGER.warn("Attempt to modify with duplicate ID: {}", finalStructureId);
                player.displayClientMessage(Component.translatable("gui.iska_utils.save_error_duplicate_id"), true);
                player.displayClientMessage(Component.translatable("gui.iska_utils.save_error_duplicate_id_hint"), true);
                return;
            }
        } else {
            // Save operation: verify there are no duplicates
            if (allClientStructures.containsKey(finalStructureId)) {
                LOGGER.warn("Attempt to save structure with duplicate ID: {}", finalStructureId);
                player.displayClientMessage(Component.translatable("gui.iska_utils.save_error_duplicate_id"), true);
                player.displayClientMessage(Component.translatable("gui.iska_utils.save_error_duplicate_id_hint"), true);
                return;
            }
        }
        
        if (machinePos == null) {
            player.displayClientMessage(Component.translatable("gui.iska_utils.structure_saver.error.invalid_machine_position"), true);
            return;
        }
        
        // Find the machine at the specified position
        ServerLevel level = (ServerLevel) player.level();
        BlockEntity blockEntity = level.getBlockEntity(machinePos);
        
        if (!(blockEntity instanceof StructureSaverMachineBlockEntity machineEntity)) {
            player.displayClientMessage(Component.translatable("gui.iska_utils.structure_saver.error.machine_not_found_at_position", machinePos.toString()), true);
            return;
        }
        
        // Verify that the machine has blueprint data
        if (!machineEntity.hasBlueprintData()) {
            player.displayClientMessage(Component.translatable("gui.iska_utils.structure_saver.error.no_blueprint_data"), true);
            return;
        }
        
        BlockPos vertex1 = machineEntity.getBlueprintVertex1();
        BlockPos vertex2 = machineEntity.getBlueprintVertex2();
        BlockPos center = machineEntity.getBlueprintCenter();
        
        if (vertex1 == null || vertex2 == null || center == null) {
            player.displayClientMessage(Component.translatable("gui.iska_utils.structure_saver.error.incomplete_blueprint_data"), true);
            return;
        }
        
        try {
            // Determine if we're in singleplayer or multiplayer
            boolean isSingleplayer = player.getServer().isSingleplayer();
            
            if (isSingleplayer) {
                // Singleplayer: save directly on the server (which is also the client)
                saveStructure(player, level, structureName, structureId, vertex1, vertex2, center, slower, placeAsPlayer, isModifyOperation, oldStructureId);
                
                // Reload structures to include the newly saved/modified one
                StructureLoader.reloadAllDefinitions(true, player);
            } else {
                // Multiplayer: send command to client to save locally

                StructureSaverMachineClientSaveS2CPacket.send(player, structureName, structureId, vertex1, vertex2, center, 
                                                             slower, placeAsPlayer, isModifyOperation, oldStructureId);
            }
            
            // Clear blueprint data after saving
            machineEntity.clearBlueprintData();
            
            if (isSingleplayer) {
                String operationType = isModifyOperation ? "modified" : "saved";
                player.displayClientMessage(Component.translatable("gui.iska_utils.structure_saver.success", structureName, operationType), true);

            } else {
                String operationType = isModifyOperation ? "modification" : "save";
                player.displayClientMessage(Component.translatable("gui.iska_utils.structure_saver.command_sent", operationType), true);

            }
            
        } catch (Exception e) {
            LOGGER.error("Error during structure save '{}': {}", structureName, e.getMessage());
            player.displayClientMessage(Component.translatable("gui.iska_utils.structure_saver.error.save_failed", e.getMessage()), true);
        }
    }
    
    /**
     * Saves the structure to the player_structures.json file
     */
    private void saveStructure(ServerPlayer player, ServerLevel level, String structureName, String structureId,
                              BlockPos vertex1, BlockPos vertex2, BlockPos center, boolean slower, boolean placeAsPlayer,
                              boolean isModifyOperation, String oldStructureId) throws IOException {
        
        // Calculate area bounds
        int minX = Math.min(vertex1.getX(), vertex2.getX());
        int maxX = Math.max(vertex1.getX(), vertex2.getX());
        int minY = Math.min(vertex1.getY(), vertex2.getY());
        int maxY = Math.max(vertex1.getY(), vertex2.getY());
        int minZ = Math.min(vertex1.getZ(), vertex2.getZ());
        int maxZ = Math.max(vertex1.getZ(), vertex2.getZ());
        
        // Scan blocks in the area
        Map<String, Character> blockToCharMap = new HashMap<>();
        List<String[]> patternLines = new ArrayList<>();
        CharacterAssigner charAssigner = new CharacterAssigner();
        
        // Scan X positions (east-west) - FIRST dimension in JSON
        for (int x = minX; x <= maxX; x++) {
            List<String> xLayers = new ArrayList<>();
            
            // Scan Y layers (height) - SECOND dimension in JSON
            for (int y = minY; y <= maxY; y++) {
                List<String> yRows = new ArrayList<>();
                
                // Scan Z strings (north-south) - THIRD dimension in JSON
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    
                    // Handle special character for center
                    if (pos.equals(center)) {
                        // If the center block is non-solid, use only '@' without adding to key
                        if (!state.isSolid()) {
                            yRows.add("@");
                        } else {
                            // If it's solid, add it to the key and use '@' in the pattern
                            String blockKey = generateBlockKey(state);
                            if (!blockToCharMap.containsKey(blockKey)) {
                                blockToCharMap.put(blockKey, charAssigner.getNextChar());
                            }
                            yRows.add("@");
                        }
                    } else {
                        // For all other blocks, check if they're solid
                        if (!state.isSolid()) {
                            // Non-solid block: use space in pattern and don't add to key
                            yRows.add(" ");
                        } else {
                            // Solid block: assign normal character
                            String blockKey = generateBlockKey(state);
                            char blockChar = blockToCharMap.computeIfAbsent(blockKey, k -> charAssigner.getNextChar());
                            yRows.add(String.valueOf(blockChar));
                        }
                    }
                }
                
                // Join Z positions into a single string for this Y layer
                StringBuilder zString = new StringBuilder();
                for (String zChar : yRows) {
                    zString.append(zChar);
                }
                xLayers.add(zString.toString());
            }
            
            // Add this X position to the pattern
            patternLines.add(xLayers.toArray(new String[0]));
        }
        
        // Create structure JSON using the user-provided ID (without prefix)
        JsonObject structureJson = createStructureJson(structureId, structureName, patternLines, blockToCharMap, level, center, slower, placeAsPlayer);
        
        // Save to file (with possible removal of old structure)
        saveToPlayerStructuresFile(structureJson, isModifyOperation, oldStructureId);
    }
    
    /**
     * Generates a unique key for a BlockState including properties
     */
    private String generateBlockKey(BlockState state) {
        Block block = state.getBlock();
        ResourceLocation blockLocation = BuiltInRegistries.BLOCK.getKey(block);
        
        if (state.getProperties().isEmpty()) {
            return blockLocation.toString();
        }
        
        // Include properties in the format block[prop1=val1,prop2=val2]
        StringBuilder key = new StringBuilder(blockLocation.toString());
        key.append("[");
        
        boolean first = true;
        for (var property : state.getProperties()) {
            if (!first) key.append(",");
            key.append(property.getName()).append("=").append(state.getValue(property).toString());
            first = false;
        }
        
        key.append("]");
        return key.toString();
    }
    
    /**
     * Creates the structure JSON object
     */
    private JsonObject createStructureJson(String structureId, String structureName, 
                                         List<String[]> patternLines, Map<String, Character> blockToCharMap,
                                         ServerLevel level, BlockPos center, boolean slower, boolean placeAsPlayer) {
        
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_utils:structure");
        root.addProperty("overwritable", true);
        
        JsonArray structureArray = new JsonArray();
        JsonObject structureObj = new JsonObject();
        
        // Structure metadata
        structureObj.addProperty("id", structureId);
        structureObj.addProperty("name", structureName);
        
        // Empty optional fields (following default_structures.json format)
        structureObj.add("can_replace", new JsonArray()); // Empty array for can_replace
        
        // Add mode flags if enabled
        if (slower) {
            structureObj.addProperty("slower", true);
        }
        if (placeAsPlayer) {
            structureObj.addProperty("place_like_player", true);
        }
        
        // Blueprint icon (correct format with type)
        JsonObject icon = new JsonObject();
        icon.addProperty("type", "minecraft:item");
        icon.addProperty("item", "iska_utils:blueprint");
        structureObj.add("icon", icon);
        
        // Empty description (optional field)
        JsonArray description = new JsonArray();
        structureObj.add("description", description);
        
        // Pattern (correct format: each row wrapped in array)
        JsonArray patternArray = new JsonArray();
        for (String[] layer : patternLines) {
            JsonArray layerArray = new JsonArray();
            for (String row : layer) {
                // Each row must be an array with a single string
                JsonArray rowArray = new JsonArray();
                rowArray.add(row);
                layerArray.add(rowArray);
            }
            patternArray.add(layerArray);
        }
        structureObj.add("pattern", patternArray);
        
        // Key (character map -> block definitions)
        JsonObject keyObj = new JsonObject();
        
        // Add special mapping for '@' (center) only if the center block is solid
        BlockState centerState = level.getBlockState(center);
        if (centerState.isSolid()) {
            String centerBlockKey = generateBlockKey(centerState);
            
            // Create wrapper object for '@'
            JsonObject centerCharObj = new JsonObject();
            
            // Display name for center
            String centerDisplayName = centerBlockKey.contains("[") ? centerBlockKey.split("\\[")[0] : centerBlockKey;
            centerCharObj.addProperty("display", centerDisplayName.replace(":", "."));
            
            // Array of alternatives for center
            JsonArray centerAlternatives = new JsonArray();
            JsonObject centerBlockDef = new JsonObject();
            
            if (centerBlockKey.contains("[")) {
                // Block with properties
                String[] parts = centerBlockKey.split("\\[", 2);
                String blockName = parts[0];
                String propertiesStr = parts[1].replace("]", "");
                
                centerBlockDef.addProperty("block", blockName);
                
                if (!propertiesStr.isEmpty()) {
                    JsonObject properties = new JsonObject();
                    for (String propPair : propertiesStr.split(",")) {
                        String[] propKV = propPair.split("=", 2);
                        if (propKV.length == 2) {
                            properties.addProperty(propKV[0], propKV[1]);
                        }
                    }
                    centerBlockDef.add("properties", properties);
                }
            } else {
                // Simple block
                centerBlockDef.addProperty("block", centerBlockKey);
            }
            
            centerAlternatives.add(centerBlockDef);
            centerCharObj.add("alternatives", centerAlternatives);
            
            keyObj.add("@", centerCharObj);
        }
        // If center is non-solid, we don't add the '@' key to JSON
        
        // Then add all other blocks (correct format with display and alternatives)
        for (Map.Entry<String, Character> entry : blockToCharMap.entrySet()) {
            String blockKey = entry.getKey();
            Character character = entry.getValue();
            
            // Create wrapper object for character
            JsonObject charObj = new JsonObject();
            
            // Display name (use base block name)
            String displayName = blockKey.contains("[") ? blockKey.split("\\[")[0] : blockKey;
            charObj.addProperty("display", displayName.replace(":", "."));
            
            // Array of alternatives
            JsonArray alternatives = new JsonArray();
            JsonObject blockDef = new JsonObject();
            
            if (blockKey.contains("[")) {
                // Block with properties: block[prop1=val1,prop2=val2]
                String[] parts = blockKey.split("\\[", 2);
                String blockName = parts[0];
                String propertiesStr = parts[1].replace("]", "");
                
                blockDef.addProperty("block", blockName);
                
                if (!propertiesStr.isEmpty()) {
                    JsonObject properties = new JsonObject();
                    for (String propPair : propertiesStr.split(",")) {
                        String[] propKV = propPair.split("=", 2);
                        if (propKV.length == 2) {
                            properties.addProperty(propKV[0], propKV[1]);
                        }
                    }
                    blockDef.add("properties", properties);
                }
            } else {
                // Simple block without properties
                blockDef.addProperty("block", blockKey);
            }
            
            alternatives.add(blockDef);
            charObj.add("alternatives", alternatives);
            
            keyObj.add(character.toString(), charObj);
        }
        structureObj.add("key", keyObj);
        
        structureArray.add(structureObj);
        root.add("structure", structureArray);
        
        return root;
    }
    
    /**
     * Saves the structure to the player_structures.json file
     */
    private void saveToPlayerStructuresFile(JsonObject newStructure, boolean isModifyOperation, String oldStructureId) throws IOException {

        
        String configPath = Config.clientStructurePath;
        if (configPath == null || configPath.trim().isEmpty()) {
            configPath = "iska_utils_client/structures";
        }
        
        Path structuresDir = Paths.get(configPath);
        Path playerStructuresFile = structuresDir.resolve("player_structures.json");
        
        // Create directory if it doesn't exist
        if (!Files.exists(structuresDir)) {
            Files.createDirectories(structuresDir);
        }
        
        JsonObject root;
        
        // Load existing file or create a new one
        if (Files.exists(playerStructuresFile)) {
            try {
                String existingContent = Files.readString(playerStructuresFile);
                root = GSON.fromJson(existingContent, JsonObject.class);
                
                // Ensure it has the correct structure
                if (root == null || !root.has("structure") || !root.get("structure").isJsonArray()) {
                    root = createEmptyPlayerStructuresRoot();
                }
            } catch (Exception e) {
                LOGGER.warn("Error loading existing player_structures.json file, creating new one: {}", e.getMessage());
                root = createEmptyPlayerStructuresRoot();
            }
        } else {
            root = createEmptyPlayerStructuresRoot();
        }
        
        // Handle modification or new save
        JsonArray structuresArray = root.getAsJsonArray("structure");
        JsonArray newStructureArray = newStructure.getAsJsonArray("structure");
        
        if (isModifyOperation && oldStructureId != null) {

            
            // Remove old structure
            boolean foundOldStructure = false;
            for (int i = structuresArray.size() - 1; i >= 0; i--) {
                JsonObject structure = structuresArray.get(i).getAsJsonObject();
                if (structure.has("id") && oldStructureId.equals(structure.get("id").getAsString())) {

                    structuresArray.remove(i);
                    foundOldStructure = true;
                    break; // Remove only the first occurrence
                }
            }
            
            if (!foundOldStructure) {
                LOGGER.warn("Old structure with ID '{}' not found in file", oldStructureId);
            }
        }
        
        // Add new structure
        for (int i = 0; i < newStructureArray.size(); i++) {
            JsonObject newStructureObj = newStructureArray.get(i).getAsJsonObject();
            if (newStructureObj.has("id")) {
                
            }
            structuresArray.add(newStructureObj);
        }
        
        // Save file
        String jsonContent = GSON.toJson(root);
        
        Files.writeString(playerStructuresFile, jsonContent, 
                         StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Creates the empty root structure for the player_structures.json file
     */
    private JsonObject createEmptyPlayerStructuresRoot() {
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_utils:structure");
        root.addProperty("overwritable", true);
        root.add("structure", new JsonArray());
        return root;
    }
    
    /**
     * Helper class to assign unique characters skipping '@'
     */
    private static class CharacterAssigner {
        private char currentChar = 'A';
        
        public char getNextChar() {
            if (currentChar == '@') {
                currentChar++; // Skip '@' reserved for center
            }
            return currentChar++;
        }
    }
} 