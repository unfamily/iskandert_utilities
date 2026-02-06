package net.unfamily.iskautils.command;

import com.mojang.logging.LogUtils;
import net.unfamily.iskautils.Config;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        sb.append("## Table of Contents\n\n");
        sb.append("1. [Commands Added by the Mod](#commands-added-by-the-mod)\n");
        sb.append("2. [Scripting System](#scripting-system)\n");
        sb.append("3. [Configurable Tags](#configurable-tags)\n");
        sb.append("4. [Internal Stages](#internal-stages)\n");
        sb.append("5. [Advanced Features](#advanced-features)\n\n");
        
        // Commands section
        sb.append("## Commands Added by the Mod\n\n");
        
        // Macro commands
        sb.append("### `/iska_utils_macro list|test`\n\n");
        sb.append("- `list` - Displays a list of all loaded macro commands\n");
        sb.append("- `test` - Allows testing macro commands without actually executing them\n\n");
        
        // Stage commands
        sb.append("### `/iska_utils_stage add|remove|list|set|clear`\n\n");
        sb.append("- `add` - Adds a stage, with parameters:\n");
        sb.append("  * `world`, `player`, or `team` to indicate the stage type (if `player` or `team`, a target must be specified)\n");
        sb.append("  * `stage` - The string identifier for the stage (recommended to avoid spaces)\n");
        sb.append("  * `silent` - Optional boolean (`true|false`, default `false`) to suppress chat feedback\n\n");
        
        sb.append("- `remove` - Removes a stage, with parameters:\n");
        sb.append("  * `world`, `player`, or `team` to indicate the stage type (if `player` or `team`, a target must be specified)\n");
        sb.append("  * `stage` - The string identifier for the stage to remove\n");
        sb.append("  * `silent` - Optional boolean (`true|false`, default `false`) to suppress chat feedback\n\n");
        
        sb.append("- `list` - Displays stages, with parameters:\n");
        sb.append("  * `world` - Shows stages saved for the world\n");
        sb.append("  * `player` - Shows stages saved for the player (target is an optional parameter, default `@s`)\n");
        sb.append("  * `team` - Shows stages saved for the team (target is an optional parameter, default `@s`)\n");
        sb.append("  * `all` - Shows stages saved for world, player, and team (target is an optional parameter, default `@s`)\n\n");
        
        sb.append("- `set` - Similar to add/remove, with parameters:\n");
        sb.append("  * `world`, `player`, or `team` to indicate the stage type (if `player` or `team`, a target must be specified)\n");
        sb.append("  * `stage` - The string identifier for the stage\n");
        sb.append("  * `value` - Boolean (`true|false`) where `true` adds the stage and `false` removes it\n");
        sb.append("  * `silent` - Optional boolean (`true|false`, default `false`) to suppress chat feedback\n\n");
        
        sb.append("- `clear` - Removes all stages, with parameters:\n");
        sb.append("  * `world`, `player`, `team`, or `all` to indicate which stages to clear (if `player`, `team`, or `all`, target can be specified, default `@s`)\n");
        sb.append("  * `silent` - Optional boolean (`true|false`, default `false`) to suppress chat feedback\n\n");

        sb.append("- `call_action <target> <action_id>` - Runs a stage action for target(s). Target supports @a, @p, @s, @r, @e, @n. Stages are rechecked at execution.\n");
        sb.append("  * `force` - Bypass onCall=false (optional, default false)\n");
        sb.append("  * `silent`, `hide` - Suppress feedback (optional)\n\n");

        // Shop commands
        sb.append("### `/iska_utils_debug hand|reload`\n\n");
        sb.append("- `hand` - Dumps the item in hand (ID, NBT, tags) for debugging\n");
        sb.append("- `reload` - Reloads all dynamic configs without a full `/reload`: command items, stage actions, stage items, shop, structures. Requires OP level 2.\n\n");

        sb.append("### `/iska_utils_shop reload|info|currencies|categories|entries|balance`\n\n");
        sb.append("- `reload` - Reloads all shop configurations from files\n");
        sb.append("- `info` - Shows general information about the shop system\n");
        sb.append("- `currencies` - Lists all available currencies\n");
        sb.append("- `categories` - Lists all available categories\n");
        sb.append("- `entries` - Lists all shop entries (optionally filtered by category)\n");
        sb.append("- `balance` - Shows the current team's balance\n\n");
        
        // Team commands
        sb.append("### `/iska_utils_team`\n\n");
        sb.append("Team management commands for the shop system:\n\n");
        sb.append("- `create <teamName>` - Creates a new team\n");
        sb.append("- `delete [teamName]` - Deletes own team or specified team (admin only)\n");
        sb.append("- `rename <newName> [teamName]` - Renames own team or specified team\n");
        sb.append("- `transfer <newLeader> [teamName]` - Transfers team leadership\n");
        sb.append("- `add <player> [teamName]` - Adds player to team (admin only)\n");
        sb.append("- `remove <player> [teamName]` - Removes player from team\n");
        sb.append("- `info [teamName]` - Shows team information\n");
        sb.append("- `list` - Lists all teams\n");
        sb.append("- `balance [teamName] [currencyId]` - Shows team balance\n");
        sb.append("- `addCurrency <currencyId> <amount> [team|player] [target]` - Adds currency to team (admin only)\n");
        sb.append("- `removeCurrency <currencyId> <amount> [team|player] [target]` - Removes currency from team (admin only)\n");
        sb.append("- `setCurrency <currencyId> <amount> [team|player] [target]` - Sets team currency balance (admin only)\n");
        sb.append("- `invite <player> [teamName]` - Invites player to team\n");
        sb.append("- `accept <teamName>` - Accepts team invitation\n");
        sb.append("- `leave` - Leaves current team\n");
        sb.append("- `assistant add|remove|list <player> [teamName]` - Manages team assistants\n\n");
        
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
        
        sb.append("- `iska_utils:structure` - For declaring custom structures that can be placed manually or automatically with the Structure Placer Machine.\n");
        sb.append("  * Location: `" + Config.externalScriptsPath + "/iska_utils_structures/`\n");
        sb.append("  * Documentation: See README.md in the structures folder for complete examples and features\n\n");
        
        sb.append("- `iska_utils:structure_monouse_item` - For declaring monouse items that give materials and place structures as rewards.\n");
        sb.append("  * Location: `" + Config.externalScriptsPath + "/iska_utils_structures/`\n\n");
        
        sb.append("- `iska_utils:shop_currency` - For declaring custom currencies for the shop system.\n");
        sb.append("  * Location: `" + Config.externalScriptsPath + "/shop/currencies/`\n\n");
        
        sb.append("- `iska_utils:shop_category` - For declaring categories to organize shop entries.\n");
        sb.append("  * Location: `" + Config.externalScriptsPath + "/shop/categories/`\n\n");
        
        sb.append("- `iska_utils:shop_entry` - For declaring items available in the shop with prices and properties.\n");
        sb.append("  * Location: `" + Config.externalScriptsPath + "/shop/entries/`\n");
        sb.append("  * Documentation: See README.txt in the shop folder for complete examples and NBT support\n\n");
        
        // Configurable Tags
        sb.append("## Configurable Tags\n\n");
        sb.append("Tags read by the mod at runtime, extensible via datapack.\n\n");
        sb.append("### Tag: `iska_utils:curio_compat/mimic` (item)\n\n");
        sb.append("Extra drop added to the Mimic loot table (Artifacts). Defines which Curio items can drop from the Mimic as additional loot.\n\n");
        sb.append("- **Pool chance:** 5% per Mimic kill\n");
        sb.append("- **Item selection:** 1 item chosen from the tag with equal weight per item\n");
        sb.append("- **Default items:** `necrotic_crystal_heart`, `mining_equitizer`\n\n");
        sb.append("Extend via datapack: create `data/iska_utils/tags/item/curio_compat/mimic.json` with `replace: false` and add your item IDs to `values`. Requires Artifacts and Curios mods.\n\n");
        sb.append("### Tag: `c:wrench_not_rotate` (block)\n\n");
        sb.append("Blacklist for the Swiss Wrench. Blocks in this tag cannot be rotated by the Swiss Wrench.\n\n");
        sb.append("- **Default:** vanilla beds (`#minecraft:beds`)\n");
        sb.append("Extend via datapack: create `data/c/tags/block/wrench_not_rotate.json` (or your namespace) with `replace: false` and add block IDs to `values`.\n\n");
        
        // Internal Stages
        sb.append("## Internal Stages\n\n");
        sb.append("The mod uses stages with the `iska_utils_internal-` prefix for internal logic. The following are documented for packdev and integrations.\n\n");
        sb.append("### Stages for packdev\n\n");
        sb.append("These stages can be assigned via datapack/macro. **Curse stages** are scalable: they can be applied at **player** (single player), **team** (all members of the player's team), or **world** (everyone). The mod checks in that order; if any level has the stage, the effect applies.\n\n");
        sb.append("- **`iska_utils_internal-curse_flight`** (player | team | world)\n");
        sb.append("  * **Use:** Disables creative flight (and from other mods) while the stage is present at the given level.\n");
        sb.append("  * **Where:** `FlightHandler` (player tick event).\n");
        sb.append("  * **Effect:** Every tick, if the stage is on the player, their team, or the world, `mayfly` and `flying` are forced to `false`; spectator is not affected.\n\n");
        sb.append("- **`iska_utils_internal-curse_flame`** (player | team | world)\n");
        sb.append("  * **Use:** Enables \"super hot\" mode for Burning Brazier and Burning Flame block without using config.\n");
        sb.append("  * **Where:** `BurningBrazierItem` (flame placement and inventoryTick), `BurningFlameBlock` (entityInside), `Config` (evil_things options comments).\n");
        sb.append("  * **Effect:** If the stage is on the player, their team, or the world, Brazier and Flame set entities on fire as with `burning_brazier_super_hot` / `burning_flame_super_hot` options. For non-player entities in the flame block, only world stage is checked.\n\n");
        sb.append("### Other internal stages\n\n");
        sb.append("- **`iska_utils_internal-funpack_flight0`** / **`iska_utils_internal-funpack_flight1`** (player)\n");
        sb.append("  * **Use:** Fanpack flight heartbeat: the mod enables flight only when `funpack_flight0` is present (set by item/curio).\n");
        sb.append("  * **Where:** `FlightHandler`, `FanpackItem`, `FanpackCurioHandler`.\n");
        sb.append("  * **Effect:** Ensures the Fanpack is actually present; do not assign manually for flight.\n\n");
        sb.append("- **`iska_utils_internal-greedy_shield_equip`** (player)\n");
        sb.append("  * **Use:** Indicates that the Greedy Shield is equipped.\n");
        sb.append("  * **Where:** `GreedyShieldItem`, `LivingIncomingDamageEventHandler`.\n\n");
        sb.append("- **`iska_utils_internal-necro_crystal_heart_equip`** (player)\n");
        sb.append("  * **Use:** Indicates that the Necrotic Crystal Heart is equipped.\n");
        sb.append("  * **Where:** `NecroticCrystalHeartItem`, `LivingIncomingDamageEventHandler`.\n\n");
        sb.append("- **`iska_utils_internal-necrotic_crystal_heart`** (player)\n");
        sb.append("  * **Use:** Removed when the player wakes up.\n");
        sb.append("  * **Where:** `PlayerSleep`.\n\n");
        sb.append("- **`iska_utils_internal-CH:<year>`** (e.g. `iska_utils_internal-CH:2025`)\n");
        sb.append("  * **Use:** Dynamic stages per year (Calendar/Date events).\n");
        sb.append("  * **Where:** `DateEvents`.\n\n");
        
        // Advanced Features
        sb.append("## Advanced Features\n\n");
        
        sb.append("### Sub-Command Implementation\n\n");
        sb.append("The system supports implementing sub-commands using the static parameter type and conditional execution with stages.\n");
        sb.append("See the README.md file in the iska_utils_macros folder for a complete example and explanation.\n\n");
        
        sb.append("### Command Reloading\n\n");
        sb.append("For a **quick reload** without restarting, use `/iska_utils_debug reload` (OP level 2). This reloads: command items, stage actions, stage items, shop, structures. It does not reload macros.\n\n");
        sb.append("For a **full reload**, use the standard `/reload` command or the mod's `/reloader` macro. This reloads all configs including macros.\n\n");
        sb.append("If you change a default generated file, set `overwritable` to `false` in the file to prevent it from being regenerated.\n\n");
        
        sb.append("---\n\n");
        sb.append("Generated by Iska Utils\n");
        
        return sb.toString();
    }
} 