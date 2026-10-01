---
navigation:
  title: Flight Nullifier
  icon: iska_utils:flight_nullifier
  parent: hubs/world_and_machines.md
  position: 23
item_ids:
  - iska_utils:flight_nullifier
categories:
  - World and machines
---
# Flight Nullifier

<ItemImage id="iska_utils:flight_nullifier" />

## What it does

Blocks **flight** inside a cubic **radius** around the block. When the target includes aerial entities, it also suppresses upward aerial movement — Ghasts, Phantoms, Blazes, and similar flyers are grounded in the zone. Same range and Range Module upgrades as the [Ender Nullifier](ender_nullifier.md).

Entities in the configured ignore list (default: `#c:bosses`) are exempt from this nullifier.

## GUI

Same layout as other nullifiers: **redstone**, **module slot**, range row, and area preview. Additionally:

- **Target** (icon button, right side): cycles through the affected group. Left-click advances, right-click goes back.
  - *Mobs only* (default): suppresses aerial flight in the zone.
  - *Players only*: blocks Survival/Adventure flight.
  - *Mobs and players*: affects both groups.

## Tips

- See also: [Climbing Nullifier](climbing_nullifier.md), [Ender Nullifier](ender_nullifier.md).
