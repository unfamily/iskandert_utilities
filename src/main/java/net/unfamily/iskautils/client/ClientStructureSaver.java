package net.unfamily.iskautils.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Gestisce il salvataggio delle strutture lato client
 */
public class ClientStructureSaver {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientStructureSaver.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    /**
     * Salva una struttura sul client
     */
    public static void saveStructure(String structureName, String structureId, 
                                   BlockPos vertex1, BlockPos vertex2, BlockPos center, 
                                   ClientLevel level, boolean slower, boolean placeAsPlayer,
                                   boolean isModifyOperation, String oldStructureId) throws IOException {
        

        
        // Logica di salvataggio identica a quella del server ma lato client
        saveToPlayerStructuresFile(createStructureJson(structureName, structureId, vertex1, vertex2, center, level, slower, placeAsPlayer), 
                                 isModifyOperation, oldStructureId);
        
        // Ricarica le strutture client
        net.unfamily.iskautils.structure.StructureLoader.reloadAllDefinitions(true);
    }
    
    private static JsonObject createStructureJson(String structureName, String structureId, 
                                                BlockPos vertex1, BlockPos vertex2, BlockPos center, 
                                                ClientLevel level, boolean slower, boolean placeAsPlayer) {
        // Implementazione completa identica al server
        
        // Calcola i limiti dell'area
        int minX = Math.min(vertex1.getX(), vertex2.getX());
        int maxX = Math.max(vertex1.getX(), vertex2.getX());
        int minY = Math.min(vertex1.getY(), vertex2.getY());
        int maxY = Math.max(vertex1.getY(), vertex2.getY());
        int minZ = Math.min(vertex1.getZ(), vertex2.getZ());
        int maxZ = Math.max(vertex1.getZ(), vertex2.getZ());
        
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        

        
        // Mappa per assegnare caratteri ai blocchi (escludendo @)
        Map<String, Character> blockToCharMap = new HashMap<>();
        char nextChar = 'A';
        
        // Pattern 3D: [Y][X][Z] (layer, row, column)
        List<String[]> patternLines = new ArrayList<>();
        
        // Scansiona l'area layer per layer (Y)
        for (int y = minY; y <= maxY; y++) {
            List<String> rows = new ArrayList<>();
            
            // Scansiona row per row (X)  
            for (int x = minX; x <= maxX; x++) {
                StringBuilder row = new StringBuilder();
                
                // Scansiona column per column (Z)
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    
                    if (pos.equals(center)) {
                        // Posizione centrale sempre @
                        row.append('@');
                        // Se il centro è solido, aggiungilo alla mappa per la chiave
                        if (state.isSolid()) {
                            String blockKey = generateBlockKey(state);
                            if (!blockToCharMap.containsKey(blockKey)) {
                                // Salta @ che è riservato per il centro
                                if (nextChar == '@') {
                                    nextChar++;
                                }
                                blockToCharMap.put(blockKey, nextChar);
                                nextChar++;
                            }
                        }
                    } else {
                        // Per tutti gli altri blocchi, controlla se è solido
                        if (!state.isSolid()) {
                            // Blocco non solido: usa spazio nel pattern e non aggiungere alla key
                            row.append(' ');
                        } else {
                            // Blocco solido: assegna carattere normale
                            String blockKey = generateBlockKey(state);
                            
                            // Assegna carattere se non esiste già
                            if (!blockToCharMap.containsKey(blockKey)) {
                                // Salta @ che è riservato per il centro
                                if (nextChar == '@') {
                                    nextChar++;
                                }
                                blockToCharMap.put(blockKey, nextChar);
                                nextChar++;
                            }
                            
                            row.append(blockToCharMap.get(blockKey));
                        }
                    }
                }
                
                rows.add(row.toString());
            }
            
            patternLines.add(rows.toArray(new String[0]));
        }
        

        
        // Crea il JSON della struttura
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_utils:structure");
        root.addProperty("overwritable", true);
        
        JsonArray structureArray = new JsonArray();
        JsonObject structureObj = new JsonObject();
        
        // Metadati struttura
        structureObj.addProperty("id", structureId);
        structureObj.addProperty("name", structureName);
        
        // Campi opzionali vuoti (seguendo il formato del default_structures.json)
        structureObj.add("can_replace", new JsonArray());
        
        // Aggiungi flag modalità se abilitati
        if (slower) {
            structureObj.addProperty("slower", true);
        }
        // place_like_player is configurable for client structures based on server config
        // if (placeAsPlayer) {
        //     structureObj.addProperty("place_like_player", true);
        // }
        
        // Icona blueprint (formato corretto con type)
        JsonObject icon = new JsonObject();
        icon.addProperty("type", "minecraft:item");
        icon.addProperty("item", "iska_utils:blueprint");
        structureObj.add("icon", icon);
        
        // Descrizione vuota (campo opzionale)
        JsonArray description = new JsonArray();
        structureObj.add("description", description);
        
        // Pattern (formato corretto: ogni riga wrappata in array)
        JsonArray patternArray = new JsonArray();
        for (String[] layer : patternLines) {
            JsonArray layerArray = new JsonArray();
            for (String row : layer) {
                // Ogni riga deve essere un array con una singola stringa
                JsonArray rowArray = new JsonArray();
                rowArray.add(row);
                layerArray.add(rowArray);
            }
            patternArray.add(layerArray);
        }
        structureObj.add("pattern", patternArray);
        
        // Key (mappatura caratteri -> blocchi)
        JsonObject keyObj = new JsonObject();
        
        // Aggiungi il blocco centrale (@) solo se è solido
        BlockState centerState = level.getBlockState(center);
        if (centerState.isSolid()) {
            String centerBlockKey = generateBlockKey(centerState);
            
            JsonObject centerCharObj = new JsonObject();
            String centerDisplayName = centerBlockKey.contains("[") ? centerBlockKey.split("\\[")[0] : centerBlockKey;
            centerCharObj.addProperty("display", centerDisplayName.replace(":", "."));
            
            JsonArray centerAlternatives = new JsonArray();
            JsonObject centerBlockDef = new JsonObject();
            
            if (centerBlockKey.contains("[")) {
                // Blocco con proprietà
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
                // Blocco semplice
                centerBlockDef.addProperty("block", centerBlockKey);
            }
            
            centerAlternatives.add(centerBlockDef);
            centerCharObj.add("alternatives", centerAlternatives);
            
            keyObj.add("@", centerCharObj);
        }
        // Se il centro è non solido, non aggiungiamo la chiave '@' al JSON
        
        // Poi aggiungi tutti gli altri blocchi (formato corretto con display e alternatives)
        for (Map.Entry<String, Character> entry : blockToCharMap.entrySet()) {
            String blockKey = entry.getKey();
            Character character = entry.getValue();
            
            // Crea l'oggetto wrapper per il carattere
            JsonObject charObj = new JsonObject();
            
            // Display name (usa il nome del blocco base)
            String displayName = blockKey.contains("[") ? blockKey.split("\\[")[0] : blockKey;
            charObj.addProperty("display", displayName.replace(":", "."));
            
            // Array di alternative
            JsonArray alternatives = new JsonArray();
            JsonObject blockDef = new JsonObject();
            
            if (blockKey.contains("[")) {
                // Blocco con proprietà: block[prop1=val1,prop2=val2]
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
                // Blocco semplice senza proprietà
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
     * Genera una chiave univoca per un BlockState includendo le proprietà
     */
    private static String generateBlockKey(BlockState state) {
        Block block = state.getBlock();
        ResourceLocation blockLocation = BuiltInRegistries.BLOCK.getKey(block);
        
        if (state.getProperties().isEmpty()) {
            return blockLocation.toString();
        }
        
        // Includi le proprietà nel formato block[prop1=val1,prop2=val2]
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
    
    private static void saveToPlayerStructuresFile(JsonObject newStructure, boolean isModifyOperation, String oldStructureId) throws IOException {

        
        String configPath = Config.clientStructurePath;
        if (configPath == null || configPath.trim().isEmpty()) {
            configPath = "iska_utils_client/structures";
        }
        
        Path structuresDir = Paths.get(configPath);
        Path playerStructuresFile = structuresDir.resolve("player_structures.json");
        
        // Crea la directory se non esiste
        if (!Files.exists(structuresDir)) {
            Files.createDirectories(structuresDir);
        }
        
        JsonObject root;
        
        // Carica il file esistente o crea uno nuovo
        if (Files.exists(playerStructuresFile)) {
            try {
                String existingContent = Files.readString(playerStructuresFile);
                root = GSON.fromJson(existingContent, JsonObject.class);
                
                // Assicurati che abbia la struttura corretta
                if (root == null || !root.has("structure") || !root.get("structure").isJsonArray()) {
                    root = createEmptyPlayerStructuresRoot();
                }
            } catch (Exception e) {
                LOGGER.warn("Errore nel caricamento del file player_structures.json esistente, ne creo uno nuovo: {}", e.getMessage());
                root = createEmptyPlayerStructuresRoot();
            }
        } else {
            root = createEmptyPlayerStructuresRoot();
        }
        
        // Gestisci la modifica o il nuovo salvataggio
        JsonArray structuresArray = root.getAsJsonArray("structure");
        JsonArray newStructureArray = newStructure.getAsJsonArray("structure");
        
        if (isModifyOperation && oldStructureId != null) {

            
            // Rimuovi la struttura vecchia
            boolean foundOldStructure = false;
            for (int i = structuresArray.size() - 1; i >= 0; i--) {
                JsonObject structure = structuresArray.get(i).getAsJsonObject();
                if (structure.has("id") && oldStructureId.equals(structure.get("id").getAsString())) {

                    structuresArray.remove(i);
                    foundOldStructure = true;
                    break; // Rimuovi solo la prima occorrenza
                }
            }
            
            if (!foundOldStructure) {
                LOGGER.warn("Old structure with ID '{}' not found in file", oldStructureId);
            }
        }
        
        // Aggiungi la nuova struttura
        for (int i = 0; i < newStructureArray.size(); i++) {
            JsonObject newStructureObj = newStructureArray.get(i).getAsJsonObject();
            if (newStructureObj.has("id")) {
                
            }
            structuresArray.add(newStructureObj);
        }
        
        // Salva il file
        String jsonContent = GSON.toJson(root);
        
        Files.writeString(playerStructuresFile, jsonContent, 
                         StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Crea la struttura root vuota per il file player_structures.json
     */
    private static JsonObject createEmptyPlayerStructuresRoot() {
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_utils:structure");
        root.addProperty("overwritable", true);
        root.add("structure", new JsonArray());
        return root;
    }
}