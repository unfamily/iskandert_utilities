---
navigation:
  title: Blazing Altar
  icon: iska_utils:blazing_altar
  parent: hubs/combat_and_travel.md
  position: 23
item_ids:
  - iska_utils:blazing_altar
  - iska_utils:range_module
categories:
  - Combat and travel
---
# Blazing Altar

<ItemImage id="iska_utils:blazing_altar" />

## Overview

Area controller for **Burning Flame** / **Cursed Burning Flame** placement and optional **natural spawn** blocking. Works only in **already loaded** chunks (no chunk loading).

## Placer slot

- Insert a **Burning Brazier** or **Cursed Candle** to auto-place matching flames in the configured chunk radius.
- Empty slot: no flame placement; spawn blocking (if enabled) still works.
- Brazier durability increases on each placement but **never** breaks (capped below max).

## GUI

- **Spawn filter**: Off / All / Hostile / Passive — affects only `NATURAL` mob spawns in the area.
- **Chunk radius** (Chebyshev): base max from config (default 4); the range bar shows **current / max**.
- **Range module** slot (left row, next to redstone): stack **range modules** for +3 chunk max radius each (up to 4 modules by default, config).
- **Ground only**: flames only on top of solid ground (default on). With ground off, flames may place in open air.
- **Light-sensitive blocks** (config `[general_utilities.blazing_altar]`): by default mushrooms, entropic soil, and dreadful dirt must keep **block light 0** on the block and the space above it — any simulated flame contribution is rejected (configurable via `005_light_sensitive_max_block_light`, default `0`).
- **Flame Vision**: global client toggle to see mod flame blocks (also toggled with left-click in air or on a brazier/candle).
- **Redstone**: default **ignored** (always active). Other modes match factory machines (no pulse).
- **Extinguish / break**: removing flames scans the area **progressively** (chunk columns per tick, config `008_extinguish_columns_per_tick`) so large radii do not freeze the server. Breaking the altar also schedules cleanup of flames in range.

## Visibility

Burning and cursed flame blocks are hidden on the client unless **Flame Vision** is enabled (GUI, or left-click in air / on brazier or candle).
