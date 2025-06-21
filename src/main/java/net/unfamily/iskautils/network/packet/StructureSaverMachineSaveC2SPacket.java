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
 * Packet per salvare una struttura dallo Structure Saver Machine
 */
public class StructureSaverMachineSaveC2SPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSaverMachineSaveC2SPacket.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    private final String structureName;
    private final String structureId;
    private final BlockPos machinePos;
    
    /**
     * Crea un nuovo packet per salvare una struttura
     * @param structureName Il nome della struttura da salvare
     * @param structureId L'ID della struttura da salvare
     * @param machinePos La posizione della macchina
     */
    public StructureSaverMachineSaveC2SPacket(String structureName, String structureId, BlockPos machinePos) {
        this.structureName = structureName;
        this.structureId = structureId;
        this.machinePos = machinePos;
    }
    
    /**
     * Gestisce il packet sul lato server
     * @param player Il giocatore che ha inviato il packet
     */
    public void handle(ServerPlayer player) {
        LOGGER.info("=== PROCESSING STRUCTURE SAVER MACHINE SAVE REQUEST ===");
        LOGGER.info("Structure name: '{}'", structureName);
        LOGGER.info("Structure ID: '{}'", structureId);
        LOGGER.info("Machine pos: {}", machinePos);
        
        if (player == null) {
            LOGGER.error("Server player is null while handling StructureSaverMachineSaveC2SPacket");
            return;
        }
        
        LOGGER.info("Player: {}", player.getName().getString());
        
        if (structureName == null || structureName.trim().isEmpty()) {
            player.displayClientMessage(Component.literal("§cNome struttura non valido!"), true);
            return;
        }
        
        if (structureId == null || structureId.trim().isEmpty()) {
            player.displayClientMessage(Component.literal("§cID struttura non valido!"), true);
            return;
        }
        
        // Controllo duplicati: verifica se esiste già una struttura con lo stesso ID finale
        String playerNickname = player.getName().getString();
        String finalStructureId = "client_" + playerNickname + "_" + structureId;
        
        // Ottieni tutte le strutture client caricate per verificare duplicati
        var allClientStructures = net.unfamily.iskautils.structure.StructureLoader.getClientStructures();
        if (allClientStructures.containsKey(finalStructureId)) {
            LOGGER.warn("Tentativo di salvare struttura con ID duplicato: {}", finalStructureId);
            player.displayClientMessage(Component.translatable("gui.iska_utils.save_error_duplicate_id"), true);
            player.displayClientMessage(Component.translatable("gui.iska_utils.save_error_duplicate_id_hint"), true);
            return;
        }
        
        if (machinePos == null) {
            player.displayClientMessage(Component.literal("§cPosizione macchina non valida!"), true);
            return;
        }
        
        // Trova la macchina alla posizione specificata
        ServerLevel level = (ServerLevel) player.level();
        BlockEntity blockEntity = level.getBlockEntity(machinePos);
        
        if (!(blockEntity instanceof StructureSaverMachineBlockEntity machineEntity)) {
            player.displayClientMessage(Component.literal("§cStructure Saver Machine non trovata alla posizione: " + machinePos), true);
            return;
        }
        
        // Verifica che la macchina abbia i dati blueprint
        if (!machineEntity.hasBlueprintData()) {
            player.displayClientMessage(Component.literal("§cNessun dato blueprint trovato! Importa prima un blueprint con le coordinate."), true);
            return;
        }
        
        BlockPos vertex1 = machineEntity.getBlueprintVertex1();
        BlockPos vertex2 = machineEntity.getBlueprintVertex2();
        BlockPos center = machineEntity.getBlueprintCenter();
        
        if (vertex1 == null || vertex2 == null || center == null) {
            player.displayClientMessage(Component.literal("§cDati blueprint incompleti!"), true);
            return;
        }
        
        try {
            // Determina se siamo in singleplayer o multiplayer
            boolean isSingleplayer = player.getServer().isSingleplayer();
            
            if (isSingleplayer) {
                // Singleplayer: salva direttamente sul server (che è anche il client)
                saveStructure(player, level, structureName, structureId, vertex1, vertex2, center);
                
                // Ricarica le strutture per includere quella appena salvata
                StructureLoader.reloadAllDefinitions(true, player);
            } else {
                // Multiplayer: invia comando al client per salvare localmente
                LOGGER.info("Multiplayer detected - sending save command to client");
                StructureSaverMachineClientSaveS2CPacket.send(player, structureName, structureId, vertex1, vertex2, center);
            }
            
            // Pulisci i dati blueprint dopo il salvataggio
            machineEntity.clearBlueprintData();
            
            if (isSingleplayer) {
                player.displayClientMessage(Component.literal("§aStruttura '§f" + structureName + "§a' salvata con successo!"), true);
                LOGGER.info("Player {} saved structure '{}' (singleplayer)", player.getName().getString(), structureName);
            } else {
                player.displayClientMessage(Component.literal("§6Comando di salvataggio inviato al client..."), true);
                LOGGER.info("Save command for structure '{}' sent to client for player {}", structureName, player.getName().getString());
            }
            
        } catch (Exception e) {
            LOGGER.error("Errore durante il salvataggio della struttura '{}': {}", structureName, e.getMessage());
            player.displayClientMessage(Component.literal("§cErrore durante il salvataggio: " + e.getMessage()), true);
        }
    }
    
    /**
     * Salva la struttura nel file player_structures.json
     */
    private void saveStructure(ServerPlayer player, ServerLevel level, String structureName, String structureId,
                              BlockPos vertex1, BlockPos vertex2, BlockPos center) throws IOException {
        
        // Calcola i bounds dell'area
        int minX = Math.min(vertex1.getX(), vertex2.getX());
        int maxX = Math.max(vertex1.getX(), vertex2.getX());
        int minY = Math.min(vertex1.getY(), vertex2.getY());
        int maxY = Math.max(vertex1.getY(), vertex2.getY());
        int minZ = Math.min(vertex1.getZ(), vertex2.getZ());
        int maxZ = Math.max(vertex1.getZ(), vertex2.getZ());
        
        // Scandisce i blocchi nell'area
        Map<String, Character> blockToCharMap = new HashMap<>();
        List<String[]> patternLines = new ArrayList<>();
        CharacterAssigner charAssigner = new CharacterAssigner();
        
        // Scandisce layer per layer (Y)
        for (int y = minY; y <= maxY; y++) {
            List<String> layerRows = new ArrayList<>();
            
            // Scandisce righe (X)  
            for (int x = minX; x <= maxX; x++) {
                StringBuilder row = new StringBuilder();
                
                // Scandisce colonne (Z)
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();
                    
                    // Genera la chiave univoca del blocco (include proprietà)
                    String blockKey = generateBlockKey(state);
                    
                    // Gestisci il carattere speciale per il centro
                    if (pos.equals(center)) {
                        // Se il blocco del centro ha già un carattere assegnato, usalo per @
                        // Altrimenti assegna il carattere successivo e poi usa @ per il centro
                        if (!blockToCharMap.containsKey(blockKey)) {
                            blockToCharMap.put(blockKey, charAssigner.getNextChar());
                        }
                        // Nel pattern, il centro è sempre '@' ma nella key sarà mappato al blocco reale
                        row.append('@');
                    } else {
                        // Per tutti gli altri blocchi, assegna un carattere normale
                        char blockChar = blockToCharMap.computeIfAbsent(blockKey, k -> charAssigner.getNextChar());
                        row.append(blockChar);
                    }
                }
                layerRows.add(row.toString());
            }
            
            // Aggiungi questo layer al pattern
            patternLines.add(layerRows.toArray(new String[0]));
        }
        
        // Crea la struttura JSON usando l'ID fornito dall'utente (senza prefisso)
        JsonObject structureJson = createStructureJson(structureId, structureName, patternLines, blockToCharMap, level, center);
        
        // Salva nel file
        saveToPlayerStructuresFile(structureJson);
    }
    
    /**
     * Genera una chiave univoca per un BlockState includendo le proprietà
     */
    private String generateBlockKey(BlockState state) {
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
    
    /**
     * Crea l'oggetto JSON della struttura
     */
    private JsonObject createStructureJson(String structureId, String structureName, 
                                         List<String[]> patternLines, Map<String, Character> blockToCharMap,
                                         ServerLevel level, BlockPos center) {
        
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_utils:structure");
        root.addProperty("overwritable", true);
        
        JsonArray structureArray = new JsonArray();
        JsonObject structureObj = new JsonObject();
        
        // Metadati struttura
        structureObj.addProperty("id", structureId);
        structureObj.addProperty("name", structureName);
        
        // Campi opzionali vuoti (seguendo il formato del default_structures.json)
        structureObj.add("can_replace", new JsonArray()); // Array vuoto per can_replace
        
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
        
        // Key (mappa caratteri -> definizioni blocchi)
        JsonObject keyObj = new JsonObject();
        
        // Prima aggiungi la mappatura speciale per '@' (centro) con formato corretto
        BlockState centerState = level.getBlockState(center);
        String centerBlockKey = generateBlockKey(centerState);
        
        // Crea l'oggetto wrapper per '@'
        JsonObject centerCharObj = new JsonObject();
        
        // Display name per il centro
        String centerDisplayName = centerBlockKey.contains("[") ? centerBlockKey.split("\\[")[0] : centerBlockKey;
        centerCharObj.addProperty("display", centerDisplayName.replace(":", "."));
        
        // Array di alternative per il centro
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
     * Salva la struttura nel file player_structures.json
     */
    private void saveToPlayerStructuresFile(JsonObject newStructure) throws IOException {
        LOGGER.info("=== SAVING STRUCTURE TO FILE ===");
        
        String configPath = Config.clientStructurePath;
        if (configPath == null || configPath.trim().isEmpty()) {
            configPath = "iska_utils_client/structures";
        }
        
        LOGGER.info("Config path: '{}'", configPath);
        
        Path structuresDir = Paths.get(configPath);
        Path playerStructuresFile = structuresDir.resolve("player_structures.json");
        
        LOGGER.info("Structures directory: {}", structuresDir.toAbsolutePath());
        LOGGER.info("Player structures file: {}", playerStructuresFile.toAbsolutePath());
        
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
        
        // Aggiungi la nuova struttura
        JsonArray structuresArray = root.getAsJsonArray("structure");
        JsonArray newStructureArray = newStructure.getAsJsonArray("structure");
        
        // Aggiungi tutte le strutture dal nuovo oggetto (dovrebbe essere solo una)
        for (int i = 0; i < newStructureArray.size(); i++) {
            structuresArray.add(newStructureArray.get(i));
        }
        
        // Salva il file
        String jsonContent = GSON.toJson(root);
        LOGGER.info("Writing JSON content ({} characters) to file...", jsonContent.length());
        
        Files.writeString(playerStructuresFile, jsonContent, 
                         StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        LOGGER.info("=== STRUCTURE SUCCESSFULLY SAVED ===");
        LOGGER.info("File path: {}", playerStructuresFile.toAbsolutePath());
        LOGGER.info("File size: {} bytes", Files.size(playerStructuresFile));
    }
    
    /**
     * Crea la struttura root vuota per il file player_structures.json
     */
    private JsonObject createEmptyPlayerStructuresRoot() {
        JsonObject root = new JsonObject();
        root.addProperty("type", "iska_utils:structure");
        root.addProperty("overwritable", true);
        root.add("structure", new JsonArray());
        return root;
    }
    
    /**
     * Classe helper per assegnare caratteri univoci saltando '@'
     */
    private static class CharacterAssigner {
        private char currentChar = 'A';
        
        public char getNextChar() {
            if (currentChar == '@') {
                currentChar++; // Salta '@' riservato per il centro
            }
            return currentChar++;
        }
    }
} 