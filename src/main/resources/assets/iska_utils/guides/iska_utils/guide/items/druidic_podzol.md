---
navigation:
  title: Druidic Podzol
  icon: iska_utils:druidic_podzol
  parent: hubs/entropic_terrain.md
  position: 2
item_ids:
  - iska_utils:druidic_podzol
categories:
  - Entropy materials
---
# Druidic Podzol

<ItemImage id="iska_utils:druidic_podzol" />

Right-click connected **grass**, **dirt**, or **podzol** with <ItemImage id="iska_utils:druidic_agglomeration" /> **Druidic Agglomeration** to convert a linked patch within a circular area (radius 7). The podzol does **not** spread on its own afterward (except rare random-tick spread onto adjacent convertible blocks).

## Behavior

- In **light**, connected patches form a network that periodically spawns **biome animals** (configurable allow/deny). Spawned animals have **no** special effects.
- In **darkness**, the block does **nothing** — no spawns, **no** block change, and **no** dormant or inert form (unlike Entropic Soil → Entropic Dirt).
- **Redstone** on any block in the network can **accelerate** the next spawn once per cycle (shorter random countdown; further pulses are ignored until it fires).

## Drops

Breaks like podzol: drops **dirt** unless mined with **Silk Touch** (then drops the podzol block).

## Contrast with Entropic Soil

See **Entropic Soil** for darkness spawns, light decay to **Entropic Dirt**, and restoring dirt with **Drop of Entropy**. Druidic Podzol has no equivalent decay or restore item.
