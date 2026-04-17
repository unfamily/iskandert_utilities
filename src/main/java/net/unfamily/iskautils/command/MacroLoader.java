package net.unfamily.iskautils.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.data.load.IskaUtilsLoadJson;
import net.unfamily.iskautils.data.load.IskaUtilsLoadPaths;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads command macros from external JSON files
 */
@EventBusSubscriber(modid = IskaUtils.MOD_ID)
public class MacroLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Stores files with overwritable=false to prevent them from being overwritten
    private static final Map<String, Boolean> PROTECTED_MACROS = new HashMap<>();
    
    /**
     * Loads command macros from {@code data/<namespace>/load/iska_utils_macros/}.
     */
    public static void scanConfigDirectory() {
        LOGGER.info("Loading command macros from datapack...");
        try {
            PROTECTED_MACROS.clear();
            loadJsonMacrosFrom(null);
            registerCommandsIfServerAvailable();
        } catch (Exception e) {
            LOGGER.error("Error loading macros: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    private static void loadJsonMacrosFrom(ResourceManager resourceManagerOrNull) {
        Map<Identifier, JsonElement> merged = resourceManagerOrNull != null
                ? IskaUtilsLoadJson.collectMergedJson(resourceManagerOrNull,
                id -> IskaUtilsLoadPaths.isJsonUnderLoadSubdir(id, IskaUtilsLoadPaths.MACROS))
                : IskaUtilsLoadJson.collectFromModJarOnly(IskaUtilsLoadPaths.MACROS);
        for (var e : IskaUtilsLoadJson.orderedEntries(merged)) {
            if (!e.getValue().isJsonObject()) {
                continue;
            }
            String p = e.getKey().getPath();
            int slash = p.lastIndexOf('/');
            String macroFileId = slash >= 0 ? p.substring(slash + 1) : p;
            parseConfigJson(macroFileId, e.getKey().toString(), e.getValue().getAsJsonObject());
        }
    }
    

    /**
     * Parses a configuration JSON object
     */
    private static void parseConfigJson(String macroId, String filePath, JsonObject json) {
        try {
            // Check configuration type
            if (!json.has("type")) {
                LOGGER.warn("File {} has no 'type' field, ignored", filePath);
                return;
            }
            
            String type = json.get("type").getAsString();
            
            // Verify that the type is correct
            if (!"iska_utils:commands_macro".equals(type)) {
                LOGGER.warn("Unsupported type '{}' in file {}. Must be 'iska_utils:commands_macro'", type, filePath);
                return;
            }
            
            // Check overwritable flag
            boolean overwritable = true;
            if (json.has("overwritable")) {
                overwritable = json.get("overwritable").getAsBoolean();
            }
            
            // Process the commands array
            if (json.has("commands") && json.get("commands").isJsonArray()) {
                JsonArray macrosArray = json.get("commands").getAsJsonArray();
                LOGGER.info("Found {} macros in file {}", macrosArray.size(), filePath);
                
                for (int i = 0; i < macrosArray.size(); i++) {
                    JsonElement macroElem = macrosArray.get(i);
                    if (macroElem.isJsonObject()) {
                        JsonObject macroObj = macroElem.getAsJsonObject();
                        processMacroJson(macroObj, overwritable);
                    } else {
                        LOGGER.warn("Element {} in 'commands' array is not a valid JSON object in {}", i, filePath);
                    }
                }
            } else {
                LOGGER.warn("Commands macro file {} does not have a valid 'commands' array", filePath);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error parsing file {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Processes a single macro JSON object
     */
    private static void processMacroJson(JsonObject json, boolean overwritable) {
        try {
            // Check required fields
            if (!json.has("command") || !json.has("do")) {
                LOGGER.warn("Invalid macro: missing required fields 'command' or 'do'");
                return;
            }
            
            String macroId = json.get("command").getAsString();
            
            // Check if this macro is protected (defined in a non-overwritable file)
            if (PROTECTED_MACROS.containsKey(macroId) && PROTECTED_MACROS.get(macroId)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Macro '{}' is protected, ignoring new definition", macroId);
                }
                return;
            }
            
            // Store protection status
            PROTECTED_MACROS.put(macroId, !overwritable);
            
            // Create macro definition from JSON
            MacroCommand.MacroDefinition macro = MacroCommand.MacroDefinition.fromJson(json);
            
            // Register the macro
            MacroCommand.registerMacro(macro.getId(), macro);
            
        } catch (Exception e) {
            LOGGER.error("Error processing macro: {}", e.getMessage());
        }
    }
    

    /**
     * Reloads all macros from configuration files and registers commands
     */
    public static void reloadAllMacros() {
        LOGGER.info("Reloading all command macros...");
        
        try {
            // Save protected macros (overwritable=false)
            Map<String, MacroCommand.MacroDefinition> protectedMacros = new HashMap<>();
            for (String macroId : MacroCommand.getAvailableMacros()) {
                if (PROTECTED_MACROS.getOrDefault(macroId, false)) {
                    protectedMacros.put(macroId, MacroCommand.getMacro(macroId));
                }
            }
            
            // Remove all non-protected macros (create a copy of the list to avoid ConcurrentModificationException)
            List<String> macroIds = new ArrayList<>();
            MacroCommand.getAvailableMacros().forEach(macroIds::add);
            
            for (String macroId : macroIds) {
                if (!PROTECTED_MACROS.getOrDefault(macroId, false)) {
                    MacroCommand.unregisterMacro(macroId);
                }
            }
            
            // Clear previous protections
            PROTECTED_MACROS.clear();
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            loadJsonMacrosFrom(server != null ? server.getResourceManager() : null);

            // Restore protected macros that might have been overwritten
            for (Map.Entry<String, MacroCommand.MacroDefinition> entry : protectedMacros.entrySet()) {
                String macroId = entry.getKey();
                PROTECTED_MACROS.put(macroId, true); // Mark as protected
                if (!MacroCommand.hasMacro(macroId)) {
                    MacroCommand.registerMacro(macroId, entry.getValue());
                }
            }
            
            // Count available macros
            int macroCount = 0;
            for (String macro : MacroCommand.getAvailableMacros()) {
                macroCount++;
            }
            
            LOGGER.info("Macro reload completed. {} macros available.", macroCount);
            
            // Register commands if the server is active
            registerCommandsIfServerAvailable();
            
        } catch (Exception e) {
            LOGGER.error("Error reloading macros: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Registers commands for all macros in the CommandDispatcher if the server is available
     */
    private static void registerCommandsIfServerAvailable() {
        try {
            // Get the server
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.debug("Server not available, commands will be registered when the server starts");
                return;
            }
            
            // Get the CommandDispatcher
            CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
            if (dispatcher == null) {
                LOGGER.error("CommandDispatcher not available");
                return;
            }
            
            // Register commands for all macros
            for (String macroId : MacroCommand.getAvailableMacros()) {
                MacroCommand.MacroDefinition macro = MacroCommand.getMacro(macroId);
                if (macro != null) {
                    MacroCommand.registerCommand(dispatcher, macroId, macro);
                    LOGGER.debug("Registered command for macro: {}", macroId);
                }
            }
            
            LOGGER.info("Registered all macro commands in the dispatcher");
        } catch (Exception e) {
            LOGGER.error("Error registering commands: {}", e.getMessage());
        }
    }

    /**
     * Registers commands when the server is loaded
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering commands for all macros...");
        
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Register commands for all macros
        for (String macroId : MacroCommand.getAvailableMacros()) {
            MacroCommand.MacroDefinition macro = MacroCommand.getMacro(macroId);
            if (macro != null) {
                MacroCommand.registerCommand(dispatcher, macroId, macro);
                LOGGER.debug("Registered command for macro: {}", macroId);
            }
        }
    }
} 