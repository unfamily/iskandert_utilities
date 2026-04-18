# IskaUtils Datapack Extensions

This guide explains how to create custom datapacks that extend IskaUtils with command items, potion plates, and structure monouse items.

## Directory Structure

All IskaUtils datapack files go under `data/<namespace>/load/`:

```
custom_datapack/
├── pack.mcmeta
└── data/
    └── mymod/
        └── load/
            ├── iska_utils_command_items/
            ├── iska_utils_plates/
            └── iska_utils_structures_monouse/
```

## Command Items

**Path**: `data/<namespace>/load/iska_utils_command_items/*.json`

### Structure
```json
{
  "type": "iska_utils:command_item",
  "overwritable": true,
  "items": [
    {
      "id": "my_command_item",
      "creative_tab": true,
      "stack_size": 1,
      "is_foil": false,
      "stages_logic": "AND",
      "stages": [
        {
          "stage_type": "player",
          "stage": "stage_name",
          "is": true
        }
      ],
      "cooldown": 0,
      "do": []
    }
  ]
}
```

### Fields
- **id**: Unique identifier for the command item
- **creative_tab**: Whether the item appears in creative mode (default: true)
- **stack_size**: Maximum stack size 1-64 (default: 1)
- **is_foil**: Enable enchantment glow effect (default: false)
- **stages_logic**: AND, OR, DEF_AND, DEF_OR (default: AND)
- **stages**: Array of stage requirements
- **cooldown**: Cooldown in ticks (default: 0)
- **do**: Array of actions to execute

---

## Potion Plates

**Path**: `data/<namespace>/load/iska_utils_plates/*.json`

### Effect Plate
```json
{
  "type": "iska_utils:plates",
  "overwritable": true,
  "plates": [
    {
      "plate_type": "effect",
      "id": "my_slowness_plate",
      "effect": "minecraft:slowness",
      "amplifier": 0,
      "duration": 200,
      "delay": 40,
      "affects_players": true,
      "affects_mobs": true,
      "hide_particles": false,
      "creative_tab": true,
      "tooltip_lines": 0
    }
  ]
}
```

### Damage Plate
```json
{
  "plate_type": "damage",
  "id": "my_damage_plate",
  "damage_type": "minecraft:magic",
  "damage": 2.0,
  "delay": 20,
  "affects_players": true,
  "affects_mobs": true
}
```

### Special Plate
```json
{
  "plate_type": "special",
  "id": "my_fire_plate",
  "apply": "fire",
  "duration": 100,
  "delay": 40,
  "affects_players": true,
  "affects_mobs": true
}
```

### Fields
- **plate_type**: effect, damage, or special
- **id**: Unique identifier
- **duration**: Effect/fire duration in ticks
- **delay**: Delay between applications in ticks
- **affects_players**: Whether to affect players (default: true)
- **affects_mobs**: Whether to affect mobs (default: true)
- **overwritable**: At file level, allows overwriting existing configurations

---

## Structure Monouse Items

**Path**: `data/<namespace>/load/iska_utils_structures_monouse/*.json`

### Structure
```json
{
  "type": "iska_utils:structure_monouse_item",
  "overwritable": true,
  "structure": [
    {
      "id": "my_structure",
      "place": "mymod:my_structure",
      "aggressive": false,
      "give": [
        {
          "item": "minecraft:diamond",
          "count": 5
        }
      ]
    }
  ]
}
```

### Fields
- **id**: Unique identifier (dashes will be converted to underscores)
- **place**: Structure resource location to place
- **aggressive**: Whether placement is aggressive (default: false)
- **give**: Array of items to give to player after placement
  - **item**: Item resource location
  - **count**: Number of items to give

### Notes on IDs
- Structure IDs like `"my_mod-structure_name"` are converted to `"my_mod_structure_name"` internally
- The item registry name matches the converted ID

---

## Overwritable Flag

All files support the `overwritable` flag at the root level:

```json
{
  "type": "iska_utils:plates",
  "overwritable": false,
  "plates": [...]
}
```

- **true** (default): Can be overwritten by other datapacks
- **false**: Prevents overwriting by other datapacks

When `overwritable: false` is set, subsequent datapacks cannot modify that configuration.

---

## Loading Order

Files are loaded in this order:
1. IskaUtils built-in files from `iska_utils` namespace
2. Custom datapack files from other namespaces (alphabetically)

This means built-in defaults load first, then custom datapacks can override them.

---

## Datapack Placement

In Minecraft, place your datapack in:
```
world/datapacks/
```

Then reload datapacks with `/reload` command.
