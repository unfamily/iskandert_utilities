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

Block that **cancels teleportation** within a cubic **radius** around it on every axis (set in the GUI).

Entities in the configured ignore list (default: `#c:bosses`) are exempt and can still teleport through the zone.

## GUI

Right-click the block to open its control GUI:

- **Range**: Use the **-** and **+** buttons to adjust the protection radius. The current and maximum values are shown.
- **Target** (icon button, right side): cycles through the affected group. Left-click advances, right-click goes back.
  - *Mobs only* (default): affects non-player entities.
  - *Players only*: affects players in Survival and Adventure.
  - *Mobs and players*: affects both groups.
- **Redstone Mode** (icon button): cycles through Ignore, Low, High, Disabled.
  - *Ignore* (gunpowder): always active when manually enabled, ignores redstone signal.
  - *Low*: active while redstone signal is **absent** (and manually enabled).
  - *High*: active while redstone signal is **present** (and manually enabled).
  - *Disabled*: never active.
- **Shift + Right-click**: toggles manual enable/disable without opening the GUI (action-bar feedback).
- **Show / Hide** (below redstone button): toggles a visible preview border around the affected area.
- **Range Module slot** (top-left): insert Range Module items to increase the maximum achievable radius. See **Modules** → **Range Module**.

## Tips

- Place at the center of the area you want to protect.
- Useful near farms where teleporting entities break containment.
- Starts **active** (Manual mode, manually enabled) when placed.
- See also: [Wander Nullifier](wander_nullifier.md), [Soul Nullifier](soul_nullifier.md).
