package net.unfamily.iskautils.structure;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.unfamily.iskautils.Config;
import net.minecraft.server.MinecraftServer;
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
 * Loads structure definitions from external JSON files
 */
public class StructureLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Map to store loaded structures
    private static final Map<String, StructureDefinition> STRUCTURES = new HashMap<>();
    
    // Files and directories from which structures are loaded
    private static final Map<String, Boolean> PROTECTED_DEFINITIONS = new HashMap<>();
    
    // Flag to track if we're using server-synced structures (client-side only)
    private static boolean usingServerStructures = false;

    /**
     * Comparatore personalizzato per ordinare le strutture:
     * 1. Strutture server prima (non iniziano con "client_")
     * 2. Strutture client dopo (iniziano con "client_")
     * 3. All'interno di ogni gruppo, ordine alfabetico per ID
     */
    private static final java.util.Comparator<Map.Entry<String, StructureDefinition>> STRUCTURE_COMPARATOR = 
        (entry1, entry2) -> {
            String id1 = entry1.getKey();
            String id2 = entry2.getKey();
            
            boolean isClient1 = id1.startsWith("client_");
            boolean isClient2 = id2.startsWith("client_");
            
            // Se uno è client e l'altro no, il non-client viene prima
            if (isClient1 != isClient2) {
                return isClient1 ? 1 : -1; // Non-client (server) prima
            }
            
            // Se sono dello stesso tipo, ordine alfabetico
            return id1.compareTo(id2);
        };

    /**
     * Scans the configuration directory for structures
     */
    public static void scanConfigDirectory() {
        scanConfigDirectory(false); // Default behavior
    }
    
    /**
     * Scans the configuration directory for server structures only (no client structures)
     * Used during initial mod loading when player is not yet available
     */
    public static void scanConfigDirectoryServerOnly() {
        scanConfigDirectoryInternal(false, null, false); // No force, no player, no client structures
    }
    
    /**
     * Scans the configuration directory for structure definitions
     * @param forceClientStructures Se true, carica sempre le strutture client (per singleplayer e reload)
     */
    public static void scanConfigDirectory(boolean forceClientStructures) {
        scanConfigDirectory(forceClientStructures, null);
    }
    
    /**
     * Scans the configuration directory for structure definitions with specific player context
     * @param forceClientStructures Se true, carica sempre le strutture client (per singleplayer e reload)
     * @param player Il giocatore specifico (per il nickname delle strutture client)
     */
    public static void scanConfigDirectory(boolean forceClientStructures, net.minecraft.server.level.ServerPlayer player) {
        scanConfigDirectoryInternal(forceClientStructures, player, true); // Include client structures
    }
    
    /**
     * Internal method that does the actual directory scanning
     * @param forceClientStructures Se true, carica sempre le strutture client (per singleplayer e reload)
     * @param player Il giocatore specifico (per il nickname delle strutture client)
     * @param includeClientStructures Se true, include le strutture client nel caricamento
     */
    private static void scanConfigDirectoryInternal(boolean forceClientStructures, net.minecraft.server.level.ServerPlayer player, boolean includeClientStructures) {
        String configPath = Config.externalScriptsPath;
        if (configPath == null || configPath.trim().isEmpty()) {
            configPath = "kubejs/external_scripts";
        }
        
        Path structuresPath = Paths.get(configPath, "iska_utils_structures");
        
        try {
            // Create directory if it doesn't exist
            if (!Files.exists(structuresPath)) {
                Files.createDirectories(structuresPath);
                LOGGER.info("Created structures directory: {}", structuresPath);
            }
            
            // Salva le strutture client prima del clear (per preservarle durante il reload)
            Map<String, StructureDefinition> clientStructureBackup = new HashMap<>();
            Map<String, Boolean> clientProtectedBackup = new HashMap<>();
            
            for (Map.Entry<String, StructureDefinition> entry : STRUCTURES.entrySet()) {
                if (entry.getKey().startsWith("client_")) {
                    clientStructureBackup.put(entry.getKey(), entry.getValue());
                    clientProtectedBackup.put(entry.getKey(), PROTECTED_DEFINITIONS.get(entry.getKey()));
                }
            }
            
            // Clear previous structures
            PROTECTED_DEFINITIONS.clear();
            STRUCTURES.clear();
            
            // Ripristina le strutture client salvate
            STRUCTURES.putAll(clientStructureBackup);
            PROTECTED_DEFINITIONS.putAll(clientProtectedBackup);
            
            // Generate default file if it doesn't exist
            Path defaultStructuresFile = structuresPath.resolve("default_structures.json");
            if (!Files.exists(defaultStructuresFile) || shouldRegenerateDefaultStructures(defaultStructuresFile)) {
                LOGGER.debug("Generating or regenerating default_structures.json file");
                generateDefaultStructures(structuresPath);
            }
            
            // Scan all JSON files in the directory
            try (Stream<Path> files = Files.walk(structuresPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted() // Process in alphabetical order
                     .forEach(StructureLoader::scanConfigFile);
            }
            
            int regularStructuresCount = STRUCTURES.size();
            LOGGER.info("Regular structure scanning completed. Loaded {} structures", regularStructuresCount);
            
            // Scan client structures if enabled and requested
            if (includeClientStructures) {
                scanClientStructures(forceClientStructures, player);
            } else {
                LOGGER.debug("Skipping client structures as requested");
            }
            
            int totalStructuresCount = STRUCTURES.size();
            LOGGER.info("Total structure scanning completed. Loaded {} structures ({} regular, {} client)", 
                       totalStructuresCount, regularStructuresCount, totalStructuresCount - regularStructuresCount);
            
            // Generate comprehensive documentation
            StructureDocumentationGenerator.generateDocumentation();
            
        } catch (Exception e) {
            LOGGER.error("Error while scanning structures directory: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if the default_structures.json file should be regenerated
     */
    private static boolean shouldRegenerateDefaultStructures(Path defaultFile) {
        try {
            if (!Files.exists(defaultFile)) return true;
            
            // Read the file and check if it has the overwritable field
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
            LOGGER.warn("Error reading default structures file, it will be regenerated: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Generates the default structures file
     */
    private static void generateDefaultStructures(Path structuresPath) {
        try {
            Path defaultStructuresPath = structuresPath.resolve("default_structures.json");
            
            // Copy the content of the internal default file
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
                "                \"A\": {\n" +
                "                    \"display\": \"iska_utils.wither_proof_block\",\n" +
                "                    \"alternatives\": [\n" +
                "                        {\n" +
                "                            \"block\": \"iska_utils:wither_proof_block\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"block\": \"mob_grinding_utils:tinted_glass\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                \"@\": {\n" +
                "                    \"display\": \"minecraft.piston\",\n" +
                "                    \"alternatives\": [\n" +
                "                        {\n" +
                "                            \"block\": \"minecraft:piston\",\n" +
                "                            \"properties\": {\n" +
                "                                \"facing\": \"up\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                \"L\": {\n" +
                "                    \"display\": \"minecraft.lever\",\n" +
                "                    \"alternatives\": [\n" +
                "                        {\n" +
                "                            \"block\": \"minecraft:lever\",\n" +
                "                            \"properties\": {\n" +
                "                                \"face\": \"floor\",\n" +
                "                                \"facing\": \"east\",\n" +
                "                                \"powered\": \"false\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            }\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": \"iska_utils-wither_summoning\",\n" +
                "            \"name\": \"Wither Summoning Structure\",\n" +
                "            \"can_replace\": [],\n" +
                "            \"slower\": true,\n" +
                "            \"place_like_player\": true,\n" +
                "            \"icon\": {\n" +
                "                \"type\": \"minecraft:item\",\n" +
                "                \"item\": \"minecraft:wither_skeleton_skull\"\n" +
                "            },\n" +
                "            \"description\": [\"Structure to summon the Wither boss\"],\n" +
                "            \"pattern\": [\n" +
                "                [[\" @ \"],[\" A \"],[\"AAA\"],[\"BBB\"]]\n" +
                "            ],\n" +
                "            \"key\": {\n" +
                "                \"A\": {\n" +
                "                    \"display\": \"minecraft.soul_sand\",\n" +
                "                    \"alternatives\": [\n" +
                "                        {\n" +
                "                            \"block\": \"minecraft:soul_sand\"\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"block\": \"minecraft:soul_soil\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                \"B\": {\n" +
                "                    \"display\": \"minecraft.wither_skeleton_skull\",\n" +
                "                    \"alternatives\": [\n" +
                "                        {\n" +
                "                            \"block\": \"minecraft:wither_skeleton_skull\",\n" +
                "                            \"properties\": {\n" +
                "                                \"rotation\": \"0\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    ]\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}";
            
            Files.write(defaultStructuresPath, defaultStructuresContent.getBytes());
            LOGGER.info("Created example structures file: {}", defaultStructuresPath);
            
        } catch (IOException e) {
            LOGGER.error("Unable to create default structures file: {}", e.getMessage());
        }
    }

    /**
     * Scans client structures if enabled
     */
    private static void scanClientStructures() {
        scanClientStructures(false); // Default behavior
    }
    
    /**
     * Scans client structures if enabled
     * @param forceLoad Se true, carica sempre le strutture client (per singleplayer e reload)
     */
    private static void scanClientStructures(boolean forceLoad) {
        scanClientStructures(forceLoad, null);
    }
    
    /**
     * Scans client structures if enabled with specific player context
     * @param forceLoad Se true, carica sempre le strutture client (per singleplayer e reload)
     * @param player Il giocatore specifico (per il nickname delle strutture client)
     */
    private static void scanClientStructures(boolean forceLoad, net.minecraft.server.level.ServerPlayer player) {
        // Determina se siamo sul server o client
        boolean isServer = isRunningOnServer();
        
        // Se forceLoad è true (reload), carica sempre le strutture client
        if (!forceLoad && isServer && !Config.acceptClientStructure) {
            LOGGER.debug("Server configured to not accept client structures, skipping");
            return;
        }
        
        if (forceLoad) {
            // Force loading per reload o singleplayer
        }
        
        String clientStructurePath = Config.clientStructurePath;
        if (clientStructurePath == null || clientStructurePath.trim().isEmpty()) {
            clientStructurePath = "iska_utils_client/structures";
        }
        
        Path clientStructuresPath = Paths.get(clientStructurePath);
        
        try {
            if (!Files.exists(clientStructuresPath)) {
                // Crea la directory se non esiste (sia su client che su server per sviluppo)
                Files.createDirectories(clientStructuresPath);
                LOGGER.info("Created client structures directory: {}", clientStructuresPath);
                // Se la directory è appena stata creata, non ci sono file da scansionare
                return;
            }
            
            LOGGER.info("Scanning client structures from: {}", clientStructuresPath);
            
            // Scan all JSON files in the client directory
            try (Stream<Path> files = Files.walk(clientStructuresPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted() // Process in alphabetical order
                     .forEach(path -> scanClientConfigFile(path, isServer, player));
            }
            
        } catch (Exception e) {
            LOGGER.error("Error while scanning client structures directory: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Checks if we're running on a server
     */
    private static boolean isRunningOnServer() {
        try {
            // Prova a ottenere il server corrente
            MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            return server != null;
        } catch (Exception e) {
            // Se non riusciamo a determinarlo, assumiamo che siamo sul client
            return false;
        }
    }
    
    /**
     * Gets the current player nickname for prefixing client structures
     */
    private static String getCurrentPlayerNickname() {
        return getCurrentPlayerNickname(null);
    }
    
    /**
     * Gets the current player nickname for prefixing client structures with specific player context
     * @param specificPlayer Il giocatore specifico (per il nickname delle strutture client)
     * @return Il nickname del giocatore o un fallback se non determinabile
     */
    private static String getCurrentPlayerNickname(net.minecraft.server.level.ServerPlayer specificPlayer) {
        try {
            // Se abbiamo un giocatore specifico (dal comando), usalo
            if (specificPlayer != null) {
                String nickname = specificPlayer.getName().getString();
                LOGGER.debug("Using specific player nickname: {}", nickname);
                return nickname;
            }
            
            // Controlla se siamo lato server
            boolean isServer = isRunningOnServer();
            
            if (!isServer) {
                // Siamo lato client - prova a ottenere il giocatore client
                try {
                    // Usa reflection per evitare errori quando la classe client non è disponibile lato server
                    Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                    Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
                    if (minecraft != null) {
                        Object player = minecraftClass.getField("player").get(minecraft);
                        if (player != null) {
                            Object nameComponent = player.getClass().getMethod("getName").invoke(player);
                            String nickname = (String) nameComponent.getClass().getMethod("getString").invoke(nameComponent);
                            LOGGER.debug("Using client player nickname: {}", nickname);
                            return nickname;
                        }
                    }
                } catch (Exception clientException) {
                    LOGGER.debug("Failed to get client player via reflection: {}", clientException.getMessage());
                }
            } else {
                // Siamo lato server - prova a ottenere un giocatore dal server
                try {
                    MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        var players = server.getPlayerList().getPlayers();
                        if (!players.isEmpty()) {
                            // Se è singleplayer, prendi il primo giocatore
                            if (server.isSingleplayer()) {
                                String nickname = players.get(0).getName().getString();
                                LOGGER.debug("Using singleplayer first player nickname: {}", nickname);
                                return nickname;
                            } else {
                                // Se è multiplayer, prova a usare il primo giocatore online (fallback)
                                String nickname = players.get(0).getName().getString();
                                LOGGER.debug("Using first online player nickname as fallback: {}", nickname);
                                return nickname;
                            }
                        }
                    }
                } catch (Exception serverException) {
                    LOGGER.debug("Failed to get server player: {}", serverException.getMessage());
                }
            }
            
            // Fallback: usa un nickname generico basato sul thread/context corrente
            String fallbackNickname = generateFallbackNickname();
            LOGGER.warn("Could not determine player nickname, using fallback: {}", fallbackNickname);
            return fallbackNickname;
            
        } catch (Exception e) {
            // Fallback finale in caso di errori
            String fallbackNickname = generateFallbackNickname();
            LOGGER.warn("Error determining player nickname: {}, using fallback: {}", e.getMessage(), fallbackNickname);
            return fallbackNickname;
        }
    }
    
    /**
     * Genera un nickname di fallback quando non riesce a determinare il nickname del giocatore
     */
    private static String generateFallbackNickname() {
        try {
            // Prova a ottenere informazioni dal sistema per creare un fallback unico
            String systemUser = System.getProperty("user.name", "unknown");
            long timestamp = System.currentTimeMillis() % 10000; // Ultime 4 cifre del timestamp
            
            // Pulisci il nome utente sistema da caratteri non validi
            systemUser = systemUser.replaceAll("[^a-zA-Z0-9_]", "");
            if (systemUser.length() > 10) {
                systemUser = systemUser.substring(0, 10);
            }
            if (systemUser.isEmpty()) {
                systemUser = "player";
            }
            
            String fallback = systemUser + "_" + timestamp;
            LOGGER.debug("Generated fallback nickname: {}", fallback);
            return fallback;
            
        } catch (Exception e) {
            // Fallback assoluto
            long timestamp = System.currentTimeMillis() % 10000;
            String absoluteFallback = "player_" + timestamp;
            LOGGER.debug("Generated absolute fallback nickname: {}", absoluteFallback);
            return absoluteFallback;
        }
    }
    
    /**
     * Scans a single client configuration file for structures
     */
    private static void scanClientConfigFile(Path configFile, boolean isServer) {
        scanClientConfigFile(configFile, isServer, null);
    }
    
    /**
     * Scans a single client configuration file for structures with specific player context
     */
    private static void scanClientConfigFile(Path configFile, boolean isServer, net.minecraft.server.level.ServerPlayer player) {
        LOGGER.debug("Scanning client configuration file: {} (server: {})", configFile, isServer);
        
        // Ottieni il nickname del giocatore (ora con fallback garantito)
        String playerNickname = getCurrentPlayerNickname(player);
        
        // Crea un prefisso unico con il nickname del giocatore
        String definitionId = "client_" + playerNickname + "_" + configFile.getFileName().toString().replace(".json", "");
        
        LOGGER.debug("Client structure will be prefixed as: {}", definitionId);
        
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            parseConfigFromStreamWithPrefix(definitionId, configFile.toString(), inputStream, playerNickname);
            
            if (isServer) {
                LOGGER.info("Server accepted client structure from player '{}': {}", playerNickname, configFile);
            } else {
                LOGGER.info("Loaded client structure for player '{}': {}", playerNickname, configFile);
            }
        } catch (Exception e) {
            LOGGER.error("Error reading client structure file {}: {}", configFile, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Scans a single configuration file for structures
     */
    private static void scanConfigFile(Path configFile) {
        LOGGER.debug("Scanning configuration file: {}", configFile);
        
        String definitionId = configFile.getFileName().toString().replace(".json", "");
        
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            parseConfigFromStream(definitionId, configFile.toString(), inputStream);
        } catch (Exception e) {
            LOGGER.error("Error reading structures file {}: {}", configFile, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Parses configuration from an input stream
     */
    private static void parseConfigFromStream(String definitionId, String filePath, InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject json = jsonElement.getAsJsonObject();
                parseConfigJson(definitionId, filePath, json);
            } else {
                LOGGER.error("Invalid JSON in structures file: {}", filePath);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing structures file {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Parses configuration from an input stream with player nickname prefix applied to structure IDs
     */
    private static void parseConfigFromStreamWithPrefix(String definitionId, String filePath, InputStream inputStream, String playerNickname) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject json = jsonElement.getAsJsonObject();
                
                // Applica il prefisso del giocatore agli ID delle strutture nel JSON
                JsonObject modifiedJson = applyPlayerPrefixToStructureIds(json, playerNickname);
                
                parseConfigJson(definitionId, filePath, modifiedJson);
            } else {
                LOGGER.error("Invalid JSON in client structures file: {}", filePath);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing client structures file {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Applies player nickname prefix to all structure IDs in the JSON
     */
    private static JsonObject applyPlayerPrefixToStructureIds(JsonObject originalJson, String playerNickname) {
        try {
            // Crea una copia del JSON originale
            JsonObject modifiedJson = GSON.fromJson(GSON.toJson(originalJson), JsonObject.class);
            
            // Se ha l'array "structure", modifica gli ID al suo interno
            if (modifiedJson.has("structure") && modifiedJson.get("structure").isJsonArray()) {
                JsonArray structuresArray = modifiedJson.getAsJsonArray("structure");
                
                for (JsonElement structureElement : structuresArray) {
                    if (structureElement.isJsonObject()) {
                        JsonObject structureObj = structureElement.getAsJsonObject();
                        
                        // Se ha un campo "id", aggiungi il prefisso del giocatore
                        if (structureObj.has("id")) {
                            String originalId = structureObj.get("id").getAsString();
                            String prefixedId = "client_" + playerNickname + "_" + originalId;
                            structureObj.addProperty("id", prefixedId);
                            
                            LOGGER.debug("Modified structure ID: {} -> {}", originalId, prefixedId);
                        }
                    }
                }
            }
            
            return modifiedJson;
        } catch (Exception e) {
            LOGGER.error("Error applying player prefix to structure IDs: {}", e.getMessage());
            return originalJson; // Ritorna l'originale in caso di errore
        }
    }

    /**
     * Processes configuration from JSON object
     */
    private static void parseConfigJson(String definitionId, String filePath, JsonObject json) {
        try {
            // Check if this is a structure definition file
            if (!json.has("type") || !json.get("type").getAsString().equals("iska_utils:structure")) {
                LOGGER.debug("Skipped file {} - not a structure definition", filePath);
                return;
            }
            
            // Get overwritable status
            boolean overwritable = true;
            if (json.has("overwritable")) {
                overwritable = json.get("overwritable").getAsBoolean();
            }
            
            // Check if this is a protected file
            if (PROTECTED_DEFINITIONS.containsKey(definitionId) && !PROTECTED_DEFINITIONS.get(definitionId)) {
                LOGGER.debug("Skipped protected structure definition: {}", definitionId);
                return;
            }
            
            // Update protection status
            PROTECTED_DEFINITIONS.put(definitionId, overwritable);
            
            // Process structures
            processStructuresJson(json);
            
        } catch (Exception e) {
            LOGGER.error("Error processing structure definition {}: {}", definitionId, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes structure array from definition file
     */
    private static void processStructuresJson(JsonObject json) {
        if (!json.has("structure") || !json.get("structure").isJsonArray()) {
            LOGGER.error("Structure definition file missing 'structure' array");
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
     * Processes a single structure definition
     */
    private static void processStructureDefinition(JsonObject structureJson) {
        try {
            // Get required ID
            if (!structureJson.has("id") || !structureJson.get("id").isJsonPrimitive()) {
                LOGGER.error("Structure definition missing 'id' field");
                return;
            }
            
            String structureId = structureJson.get("id").getAsString();
            
            // Create structure definition
            StructureDefinition definition = new StructureDefinition();
            definition.setId(structureId);
            
            // Parse name
            if (structureJson.has("name")) {
                definition.setName(structureJson.get("name").getAsString());
            } else {
                definition.setName(structureId); // Use ID as fallback
            }
            
            // Parse can_force
            if (structureJson.has("can_force")) {
                definition.setCanForce(structureJson.get("can_force").getAsBoolean());
            }
            
            // Parse slower
            if (structureJson.has("slower")) {
                definition.setSlower(structureJson.get("slower").getAsBoolean());
            }
            
            // Parse place_like_player
            if (structureJson.has("place_like_player")) {
                definition.setPlaceAsPlayer(structureJson.get("place_like_player").getAsBoolean());
            }
            
            // Parse can_replace
            if (structureJson.has("can_replace") && structureJson.get("can_replace").isJsonArray()) {
                JsonArray replaceArray = structureJson.getAsJsonArray("can_replace");
                List<String> canReplace = new ArrayList<>();
                for (JsonElement element : replaceArray) {
                    canReplace.add(element.getAsString());
                }
                definition.setCanReplace(canReplace);
            }
            
            // Parse icon
            if (structureJson.has("icon") && structureJson.get("icon").isJsonObject()) {
                JsonObject iconJson = structureJson.getAsJsonObject("icon");
                StructureDefinition.IconDefinition icon = new StructureDefinition.IconDefinition();
                
                if (iconJson.has("item")) {
                    icon.setItem(iconJson.get("item").getAsString());
                }
                if (iconJson.has("count")) {
                    icon.setCount(iconJson.get("count").getAsInt());
                }
                
                definition.setIcon(icon);
            }
            
            // Parse description
            if (structureJson.has("description")) {
                if (structureJson.get("description").isJsonArray()) {
                    JsonArray descArray = structureJson.getAsJsonArray("description");
                    // Take only first element as single string
                    if (descArray.size() > 0) {
                        definition.setDescription(descArray.get(0).getAsString());
                    }
                } else {
                    definition.setDescription(structureJson.get("description").getAsString());
                }
            }
            
            // Parse pattern
            if (structureJson.has("pattern") && structureJson.get("pattern").isJsonArray()) {
                JsonArray patternArray = structureJson.getAsJsonArray("pattern");
                String[][][][] pattern = parsePattern(patternArray);
                definition.setPattern(pattern);
            }
            
            // Parse key
            if (structureJson.has("key") && structureJson.get("key").isJsonObject()) {
                JsonObject keyJson = structureJson.getAsJsonObject("key");
                Map<String, List<StructureDefinition.BlockDefinition>> key = parseKey(keyJson);
                definition.setKey(key);
            }
            
            // Parse stages - now a list of strings
            if (structureJson.has("stages") && structureJson.get("stages").isJsonArray()) {
                JsonArray stagesArray = structureJson.getAsJsonArray("stages");
                List<String> stages = new ArrayList<>();
                for (JsonElement stageElement : stagesArray) {
                    stages.add(stageElement.getAsString());
                }
                definition.setStages(stages);
            }
            
            // Register structure definition
            STRUCTURES.put(structureId, definition);
            LOGGER.debug("Registered structure definition: {}", structureId);
            
        } catch (Exception e) {
            LOGGER.error("Error processing structure definition: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Parses structure pattern
     * JSON format: [X][Y][Z] where each Z is a string of characters
     * Output: [layer=Y][row=X][col=Z][depth=characters] for StructureDefinition compatibility
     */
    private static String[][][][] parsePattern(JsonArray patternArray) {
        // JSON is in [X][Y][Z] format, we need to convert to [Y][X][Z][characters]
        int xPositions = patternArray.size(); // Number of X positions
        if (xPositions == 0) return new String[0][0][0][0];
        
        JsonArray firstX = patternArray.get(0).getAsJsonArray();
        int yLayers = firstX.size(); // Number of Y layers
        if (yLayers == 0) return new String[0][0][0][0];
        
        JsonArray firstY = firstX.get(0).getAsJsonArray();
        int zStrings = firstY.size(); // Number of Z strings
        if (zStrings == 0) return new String[0][0][0][0];
        
        // Create array in final format [Y][X][Z][characters]
        String[][][][] pattern = new String[yLayers][xPositions][zStrings][];
        
        // Iterate through JSON [X][Y][Z] and reorganize to [Y][X][Z][characters]
        for (int x = 0; x < xPositions; x++) {
            JsonArray xArray = patternArray.get(x).getAsJsonArray();
            for (int y = 0; y < yLayers; y++) {
                JsonArray yArray = xArray.get(y).getAsJsonArray();
                for (int z = 0; z < zStrings; z++) {
                    String cellValue = yArray.get(z).getAsString();
                    // Split string into individual characters for multiple Z positions
                    pattern[y][x][z] = cellValue.split("");
                }
            }
        }
        
        return pattern;
    }

    /**
     * Parses key of block definitions
     */
    private static Map<String, List<StructureDefinition.BlockDefinition>> parseKey(JsonObject keyJson) {
        Map<String, List<StructureDefinition.BlockDefinition>> key = new HashMap<>();
        
        for (String keyChar : keyJson.keySet()) {
            JsonElement keyElement = keyJson.get(keyChar);
            List<StructureDefinition.BlockDefinition> blockDefs = new ArrayList<>();
            String groupDisplayName = null;
            
            if (keyElement.isJsonObject()) {
                // New format with display and alternatives
                JsonObject keyObject = keyElement.getAsJsonObject();
                
                if (keyObject.has("display")) {
                    groupDisplayName = keyObject.get("display").getAsString();
                }
                
                if (keyObject.has("alternatives") && keyObject.get("alternatives").isJsonArray()) {
                    JsonArray alternativesArray = keyObject.getAsJsonArray("alternatives");
                    parseAlternatives(alternativesArray, blockDefs, groupDisplayName);
                }
            } else if (keyElement.isJsonArray()) {
                // Legacy format - direct array
                JsonArray blockDefArray = keyElement.getAsJsonArray();
                parseAlternatives(blockDefArray, blockDefs, groupDisplayName);
            }
            
            key.put(keyChar, blockDefs);
        }
        
        return key;
    }
    
    /**
     * Helper to parse block alternatives
     */
    private static void parseAlternatives(JsonArray alternativesArray, List<StructureDefinition.BlockDefinition> blockDefs, String groupDisplayName) {
        boolean isFirstBlock = true;
        
        for (JsonElement blockDefElement : alternativesArray) {
            JsonObject blockDefJson = blockDefElement.getAsJsonObject();
            StructureDefinition.BlockDefinition blockDef = new StructureDefinition.BlockDefinition();
            
            // First block in group gets the group display name
            if (isFirstBlock && groupDisplayName != null) {
                blockDef.setDisplay(groupDisplayName);
                isFirstBlock = false;
            }
            
            if (blockDefJson.has("block")) {
                blockDef.setBlock(blockDefJson.get("block").getAsString());
            }
            
            // Parse properties
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
    }

    /**
     * Gets a structure definition by ID
     */
    public static StructureDefinition getStructure(String id) {
        return STRUCTURES.get(id);
    }

    /**
     * Gets all structure definitions sorted by ID
     */
    public static Map<String, StructureDefinition> getAllStructures() {
        return STRUCTURES.entrySet().stream()
                .sorted(STRUCTURE_COMPARATOR)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
    }
    
    /**
     * Gets all server-side structures (excluding client-only structures when running on server)
     */
    public static Map<String, StructureDefinition> getServerStructures() {
        boolean isServer = isRunningOnServer();
        if (!isServer) {
            // Se siamo sul client, restituisci tutte le strutture
            return getAllStructures();
        }
        
        // Se siamo sul server, filtra le strutture client se il flag è disabilitato
        if (!Config.acceptClientStructure) {
            return STRUCTURES.entrySet().stream()
                    .filter(entry -> !entry.getKey().startsWith("client_"))
                    .sorted(STRUCTURE_COMPARATOR)
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            java.util.LinkedHashMap::new
                    ));
        }
        
        // Se il server accetta strutture client, restituisci tutte
        return getAllStructures();
    }
    
    /**
     * Gets structures to sync to clients (NEVER includes client structures from server)
     */
    public static Map<String, StructureDefinition> getStructuresForSync() {
        // Le strutture client del server NON devono mai essere sincronizzate ai client
        // Ogni client deve usare le sue strutture client locali
        return STRUCTURES.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("client_"))
                .sorted(STRUCTURE_COMPARATOR)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
    }
    
    /**
     * Gets only client structures
     */
    public static Map<String, StructureDefinition> getClientStructures() {
        return STRUCTURES.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("client_"))
                .sorted(STRUCTURE_COMPARATOR)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
    }

    /**
     * Reloads all structure definitions
     */
    public static void reloadAllDefinitions() {
        LOGGER.info("Reloading all structure definitions...");
        reloadAllDefinitions(true); // Force reload client structures
    }
    
    /**
     * Reloads all structure definitions with control over client structure loading
     * @param forceClientStructures Se true, carica sempre le strutture client (per singleplayer e reload)
     */
    public static void reloadAllDefinitions(boolean forceClientStructures) {
        LOGGER.info("Reloading all structure definitions (force client: {})...", forceClientStructures);
        scanConfigDirectory(forceClientStructures);
    }
    
    /**
     * Reloads all structure definitions with control over client structure loading and specific player context
     * @param forceClientStructures Se true, carica sempre le strutture client (per singleplayer e reload)
     * @param player Il giocatore che ha eseguito il comando (per ottenere il nickname per le strutture client)
     */
    public static void reloadAllDefinitions(boolean forceClientStructures, net.minecraft.server.level.ServerPlayer player) {
        LOGGER.info("Reloading all structure definitions (force client: {}, player: {})...", 
                   forceClientStructures, player != null ? player.getName().getString() : "console");
        scanConfigDirectory(forceClientStructures, player);
    }

    /**
     * Gets the list of available structure IDs sorted alphabetically
     */
    public static List<String> getAvailableStructureIds() {
        return STRUCTURES.entrySet().stream()
                .sorted(STRUCTURE_COMPARATOR)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Sincronizza le strutture ricevute dal server (solo lato client in multiplayer)
     * @param serverStructureData Mappa delle strutture del server in formato JSON
     * @param serverAcceptsClientStructures Flag che indica se il server accetta strutture client
     */
    public static void syncFromServer(Map<String, String> serverStructureData, boolean serverAcceptsClientStructures) {
        LOGGER.info("Sincronizzazione strutture dal server in corso...");
        
        // Controlla se siamo in modalità singleplayer - se sì, non sincronizzare
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft != null && minecraft.hasSingleplayerServer()) {
                LOGGER.debug("Modalità singleplayer detected, non sincronizzare strutture dal server");
                return;
            }
        } catch (Exception e) {
            // Se non riusciamo a determinarlo, procedi con la sincronizzazione
            LOGGER.debug("Non riuscito a determinare se siamo in singleplayer, procedendo con la sincronizzazione");
        }
        
        // Salva le strutture locali protette E le strutture client (se ce ne sono)
        Map<String, StructureDefinition> localProtectedStructures = new HashMap<>();
        Map<String, StructureDefinition> localClientStructures = new HashMap<>();
        Map<String, Boolean> localClientProtected = new HashMap<>();
        
        for (Map.Entry<String, StructureDefinition> entry : STRUCTURES.entrySet()) {
            String structureId = entry.getKey();
            
            // Salva strutture protette (non client)
            if (!structureId.startsWith("client_") && PROTECTED_DEFINITIONS.getOrDefault(structureId, false)) {
                localProtectedStructures.put(structureId, entry.getValue());
            }
            
            // Salva SEMPRE le strutture client (per ripristinarle se il server lo permette)
            if (structureId.startsWith("client_")) {
                localClientStructures.put(structureId, entry.getValue());
                localClientProtected.put(structureId, PROTECTED_DEFINITIONS.getOrDefault(structureId, false));
            }
        }
        
        LOGGER.debug("Salvate {} strutture protette e {} strutture client prima della sincronizzazione", 
                    localProtectedStructures.size(), localClientStructures.size());
        
        // Pulisci le strutture esistenti
        STRUCTURES.clear();
        PROTECTED_DEFINITIONS.clear();
        
        // Ripristina le strutture protette locali
        STRUCTURES.putAll(localProtectedStructures);
        for (String protectedId : localProtectedStructures.keySet()) {
            PROTECTED_DEFINITIONS.put(protectedId, true);
        }
        
        // Carica le strutture dal server
        for (Map.Entry<String, String> entry : serverStructureData.entrySet()) {
            String structureId = entry.getKey();
            String jsonData = entry.getValue();
            
            try {
                // Parse il JSON e crea la struttura
                JsonElement jsonElement = GSON.fromJson(jsonData, JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    JsonObject json = jsonElement.getAsJsonObject();
                    
                    // Se ha il campo "structure", estrai l'array
                    if (json.has("structure")) {
                        JsonArray structureArray = json.getAsJsonArray("structure");
                        if (structureArray.size() > 0) {
                            JsonObject structureObj = structureArray.get(0).getAsJsonObject();
                            processStructureDefinition(structureObj);
                        }
                    } else {
                        // Altrimenti, tratta l'oggetto come una definizione di struttura
                        processStructureDefinition(json);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Errore nel parsing della struttura {} ricevuta dal server: {}", structureId, e.getMessage());
            }
        }
        
        // Ripristina le strutture client locali solo se il server lo permette
        if (serverAcceptsClientStructures && !localClientStructures.isEmpty()) {
            LOGGER.info("Il server accetta strutture client, ripristinando {} strutture client locali salvate...", localClientStructures.size());
            
            // Ripristina le strutture client salvate
            STRUCTURES.putAll(localClientStructures);
            PROTECTED_DEFINITIONS.putAll(localClientProtected);
            
            LOGGER.info("Ripristinate {} strutture client locali", localClientStructures.size());
        } else if (serverAcceptsClientStructures) {
            // Se il server accetta le strutture client ma non ne avevamo salvate, prova a caricarle da disco
            LOGGER.info("Il server accetta strutture client, tentando di caricare da disco...");
            scanClientStructures();
            int clientStructuresCount = (int) STRUCTURES.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("client_"))
                    .count();
            LOGGER.info("Caricate {} strutture client dal disco", clientStructuresCount);
        } else {
            LOGGER.info("Il server non accetta strutture client (o non erano presenti), saltando il ripristino delle strutture client locali");
        }
        
        // Imposta il flag che stiamo usando strutture del server (solo per multiplayer)
        usingServerStructures = true;
        
        int finalClientCount = (int) STRUCTURES.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("client_"))
                .count();
        
        LOGGER.info("Sincronizzazione completata. Strutture caricate: {} (di cui {} server, {} client, {} protette)", 
                   STRUCTURES.size(), 
                   STRUCTURES.size() - finalClientCount - localProtectedStructures.size(),
                   finalClientCount,
                   localProtectedStructures.size());
        LOGGER.info("Client ora utilizza le strutture del server (multiplayer) con client structures: {}", 
                   serverAcceptsClientStructures ? "abilitate" : "disabilitate");
        
        // Rigenera la documentazione
        try {
            StructureDocumentationGenerator.generateDocumentation();
        } catch (Exception e) {
            LOGGER.warn("Errore nella generazione della documentazione dopo la sincronizzazione: {}", e.getMessage());
        }
    }
    
    /**
     * Verifica se il client sta attualmente utilizzando strutture sincronizzate dal server
     * @return true se le strutture provengono dal server, false se sono locali
     */
    public static boolean isUsingServerStructures() {
        return usingServerStructures;
    }
    
    /**
     * Forza il ritorno alle strutture locali (ad esempio dopo disconnessione dal server)
     */
    public static void resetToLocalStructures() {
        if (usingServerStructures) {
            LOGGER.info("Ritornando alle strutture locali...");
            usingServerStructures = false;
            scanConfigDirectory(); // Ricarica le strutture locali
        }
    }
} 