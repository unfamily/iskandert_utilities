---
navigation:
  title: Scanner and chips
  icon: iska_utils:scanner
  parent: hubs/tools_and_scanner.md
  position: 15
item_ids:
  - iska_utils:scanner
  - iska_utils:scanner_chip
  - iska_utils:scanner_chip_ores
  - iska_utils:scanner_chip_mobs
  - iska_utils:scanner_chip_spawners
  - iska_utils:scanner_chip_loot
  - iska_utils:scanner_chip_liquid
categories:
  - Useful tools
---
# Scanner and chips

<ItemImage id="iska_utils:scanner" />

## Scanner

- Highlights matching blocks or mobs for a limited time; supports Scanner Chips for storing and transferring targets.

<ItemGrid>
  <ItemIcon id="iska_utils:scanner" />
  <ItemIcon id="iska_utils:scanner_chip" />
  <ItemIcon id="iska_utils:scanner_chip_ores" />
  <ItemIcon id="iska_utils:scanner_chip_mobs" />
  <ItemIcon id="iska_utils:scanner_chip_spawners" />
  <ItemIcon id="iska_utils:scanner_chip_loot" />
  <ItemIcon id="iska_utils:scanner_chip_liquid" />
</ItemGrid>

## Scanner Chip (blank)

- **Shift + use on a block** to store a block target in the chip.
- To transfer the chip target into the **Scanner**, hold the scanner in your **main hand** and **use the chip**.

## Scanner Chip (Ores)

- Pre-set to scan **ores**.
- **Shift + use** to cycle the mining-level filter (shown in chat / tooltip).
- Hold the scanner in your **main hand** and **use the chip** to transfer the ore-scan target into the scanner.

## Scanner Chip (Mobs)

- Pre-set to scan **all mobs**.
- **Shift + use** to cycle mob categories.
- Hold the scanner in your **main hand** and **use the chip** to transfer the mob-scan target into the scanner.

## Scanner Chip (Spawners)

- Pre-set to scan **spawner blocks** (monster, trial, and related spawners).
- **Shift + use** to cycle: all spawners, monster spawners only, or trial spawners only.
- Transfer to the scanner like other specialized chips.

## Scanner Chip (Loot)

- Pre-set to find **storage containers with loot** (unopened or with contents).
- **Shift + use** to cycle loot modes:
  1. Containers with loot (default)
  2. Empty containers
  3. Opened containers that still hold loot for you (when supported by compatible mods, e.g. Lootr)
- Detects chests, barrels, shulkers, decorated pots, modded containers, and **Lootr** blocks.
- Loot **entities** (e.g. Lootr item frames) use billboard markers.
- Block marker colors differ for Lootr vs vanilla/mod containers.

## Scanner Chip (Liquid)

- Pre-set to scan **all fluids** in range.
- **Shift + use on a fluid block** (or the face of a block adjacent to fluid) to filter that fluid only. Flowing and source fluids are normalized (e.g. `minecraft:water`).
- **Shift + use on any other block** to reset the filter to all fluids.
- Transfer to the scanner like other specialized chips.
