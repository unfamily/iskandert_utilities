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

Block that **cancels mob teleports** within a cubic **radius** around it on every axis (set in the GUI). Does not affect players.

## GUI

Right-click the block to open its control GUI:

- **Range**: Use the **-** and **+** buttons to adjust the protection radius. The current and maximum values are shown.
- **Redstone Mode** (icon button): cycles through Ignore, Low, High, Disabled.
  - *Ignore* (gunpowder): always active when manually enabled, ignores redstone signal.
  - *Low*: active while redstone signal is **absent** (and manually enabled).
  - *High*: active while redstone signal is **present** (and manually enabled).
  - *Disabled*: never active.
- **Shift + Right-click**: toggles manual enable/disable without opening the GUI (action-bar feedback).
- **Show / Hide** (below redstone button): toggles a visible preview border around the affected area.
- **Range Module slot** (top-left): insert Range Module items to increase the maximum achievable radius.

## Tips

- Place at the center of the area you want to protect.
- Useful near mob farms where teleporting mobs break containment.
- Starts **active** (Manual mode, manually enabled) when placed.
- See also: [Wander Nullifier](wander_nullifier.md), [Soul Nullifier](soul_nullifier.md).
