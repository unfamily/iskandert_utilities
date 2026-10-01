# Iska Utils — tool behaviors (datapack)

Assign AOE break behavior to **existing items** (no new items required).

## Locations

- `data/<namespace>/load/iska_utils_tools/*.json`
- `data/<namespace>/load/<file>.json` with `"type": "iska_utils:tools"`
- `data/<namespace>/iska_utils/tools/<id>.json` (one tool per file)

## Batch format

```json
{
  "type": "iska_utils:tools",
  "tools": [
    {
      "item": "iska_utils:entropic_paxel",
      "behavior": "paxel",
      "range": 1,
      "harvest_tags": ["minecraft:mineable/pickaxe"]
    }
  ]
}
```

## Single-tool file (`iska_utils/tools/` or one entry)

```json
{
  "item": "iska_utils:entropic_paxel",
  "behavior": "paxel",
  "range": 1
}
```

## Fields

| Field | Description |
|-------|-------------|
| `item` | Item id to attach behavior to (required) |
| `behavior` | `lumberjack`, `excavator`, `scythe`, or `paxel` (required) |
| `range` | AOE radius; default `1` |
| `harvest_tags` | Optional block tag ids; if omitted, sensible defaults per behavior |

## Behavior summary

- **lumberjack** — connected logs above the broken block
- **excavator** — cube around the broken block (pickaxe/shovel tags by default)
- **paxel** — cube (pickaxe/axe/shovel tags by default)
- **scythe** — horizontal disk (crops/flowers/hoe tags by default)

Reload datapacks or use `/reload` (IskaUtils load phase) to apply.
