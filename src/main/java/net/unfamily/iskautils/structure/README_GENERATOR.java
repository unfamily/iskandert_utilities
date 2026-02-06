package net.unfamily.iskautils.structure;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates comprehensive README documentation for the Structure Scripting System
 */
public class README_GENERATOR {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Generates the complete README.md file for the structure scripting system
     */
    public static void generateReadme() {
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
            
            String readmeContent = generateReadmeContent();
            Files.write(readmePath, readmeContent.getBytes());
            
            LOGGER.info("Generated comprehensive README.md at: {}", readmePath);
            
        } catch (IOException e) {
            LOGGER.error("Failed to generate README.md: {}", e.getMessage());
        }
    }
    
    private static String generateReadmeContent() {
        return """
# Iska Utils - Structure Scripting System
## Complete Documentation & Examples

### Table of Contents
1. [Overview](#overview)
2. [File Structure](#file-structure)
3. [Structure Definitions](#structure-definitions)
4. [Structure Monouse Items](#structure-monouse-items)
5. [Advanced Features](#advanced-features)
6. [Block Properties](#block-properties)
7. [Tags and Replacements](#tags-and-replacements)
8. [Stage Integration](#stage-integration)
9. [Complete Examples](#complete-examples)
10. [Best Practices](#best-practices)

---

## Overview

The Iska Utils Structure Scripting System allows you to define complex structures that can be:
- **Placed manually** using Structure Placer Items
- **Auto-placed** using the Structure Placer Machine (with energy consumption)
- **Given as rewards** through Structure Monouse Items
- **Stage-gated** for progression systems
- **Highly customizable** with block alternatives, properties, and replacement rules

---

## File Structure

```
kubejs/external_scripts/iska_utils_structures/
├── README.md                    # This documentation
├── default_structures.json     # Auto-generated examples
├── default_monouse.json        # Auto-generated monouse examples
├── my_custom_structures.json   # Your custom structures
└── progression_structures.json # Stage-locked structures
```

### File Protection System
- Files with `"overwritable": true` will be regenerated on updates
- Files with `"overwritable": false` are protected from auto-regeneration
- Use protected files for your custom content

---

## Structure Definitions

### Basic Structure File Format

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

### Structure Properties

#### Required Properties
- `id`: Unique identifier (format: `modid-structure_name`)
- `name`: Display name for the structure
- `pattern`: 3D pattern definition
- `key`: Block definitions for pattern characters

#### Optional Properties
- `description`: Detailed description
- `icon`: Display icon in GUIs
- `can_force`: Allow forced placement (default: false)
- `slower`: Place blocks with 5-tick delays (default: false)
- `place_like_player`: Simulate player placement (default: false)
- `can_replace`: List of blocks that can be replaced
- `stages`: Required progression stages

---

## Pattern System

### Pattern Format
Patterns use a 3D array structure: `[X][Y][Z]`
- **X**: West-East positions
- **Y**: Bottom-Top layers (height)
- **Z**: North-South strings (each character = 1 block)

### Pattern Examples

#### Simple 3x3 Platform
```json
"pattern": [
    [["AAA"], ["AAA"], ["AAA"]]
]
```

#### Multi-Layer Tower
```json
"pattern": [
    [["AAA"], ["ABA"], ["AAA"]],
    [["   "], [" C "], ["   "]],
    [["   "], [" D "], ["   "]]
]
```

#### Complex Structure with Center Marker
```json
"pattern": [
    [["AAAAA"], ["A   A"], ["A @ A"], ["A   A"], ["AAAAA"]],
    [["BBBBB"], ["B   B"], ["B   B"], ["B   B"], ["BBBBB"]],
    [["CCCCC"], ["CCCCC"], ["CCCCC"], ["CCCCC"], ["CCCCC"]]
]
```

### Special Characters
- `@`: Structure center (where player places/machine targets)
- ` ` (space): Air/empty space
- Any other character: Defined in the key section

---

## Block Definitions & Alternatives

### Simple Block Definition
```json
"A": {
    "display": "minecraft.stone",
    "alternatives": [
        {"block": "minecraft:stone"}
    ]
}
```

### Multiple Alternatives
```json
"W": {
    "display": "minecraft.planks",
    "alternatives": [
        {"block": "minecraft:oak_planks"},
        {"block": "minecraft:birch_planks"},
        {"block": "minecraft:spruce_planks"},
        {"block": "minecraft:jungle_planks"}
    ]
}
```

### Blocks with Properties
```json
"S": {
    "display": "minecraft.stairs",
    "alternatives": [
        {
            "block": "minecraft:oak_stairs",
            "properties": {
                "facing": "north",
                "half": "bottom",
                "shape": "straight"
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

---

## Advanced Features

### Force Placement
```json
{
    "id": "my_mod-force_structure",
    "can_force": true,
    "can_replace": [
        "minecraft:dirt",
        "minecraft:grass_block",
        "$replaceable",
        "#minecraft:flowers"
    ]
}
```

### Slower Placement (for dramatic effect)
```json
{
    "id": "my_mod-slow_structure",
    "slower": true,
    "place_like_player": true
}
```

### Player-Like Placement
When `place_like_player` is true:
- Blocks are placed as if a player did it
- Triggers block placement events
- Respects mod interactions
- Useful for blocks that need special placement logic

---

## Tags and Replacements

### Replacement Types

#### Special Replacement Tokens
```json
"can_replace": [
    "$replaceable",     // Any replaceable block (flowers, grass, etc.)
    "$fluids",         // Any fluid blocks
    "$air",            // Air blocks only
    "$water",          // Water blocks
    "$lava",           // Lava blocks
    "$plants",         // All plant blocks
    "$dirt",           // Dirt-type blocks
    "$logs",           // Log blocks
    "$leaves",         // Leaf blocks
    "$stone",          // Stone-type blocks
    "$ores"            // Ore blocks
]
```

#### Block Tags (with # prefix)
```json
"can_replace": [
    "#minecraft:flowers",
    "#minecraft:small_flowers",
    "#minecraft:tall_flowers",
    "#minecraft:saplings",
    "#minecraft:logs",
    "#minecraft:leaves",
    "#forge:ores",
    "#forge:storage_blocks"
]
```

#### Specific Blocks
```json
"can_replace": [
    "minecraft:dirt",
    "minecraft:grass_block",
    "minecraft:stone",
    "modname:custom_block"
]
```

---

## Stage Integration

### Stage-Locked Structures
```json
{
    "id": "endgame_structure",
    "name": "Endgame Super Structure",
    "stages": [
        "chapter_1_complete",
        "has_nether_star",
        "endgame_unlocked"
    ],
    "pattern": [...],
    "key": {...}
}
```

### Stage Checking
- **ALL** listed stages must be completed
- Uses the Iska Utils stage system
- Empty/missing stages array = no requirements
- Perfect for progression-based building

---

## Structure Monouse Items

### Monouse Item Definition File
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

### Monouse Properties
- `id`: Structure ID to reference (with dashes)
- `place`: Which structure to actually place
- `give`: Items given to player on use

---

## Complete Examples

### Example 1: Simple House
```json
{
    "type": "iska_utils:structure",
    "overwritable": false,
    "structure": [
        {
            "id": "example-simple_house",
            "name": "Simple House",
            "description": "A basic wooden house structure",
            "icon": {
                "type": "minecraft:item",
                "item": "minecraft:oak_planks"
            },
            "can_replace": ["$replaceable", "$air"],
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
                        {"block": "minecraft:birch_planks"}
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

### Example 2: Advanced Redstone Contraption
```json
{
    "type": "iska_utils:structure",
    "overwritable": false,
    "structure": [
        {
            "id": "example-redstone_machine",
            "name": "Redstone Machine",
            "description": "Complex redstone contraption with multiple components",
            "icon": {
                "type": "minecraft:item",
                "item": "minecraft:redstone"
            },
            "can_force": true,
            "slower": true,
            "place_like_player": true,
            "can_replace": ["$replaceable", "$air", "minecraft:dirt", "minecraft:grass_block"],
            "stages": ["redstone_engineer", "advanced_builder"],
            "pattern": [
                [["SSSSS"], ["S   S"], ["S@RRS"], ["S   S"], ["SSSSS"]],
                [["     "], ["  P  "], ["  D  "], ["  H  "], ["     "]],
                [["     "], ["  L  "], ["     "], ["     "], ["     "]]
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
                            "properties": {
                                "facing": "up"
                            }
                        }
                    ]
                },
                "D": {
                    "display": "minecraft.dispenser",
                    "alternatives": [
                        {
                            "block": "minecraft:dispenser",
                            "properties": {
                                "facing": "up"
                            }
                        }
                    ]
                },
                "H": {
                    "display": "minecraft.hopper",
                    "alternatives": [
                        {
                            "block": "minecraft:hopper",
                            "properties": {
                                "facing": "down"
                            }
                        }
                    ]
                },
                "L": {
                    "display": "minecraft.lever",
                    "alternatives": [
                        {
                            "block": "minecraft:lever",
                            "properties": {
                                "face": "floor",
                                "facing": "north",
                                "powered": "false"
                            }
                        }
                    ]
                }
            }
        }
    ]
}
```

### Example 3: Farm Structure with Alternatives
```json
{
    "type": "iska_utils:structure",
    "overwritable": false,
    "structure": [
        {
            "id": "example-auto_farm",
            "name": "Automated Farm",
            "description": "Self-sustaining crop farm with water management",
            "icon": {
                "type": "minecraft:item",
                "item": "minecraft:wheat_seeds"
            },
            "can_replace": ["$replaceable", "#minecraft:flowers", "minecraft:grass_block"],
            "pattern": [
                [["FFFFFFFFF"], ["FDDDDDDD"], ["FWDDDDDDF"], ["FDDDDDDD"], ["FFFFFFFFF"]],
                [["         "], ["         "], ["    @    "], ["         "], ["         "]]
            ],
            "key": {
                "F": {
                    "display": "minecraft.farmland",
                    "alternatives": [
                        {"block": "minecraft:farmland"}
                    ]
                },
                "D": {
                    "display": "minecraft.dirt",
                    "alternatives": [
                        {"block": "minecraft:dirt"},
                        {"block": "minecraft:grass_block"}
                    ]
                },
                "W": {
                    "display": "minecraft.water",
                    "alternatives": [
                        {"block": "minecraft:water"}
                    ]
                }
            }
        }
    ]
}
```

---

## Best Practices

### 1. Structure Design
- Always include an `@` center marker for precise placement
- Keep patterns readable with consistent spacing
- Use meaningful character assignments (W=Wood, S=Stone, etc.)

### 2. Block Alternatives
- Provide multiple alternatives for flexibility
- Order alternatives by preference (first = preferred)
- Include modded blocks as alternatives when possible

### 3. Performance Considerations
- Use `slower: true` for large structures to prevent lag
- Consider `can_replace` carefully to avoid unwanted destruction
- Test structures in creative mode first

### 4. Stage Integration
- Use stages for progression-locked content
- Make stage names descriptive and consistent
- Document stage requirements clearly

### 5. File Organization
- Group related structures in themed files
- Use descriptive file names
- Set `overwritable: false` for custom content
- Comment complex patterns in descriptions

### 6. Testing
- Test all alternatives work correctly
- Verify stage requirements function
- Check rotation and placement behavior
- Test with both manual and machine placement

---

## Machine Integration

### Structure Placer Machine
- Automatically places structures using materials from inventory
- Consumes energy per block placed (configurable)
- Supports all structure features (stages, alternatives, etc.)
- Stops when out of energy or materials
- Respects replacement rules and block alternatives

### Energy Consumption
- Default: 50 RF/FE per block placed
- Configurable in mod settings
- Machine stops if insufficient energy
- Items not consumed if energy unavailable

---

## Error Handling

### Common Issues
1. **Structure not loading**: Check JSON syntax
2. **Blocks not placing**: Verify block IDs exist
3. **Properties ignored**: Check property names match block properties
4. **Stages not working**: Ensure stage system is properly configured

### Debug Tips
- Check game logs for parsing errors
- Verify mod compatibility for block alternatives
- Test patterns in creative mode
- Use `/iska_utils_debug reload` (quick) or `/reload` (full) to refresh structures without restart

---

## Version Compatibility

This documentation is for Iska Utils version 1.0+
- Requires NeoForge
- Compatible with KubeJS for advanced scripting
- Stage system requires proper stage mod integration

---

*Generated automatically by Iska Utils Structure Scripting System*
*For updates and support, visit: [Your Mod Repository]*
""";
    }
} 