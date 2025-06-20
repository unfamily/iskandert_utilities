package net.unfamily.iskautils.structure;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates comprehensive documentation for the Structure Scripting System
 */
public class StructureDocumentationGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Generates the complete README.md file for the structure scripting system
     */
    public static void generateDocumentation() {
        try {
            String configPath = net.unfamily.iskautils.Config.externalScriptsPath;
            if (configPath == null || configPath.trim().isEmpty()) {
                configPath = "kubejs/external_scripts";
            }
            
            Path structuresPath = Paths.get(configPath, "iska_utils_structures");
            
            // Create directory if it doesn't exist
            if (!Files.exists(structuresPath)) {
                Files.createDirectories(structuresPath);
            }
            
            Path readmePath = structuresPath.resolve("README.md");
            
            String readmeContent = generateFullDocumentation();
            Files.write(readmePath, readmeContent.getBytes());
            
            LOGGER.info("Generated Structure Scripting System documentation at: {}", readmePath);
            
        } catch (IOException e) {
            LOGGER.error("Failed to generate structure documentation: {}", e.getMessage());
        }
    }
    
    private static String generateFullDocumentation() {
        StringBuilder doc = new StringBuilder();
        
        doc.append(getDocumentationHeader());
        doc.append(getOverviewSection());
        doc.append(getFileStructureSection());
        doc.append(getStructureDefinitionsSection());
        doc.append(getPatternSystemSection());
        doc.append(getBlockDefinitionsSection());
        doc.append(getAdvancedFeaturesSection());
        doc.append(getTagsAndReplacementsSection());
        doc.append(getStageIntegrationSection());
        doc.append(getMonouseItemsSection());
        doc.append(getCompleteExamplesSection());
        doc.append(getBestPracticesSection());
        doc.append(getMachineIntegrationSection());
        doc.append(getErrorHandlingSection());
        doc.append(getFooter());
        
        return doc.toString();
    }
    
    private static String getDocumentationHeader() {
        return """
# Iska Utils - Structure Scripting System
## Complete Documentation & Examples

### Table of Contents
1. [Overview](#overview)
2. [File Structure](#file-structure)
3. [Structure Definitions](#structure-definitions)
4. [Pattern System](#pattern-system)
5. [Block Definitions](#block-definitions)
6. [Advanced Features](#advanced-features)
7. [Tags and Replacements](#tags-and-replacements)
8. [Stage Integration](#stage-integration)
9. [Structure Monouse Items](#structure-monouse-items)
10. [Complete Examples](#complete-examples)
11. [Best Practices](#best-practices)
12. [Machine Integration](#machine-integration)
13. [Error Handling](#error-handling)

---

""";
    }
    
    private static String getOverviewSection() {
        return """
## Overview

The Iska Utils Structure Scripting System is a powerful framework that allows you to define complex multi-block structures through JSON configuration files. These structures can be:

- **📏 Placed manually** using Structure Placer Items
- **⚡ Auto-placed** using the Structure Placer Machine (with energy consumption)
- **🎁 Given as rewards** through Structure Monouse Items  
- **🔒 Stage-gated** for progression systems
- **🔧 Highly customizable** with block alternatives, properties, and replacement rules

### Key Features
- **3D Pattern Definition**: Define structures using intuitive 3D arrays
- **Block Alternatives**: Multiple block options for each pattern position
- **Smart Replacement**: Tag-based and rule-based block replacement
- **Energy Integration**: Automatic energy consumption for machine placement
- **Stage System**: Progression-locked structures
- **Player Simulation**: Place blocks as if a player did it
- **Performance Options**: Slower placement for dramatic effect

---

""";
    }
    
    private static String getFileStructureSection() {
        return """
## File Structure

```
kubejs/external_scripts/iska_utils_structures/
├── README.md                    # This comprehensive documentation
├── default_structures.json     # Auto-generated example structures
├── default_monouse.json        # Auto-generated monouse item examples
├── my_custom_structures.json   # Your custom structure definitions
├── progression_structures.json # Stage-locked structures
└── themed_collections/         # Organized by theme
    ├── farms.json
    ├── redstone_machines.json
    └── decorative_buildings.json
```

### File Protection System
- **`"overwritable": true`** - Files will be regenerated on mod updates
- **`"overwritable": false`** - Files are protected from auto-regeneration
- **Always use `overwritable: false`** for your custom content

---

""";
    }
    
    private static String getStructureDefinitionsSection() {
        return """
## Structure Definitions

### Basic Structure File Template

```json
{
    "type": "iska_utils:structure",
    "overwritable": false,
    "structure": [
        {
            "id": "my_mod-my_structure",
            "name": "My Custom Structure",
            "description": "A detailed description of what this structure does",
            "icon": {
                "type": "minecraft:item",
                "item": "minecraft:diamond_block",
                "count": 1
            },
            "pattern": [
                [["AAA"], ["ABA"], ["AAA"]]
            ],
            "key": {
                "A": {
                    "display": "minecraft.stone",
                    "alternatives": [
                        {"block": "minecraft:stone"},
                        {"block": "minecraft:cobblestone"}
                    ]
                },
                "B": {
                    "display": "minecraft.chest",
                    "alternatives": [
                        {"block": "minecraft:chest"}
                    ]
                }
            }
        }
    ]
}
```

### Structure Properties Reference

#### ✅ Required Properties
- **`id`**: Unique identifier (format: `modid-structure_name`)
- **`name`**: Human-readable display name
- **`pattern`**: 3D structure layout definition
- **`key`**: Block definitions for pattern characters

#### ⚙️ Optional Properties
- **`description`**: Detailed tooltip description
- **`icon`**: Display icon in GUIs and tooltips
- **`can_force`**: Allow forced placement over existing blocks
- **`slower`**: Place blocks with 5-tick delays for dramatic effect
- **`place_like_player`**: Simulate player placement (triggers events)
- **`can_replace`**: List of blocks/tags that can be replaced
- **`stages`**: Required progression stages (ALL must be completed)

---

""";
    }
    
    private static String getPatternSystemSection() {
        return """
## Pattern System

### 🎯 Pattern Format Explanation
Patterns use a 3D array structure: **`[X][Y][Z]`**
- **X-axis**: West ← → East positions
- **Y-axis**: Bottom ↑ Top layers (height)  
- **Z-axis**: North ↕ South strings (each character = 1 block)

### Pattern Examples

#### Simple 3x3 Platform
```json
"pattern": [
    [["AAA"], ["AAA"], ["AAA"]]
]
```

#### Multi-Layer Tower (4 layers high)
```json
"pattern": [
    [["AAA"], ["ABA"], ["AAA"]],  // Layer 1 (ground)
    [["   "], [" C "], ["   "]],  // Layer 2 
    [["   "], [" D "], ["   "]],  // Layer 3
    [["   "], [" E "], ["   "]]   // Layer 4 (top)
]
```

#### Complex Structure with Center Marker
```json
"pattern": [
    [["AAAAA"], ["A   A"], ["A @ A"], ["A   A"], ["AAAAA"]],  // 5x5 foundation
    [["BBBBB"], ["B   B"], ["B   B"], ["B   B"], ["BBBBB"]],  // Walls
    [["CCCCC"], ["CCCCC"], ["CCCCC"], ["CCCCC"], ["CCCCC"]]   // Roof
]
```

### 🔤 Special Pattern Characters
- **`@`**: Structure center marker (placement/machine target point)
- **` `** (space): Air/empty space (no block placed)
- **Any other character**: Must be defined in the `key` section

---

""";
    }
    
    private static String getBlockDefinitionsSection() {
        return """
## Block Definitions & Alternatives

### 🧱 Simple Block Definition
```json
"A": {
    "display": "minecraft.stone",
    "alternatives": [
        {"block": "minecraft:stone"}
    ]
}
```

### 🔄 Multiple Alternatives (Flexible Materials)
```json
"W": {
    "display": "minecraft.planks",
    "alternatives": [
        {"block": "minecraft:oak_planks"},
        {"block": "minecraft:birch_planks"},
        {"block": "minecraft:spruce_planks"},
        {"block": "minecraft:jungle_planks"},
        {"block": "minecraft:acacia_planks"},
        {"block": "minecraft:dark_oak_planks"}
    ]
}
```

### ⚙️ Blocks with Properties (Precise Control)
```json
"S": {
    "display": "minecraft.stairs",
    "alternatives": [
        {
            "block": "minecraft:oak_stairs",
            "properties": {
                "facing": "north",
                "half": "bottom",
                "shape": "straight",
                "waterlogged": "false"
            }
        },
        {
            "block": "minecraft:stone_stairs",
            "properties": {
                "facing": "north",
                "half": "bottom"
            }
        }
    ]
}
```

### 🎨 Display Names
The `display` field controls how blocks appear in tooltips and GUIs:
- Use format: `modid.block_name` or `minecraft.block_name`
- First alternative is used if display name can't resolve
- Helps with mod compatibility and localization

---

""";
    }
    
    private static String getAdvancedFeaturesSection() {
        return """
## Advanced Features

### 💪 Force Placement
```json
{
    "id": "my_mod-force_structure",
    "name": "Bulldozer Structure",
    "can_force": true,
    "can_replace": [
        "minecraft:dirt",
        "minecraft:grass_block",
        "$replaceable",
        "#minecraft:flowers"
    ]
}
```

### 🎬 Slower Placement (Cinematic Effect)
```json
{
    "id": "my_mod-epic_structure", 
    "name": "Epic Monument",
    "slower": true,
    "place_like_player": true
}
```

### 👤 Player-Like Placement
When `place_like_player` is **true**:
- ✅ Blocks are placed as if a player did it
- ✅ Triggers block placement events  
- ✅ Respects mod interactions and restrictions
- ✅ Essential for blocks requiring special placement logic
- ⚠️ Slightly slower than direct placement

---

""";
    }
    
    private static String getTagsAndReplacementsSection() {
        return """
## Tags and Replacements

### 🏷️ Special Replacement Tokens
```json
"can_replace": [
    "$replaceable",     // Any replaceable block (flowers, grass, etc.)
    "$fluids",         // Any fluid blocks (water, lava, etc.)
    "$air",            // Air blocks only
    "$water",          // Water source/flowing blocks
    "$lava",           // Lava source/flowing blocks
    "$plants",         // All plant-type blocks
    "$dirt",           // Dirt-family blocks
    "$logs",           // All log blocks
    "$leaves",         // All leaf blocks  
    "$stone",          // Stone-family blocks
    "$ores",           // All ore blocks
    "$glass",          // Glass and glass panes
    "$wool"            // All wool blocks
]
```

### 🏷️ Block Tags (with # prefix)
```json
"can_replace": [
    "#minecraft:flowers",
    "#minecraft:small_flowers", 
    "#minecraft:tall_flowers",
    "#minecraft:saplings",
    "#minecraft:logs",
    "#minecraft:leaves",
    "#forge:ores",
    "#forge:storage_blocks",
    "#minecraft:dirt",
    "#minecraft:stone_variants"
]
```

### 🎯 Specific Blocks
```json
"can_replace": [
    "minecraft:dirt",
    "minecraft:grass_block", 
    "minecraft:stone",
    "modname:custom_block",
    "another_mod:special_block"
]
```

### 🧠 Smart Replacement Logic
The system processes replacements in this order:
1. **Specific blocks** (exact matches)
2. **Special tokens** (`$replaceable`, etc.)
3. **Block tags** (`#minecraft:flowers`, etc.)
4. If none match and `can_force` is false, placement fails

---

""";
    }
    
    private static String getStageIntegrationSection() {
        return """
## Stage Integration

### 🔒 Stage-Locked Structures
```json
{
    "id": "endgame_structure",
    "name": "Endgame Super Structure", 
    "description": "Only available after completing the main quest line",
    "stages": [
        "chapter_1_complete",
        "has_nether_star", 
        "endgame_unlocked",
        "master_builder"
    ],
    "pattern": [...],
    "key": {...}
}
```

### 📋 Stage System Rules
- **ALL** listed stages must be completed by the player
- Uses the integrated Iska Utils stage system
- Empty or missing `stages` array = no requirements
- Perfect for progression-based modpacks
- Stages are checked at placement time

### 🎮 Stage Integration Examples
```json
// Early game structure
"stages": ["basic_tools"]

// Mid game structure  
"stages": ["nether_access", "blaze_rods_obtained"]

// End game structure
"stages": ["dragon_defeated", "endgame_materials", "master_builder"]

// No requirements
"stages": []
```

---

""";
    }
    
    private static String getMonouseItemsSection() {
        return """
## Structure Monouse Items

### 🎁 Monouse Item Definition File
```json
{
    "type": "iska_utils:structure_monouse_item",
    "overwritable": false,
    "structure": [
        {
            "id": "iska_utils-wither_grinder",
            "place": "iska_utils-wither_grinder", 
            "give": [
                {
                    "item": "minecraft:wither_skeleton_skull",
                    "count": 3
                },
                {
                    "item": "minecraft:soul_sand",
                    "count": 4
                },
                {
                    "item": "iska_utils:wither_proof_block", 
                    "count": 26
                }
            ]
        }
    ]
}
```

### 🎁 Monouse Properties
- **`id`**: Structure ID to reference (must match existing structure)
- **`place`**: Which structure definition to actually place
- **`give`**: Array of items given to player on activation

### 🎮 How Monouse Items Work
1. Player right-clicks with monouse item
2. System gives all specified items to player
3. System places the specified structure at target location
4. Monouse item is consumed
5. Perfect for quest rewards or shop purchases

---

""";
    }
    
    private static String getCompleteExamplesSection() {
        return """
## Complete Examples

### 🏠 Example 1: Simple House
```json
{
    "type": "iska_utils:structure",
    "overwritable": false,
    "structure": [
        {
            "id": "example-simple_house",
            "name": "Simple Wooden House",
            "description": "A cozy starter house with basic amenities",
            "icon": {
                "type": "minecraft:item",
                "item": "minecraft:oak_planks"
            },
            "can_replace": ["$replaceable", "$air", "$plants"],
            "pattern": [
                [["WWWWW"], ["W   W"], ["W @ W"], ["W   W"], ["WWWWW"]],
                [["WWWWW"], ["W   W"], ["W   W"], ["W   W"], ["WWWWW"]],
                [["WWWWW"], ["W   W"], ["W   W"], ["W   W"], ["WWWWW"]],
                [["RRRRR"], ["RRRRR"], ["RRRRR"], ["RRRRR"], ["RRRRR"]]
            ],
            "key": {
                "W": {
                    "display": "minecraft.oak_planks",
                    "alternatives": [
                        {"block": "minecraft:oak_planks"},
                        {"block": "minecraft:birch_planks"},
                        {"block": "minecraft:spruce_planks"}
                    ]
                },
                "R": {
                    "display": "minecraft.oak_slab",
                    "alternatives": [
                        {
                            "block": "minecraft:oak_slab",
                            "properties": {
                                "type": "bottom"
                            }
                        }
                    ]
                }
            }
        }
    ]
}
```

### ⚡ Example 2: Redstone Machine
```json
{
    "type": "iska_utils:structure", 
    "overwritable": false,
    "structure": [
        {
            "id": "example-redstone_machine",
            "name": "Automated Redstone Machine",
            "description": "Complex contraption with pistons and dispensers",
            "icon": {
                "type": "minecraft:item",
                "item": "minecraft:redstone"
            },
            "can_force": true,
            "slower": true,
            "place_like_player": true,
            "can_replace": ["$replaceable", "$air", "$dirt"],
            "stages": ["redstone_engineer"],
            "pattern": [
                [["SSSSS"], ["S   S"], ["S@RRS"], ["S   S"], ["SSSSS"]],
                [["     "], ["  P  "], ["  D  "], ["  H  "], ["     "]]
            ],
            "key": {
                "S": {
                    "display": "minecraft.stone",
                    "alternatives": [
                        {"block": "minecraft:stone"},
                        {"block": "minecraft:cobblestone"}
                    ]
                },
                "R": {
                    "display": "minecraft.redstone_wire",
                    "alternatives": [
                        {"block": "minecraft:redstone_wire"}
                    ]
                },
                "P": {
                    "display": "minecraft.piston",
                    "alternatives": [
                        {
                            "block": "minecraft:piston",
                            "properties": {"facing": "up"}
                        }
                    ]
                },
                "D": {
                    "display": "minecraft.dispenser",
                    "alternatives": [
                        {
                            "block": "minecraft:dispenser",
                            "properties": {"facing": "up"}
                        }
                    ]
                },
                "H": {
                    "display": "minecraft.hopper",
                    "alternatives": [
                        {
                            "block": "minecraft:hopper",
                            "properties": {"facing": "down"}
                        }
                    ]
                }
            }
        }
    ]
}
```

---

""";
    }
    
    private static String getBestPracticesSection() {
        return """
## Best Practices

### 🎨 1. Structure Design
- **Always include `@` center marker** for precise placement targeting
- **Keep patterns readable** with consistent spacing and logical character assignments
- **Use meaningful characters** (W=Wood, S=Stone, R=Redstone, etc.)
- **Comment complex patterns** in the description field
- **Test in creative mode** before finalizing

### 🔄 2. Block Alternatives  
- **Provide multiple alternatives** for material flexibility
- **Order by preference** (first alternative = most preferred)
- **Include modded alternatives** when targeting modpack environments
- **Group similar blocks** logically (all wood types together)

### ⚡ 3. Performance Considerations
- **Use `slower: true`** for very large structures to prevent lag spikes
- **Be selective with `can_replace`** to avoid unintended destruction
- **Test replacement rules** thoroughly in survival mode
- **Consider chunk loading** for massive structures

### 🔒 4. Stage Integration
- **Use descriptive stage names** that clearly indicate requirements
- **Group related stages** logically (all chapter stages together)
- **Document stage dependencies** in structure descriptions  
- **Test stage checking** with actual progression

### 📁 5. File Organization
- **Group thematically related structures** in the same file
- **Use descriptive file names** (`farms.json`, `redstone_machines.json`)
- **Always set `overwritable: false`** for custom content  
- **Comment file purposes** at the top

---

""";
    }
    
    private static String getMachineIntegrationSection() {
        return """
## Machine Integration

### ⚙️ Structure Placer Machine
The Structure Placer Machine provides automated structure placement with advanced features:

#### Features
- **🔋 Energy Integration**: Consumes configurable RF/FE per block placed
- **📦 Inventory Management**: Automatically sources materials from connected inventories
- **🔄 Smart Material Selection**: Uses block alternatives when preferred materials unavailable
- **⏸️ Graceful Stopping**: Pauses when out of energy or materials
- **🔒 Stage Checking**: Respects progression requirements
- **⚡ Redstone Control**: Multiple redstone modes for automation

#### Energy System
- **Default Consumption**: 50 RF/FE per block placed
- **Configurable**: Adjustable in mod configuration files
- **Smart Consumption**: Energy only consumed when blocks successfully placed
- **No Waste**: Items not consumed if insufficient energy

#### Redstone Modes
- **Ignore**: Always active (default)
- **Low Signal**: Active when redstone signal low/off
- **High Signal**: Active when redstone signal high/on  
- **Once**: Single activation per redstone pulse

---

""";
    }
    
    private static String getErrorHandlingSection() {
        return """
## Error Handling

### 🚨 Common Issues & Solutions

#### 1. **Structure Not Loading**
- **Cause**: JSON syntax errors
- **Solution**: Validate JSON syntax, check brackets and commas
- **Debug**: Check game logs for parsing errors

#### 2. **Blocks Not Placing**
- **Cause**: Invalid block IDs or missing mods
- **Solution**: Verify all block IDs exist and mods are loaded
- **Debug**: Test in creative mode with `/give` commands

#### 3. **Properties Ignored**
- **Cause**: Incorrect property names or values
- **Solution**: Check Minecraft wiki for valid properties
- **Debug**: Use F3 debug screen to inspect block properties

#### 4. **Stages Not Working**  
- **Cause**: Stage system not configured or incorrect stage names
- **Solution**: Verify stage mod integration and stage names
- **Debug**: Test with stage commands

### 🛠️ Debug Tips
- **Enable Debug Logging**: Check mod configuration for debug options
- **Use Creative Mode**: Test structures safely without resource loss
- **Check Game Logs**: Look for structure loading and parsing messages
- **Test Incrementally**: Start with simple patterns and add complexity
- **Use `/reload`** command to refresh structures without game restart

---

""";
    }
    
    private static String getFooter() {
        return """
## Version Compatibility

### 📋 System Requirements
- **Minecraft**: 1.20.1+
- **NeoForge**: Latest stable version
- **Iska Utils**: 1.0.0+

### 🔗 Mod Compatibility
- **✅ KubeJS**: Enhanced scripting capabilities
- **✅ JEI/REI**: Recipe and structure viewing
- **✅ Energy Mods**: RF/FE energy integration
- **✅ Storage Mods**: Inventory automation

---

*🤖 Generated automatically by Iska Utils Structure Scripting System*
*📅 Last updated: "'" + java.time.LocalDateTime.now().toString() + "'"*

### JSON Schema Reference

#### Structure Definition Schema
```json
{
    "type": "iska_utils:structure",
    "overwritable": boolean,
    "structure": [
        {
            "id": "string (required)",
            "name": "string (required)", 
            "description": "string (optional)",
            "icon": {
                "type": "minecraft:item",
                "item": "string",
                "count": number
            },
            "can_force": boolean,
            "slower": boolean,
            "place_like_player": boolean,
            "can_replace": ["string"],
            "stages": ["string"],
            "pattern": [[[["string"]]]],
            "key": {
                "char": {
                    "display": "string",
                    "alternatives": [{
                        "block": "string",
                        "properties": {"key": "value"}
                    }]
                }
            }
        }
    ]
}
```

#### Monouse Item Schema
```json
{
    "type": "iska_utils:structure_monouse_item",
    "overwritable": boolean,
    "structure": [
        {
            "id": "string (required)",
            "place": "string (required)",
            "give": [
                {
                    "item": "string (required)",
                    "count": number (required)
                }
            ]
        }
    ]
}
```
""";
    }
} 