---
navigation:
  title: Entropic Soil
  icon: iska_utils:entropic_soil
  parent: hubs/entropic_terrain.md
  position: 0
item_ids:
  - iska_utils:entropic_soil
  - iska_utils:entropic_dirt
categories:
  - Entropy materials
---
# Entropic Soil

<ItemImage id="iska_utils:entropic_soil" />

Right-click connected **grass** or **dirt** with <ItemImage id="iska_utils:entropic_agglomeration" /> **Entropic Agglomeration** to convert a linked patch within a circular area (radius 7). The soil does **not** spread on its own afterward.

## Behavior

- **Light** (sky or block light) turns entropic soil into **Entropic Dirt**.
- In **darkness**, connected patches form a network that periodically spawns biome mobs (configurable allow/deny). Spawned mobs gain **Entropic Empowerment**.
- **Redstone** on any block in the network can **accelerate** the next spawn once per cycle (shorter random countdown; further pulses are ignored until it fires).

## Entropic Dirt

<ItemImage id="iska_utils:entropic_dirt" />

The darkened form of entropic soil. It does **not** recover on its own. Restore it to entropic soil with a <ItemImage id="iska_utils:drop_of_entropy" /> **Drop of Entropy** (right-click).
