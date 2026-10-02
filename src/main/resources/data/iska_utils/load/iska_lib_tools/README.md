# Library tools (Iskandert's Library)

Owned by **Iskandert's Library**. JSON is **startup-only**: creates item `iska_lib:<id>` and binds AOE.
New ids need a **game restart**. `/reload` only refreshes AOE for already-registered tools.

## Locations

- Any `data/<namespace>/load/…` with `"type": "iska_lib:tools"` (also scanned at mod construction from bootstrap datapack paths)
- Recommended: `load/iska_lib_tools/`
- Sidecar: `data/<namespace>/iska_lib/tools/<id>.json`

## Example

```json
{
  "type": "iska_lib:tools",
  "id": "super_pick",
  "behavior": "paxel",
  "range": 1,
  "durability": -1
}
```

| Field | Description |
|-------|-------------|
| `id` | Path-only or `iska_lib:<path>` (JSON cannot target other modids) |
| `behavior` | `lumberjack`, `excavator`, `scythe`, or `paxel` |
| `range` | AOE radius; default `1` |
| `durability` | Optional. Omit = vanilla. `-1` = unbreakable / infinite AOE. `>= 0` = item durability + flat AOE extras |

Java (any mod item, no new item): `ToolBehaviorLoader.register(itemId, behavior, range[, harvestTags[, durability]])`.

See wiki: [Tools-Declare](https://github.com/unfamily/iskandert_utilities/wiki/Tools-Declare)
