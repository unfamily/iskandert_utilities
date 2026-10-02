# Tool AOE behaviors (Iskandert's Library)

Owned by **Iskandert's Library**. Bind AOE to an **existing** item via `"id"`.

## Locations

- Any `data/<namespace>/load/…` with `"type": "iska_lib:tools"`
- Recommended: `load/iska_lib_tools/`
- Sidecar: `data/<namespace>/iska_lib/tools/<id>.json`

## Example

```json
{
  "type": "iska_lib:tools",
  "id": "minecraft:diamond_pickaxe",
  "behavior": "paxel",
  "range": 1,
  "durability": -1
}
```

| Field | Description |
|-------|-------------|
| `id` | Item id (`mod:item`, or path-only → `iska_lib:<path>`) |
| `behavior` | `lumberjack`, `excavator`, `scythe`, or `paxel` |
| `range` | AOE radius; default `1` |
| `durability` | Optional. Omit = vanilla. `-1` = infinite AOE extras. `>= 0` = flat once for extras |

Java: `ToolBehaviorLoader.register(itemId, behavior, range[, harvestTags[, durability]])`.

See wiki: [Tools-Declare](https://github.com/unfamily/iskandert_utilities/wiki/Tools-Declare)
