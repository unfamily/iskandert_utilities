package net.unfamily.iskautils.command;

import com.mojang.logging.LogUtils;
import net.unfamily.iskautils.Config;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Generates an index file containing documentation about mod commands
 */
public class IndexGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Creates the index file in the configured path
     */
    public static void generateIndex() {
        LOGGER.info("Generating Iska Utils index file...");
        
        try {
            // Get the configured path for external scripts
            String externalScriptsBasePath = Config.externalScriptsPath;
            if (externalScriptsBasePath == null || externalScriptsBasePath.trim().isEmpty()) {
                externalScriptsBasePath = "kubejs/external_scripts"; // default path
            }
            
            // Create directory if it doesn't exist
            Path basePath = Paths.get(externalScriptsBasePath);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            
            // Generate the index file
            Path indexPath = basePath.resolve("iska_utils_index.md");
            
            String indexContent = generateIndexContent();
            Files.writeString(indexPath, indexContent);
            
            LOGGER.info("Generated index file at: {}", indexPath.toAbsolutePath());
            
        } catch (IOException e) {
            LOGGER.error("Error generating index file: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Generates the content for the index file
     */
    private static String generateIndexContent() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# Iska Utils - Documentation Index\n\n");
        sb.append("This document provides an overview of the commands and scripting systems provided by the Iska Utils mod.\n\n");
        
        // Commands section
        sb.append("## Commands Added by the Mod\n\n");
        
        // Macro commands
        sb.append("### `/iska_utils_macro list|test`\n\n");
        sb.append("- `list` - Displays a list of all loaded macro commands\n");
        sb.append("- `test` - Allows testing macro commands without actually executing them\n\n");
        
        // Stage commands
        sb.append("### `/iska_utils_stage add|remove|list|set|clear`\n\n");
        sb.append("- `add` - Adds a stage, with parameters:\n");
        sb.append("  * `world` or `player` to indicate the stage type (if `player`, a target must be specified)\n");
        sb.append("  * `stage` - The string identifier for the stage (recommended to avoid spaces)\n");
        sb.append("  * `silent` - Optional boolean (`true|false`, default `false`) to suppress chat feedback\n\n");
        
        sb.append("- `remove` - Removes a stage, with parameters:\n");
        sb.append("  * `world` or `player` to indicate the stage type (if `player`, a target must be specified)\n");
        sb.append("  * `stage` - The string identifier for the stage to remove\n");
        sb.append("  * `silent` - Optional boolean (`true|false`, default `false`) to suppress chat feedback\n\n");
        
        sb.append("- `list` - Displays stages, with parameters:\n");
        sb.append("  * `world` - Shows stages saved for the world\n");
        sb.append("  * `player` - Shows stages saved for the player (target is an optional parameter, default `@s`)\n");
        sb.append("  * `all` - Shows stages saved for both world and player (target is an optional parameter, default `@s`)\n\n");
        
        sb.append("- `set` - Similar to add/remove, with parameters:\n");
        sb.append("  * `world` or `player` to indicate the stage type (if `player`, a target must be specified)\n");
        sb.append("  * `stage` - The string identifier for the stage\n");
        sb.append("  * `value` - Boolean (`true|false`) where `true` adds the stage and `false` removes it\n");
        sb.append("  * `silent` - Optional boolean (`true|false`, default `false`) to suppress chat feedback\n\n");
        
        sb.append("- `clear` - Removes all stages, with parameters:\n");
        sb.append("  * `world` or `player` or `all` to indicate which stages to clear (if `player` or `all`, target can be specified, default `@s`)\n");
        sb.append("  * `silent` - Optional boolean (`true|false`, default `false`) to suppress chat feedback\n\n");
        
        // Scripting System
        sb.append("## Scripting System\n\n");
        sb.append("All files are JSON format and should be placed in their respective directories/folders.\n");
        sb.append("The default base directory can be changed in the mod's config file (currently set to: `" + Config.externalScriptsPath + "`).\n\n");
        
        sb.append("Files are identified by their `type` parameter:\n\n");
        
        sb.append("- `iska_utils:commands_macro` - For declaring custom commands that group multiple commands with various conditions and parameters.\n");
        sb.append("  * Location: `" + Config.externalScriptsPath + "/iska_utils_macros/`\n\n");
        
        sb.append("- `iska_utils:command_item` - For declaring items that execute a series of commands, including macros.\n");
        sb.append("  * Location: `" + Config.externalScriptsPath + "/command_items/`\n\n");
        
        sb.append("- `iska_utils:stage_item` - For declaring stages that limit items, allowing filtering by inventory. For example, this can block items that go into curios slots.\n");
        sb.append("  * Location: `" + Config.externalScriptsPath + "/stage_items/`\n\n");
        
        sb.append("- `iska_utils:plates` - For declaring custom plates that apply potion effects, direct damage, or predefined statuses like fire and freeze.\n");
        sb.append("  * Location: `" + Config.externalScriptsPath + "/potion_plates/`\n\n");
        
        // Advanced Features
        sb.append("## Advanced Features\n\n");
        
        sb.append("### Sub-Command Implementation\n\n");
        sb.append("The system supports implementing sub-commands using the static parameter type and conditional execution with stages.\n");
        sb.append("See the README.md file in the iska_utils_macros folder for a complete example and explanation.\n\n");
        
        sb.append("### Command Reloading\n\n");
        sb.append("All commands can be reloaded with the standard `/reload` command or the improved version provided by this mod: `/reloader`.\n");
        sb.append("This allows you to modify commands at any time. If you change a default generated file, set `overwritable` to `false` in the file to prevent it from being regenerated.\n\n");
        
        sb.append("---\n\n");
        sb.append("Generated by Iska Utils\n");
        
        return sb.toString();
    }
} 