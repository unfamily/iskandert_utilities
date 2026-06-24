---
navigation:
  title: Ender Nullifier
  icon: iska_utils:ender_nullifier
  parent: hubs/world_and_machines.md
  position: 22
item_ids:
  - iska_utils:ender_nullifier
categories:
  - World and machines
---
# Ender Nullifier

<ItemImage id="iska_utils:ender_nullifier" />

## What it does

Directional block that **cancels mob teleports** (Endermen, Shulkers, chorus-fruit hops, etc.) inside a configurable radius around the front face. Players are not affected.

## Controls

- **Right-click**: toggle enabled / disabled (action bar feedback).
- **Shift + right-click**: cycle redstone mode — manual, low, high, pulse (same pattern as other Iska machines).
- **Comparator** reads powered state when redstone modes apply.

## Tips

- Place facing the area you want to protect; range defaults to **8** blocks (see config `enderNullifierRadius`).
- Useful near mob farms or end-themed builds where teleporting mobs break containment.
