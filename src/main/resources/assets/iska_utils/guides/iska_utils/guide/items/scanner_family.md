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
  - Tools and scanner
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

- Pre-set to scan **all spawner blocks** (monster, trial, and any block ids listed in `scanner_spawner_entries` config).
- **Shift + use** to cycle: all spawners, monster spawners only, or trial spawners only.
- Marker colors are configurable per block id (`106_scanner_spawner_entries`, default `012_scannerDefaultSpawnerColor`).
- Transfer to the scanner like other specialized chips.

## Scanner Chip (Loot)

- Pre-set to find **storage containers with loot** (unopened or with contents).
- **Shift + use** to cycle loot modes:
  1. Containers with loot (default)
  2. Empty containers
  3. Opened containers that still hold loot for you (when supported by compatible mods, e.g. Lootr)
- Detects chests, barrels, shulkers, decorated pots, modded containers (via `104_scanner_loot_tags`), and **Lootr** blocks.
- Loot **entities** (e.g. Lootr item frames) use billboard markers; colors in `107_scanner_loot_entity_entries`.
- Block marker colors: Lootr vs vanilla/mod (`103_scanner_loot_entries`, defaults `009_scannerDefaultLootColor` / `010_scannerDefaultLootrColor`).

## Scanner Chip (Liquid)

- Pre-set to scan **all fluids** in range.
- **Shift + use on a fluid block** (or the face of a block adjacent to fluid) to filter that fluid only. Flowing and source fluids are normalized (e.g. `minecraft:water`).
- **Shift + use on any other block** to reset the filter to all fluids.
- Marker colors are configurable per fluid id (`105_scanner_fluid_entries`, default `011_scannerDefaultLiquidColor`).
- Transfer to the scanner like other specialized chips.
