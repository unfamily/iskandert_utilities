---
navigation:
  title: Mob Reaper
  icon: iska_utils:mob_reaper
  parent: hubs/world_and_machines.md
  position: 45
item_ids:
  - iska_utils:mob_reaper
  - iska_utils:normal_damage_module
  - iska_utils:lethal_damage_module
  - iska_utils:enchant_module
  - iska_utils:beheading_module
  - iska_utils:luck_module
  - iska_utils:experience_module
categories:
  - World and machines
---
# Mob Reaper

<ItemImage id="iska_utils:mob_reaper" />

## Purpose

The Mob Reaper is an automated **combat machine**. On a fixed interval it damages living entities nearby using a **fake player** kill credit (loot tables, player kills, enchantments). Pair it with a **Collecting Crate** downstream to vacuum drops and XP.

## Placement

- **Floor**: faces the player on place; **sneak + place** flips direction.
- **Wall**: click a side face; `FACING` points **away from the wall** (kill direction). Some servers disable wall placement.
- **Vector plate**: on a **Vector Block** below, the reaper **moves with the plate** and does not drop as an item when the plate is broken.

The blade spins and deals damage only while **active** (see redstone).

## GUI

Ghost icons show valid modules when a slot is empty. Live stats: **damage**, **beheading %**, **luck**, **XP multiplier**, and **lethal active** when applicable.

| Control | Action |
| ------- | ------ |
| **Target type** (right side) | Cycles: **Mobs only** → **Mobs and players** → **Players only**. Left click forward, right click backward. |
| **Age filter** (far right) | Cycles: **All ages** → **Adults only** → **Babies only**. Skips baby or adult mobs when filtering. |
| **Redstone mode** | Same family as other machines: **Ignore**, **Low**, **High**, **Disabled** (left / right click). |
| **✕** | Close |

## Modules

Use **one damage module type** at a time: **Normal** (stackable) **or** **Lethal** (single). The other upgrades stack independently.

| Module | Effect |
| ------ | ------ |
| Normal damage | Base damage + bonus per stacked module. |
| Lethal damage | Fixed very high damage; replaces normal damage modules. |
| Enchant | Holds a **weapon**; enchantments apply to damage and post-attack effects. |
| Beheading | Extra **skull** drop chance per module level. |
| Luck | Applies **Luck** to the fake player for loot rolls. |
| Experience | Multiplier on **bonus XP orbs** when a target dies. |

<ItemGrid>
  <ItemIcon id="iska_utils:normal_damage_module" />
  <ItemIcon id="iska_utils:lethal_damage_module" />
  <ItemIcon id="iska_utils:enchant_module" />
  <ItemIcon id="iska_utils:beheading_module" />
  <ItemIcon id="iska_utils:luck_module" />
  <ItemIcon id="iska_utils:experience_module" />
</ItemGrid>

Module inventory is **hopper / pipe** compatible.

## Redstone

- **Ignore** (default): always attacks when cooldown allows.
- **Low**: attacks when **no** redstone signal.
- **High**: attacks only **with** redstone.
- **Disabled**: never attacks.

## Tips

- Use **Mobs only** on servers unless you intend PvP traps.
- **Enchant module** + looting / sharpness scales farms; **Collecting Crate** in the loot path collects orbs and items.
- Wall-mounted reapers work well in mob elevators with vector plates.
