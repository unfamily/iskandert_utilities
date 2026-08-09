---
navigation:
  title: Blazing Altar
  icon: iska_utils:blazing_altar
  parent: hubs/combat_and_travel.md
  position: 23
item_ids:
  - iska_utils:blazing_altar
categories:
  - Combat and travel
---
# Blazing Altar

<ItemImage id="iska_utils:blazing_altar" />

## Overview

Area controller for **Burning Flame** / **Cursed Burning Flame** placement and optional **natural spawn** blocking. Works only in **already loaded** chunks (no chunk loading).

## What it does

- If you do **not** insert a **Burning Brazier** or an **Arcane Candle**, the altar **prevents natural mob spawning** in its area (according to the spawn filter and redstone).
- Insert a **Burning Brazier** or **Arcane Candle** to also auto-place matching flames (**Burning Flame** / **Cursed Burning Flame**) in the chunk radius.
- With a Brazier, remaining durability **decreases** on each placement but the item **never** breaks (capped so it cannot fully deplete).

## GUI

- **Spawn filter**: Off / All / Hostile / Passive — affects only `NATURAL` mob spawns in the area.
- **Chunk radius** (Chebyshev): area size for flames and spawn filter.
- **Ground only**: flames only on top of solid ground (default on). With ground off, flames may place in open air.
- **Light-sensitive blocks**: mushrooms, entropic soil, and dreadful dirt must stay in darkness on the block and the space above it — flame placement that would light them is rejected.
- **Flame Vision**: global client toggle to see mod flame blocks (also toggled with left-click in air or on a brazier/candle).
- **Show**: corner pillars marking the extreme chunks of the coverage area.
- **Redstone**: default **ignored** (always active). Other modes match factory machines (no pulse).
- **Extinguish / break**: removing flames scans the area progressively so large radii do not freeze the server. Breaking the altar also schedules cleanup of flames in range. Extinguish can restore Brazier durability when flames are removed.

## Visibility

**Burning Flame** and **Cursed Burning Flame** blocks are hidden on the client unless **Flame Vision** is enabled (GUI, or left-click in air / on brazier or candle).
