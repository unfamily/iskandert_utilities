---
navigation:
  title: Collecting Crate
  icon: iska_utils:collecting_crate
  parent: hubs/world_and_machines.md
  position: 46
item_ids:
  - iska_utils:collecting_crate
  - iska_utils:range_module
categories:
  - World and machines
---
# Collecting Crate

<ItemImage id="iska_utils:collecting_crate" />

## Purpose

Vacuums **item entities** and **experience orbs** inside a box around the block. Stores items in an internal inventory and XP as **fluid experience** (compatible with experience fluid tags). Ideal under **Mob Reaper** farms or any kill chamber.

## Collection area

- Box extends **left**, **right**, **up**, and **behind** relative to the block’s facing (not in front of the crate face).
- **Range module** in the module slot raises max extents (stack modules to reach the limit).
- **Preview** toggles a client-side outline of the active box.

Adjust extents with **+ / −** buttons (Up, Left, Right, Behind). Modifiers:

- **Left click**: +1  
- **Right click**: −1  
- **Shift**: ±10  
- **Alt / Ctrl**: ±5  

## Collect mode

Cycles with the mode button:

- **XP and items** (default)
- **XP only**
- **Items only**

## XP tank

- XP is stored as fluid; GUI shows **stored levels**.
- **Collect all XP to player** — pulls stored XP to you.
- **Deposit all XP from player** — empties your XP into the tank (respecting capacity).

## Storage and automation

- **Item** and **fluid** handlers on the block for hoppers and pipes.

## Redstone

Same modes as other machines: **Ignore**, **Low**, **High**, **Disabled**. When disabled or gated off, collection stops.

## Tips

- Place the crate **behind** the kill box relative to its facing so the box covers the drop zone.
- Use **items only** if XP is piped elsewhere; use **XP only** for experience farms without item clutter.
