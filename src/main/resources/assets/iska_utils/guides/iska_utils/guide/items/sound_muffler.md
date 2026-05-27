---
navigation:
  title: Sound Muffler
  icon: iska_utils:sound_muffler
  parent: hubs/world_and_machines.md
  position: 52
item_ids:
  - iska_utils:sound_muffler
categories:
  - World and machines
  - Ambience
---
# Sound Muffler

<ItemImage id="iska_utils:sound_muffler" />

## What it does

- Attenuates **played sounds** for players inside its **spherical range** (minimum **8** blocks; upper cap may come from config). Volume is **per category**, not a single master knob.
- **Music** is not handled through these sliders (client music stays separate).

## Main GUI

- **Range**: enlarge or shrink the bubble where muffling applies; further controls often use stepped +/- buttons (see tooltips).
- **One row per category** (typical order): **All**, **Other** (uncatalogued / odd mod sounds), **Records**, **Weather**, **Blocks**, **Hostile**, **Neutral**, **Players**, **Ambient**, **Voice**.
- Each category has its own **0–100%** level: **0** = effectively mute that bucket inside the range, **100** = no reduction for that bucket.

## Filter screen

- Open **Filter** to manage a **list of sound IDs** (e.g. `minecraft:entity.creeper.primed`).
- Toggle **allow list** vs **deny list** (how the list interacts with this muffler):
  - **Allow list**: IDs on the list are **exempt** — this block **does not** attenuate those sounds (everything else still uses the category sliders).
  - **Deny list**: only IDs on the list are attenuated by this block; sounds **not** listed pass through **unchanged** by this muffler.
- When the list is **empty**, the filter is inactive and **only** the category volumes apply.
- Use **search** to find IDs quickly on busy packs.
