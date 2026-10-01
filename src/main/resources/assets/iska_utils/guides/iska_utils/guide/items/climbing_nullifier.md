---
navigation:
  title: Climbing Nullifier
  icon: iska_utils:climbing_nullifier
  parent: hubs/world_and_machines.md
  position: 24
item_ids:
  - iska_utils:climbing_nullifier
categories:
  - World and machines
---
# Climbing Nullifier

<ItemImage id="iska_utils:climbing_nullifier" />

## What it does

Stops **climbing** on ladders, scaffolding, and other climbable blocks inside a cubic **radius**. Also blocks **Gauntlet of Climbing** wall-climb while inside the zone. Same range and modules as the [Ender Nullifier](ender_nullifier.md).

Entities in the configured ignore list (default: `#c:bosses`) are exempt from this nullifier.

## GUI

- **Target** (icon button, right side): cycles through the affected group. Left-click advances, right-click goes back.
  - *Mobs only* (default): stops non-player entities from climbing.
  - *Players only*: stops players from climbing (including Gauntlet).
  - *Mobs and players*: affects both groups.
- **Redstone**, **module slot**, range, and area preview — same as other nullifiers.

## Tips

- Useful to keep entities from scaling farms or walls.
- See also: [Flight Nullifier](flight_nullifier.md), [Ender Nullifier](ender_nullifier.md).
