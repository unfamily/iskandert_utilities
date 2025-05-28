package net.unfamily.iskautils.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
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
 * Carica macro di comandi da file JSON esterni
 */
public class MacroLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Memorizza i file con overwritable=false per evitare che vengano sovrascritti
    private static final Map<String, Boolean> PROTECTED_MACROS = new HashMap<>();
    
    /**
     * Scansiona la directory di configurazione per le macro di comandi
     */
    public static void scanConfigDirectory() {
        LOGGER.info("Scansione della directory di configurazione per le macro di comandi...");
        
        try {
            // Ottieni il percorso configurato per gli script esterni
            String externalScriptsBasePath = net.unfamily.iskautils.Config.externalScriptsPath;
            if (externalScriptsBasePath == null || externalScriptsBasePath.trim().isEmpty()) {
                externalScriptsBasePath = "kubejs/external_scripts"; // percorso predefinito
            }
            
            // Crea la directory per le macro di comandi se non esiste
            Path configPath = Paths.get(externalScriptsBasePath, "iska_utils_macros");
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                LOGGER.info("Creata directory per le macro di comandi: {}", configPath.toAbsolutePath());
                
                // Crea un file README per spiegare la directory
                createReadme(configPath);
                
                // Genera configurazioni predefinite
                generateDefaultConfigurations(configPath);
                return;
            }
            
            if (!Files.isDirectory(configPath)) {
                LOGGER.warn("Il percorso per le macro esiste ma non è una directory: {}", configPath);
                return;
            }
            
            LOGGER.info("Scansione della directory per le macro: {}", configPath.toAbsolutePath());
            
            // Verifica e rigenera README se mancante
            Path readmePath = configPath.resolve("README.md");
            if (!Files.exists(readmePath)) {
                LOGGER.info("README.md mancante, generazione in corso...");
                createReadme(configPath);
            }
            
            // Pulisce le protezioni precedenti
            PROTECTED_MACROS.clear();
            
            // Verifica se il file default_commands_macro.json esiste e controlla se è overwritable
            Path defaultCommandsFile = configPath.resolve("default_commands_macro.json");
            if (!Files.exists(defaultCommandsFile) || shouldRegenerateDefaultCommandsMacro(defaultCommandsFile)) {
                LOGGER.info("Generazione o rigenerazione del file default_commands_macro.json");
                generateDefaultCommandsMacro(configPath);
            }
            
            // Scansiona tutti i file JSON nella directory
            try (Stream<Path> files = Files.walk(configPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted() // Elaborazione in ordine alfabetico
                     .forEach(MacroLoader::scanConfigFile);
            }
            
            LOGGER.info("Scansione della directory delle macro completata");
            
        } catch (Exception e) {
            LOGGER.error("Errore durante la scansione della directory delle macro: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Controlla se il file default_commands_macro.json dovrebbe essere rigenerato
     */
    private static boolean shouldRegenerateDefaultCommandsMacro(Path filePath) {
        try {
            try (InputStream inputStream = Files.newInputStream(filePath);
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                
                JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    JsonObject json = jsonElement.getAsJsonObject();
                    
                    // Controlla se il campo overwritable esiste ed è true
                    if (json.has("overwritable")) {
                        return json.get("overwritable").getAsBoolean();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Errore nella lettura del file default_commands_macro.json: {}", e.getMessage());
        }
        
        // Se non è possibile leggere il file o non ha il campo overwritable, rigeneralo
        return true;
    }
    
    /**
     * Crea un file README nella directory di configurazione
     */
    private static void createReadme(Path configPath) {
        try {
            Path readmePath = configPath.resolve("README.md");
            String readmeContent = "# Iska Utils - Command Macros\n" +
                "\n" +
                "This directory allows you to create command macros that can be executed directly in the game.\n" +
                "\n" +
                "## Format\n" +
                "\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"iska_utils:commands_macro\",\n" +
                "  \"overwritable\": true,\n" +
                "  \"commands\": [\n" +
                "    {\n" +
                "      \"command\": \"reloader\",\n" +
                "      \"level\": 2,\n" +
                "      \"do\": [\n" +
                "        {\"execute\": \"kubejs reload server-scripts\"},\n" +
                "        {\"execute\": \"reload\"},\n" +
                "        {\"delay\": 60},\n" +
                "        {\"execute\": \"custommachinery reload\"}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"command\": \"spawnmob\",\n" +
                "      \"level\": 0,\n" +
                "      \"stages_logic\": \"OR\",\n" +
                "      \"stages\": [\n" +
                "        {\"stage\": \"example_stage_0\"},\n" +
                "        {\"stage\": \"example_stage_1\"}\n" +
                "      ],\n" +
                "      \"parameters\": [\n" +
                "        {\n" +
                "          \"type\": \"static\",\n" +
                "          \"list\": [\n" +
                "            {\"declare\": \"zombie\"},\n" +
                "            {\"declare\": \"skeleton\"},\n" +
                "            {\"declare\": \"creeper\"}\n" +
                "          ]\n" +
                "        }\n" +
                "      ],\n" +
                "      \"do\": [\n" +
                "        {\"execute\": \"execute at @s run summon minecraft:#0 ~ ~ ~\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n" +
                "\n" +
                "## Fields\n" +
                "\n" +
                "### File Fields\n" +
                "- `type`: Must be **\"iska_utils:commands_macro\"** [**required**]\n" +
                "- `overwritable`: Whether the macros in this file can be overwritten by other files loaded later [optional, default: true]\n" +
                "- `commands`: Array of command macro definitions [**required**]\n" +
                "\n" +
                "### Macro Fields\n" +
                "- `command`: Unique identifier for the macro, which becomes the command name (e.g., `/reloader`) [**required**]\n" +
                "- `level`: Required permission level (0-4) [optional, default: 0]\n" +
                "  - 0: Any player\n" +
                "  - 1-4: OP level required\n" +
                "- `stages_logic`: Logic for stage requirements evaluation: \"AND\" (all required) or \"OR\" (any one required) [optional, default: AND]\n" +
                "- `stages`: Array of game stages that must be unlocked to use this macro [optional]\n" +
                "  - `{\"stage\": \"stage_id\"}`: ID of a required game stage\n" +
                "- `parameters`: Array of parameter definitions for the command [optional]\n" +
                "  - `type`: Type of parameter (`string`, `word`, `int`, `float`, `double`, `boolean`, `target`, `static`) [**required**]\n" +
                "  - `required`: Whether the parameter is required (true) or optional (false) [optional, default: true]\n" +
                "  - `list`: For static type only, array of allowed values for autocomplete [**required for static type**]\n" +
                "    - `{\"declare\": \"value\"}`: Defines a static value for the parameter\n" +
                "- `do`: Array of actions to execute in sequence [**required**]\n" +
                "  - `{\"execute\": \"command\"}`: Execute a server command\n" +
                "  - `{\"delay\": ticks}`: Wait for specified ticks (20 ticks = 1 second)\n" +
                "\n" +
                "### Parameter Types\n" +
                "- `string`: Any text (greedily captures all remaining text)\n" +
                "- `word`: A single word without spaces\n" +
                "- `int`: An integer number\n" +
                "- `float`/`double`: A decimal number\n" +
                "- `boolean`: true or false\n" +
                "- `target`: Player selector (e.g., @p, @a, or player name)\n" +
                "- `static`: Value from a predefined list with autocomplete (like vanilla command arguments)\n" +
                "\n" +
                "### Parameter Usage in Commands\n" +
                "Parameters can be used in commands using `#index` notation where index is the parameter's position (0-based).\n" +
                "For example, `#0` refers to the first parameter, `#1` to the second, etc.\n" +
                "\n" +
                "### Game Stages\n" +
                "The game stages system allows you to lock macros behind progression milestones. This system will integrate with other\n" +
                "features in the mod (implemented in a future update). When implemented, players will need to unlock the required\n" +
                "stages to use stage-restricted macros.\n" +
                "\n" +
                "## Notes\n" +
                "\n" +
                "- Commands are executed in the context of the player who triggered the macro\n" +
                "- Changes require a game restart to apply\n" +
                "- For security reasons, macros are limited to players with the appropriate permission level\n" +
                "- When macros with the same command name are found in multiple files:\n" +
                "  - If a file has `overwritable: false`, its macros cannot be overwritten\n" +
                "  - If a file has `overwritable: true` or no overwritable field, its macros can be overwritten by later files\n" +
                "  - Files are processed in alphabetical order\n" +
                "\n" +
                "## Default Commands Macro\n" +
                "\n" +
                "The mod automatically generates a file called `default_commands_macro.json` with some example macros.\n" +
                "- This file has `overwritable: true` by default, which means it will be regenerated on each start\n" +
                "- If you want to keep your changes to this file, set `overwritable: false`\n" +
                "- Best practice: Instead of editing the default file, create your own files with custom macros\n" +
                "\n" +
                "## Example Use Cases\n" +
                "\n" +
                "### Reload Scripts and Configurations\n" +
                "A macro that reloads various mod systems with appropriate delays between commands.\n" +
                "\n" +
                "### Parametrized Commands\n" +
                "Commands like `/echo Hello World` or `/msg player1 Hello there` that take parameters.\n" +
                "\n" +
                "### Static Parameter Commands\n" +
                "Custom commands with tab-completion like `/spawnmob zombie` or `/setbiome desert` that provide convenient shortcuts with predefined options.\n" +
                "\n" +
                "### Progression-Based Commands\n" +
                "Commands that are only available once players have reached certain milestones or completed specific quests.\n" +
                "\n" +
                "### Time Cycle Demonstration\n" +
                "A macro that cycles through different times of day with delays between changes.\n" +
                "\n" +
                "### Weather Control\n" +
                "A macro that changes weather conditions with appropriate timing.\n";
            
            Files.writeString(readmePath, readmeContent);
            LOGGER.info("Creato file README: {}", readmePath);
            
        } catch (Exception e) {
            LOGGER.warn("Impossibile creare il file README: {}", e.getMessage());
        }
    }
    
    /**
     * Scansiona un singolo file di configurazione
     */
    private static void scanConfigFile(Path configFile) {
        try {
            LOGGER.debug("Scansione del file di configurazione: {}", configFile);
            String macroId = configFile.getFileName().toString();
            
            try (InputStream inputStream = Files.newInputStream(configFile)) {
                parseConfigFromStream(macroId, configFile.toString(), inputStream);
            }
            
        } catch (Exception e) {
            LOGGER.error("Errore durante la scansione del file {}: {}", configFile, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Analizza la configurazione da un input stream
     */
    private static void parseConfigFromStream(String macroId, String filePath, InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                parseConfigJson(macroId, filePath, jsonElement.getAsJsonObject());
            } else {
                LOGGER.error("Il file {} non contiene un oggetto JSON valido", filePath);
            }
        } catch (Exception e) {
            LOGGER.error("Errore nell'analisi del file {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Analizza un oggetto JSON di configurazione
     */
    private static void parseConfigJson(String macroId, String filePath, JsonObject json) {
        try {
            // Verifica il tipo di configurazione
            if (!json.has("type")) {
                LOGGER.warn("File {} non ha il campo 'type', ignorato", filePath);
                return;
            }
            
            String type = json.get("type").getAsString();
            
            // Verifica che il tipo sia corretto
            if (!"iska_utils:commands_macro".equals(type)) {
                LOGGER.warn("Tipo non supportato '{}' nel file {}. Deve essere 'iska_utils:commands_macro'", type, filePath);
                return;
            }
            
            // Verifica overwritable flag
            boolean overwritable = true;
            if (json.has("overwritable")) {
                overwritable = json.get("overwritable").getAsBoolean();
            }
            
            // Elabora l'array di comandi
            if (json.has("commands") && json.get("commands").isJsonArray()) {
                JsonArray macrosArray = json.get("commands").getAsJsonArray();
                LOGGER.info("Trovate {} macro nel file {}", macrosArray.size(), filePath);
                
                for (int i = 0; i < macrosArray.size(); i++) {
                    JsonElement macroElem = macrosArray.get(i);
                    if (macroElem.isJsonObject()) {
                        JsonObject macroObj = macroElem.getAsJsonObject();
                        processMacroJson(macroObj, overwritable);
                    } else {
                        LOGGER.warn("Elemento {} nell'array 'commands' non è un oggetto JSON valido in {}", i, filePath);
                    }
                }
            } else {
                LOGGER.warn("File commands_macro {} non ha un array 'commands' valido", filePath);
            }
            
        } catch (Exception e) {
            LOGGER.error("Errore nell'analisi del file {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Elabora un singolo oggetto JSON di macro
     */
    private static void processMacroJson(JsonObject json, boolean overwritable) {
        try {
            // Verifica i campi obbligatori
            if (!json.has("command") || !json.has("do")) {
                LOGGER.warn("Macro non valida: mancano campi obbligatori 'command' o 'do'");
                return;
            }
            
            String macroId = json.get("command").getAsString();
            
            // Controlla se questa macro è protetta (definita in un file non sovrascrivibile)
            if (PROTECTED_MACROS.containsKey(macroId) && PROTECTED_MACROS.get(macroId)) {
                LOGGER.debug("Macro '{}' protetta, ignoro nuova definizione", macroId);
                return;
            }
            
            // Memorizza lo stato di protezione
            PROTECTED_MACROS.put(macroId, !overwritable);
            
            // Crea la definizione della macro dal JSON
            MacroCommand.MacroDefinition macro = MacroCommand.MacroDefinition.fromJson(json);
            
            // Registra la macro
            MacroCommand.registerMacro(macroId, macro);
            
        } catch (Exception e) {
            LOGGER.error("Errore nell'elaborazione della macro: {}", e.getMessage());
        }
    }
    
    /**
     * Genera configurazioni di macro predefinite
     */
    private static void generateDefaultConfigurations(Path configPath) {
        try {
            // Crea il file default_commands_macro.json
            generateDefaultCommandsMacro(configPath);
        } catch (Exception e) {
            LOGGER.error("Errore nella generazione delle configurazioni predefinite: {}", e.getMessage());
        }
    }
    
    /**
     * Genera il file default_commands_macro.json
     */
    private static void generateDefaultCommandsMacro(Path configPath) throws IOException {
        // Semplifichiamo ulteriormente e assicuriamoci che il formato sia corretto
        String content = "{\n" +
            "  \"type\": \"iska_utils:commands_macro\",\n" +
            "  \"overwritable\": true,\n" +
            "  \"commands\": [\n" +
            "    {\n" +
            "      \"command\": \"reloader\",\n" +
            "      \"level\": 2,\n" +
            "      \"do\": [\n" +
            "        {\"execute\": \"say Inizio ricarica script e configurazioni...\"},\n" +
            "        {\"execute\": \"kubejs reload server-scripts\"},\n" +
            "        {\"delay\": 60},\n" +
            "        {\"execute\": \"reload\"},\n" +
            "        {\"delay\": 60},\n" +
            "        {\"execute\": \"custommachinery reload\"},\n" +
            "        {\"execute\": \"say Ricarica completata!\"}\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"command\": \"echo\",\n" +
            "      \"level\": 0,\n" +
            "      \"parameters\": [\n" +
            "        {\n" +
            "          \"type\": \"string\",\n" +
            "          \"required\": true\n" +
            "        }\n" +
            "      ],\n" +
            "      \"do\": [\n" +
            "        {\"execute\": \"say #0\"}\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"command\": \"spawnmob\",\n" +
            "      \"level\": 0,\n" +
            "      \"stages_logic\": \"OR\",\n" +
            "      \"stages\": [\n" +
            "        {\"stage\": \"example_stage_0\"},\n" +
            "        {\"stage\": \"example_stage_1\"},\n" +
            "        {\"stage\": \"example_stage_2\"}\n" +
            "      ],\n" +
            "      \"parameters\": [\n" +
            "        {\n" +
            "          \"type\": \"static\",\n" +
            "          \"list\": [\n" +
            "            {\"declare\": \"zombie\"},\n" +
            "            {\"declare\": \"skeleton\"},\n" +
            "            {\"declare\": \"creeper\"},\n" +
            "            {\"declare\": \"spider\"},\n" +
            "            {\"declare\": \"enderman\"},\n" +
            "            {\"declare\": \"villager\"}\n" +
            "          ]\n" +
            "        }\n" +
            "      ],\n" +
            "      \"do\": [\n" +
            "        {\"execute\": \"execute at @s run summon minecraft:#0 ~ ~ ~\"}\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        Path filePath = configPath.resolve("default_commands_macro.json");
        Files.writeString(filePath, content);
        LOGGER.info("Generato file default_commands_macro.json: {}", filePath);
        
        // Aggiungi anche un file di test più semplice per debugging
        String simpleContent = "{\n" +
            "  \"type\": \"iska_utils:commands_macro\",\n" +
            "  \"overwritable\": true,\n" +
            "  \"commands\": [\n" +
            "    {\n" +
            "      \"command\": \"test1\",\n" +
            "      \"level\": 0,\n" +
            "      \"do\": [{\"execute\": \"say Test 1 executed\"}]\n" +
            "    },\n" +
            "    {\n" +
            "      \"command\": \"test2\",\n" +
            "      \"level\": 0,\n" +
            "      \"do\": [{\"execute\": \"say Test 2 executed\"}]\n" +
            "    },\n" +
            "    {\n" +
            "      \"command\": \"spawn\",\n" +
            "      \"level\": 0,\n" +
            "      \"parameters\": [\n" +
            "        {\n" +
            "          \"type\": \"static\",\n" +
            "          \"list\": [\n" +
            "            {\"declare\": \"zombie\"},\n" +
            "            {\"declare\": \"skeleton\"},\n" +
            "            {\"declare\": \"creeper\"}\n" +
            "          ]\n" +
            "        }\n" +
            "      ],\n" +
            "      \"do\": [\n" +
            "        {\"execute\": \"execute at @s run summon minecraft:zombie ~ ~ ~\"},\n" +
            "        {\"execute\": \"say Hai evocato uno zombie!\"}\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"command\": \"spawn_skeleton\",\n" +
            "      \"level\": 0,\n" +
            "      \"do\": [\n" +
            "        {\"execute\": \"execute at @s run summon minecraft:skeleton ~ ~ ~\"},\n" +
            "        {\"execute\": \"say Hai evocato uno scheletro!\"}\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"command\": \"spawn_creeper\",\n" +
            "      \"level\": 0,\n" +
            "      \"do\": [\n" +
            "        {\"execute\": \"execute at @s run summon minecraft:creeper ~ ~ ~\"},\n" +
            "        {\"execute\": \"say Hai evocato un creeper!\"}\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        Path testFilePath = configPath.resolve("test_macros.json");
        Files.writeString(testFilePath, simpleContent);
        LOGGER.info("Generato file di test: {}", testFilePath);
        
        // Crea un file di istruzioni addizionale in formato .txt che descrive il file default e avvisa l'utente
        Path instructionsPath = configPath.resolve("INSTRUCTIONS.txt");
        String instructionsContent = 
            "DEFAULT COMMANDS MACRO INSTRUCTIONS\n" +
            "----------------------------------\n\n" +
            "The file 'default_commands_macro.json' contains example macros that are automatically generated.\n" +
            "If you want to keep your changes to this file, open it and set \"overwritable\": false.\n" +
            "Otherwise, it will be regenerated each time the game starts.\n\n" +
            "Best practice: Instead of editing the default file, create your own files with custom macros.\n\n" +
            "For complete documentation, please refer to the README.md file in this directory.";
        
        Files.writeString(instructionsPath, instructionsContent);
    }

    /**
     * Ricarica tutte le macro da disco
     * Questo metodo può essere chiamato quando viene eseguito il comando /reload
     */
    public static void reloadAllMacros() {
        LOGGER.info("Ricaricamento delle macro dei comandi...");
        
        try {
            // Pulisci le definizioni esistenti
            List<String> existingMacros = new ArrayList<>();
            MacroCommand.getAvailableMacros().forEach(existingMacros::add);
            
            LOGGER.info("Eliminazione di {} macro esistenti", existingMacros.size());
            
            // Elimina le macro esistenti
            for (String macroId : existingMacros) {
                MacroCommand.unregisterMacro(macroId);
                LOGGER.debug("Eliminata macro: {}", macroId);
            }
            
            // Pulisci la mappa di protezione
            PROTECTED_MACROS.clear();
            
            // Effettua una nuova scansione della directory
            scanConfigDirectory();
            
            // Conta le macro disponibili dopo la ricarica
            final int[] macroCount = {0};
            MacroCommand.getAvailableMacros().forEach(macro -> macroCount[0]++);
            
            LOGGER.info("Ricaricamento macro completato. Macro disponibili: {}", macroCount[0]);
            
            // Riregistra i comandi con il CommandDispatcher se siamo in un server attivo
            net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                try {
                    // Ottieni il CommandDispatcher
                    com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher = 
                        server.getCommands().getDispatcher();
                    
                    // Prima rimuovi tutti i nodi dei comandi delle macro
                    // per evitare conflitti di registrazione
                    LOGGER.info("Pulizia dei comandi esistenti dal dispatcher...");
                    
                    // Richiedi al server di ricostruire il dispatcher
                    LOGGER.info("Server attivo trovato, riregistrazione dei comandi macro...");
                    
                    // Registra tutte le macro disponibili
                    ExecuteMacroCommand.register(dispatcher);
                    
                    // Sincronizza i comandi con i client connessi
                    LOGGER.info("Sincronizzazione dei comandi con i client...");
                    for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
                        server.getCommands().sendCommands(serverPlayer);
                    }
                    
                    LOGGER.info("Comandi aggiornati e sincronizzati sul server");
                } catch (Exception e) {
                    LOGGER.error("Errore durante la sincronizzazione dei comandi: {}", e.getMessage());
                    if (LOGGER.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                }
            } else {
                LOGGER.warn("Server non trovato, impossibile riregistrare i comandi macro");
            }
        } catch (Exception e) {
            LOGGER.error("Errore durante il ricaricamento delle macro: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
} 