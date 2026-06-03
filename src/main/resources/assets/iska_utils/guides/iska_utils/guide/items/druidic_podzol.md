---
navigation:
  title: Druidic Podzol
  icon: iska_utils:druidic_podzol
  parent: hubs/mobfarm_soils.md
  position: 2
item_ids:
  - iska_utils:druidic_podzol
categories:
  - MobFarm Soils
---
# Druidic Podzol

<ItemImage id="iska_utils:druidic_podzol" />

Use <ItemImage id="iska_utils:druidic_agglomeration" /> **Druidic Agglomeration** for instant patch conversion — see that page.

## Behavior

- In **light**, **each** podzol block runs its own spawn timer for **biome animals** (configurable allow/deny). Animals above the configured max HP (default **60**) are skipped.
- In **darkness**, the block does **nothing** — no spawns, **no** block change (unlike Entropic Soil → Entropic Dirt).
- **Redstone** on a **connected patch** (one clock on the edge is enough) switches every lit podzol in that patch to fast spawn timers (like Mob Grinding Utils delightful dirt): each valid tile tries on its own **20–60 tick** cooldown (max **8** animals nearby per block). Neighbor updates refresh the patch flag and can trigger an immediate attempt.

## Natural spread

- Slowly converts adjacent **dirt and podzol** only — not grass, not entropic soil/dirt.
- Does not spread onto druidic podzol that is already placed.

## Drops

Breaks like podzol: drops **dirt** unless mined with **Silk Touch** (then drops the podzol block).

## Contrast with Entropic Soil

See **Entropic Soil** for darkness spawns, light decay, and **Drop of Entropy** restore. Natural spread and agglomeration follow corruptive vs blessed rules described in **MobFarm Soils**.
