package net.unfamily.iskautils.structure;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.unfamily.iskautils.Config;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Carica le definizioni delle strutture dai file JSON esterni
 */
public class StructureLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Mappa per memorizzare le strutture caricate
    private static final Map<String, StructureDefinition> STRUCTURES = new HashMap<>();
    
    // File e directory da cui sono caricate le strutture
    private static final Map<String, Boolean> PROTECTED_DEFINITIONS = new HashMap<>();

    /**
     * Scansiona la directory di configurazione per le strutture
     */
    public static void scanConfigDirectory() {
        String configPath = Config.externalScriptsPath;
        if (configPath == null || configPath.trim().isEmpty()) {
            configPath = "kubejs/external_scripts";
        }
        
        Path structuresPath = Paths.get(configPath, "iska_utils_structures");
        
        try {
            // Crea la directory se non esiste
            if (!Files.exists(structuresPath)) {
                Files.createDirectories(structuresPath);
                LOGGER.info("Creata directory per le strutture: {}", structuresPath);
            }
            
            // Pulisci le strutture precedenti
            PROTECTED_DEFINITIONS.clear();
            STRUCTURES.clear();
            
            // Genera il file di default se non esiste
            Path defaultStructuresFile = structuresPath.resolve("default_structures.json");
            if (!Files.exists(defaultStructuresFile) || shouldRegenerateDefaultStructures(defaultStructuresFile)) {
                LOGGER.debug("Generazione o rigenerazione del file default_structures.json");
                generateDefaultStructures(structuresPath);
            }
            
            // Scansiona tutti i file JSON nella directory
            try (Stream<Path> files = Files.walk(structuresPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted() // Processa in ordine alfabetico
                     .forEach(StructureLoader::scanConfigFile);
            }
            
            LOGGER.info("Scansione strutture completata. Caricate {} strutture", STRUCTURES.size());
            
        } catch (Exception e) {
            LOGGER.error("Errore durante la scansione della directory delle strutture: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Controlla se il file default_structures.json deve essere rigenerato
     */
    private static boolean shouldRegenerateDefaultStructures(Path defaultFile) {
        try {
            if (!Files.exists(defaultFile)) return true;
            
            // Leggi il file e controlla se ha il campo overwritable
            try (InputStream inputStream = Files.newInputStream(defaultFile)) {
                JsonElement jsonElement = GSON.fromJson(new InputStreamReader(inputStream), JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    JsonObject json = jsonElement.getAsJsonObject();
                    if (json.has("overwritable")) {
                        return json.get("overwritable").getAsBoolean();
                    }
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("Errore nel leggere il file di default delle strutture, verrà rigenerato: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Genera il file di strutture di default
     */
    private static void generateDefaultStructures(Path structuresPath) {
        try {
            Path defaultStructuresPath = structuresPath.resolve("default_structures.json");
            
            // Copia il contenuto del file di default interno (quello che hai fornito)
            String defaultStructuresContent = 
                "{\n" +
                "    \"type\": \"iska_utils:structure\",\n" +
                "    \"overwritable\": true,\n" +
                "    \"structure\": [\n" +
                "        {\n" +
                "            \"id\": \"iska_utils-wither_grinder\",\n" +
                "            \"name\": \"Iskandert's Wither Grinder\",\n" +
                "            \"can_force\": true,\n" +
                "            \"can_replace\": [\n" +
                "            ],\n" +
                "            \"icon\": {\n" +
                "                \"type\": \"minecraft:item\",\n" +
                "                \"item\": \"minecraft:wither_skeleton_skull\"\n" +
                "            },\n" +
                "            \"description\": [\"Easy way to kill withers\"],\n" +
                "            \"pattern\": [\n" +
                "                [[\"   \"],[\"   \"],[\"   \"],[\"   \"],[\"AAA\"], [\"AAA\"], [\"AAA\"]],\n" +
                "                [[\"L@ \"],[\"   \"],[\"   \"],[\"   \"],[\"AAA\"], [\"A A\"], [\"AAA\"]],\n" +
                "                [[\"   \"],[\"   \"],[\"   \"],[\"   \"],[\"AAA\"], [\"AAA\"], [\"AAA\"]]\n" +
                "            ],\n" +
                "            \"key\": {\n" +
                "                \"A\": [\n" +
                "                    {\n" +
                "                        \"block\": \"iska_utils:wither_proof_block\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"block\": \"mob_grinding_utils:tinted_glass\"\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"@\": [\n" +
                "                    {\n" +
                "                        \"block\": \"minecraft:piston\",\n" +
                "                        \"properties\": {\n" +
                "                            \"facing\": \"up\"\n" +
                "                        }\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"L\": [\n" +
                "                    {\n" +
                "                        \"block\": \"minecraft:lever\",\n" +
                "                        \"properties\": {\n" +
                "                            \"face\": \"floor\",\n" +
                "                            \"facing\": \"east\",\n" +
                "                            \"powered\": \"false\"\n" +
                "                        }\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}";
            
            Files.write(defaultStructuresPath, defaultStructuresContent.getBytes());
            LOGGER.info("Creato file di strutture di esempio: {}", defaultStructuresPath);
            
        } catch (IOException e) {
            LOGGER.error("Impossibile creare il file di strutture di default: {}", e.getMessage());
        }
    }

    /**
     * Scansiona un singolo file di configurazione per le strutture
     */
    private static void scanConfigFile(Path configFile) {
        LOGGER.debug("Scansione file di configurazione: {}", configFile);
        
        String definitionId = configFile.getFileName().toString().replace(".json", "");
        
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            parseConfigFromStream(definitionId, configFile.toString(), inputStream);
        } catch (Exception e) {
            LOGGER.error("Errore nella lettura del file di strutture {}: {}", configFile, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Analizza la configurazione da un input stream
     */
    private static void parseConfigFromStream(String definitionId, String filePath, InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject json = jsonElement.getAsJsonObject();
                parseConfigJson(definitionId, filePath, json);
            } else {
                LOGGER.error("JSON non valido nel file di strutture: {}", filePath);
            }
        } catch (Exception e) {
            LOGGER.error("Errore nell'analisi del file di strutture {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Analizza la configurazione da un oggetto JSON
     */
    private static void parseConfigJson(String definitionId, String filePath, JsonObject json) {
        try {
            // Controlla se questo è un file di definizione di strutture
            if (!json.has("type") || !json.get("type").getAsString().equals("iska_utils:structure")) {
                LOGGER.debug("Saltato file {} - non è una definizione di struttura", filePath);
                return;
            }
            
            // Ottieni lo stato di sovrascrivibilità
            boolean overwritable = true;
            if (json.has("overwritable")) {
                overwritable = json.get("overwritable").getAsBoolean();
            }
            
            // Controlla se questo è un file protetto
            if (PROTECTED_DEFINITIONS.containsKey(definitionId) && !PROTECTED_DEFINITIONS.get(definitionId)) {
                LOGGER.debug("Saltata definizione di struttura protetta: {}", definitionId);
                return;
            }
            
            // Aggiorna lo stato di protezione
            PROTECTED_DEFINITIONS.put(definitionId, overwritable);
            
            // Processa le strutture
            processStructuresJson(json);
            
        } catch (Exception e) {
            LOGGER.error("Errore nell'elaborazione della definizione di struttura {}: {}", definitionId, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Processa l'array delle strutture da un file di definizione
     */
    private static void processStructuresJson(JsonObject json) {
        if (!json.has("structure") || !json.get("structure").isJsonArray()) {
            LOGGER.error("File di definizione di struttura mancante dell'array 'structure'");
            return;
        }
        
        JsonArray structuresArray = json.getAsJsonArray("structure");
        for (JsonElement structureElement : structuresArray) {
            if (structureElement.isJsonObject()) {
                processStructureDefinition(structureElement.getAsJsonObject());
            }
        }
    }

    /**
     * Processa una singola definizione di struttura
     */
    private static void processStructureDefinition(JsonObject structureJson) {
        try {
            // Ottieni l'ID richiesto
            if (!structureJson.has("id") || !structureJson.get("id").isJsonPrimitive()) {
                LOGGER.error("Definizione di struttura mancante del campo 'id'");
                return;
            }
            
            String structureId = structureJson.get("id").getAsString();
            
            // Crea la definizione di struttura
            StructureDefinition definition = new StructureDefinition();
            definition.setId(structureId);
            
            // Analizza il nome
            if (structureJson.has("name")) {
                definition.setName(structureJson.get("name").getAsString());
            } else {
                definition.setName(structureId); // Usa l'ID come fallback
            }
            
            // Analizza can_force
            if (structureJson.has("can_force")) {
                definition.setCanForce(structureJson.get("can_force").getAsBoolean());
            }
            
            // Analizza can_replace
            if (structureJson.has("can_replace") && structureJson.get("can_replace").isJsonArray()) {
                JsonArray replaceArray = structureJson.getAsJsonArray("can_replace");
                List<String> canReplace = new ArrayList<>();
                for (JsonElement element : replaceArray) {
                    canReplace.add(element.getAsString());
                }
                definition.setCanReplace(canReplace);
            }
            
            // Analizza l'icona
            if (structureJson.has("icon") && structureJson.get("icon").isJsonObject()) {
                JsonObject iconJson = structureJson.getAsJsonObject("icon");
                StructureDefinition.IconDefinition icon = new StructureDefinition.IconDefinition();
                
                if (iconJson.has("type")) {
                    icon.setType(iconJson.get("type").getAsString());
                }
                if (iconJson.has("item")) {
                    icon.setItem(iconJson.get("item").getAsString());
                }
                if (iconJson.has("image")) {
                    icon.setImage(iconJson.get("image").getAsString());
                }
                
                definition.setIcon(icon);
            }
            
            // Analizza la descrizione
            if (structureJson.has("description") && structureJson.get("description").isJsonArray()) {
                JsonArray descArray = structureJson.getAsJsonArray("description");
                List<String> description = new ArrayList<>();
                for (JsonElement element : descArray) {
                    description.add(element.getAsString());
                }
                definition.setDescription(description);
            }
            
            // Analizza il pattern
            if (structureJson.has("pattern") && structureJson.get("pattern").isJsonArray()) {
                JsonArray patternArray = structureJson.getAsJsonArray("pattern");
                String[][][][] pattern = parsePattern(patternArray);
                definition.setPattern(pattern);
            }
            
            // Analizza la chiave
            if (structureJson.has("key") && structureJson.get("key").isJsonObject()) {
                JsonObject keyJson = structureJson.getAsJsonObject("key");
                Map<String, List<StructureDefinition.BlockDefinition>> key = parseKey(keyJson);
                definition.setKey(key);
            }
            
            // Analizza gli stage
            if (structureJson.has("stages") && structureJson.get("stages").isJsonArray()) {
                JsonArray stagesArray = structureJson.getAsJsonArray("stages");
                List<StructureDefinition.StageCondition> stages = parseStages(stagesArray);
                definition.setStages(stages);
            }
            
            // Registra la definizione di struttura
            STRUCTURES.put(structureId, definition);
            LOGGER.debug("Registrata definizione di struttura: {}", structureId);
            
        } catch (Exception e) {
            LOGGER.error("Errore nell'elaborazione della definizione di struttura: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Analizza il pattern della struttura
     * Formato JSON: [X][Y][Z] dove ogni Z è una stringa di caratteri
     * Output: [layer=Y][row=X][col=Z][depth=caratteri] per compatibilità con StructureDefinition
     */
    private static String[][][][] parsePattern(JsonArray patternArray) {
        // Il JSON è in formato [X][Y][Z], dobbiamo convertirlo in [Y][X][Z][caratteri]
        int xPositions = patternArray.size(); // Numero di posizioni X
        if (xPositions == 0) return new String[0][0][0][0];
        
        JsonArray firstX = patternArray.get(0).getAsJsonArray();
        int yLayers = firstX.size(); // Numero di layer Y
        if (yLayers == 0) return new String[0][0][0][0];
        
        JsonArray firstY = firstX.get(0).getAsJsonArray();
        int zStrings = firstY.size(); // Numero di stringhe Z
        if (zStrings == 0) return new String[0][0][0][0];
        
        // Crea array nel formato finale [Y][X][Z][caratteri]
        String[][][][] pattern = new String[yLayers][xPositions][zStrings][];
        
        // Itera attraverso il JSON [X][Y][Z] e riorganizza in [Y][X][Z][caratteri]
        for (int x = 0; x < xPositions; x++) {
            JsonArray xArray = patternArray.get(x).getAsJsonArray();
            for (int y = 0; y < yLayers; y++) {
                JsonArray yArray = xArray.get(y).getAsJsonArray();
                for (int z = 0; z < zStrings; z++) {
                    String cellValue = yArray.get(z).getAsString();
                    // Divide la stringa in caratteri individuali per le posizioni Z multiple
                    pattern[y][x][z] = cellValue.split("");
                }
            }
        }
        
        return pattern;
    }

    /**
     * Analizza la chiave delle definizioni di blocchi
     */
    private static Map<String, List<StructureDefinition.BlockDefinition>> parseKey(JsonObject keyJson) {
        Map<String, List<StructureDefinition.BlockDefinition>> key = new HashMap<>();
        
        for (String keyChar : keyJson.keySet()) {
            JsonArray blockDefArray = keyJson.getAsJsonArray(keyChar);
            List<StructureDefinition.BlockDefinition> blockDefs = new ArrayList<>();
            
            for (JsonElement blockDefElement : blockDefArray) {
                JsonObject blockDefJson = blockDefElement.getAsJsonObject();
                StructureDefinition.BlockDefinition blockDef = new StructureDefinition.BlockDefinition();
                
                if (blockDefJson.has("block")) {
                    blockDef.setBlock(blockDefJson.get("block").getAsString());
                }
                if (blockDefJson.has("tag")) {
                    blockDef.setTag(blockDefJson.get("tag").getAsString());
                }
                
                // Analizza le proprietà
                if (blockDefJson.has("properties") && blockDefJson.get("properties").isJsonObject()) {
                    JsonObject propsJson = blockDefJson.getAsJsonObject("properties");
                    Map<String, String> properties = new HashMap<>();
                    for (String propKey : propsJson.keySet()) {
                        properties.put(propKey, propsJson.get(propKey).getAsString());
                    }
                    blockDef.setProperties(properties);
                }
                
                blockDefs.add(blockDef);
            }
            
            key.put(keyChar, blockDefs);
        }
        
        return key;
    }

    /**
     * Analizza le condizioni di stage
     */
    private static List<StructureDefinition.StageCondition> parseStages(JsonArray stagesArray) {
        List<StructureDefinition.StageCondition> stages = new ArrayList<>();
        
        for (JsonElement stageElement : stagesArray) {
            JsonObject stageJson = stageElement.getAsJsonObject();
            String stageType = stageJson.get("stage_type").getAsString();
            String stage = stageJson.get("stage").getAsString();
            boolean is = stageJson.get("is").getAsBoolean();
            
            stages.add(new StructureDefinition.StageCondition(stageType, stage, is));
        }
        
        return stages;
    }

    /**
     * Ottiene una definizione di struttura per ID
     */
    public static StructureDefinition getStructure(String id) {
        return STRUCTURES.get(id);
    }

    /**
     * Ottiene tutte le definizioni di strutture
     */
    public static Map<String, StructureDefinition> getAllStructures() {
        return new HashMap<>(STRUCTURES);
    }

    /**
     * Ricarica tutte le definizioni di strutture
     */
    public static void reloadAllDefinitions() {
        LOGGER.info("Ricaricamento di tutte le definizioni di strutture...");
        scanConfigDirectory();
    }

    /**
     * Ottiene la lista degli ID delle strutture disponibili
     */
    public static List<String> getAvailableStructureIds() {
        return new ArrayList<>(STRUCTURES.keySet());
    }
} 