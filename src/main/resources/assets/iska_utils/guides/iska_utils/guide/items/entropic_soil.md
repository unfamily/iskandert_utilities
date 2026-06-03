---
navigation:
  title: Entropic Soil
  icon: iska_utils:entropic_soil
  parent: hubs/mobfarm_soils.md
  position: 0
item_ids:
  - iska_utils:entropic_soil
  - iska_utils:entropic_dirt
categories:
  - MobFarm Soils
---
# Entropic Soil

<ItemImage id="iska_utils:entropic_soil" />

Use <ItemImage id="iska_utils:entropic_agglomeration" /> **Entropic Agglomeration** for instant patch conversion — see that page. Below: block behavior and **natural** random-tick spread.

## Behavior

- **Light** (sky or block light) turns entropic soil into **Entropic Dirt** (dirt does not spread onto vanilla blocks).
- In **darkness**, **each** soil block runs its own spawn timer (configurable allow/deny). Mobs above the configured max HP (default **60**) are skipped. Spawned mobs gain **Entropic Empowerment**.
- **Redstone** on a **connected patch** (one clock on the edge is enough) switches every block in that patch to fast spawn timers (like Mob Grinding Utils dreadful dirt): each valid tile tries on its own **20–60 tick** cooldown (max **8** hostiles nearby per block). Neighbor updates refresh the patch flag and can trigger an immediate attempt.

## Natural spread

- Slowly converts adjacent **vanilla grass or dirt** (not druidic podzol, not existing entropic soil).
- Faster when the grass/dirt tile touches an **entropic soil** block (patch border creep).
- **Entropic Dirt** next to entropic soil is reclaimed back to soil quickly (much faster than vanilla spread).

## Entropic Dirt

<ItemImage id="iska_utils:entropic_dirt" />

The darkened form of entropic soil. Adjacent **Entropic Soil** reclaims it naturally, or use a <ItemImage id="iska_utils:drop_of_entropy" /> **Drop of Entropy** (right-click) for instant restore. After **long** light exposure (configurable random range per block), entropic dirt reverts to **vanilla dirt**; darkness pauses the timer.
