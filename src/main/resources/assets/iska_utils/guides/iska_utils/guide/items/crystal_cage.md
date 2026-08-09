---
navigation:
  title: Crystal Cage
  icon: iska_utils:crystal_cage
  parent: hubs/tools_and_scanner.md
  position: 60
item_ids:
  - iska_utils:crystal_cage
categories:
  - Useful tools
---
# Crystal Cage

<ItemImage id="iska_utils:crystal_cage" />

A reusable mob container.

## Capturing a mob

Right-click any non-player living entity while holding an **empty** Crystal Cage.
The mob is stored inside the item and the cage texture changes to show it is filled.
The tooltip will display the captured mob's name.

## Releasing a mob

**Right-click on a block face** — the mob spawns on the adjacent block. Clicking in the air does nothing (the cage stays filled).

The cage reverts to its empty state once the mob is released.

## Spawners

A **filled** Crystal Cage used on a <ItemImage id="minecraft:spawner" /> **Spawner**, <ItemImage id="minecraft:trial_spawner" /> **Trial Spawner**, or <ItemImage id="iska_utils:entropic_spawner" /> **Entropic Spawner** works like a spawn egg: it sets the spawn type to the captured mob and empties the cage (creative mode keeps it filled).


## Crystal Cage Trap Plate

An empty Crystal Cage can also be used to convert a placed <ItemImage id="iska_utils:plate_base_block" /> **Plate Base** into a <ItemImage id="iska_utils:crystal_cage_trap_plate" /> **Crystal Cage Trap Plate** by right-clicking it. The first living mob that walks on the plate is captured into a Crystal Cage that drops there, and the plate reverts to a Plate Base.
