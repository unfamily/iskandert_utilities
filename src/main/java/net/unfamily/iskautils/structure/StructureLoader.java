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
     * Custom comparator for sorting structures:
     * 1. Server structures first (don't start with "client_")
     * 2. Client structures after (start with "client_")
     * 3. Within each group, alphabetical order by ID
     */
    private static final java.util.Comparator<Map.Entry<String, StructureDefinition>> STRUCTURE_COMPARATOR = 
        (entry1, entry2) -> {
            String id1 = entry1.getKey();
            String id2 = entry2.getKey();
            
            boolean isClient1 = id1.startsWith("client_");
            boolean isClient2 = id2.startsWith("client_");
            
            // If one is client and the other is not, the non-client comes first
            if (isClient1 != isClient2) {
                return isClient1 ? 1 : -1; // Non-client (server) first
            }
            
            // If they are the same type, alphabetical order
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
     * @param forceClientStructures If true, always load client structures (for singleplayer and reload)
     */
    public static void scanConfigDirectory(boolean forceClientStructures) {
        scanConfigDirectory(forceClientStructures, null);
    }
    
    /**
     * Scans the configuration directory for structure definitions with specific player context
     * @param forceClientStructures If true, always load client structures (for singleplayer and reload)
     * @param player The specific player (for client structure nickname)
     */
    public static void scanConfigDirectory(boolean forceClientStructures, net.minecraft.server.level.ServerPlayer player) {
        scanConfigDirectoryInternal(forceClientStructures, player, true); // Include client structures
    }
    
    /**
     * Internal method that does the actual directory scanning
     * @param forceClientStructures If true, always load client structures (for singleplayer and reload)
     * @param player The specific player (for client structure nickname)
     * @param includeClientStructures If true, include client structures in loading
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
            }
            
            // Save client structures before clear (to preserve them during reload)
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
            
            // Restore saved client structures
            STRUCTURES.putAll(clientStructureBackup);
            PROTECTED_DEFINITIONS.putAll(clientProtectedBackup);
            
            // Generate default file if it doesn't exist
            Path defaultStructuresFile = structuresPath.resolve("default_structures.json");
            if (!Files.exists(defaultStructuresFile) || shouldRegenerateDefaultStructures(defaultStructuresFile)) {
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
            
            // Scan client structures if enabled and requested
            if (includeClientStructures) {
                scanClientStructures(forceClientStructures, player);
            }
            
            int totalStructuresCount = STRUCTURES.size();
            
            // Generate comprehensive documentation
            StructureDocumentationGenerator.generateDocumentation();
            
        } catch (Exception e) {
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
                "                    \"display\": \"minecraft.sticky_piston\",\n" +
                "                    \"alternatives\": [\n" +
                "                        {\n" +
                "                            \"block\": \"minecraft:sticky_piston\",\n" +
                "                            \"properties\": {\n" +
                "                                \"facing\": \"up\"\n" +
                "                            }\n" +
                "                        },\n" +
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
            
        } catch (IOException e) {
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
     * @param forceLoad If true, always load client structures (for singleplayer and reload)
     */
    private static void scanClientStructures(boolean forceLoad) {
        scanClientStructures(forceLoad, null);
    }
    
    /**
     * Scans client structures if enabled with specific player context
     * @param forceLoad If true, always load client structures (for singleplayer and reload)
     * @param player The specific player (for client structure nickname)
     */
    private static void scanClientStructures(boolean forceLoad, net.minecraft.server.level.ServerPlayer player) {
        // Determine if we're on server or client
        boolean isServer = isRunningOnServer();
        
        // If forceLoad is true (reload), always load client structures
        if (!forceLoad && isServer && !Config.acceptClientStructure) {
            return;
        }
        
        if (forceLoad) {
            // Force loading for reload or singleplayer
        }
        
        String clientStructurePath = Config.clientStructurePath;
        if (clientStructurePath == null || clientStructurePath.trim().isEmpty()) {
            clientStructurePath = "iska_utils_client/structures";
        }
        
        Path clientStructuresPath = Paths.get(clientStructurePath);
        
        try {
            if (!Files.exists(clientStructuresPath)) {
                // Create directory if it doesn't exist (both on client and server for development)
                Files.createDirectories(clientStructuresPath);
                // If the directory was just created, there are no files to scan
                return;
            }
            
            // Scan all JSON files in the client directory
            try (Stream<Path> files = Files.walk(clientStructuresPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted() // Process in alphabetical order
                     .forEach(path -> scanClientConfigFile(path, isServer, player));
            }
            
        } catch (Exception e) {
        }
    }
    
    /**
     * Checks if we're running on a server
     */
    private static boolean isRunningOnServer() {
        try {
            // Try to get the current server
            MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            return server != null;
        } catch (Exception e) {
            // If we can't determine it, assume we're on the client
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
     * @param specificPlayer The specific player (for client structure nickname)
     * @return The player's nickname or a fallback if not determinable
     */
    private static String getCurrentPlayerNickname(net.minecraft.server.level.ServerPlayer specificPlayer) {
        try {
            // If we have a specific player (from command), use it
            if (specificPlayer != null) {
                String nickname = specificPlayer.getName().getString();
                return nickname;
            }
            
            // Check if we're on server side
            boolean isServer = isRunningOnServer();
            
            if (!isServer) {
                // We're on client side - try to get the client player
                try {
                    // Use reflection to avoid errors when client class is not available on server side
                    Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                    Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
                    if (minecraft != null) {
                        Object player = minecraftClass.getField("player").get(minecraft);
                        if (player != null) {
                            Object nameComponent = player.getClass().getMethod("getName").invoke(player);
                            String nickname = (String) nameComponent.getClass().getMethod("getString").invoke(nameComponent);
                            return nickname;
                        }
                    }
                } catch (Exception clientException) {
                }
            } else {
                // We're on server side - try to get a player from the server
                try {
                    MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        var players = server.getPlayerList().getPlayers();
                        if (!players.isEmpty()) {
                            // If it's singleplayer, take the first player
                            if (server.isSingleplayer()) {
                                String nickname = players.get(0).getName().getString();
                                return nickname;
                            } else {
                                // If it's multiplayer, try to use the first online player (fallback)
                                String nickname = players.get(0).getName().getString();
                                return nickname;
                            }
                        }
                    }
                } catch (Exception serverException) {
                }
            }
            
            // Fallback: use a generic nickname based on current thread/context
            String fallbackNickname = generateFallbackNickname();
            return fallbackNickname;
            
        } catch (Exception e) {
            // Final fallback in case of errors
            String fallbackNickname = generateFallbackNickname();
            return fallbackNickname;
        }
    }
    
    /**
     * Generates a fallback nickname when unable to determine player nickname
     */
    private static String generateFallbackNickname() {
        try {
            // Try to get system information to create a unique fallback
            String systemUser = System.getProperty("user.name", "unknown");
            long timestamp = System.currentTimeMillis() % 10000; // Last 4 digits of timestamp
            
            // Clean system username from invalid characters
            systemUser = systemUser.replaceAll("[^a-zA-Z0-9_]", "");
            if (systemUser.length() > 10) {
                systemUser = systemUser.substring(0, 10);
            }
            if (systemUser.isEmpty()) {
                systemUser = "player";
            }
            
            String fallback = systemUser + "_" + timestamp;
            return fallback;
            
        } catch (Exception e) {
            // Absolute fallback
            long timestamp = System.currentTimeMillis() % 10000;
            String absoluteFallback = "player_" + timestamp;
            return absoluteFallback;
        }
    }
    
    /**
     * Scans a single client configuration file for structures
     */
    private static void scanClientConfigFile(Path configFile, boolean isServer, net.minecraft.server.level.ServerPlayer player) {
        // Get player nickname (now with guaranteed fallback)
        String playerNickname = getCurrentPlayerNickname(player);
        
        // Create unique prefix with player nickname
        String definitionId = "client_" + playerNickname + "_" + configFile.getFileName().toString().replace(".json", "");
        
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            parseConfigFromStreamWithPrefix(definitionId, configFile.toString(), inputStream, playerNickname);
            
            if (isServer) {
            } else {
            }
        } catch (Exception e) {
        }
    }

    /**
     * Scans a single configuration file for structures
     */
    private static void scanConfigFile(Path configFile) {
        String definitionId = configFile.getFileName().toString().replace(".json", "");
        
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            parseConfigFromStream(definitionId, configFile.toString(), inputStream);
        } catch (Exception e) {
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
            }
        } catch (Exception e) {
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
                
                // Applies the player prefix to structure IDs in the JSON
                JsonObject modifiedJson = applyPlayerPrefixToStructureIds(json, playerNickname);
                
                parseConfigJson(definitionId, filePath, modifiedJson);
            } else {
            }
        } catch (Exception e) {
        }
    }
    
    /**
     * Applies player nickname prefix to all structure IDs in the JSON
     */
    private static JsonObject applyPlayerPrefixToStructureIds(JsonObject originalJson, String playerNickname) {
        try {
            // Creates a copy of the original JSON
            JsonObject modifiedJson = GSON.fromJson(GSON.toJson(originalJson), JsonObject.class);
            
            // If it has the "structure" array, modify the IDs inside
            if (modifiedJson.has("structure") && modifiedJson.get("structure").isJsonArray()) {
                JsonArray structuresArray = modifiedJson.getAsJsonArray("structure");
                
                for (JsonElement structureElement : structuresArray) {
                    if (structureElement.isJsonObject()) {
                        JsonObject structureObj = structureElement.getAsJsonObject();
                        
                        // If it has an "id" field, add the player prefix
                        if (structureObj.has("id")) {
                            String originalId = structureObj.get("id").getAsString();
                            String prefixedId = "client_" + playerNickname + "_" + originalId;
                            structureObj.addProperty("id", prefixedId);
                        }
                    }
                }
            }
            
            return modifiedJson;
        } catch (Exception e) {
            return originalJson; // Return the original in case of error
        }
    }

    /**
     * Processes configuration from JSON object
     */
    private static void parseConfigJson(String definitionId, String filePath, JsonObject json) {
        try {
            // Check if this is a structure definition file
            if (!json.has("type") || !json.get("type").getAsString().equals("iska_utils:structure")) {
                return;
            }
            
            // Get overwritable status
            boolean overwritable = true;
            if (json.has("overwritable")) {
                overwritable = json.get("overwritable").getAsBoolean();
            }
            
            // Check if this is a protected file
            if (PROTECTED_DEFINITIONS.containsKey(definitionId) && !PROTECTED_DEFINITIONS.get(definitionId)) {
                return;
            }
            
            // Update protection status
            PROTECTED_DEFINITIONS.put(definitionId, overwritable);
            
            // Process structures
            processStructuresJson(json);
            
        } catch (Exception e) {
        }
    }

    /**
     * Processes structure array from definition file
     */
    private static void processStructuresJson(JsonObject json) {
        if (!json.has("structure") || !json.get("structure").isJsonArray()) {
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
            
            // Parse place_like_player - CONFIGURABLE for client structures
            if (structureJson.has("place_like_player")) {
                boolean placeAsPlayer = structureJson.get("place_like_player").getAsBoolean();
                // For client structures, respect the config setting
                if (structureId.startsWith("client_")) {
                    placeAsPlayer = placeAsPlayer && net.unfamily.iskautils.Config.allowClientStructurePlayerLike;
                }
                definition.setPlaceAsPlayer(placeAsPlayer);
            }
            
            // Parse overwritable
            if (structureJson.has("overwritable")) {
                definition.setOverwritable(structureJson.get("overwritable").getAsBoolean());
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
            
            // Parse machine visibility parameter (default: true)
            if (structureJson.has("machine")) {
                definition.setMachine(structureJson.get("machine").getAsBoolean());
            }
            
            // Parse hidden parameter (default: false)
            if (structureJson.has("hidden")) {
                definition.setHidden(structureJson.get("hidden").getAsBoolean());
            }
            
            // Register structure definition
            STRUCTURES.put(structureId, definition);
        } catch (Exception e) {
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
            
            // Parse display for individual blocks
            if (blockDefJson.has("display")) {
                blockDef.setDisplay(blockDefJson.get("display").getAsString());
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
            
            // Parse ignore_placement
            if (blockDefJson.has("ignore_placement")) {
                blockDef.setIgnorePlacement(blockDefJson.get("ignore_placement").getAsBoolean());
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
        
                    // If we're on the server, filter client structures if the flag is disabled
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
        // Server client structures should NEVER be synchronized to clients
        // Each client must use its own local client structures
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
     * Gets structures visible in Structure Placer Machine GUI
     * Filters out structures with machine=false
     */
    public static Map<String, StructureDefinition> getMachineVisibleStructures() {
        return getAllStructures().entrySet().stream()
                .filter(entry -> entry.getValue().isMachine()) // Only include structures where machine=true
                .sorted(STRUCTURE_COMPARATOR)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
    }
    
    /**
     * Gets structures visible via commands only
     * Returns structures with hidden=true
     */
    public static Map<String, StructureDefinition> getCommandOnlyStructures() {
        return getAllStructures().entrySet().stream()
                .filter(entry -> entry.getValue().isHidden()) // Only include structures where hidden=true
                .sorted(STRUCTURE_COMPARATOR)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
    }
    
    /**
     * Gets only player structures from player_structures.json file for the Structure Saver Machine GUI
     * This is specifically for the modify functionality - only shows structures that can be modified
     */
    public static Map<String, StructureDefinition> getPlayerStructuresForSaverMachine() {
        Map<String, StructureDefinition> playerStructures = new HashMap<>();
        
        String clientStructurePath = Config.clientStructurePath;
        if (clientStructurePath == null || clientStructurePath.trim().isEmpty()) {
            clientStructurePath = "iska_utils_client/structures";
        }
        
        Path playerStructuresFile = Paths.get(clientStructurePath).resolve("player_structures.json");
        
        if (!Files.exists(playerStructuresFile)) {
            return playerStructures; // Return empty map
        }
        
        try {
            String jsonContent = Files.readString(playerStructuresFile);
            JsonElement jsonElement = GSON.fromJson(jsonContent, JsonElement.class);
            
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject json = jsonElement.getAsJsonObject();
                
                if (json.has("structure") && json.get("structure").isJsonArray()) {
                    JsonArray structuresArray = json.getAsJsonArray("structure");
                    
                    for (JsonElement structureElement : structuresArray) {
                        if (structureElement.isJsonObject()) {
                            JsonObject structureJson = structureElement.getAsJsonObject();
                            
                            try {
                                StructureDefinition definition = parseStructureDefinitionFromJson(structureJson);
                                if (definition != null && definition.getId() != null) {
                                    playerStructures.put(definition.getId(), definition);
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
        }
        
        return playerStructures;
    }
    
    /**
     * Helper method to parse a StructureDefinition from a JsonObject
     * Used specifically by getPlayerStructuresForSaverMachine()
     */
    private static StructureDefinition parseStructureDefinitionFromJson(JsonObject structureJson) {
        if (!structureJson.has("id") || !structureJson.get("id").isJsonPrimitive()) {
            return null;
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
        
        // Parse optional fields
        if (structureJson.has("can_force")) {
            definition.setCanForce(structureJson.get("can_force").getAsBoolean());
        }
        
        if (structureJson.has("slower")) {
            definition.setSlower(structureJson.get("slower").getAsBoolean());
        }
        
        // Parse place_like_player - CONFIGURABLE for client structures
        if (structureJson.has("place_like_player")) {
            boolean placeAsPlayer = structureJson.get("place_like_player").getAsBoolean();
            // For client structures, respect the config setting
            if (structureId.startsWith("client_")) {
                placeAsPlayer = placeAsPlayer && net.unfamily.iskautils.Config.allowClientStructurePlayerLike;
            }
            definition.setPlaceAsPlayer(placeAsPlayer);
        }
        
        // Parse overwritable
        if (structureJson.has("overwritable")) {
            definition.setOverwritable(structureJson.get("overwritable").getAsBoolean());
        }
        
        // Parse machine visibility parameter (default: true)
        if (structureJson.has("machine")) {
            definition.setMachine(structureJson.get("machine").getAsBoolean());
        }
        
        // Parse hidden parameter (default: false)
        if (structureJson.has("hidden")) {
            definition.setHidden(structureJson.get("hidden").getAsBoolean());
        }
        
        // Parse description array if present
        if (structureJson.has("description") && structureJson.get("description").isJsonArray()) {
            JsonArray descArray = structureJson.getAsJsonArray("description");
            StringBuilder description = new StringBuilder();
            for (JsonElement desc : descArray) {
                if (description.length() > 0) description.append(" ");
                description.append(desc.getAsString());
            }
            definition.setDescription(description.toString());
        }
        
        // Parse icon if present
        if (structureJson.has("icon") && structureJson.get("icon").isJsonObject()) {
            JsonObject iconObj = structureJson.getAsJsonObject("icon");
            if (iconObj.has("item")) {
                StructureDefinition.IconDefinition icon = new StructureDefinition.IconDefinition();
                icon.setItem(iconObj.get("item").getAsString());
                definition.setIcon(icon);
            }
        }
        
        // Note: We don't parse pattern/key here since this is only for GUI display
        // The actual structure data isn't needed for the structure saver machine list
        
        return definition;
    }

    /**
     * Reloads all structure definitions
     */
    public static void reloadAllDefinitions() {
        reloadAllDefinitions(true); // Force reload client structures
    }
    
    /**
     * Reloads all structure definitions with control over client structure loading
     * @param forceClientStructures If true, always load client structures (for singleplayer and reload)
     */
    public static void reloadAllDefinitions(boolean forceClientStructures) {
        scanConfigDirectory(forceClientStructures);
    }
    
    /**
     * Reloads all structure definitions with control over client structure loading and specific player context
     * @param forceClientStructures If true, always load client structures (for singleplayer and reload)
     * @param player The player who executed the command (to get the nickname for client structures)
     */
    public static void reloadAllDefinitions(boolean forceClientStructures, net.minecraft.server.level.ServerPlayer player) {
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
     * Synchronizes structures received from the server (client-side only in multiplayer)
     * @param serverStructureData Map of server structures in JSON format
     * @param serverAcceptsClientStructures Flag indicating if the server accepts client structures
     */
    public static void syncFromServer(Map<String, String> serverStructureData, boolean serverAcceptsClientStructures) {
        // Check if we're in singleplayer mode - if so, don't synchronize
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft != null && minecraft.hasSingleplayerServer()) {
                return;
            }
        } catch (Exception e) {
            // If we can't determine it, proceed with synchronization
        }
        
        // Save local protected structures AND client structures (if any)
        Map<String, StructureDefinition> localProtectedStructures = new HashMap<>();
        Map<String, StructureDefinition> localClientStructures = new HashMap<>();
        Map<String, Boolean> localClientProtected = new HashMap<>();
        
        for (Map.Entry<String, StructureDefinition> entry : STRUCTURES.entrySet()) {
            String structureId = entry.getKey();
            
            // Save protected structures (non-client)
            if (!structureId.startsWith("client_") && PROTECTED_DEFINITIONS.getOrDefault(structureId, false)) {
                localProtectedStructures.put(structureId, entry.getValue());
            }
            
            // ALWAYS save client structures (to restore them if the server allows it)
            if (structureId.startsWith("client_")) {
                localClientStructures.put(structureId, entry.getValue());
                localClientProtected.put(structureId, PROTECTED_DEFINITIONS.getOrDefault(structureId, false));
            }
        }
        
        // Clear existing structures
        STRUCTURES.clear();
        PROTECTED_DEFINITIONS.clear();
        
        // Restore local protected structures
        STRUCTURES.putAll(localProtectedStructures);
        for (String protectedId : localProtectedStructures.keySet()) {
            PROTECTED_DEFINITIONS.put(protectedId, true);
        }
        
        // Load structures from server
        for (Map.Entry<String, String> entry : serverStructureData.entrySet()) {
            String structureId = entry.getKey();
            String jsonData = entry.getValue();
            
            try {
                // Parse JSON and create the structure
                JsonElement jsonElement = GSON.fromJson(jsonData, JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    JsonObject json = jsonElement.getAsJsonObject();
                    
                    // If it has the "structure" field, extract the array
                    if (json.has("structure")) {
                        JsonArray structureArray = json.getAsJsonArray("structure");
                        if (structureArray.size() > 0) {
                            JsonObject structureObj = structureArray.get(0).getAsJsonObject();
                            processStructureDefinition(structureObj);
                        }
                    } else {
                        // Otherwise, treat the object as a structure definition
                        processStructureDefinition(json);
                    }
                }
            } catch (Exception e) {
            }
        }
        
        // Restore local client structures only if the server allows it
        if (serverAcceptsClientStructures && !localClientStructures.isEmpty()) {
            // Restore saved client structures
            STRUCTURES.putAll(localClientStructures);
            PROTECTED_DEFINITIONS.putAll(localClientProtected);
        } else if (serverAcceptsClientStructures) {
            // If the server accepts client structures but we had none saved, try to load them from disk
            scanClientStructures();
        }
        
        // Set the flag that we're using server structures (only for multiplayer)
        usingServerStructures = true;
        
        int finalClientCount = (int) STRUCTURES.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("client_"))
                .count();
        
        // Regenerate documentation
        try {
            StructureDocumentationGenerator.generateDocumentation();
        } catch (Exception e) {
        }
    }
    
    /**
     * Checks if the client is currently using structures synchronized from the server
     * @return true if structures come from the server, false if they are local
     */
    public static boolean isUsingServerStructures() {
        return usingServerStructures;
    }
    
    /**
     * Forces return to local structures (e.g., after disconnection from server)
     */
    public static void resetToLocalStructures() {
        if (usingServerStructures) {
            usingServerStructures = false;
            scanConfigDirectory(); // Reload local structures
        }
    }
} 