---
navigation:
  title: Arcane Dictionary
  icon: iska_utils:arcane_dictionary
  parent: hubs/entropy_materials.md
  position: 13
item_ids:
  - iska_utils:arcane_dictionary
categories:
  - Entropy materials
---
# Arcane Dictionary

<ItemImage id="iska_utils:arcane_dictionary" />

## How to obtain

- **Suspicious Delivery** — one possible outcome when you open a package. See <ItemImage id="iska_utils:suspicious_delivery" /> **Suspicious Delivery**.
- Recyclable into one <ItemImage id="iska_utils:drop_of_entropy" /> **Drop of Entropy** like other delivery artifacts.

## What it does

The **Arcane Dictionary** is a **cursed Curio** (belt or charm) that stores random **arcane traits** (up to several traits, each with a level shown as Roman numerals). While exactly **one** dictionary is equipped in Curios and it has traits, those traits apply as passive effects.

**Two or more** dictionaries equipped in Curios at once **cancel all trait effects**.

Tooltip layout: **grey** flavor lines, **lime** mechanical lines, **purple** cursed-artifact header; trait names use **per-trait colours** from the datapack. **Hold Shift** for brief trait descriptions and catalyst reroll hints.

## Entropy charges (fuel for active traits)

Separate from XP rerolling. With **exactly one** dictionary in **Curios**, it **automatically absorbs** <ItemImage id="iska_utils:drop_of_entropy" /> **Drops of Entropy** from your **inventory** (not from Curios slots), up to a large internal buffer. Dictionaries only in inventory do not absorb.

When traits are **active**, stored entropy is **drained over time** based on each trait’s upkeep (datapack `ent_cha`; some traits cost nothing). If the buffer runs out, effects turn off until more drops are absorbed.

## Rerolling traits (XP only)

This does **not** use entropy charges.

1. Hold the dictionary in your **main hand**.
2. **Shift + right-click** — spends part of your experience. Only so much XP is taken per reroll; any experience beyond that stays with you.
3. **The more XP you offer, the better the roll tends to be** — more traits, higher levels, or both — but each reroll is still **random** until you reach the full level budget; then you always get the maximum trait count and levels.
4. Put a matching **catalyst** in your **offhand** (for example lapis for **Lucky**) to **boost** that trait’s roll chance — JEI and the Shift trait tooltip show the **new pick chance** with that catalyst. One catalyst is consumed per reroll; it does **not** guarantee the trait. Each trait on a roll is **unique** when the datapack offers enough entries; duplicates only appear if the roll asks for more traits than exist.

## Luck and Bad Luck

Which **specific** traits appear on each slot is influenced separately from XP quality.

- After the initial weighted pick, the vanilla **Luck** effect can reroll away from low-`luck` entries in the trait pool; **Bad Luck** can reroll away from high-`luck` ones. Stronger potion effects matter more.
- The **Lucky** trait applies **Luck**; the **Unlucky** trait applies **Bad Luck** — so they can nudge later rerolls too.
- Catalysts in the offhand **raise** the pick weight of matching traits (reroll chance on tooltip/JEI); they do not override luck entirely or guarantee a trait.

Datapacks can tag entries with how lucky or unlucky they are; pack authors decide which traits sit on which side of that scale.

## Arcane traits

Each reroll draws from the trait pool in the datapack. With **JEI** installed, look up the **Arcane Dictionary** to see every trait: effects at levels I–V, pool weight, luck, entropy upkeep, and reroll catalysts.

## Tips

- An **empty** dictionary (no traits) can stack in a **Deep Drawer** like other traitless single-stack items.
- Equipping it still counts as a **cursed artifact** for <ItemImage id="iska_utils:busted_crown" /> **Busted Crown**, even when empty.
