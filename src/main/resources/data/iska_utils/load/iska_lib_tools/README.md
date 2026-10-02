# Tool AOE behaviors (Iskandert's Library)

Owned by **Iskandert's Library** (`1.12.0.0.0+`). JSON always registers as `iska_lib:<id>`.

## Locations

- Any `data/<namespace>/load/…` with `"type": "iska_lib:tools"`
- Recommended: `load/iska_lib_tools/`
- Sidecar one-file: `data/<namespace>/iska_lib/tools/<id>.json`

## Batch format

```json
{
  "type": "iska_lib:tools",
  "tools": [
    {
      "id": "super_pick",
      "behavior": "paxel",
      "range": 1,
      "durability": -1,
      "harvest_tags": ["minecraft:mineable/pickaxe"]
    }
  ]
}
```

## Single-tool file

```json
{
  "type": "iska_lib:tools",
  "id": "super_pick",
  "behavior": "paxel",
  "range": 1,
  "durability": 1
}
```

## Fields

| Field | Description |
|-------|-------------|
| `type` | `iska_lib:tools` (required) |
| `id` | Path id → `iska_lib:<id>` (required) |
| `behavior` | `lumberjack`, `excavator`, `scythe`, or `paxel` |
| `range` | AOE radius; default `1` |
| `durability` | Optional. Omit = vanilla. `-1` = infinite AOE extras. `>= 0` = flat damage once for extras |
| `harvest_tags` | Optional block tag ids |

## Java API

`ToolBehaviorLoader.register(itemId, behavior, range[, harvestTags[, durability]])` — any item id; survives datapack reload and overrides JSON.

See wiki: [Tools-Declare](https://github.com/unfamily/iskandert_utilities/wiki/Tools-Declare)
