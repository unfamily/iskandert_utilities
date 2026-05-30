---
navigation:
  title: Ancient Table
  icon: iska_utils:ancient_table
  parent: hubs/entropy_materials.md
  position: 12
item_ids:
  - iska_utils:ancient_table
categories:
  - Entropy materials
---
# Ancient Table

<ItemImage id="iska_utils:ancient_table" />

## What it does

The **Ancient Table** runs the same special crafts as the <ItemImage id="iska_utils:ancient_tablet" /> **Ancient Tablet**, but **automatically**. Ingredient **order does not matter**, and wrong layouts never destroy your inputs.

Fuel is <ItemImage id="iska_utils:drop_of_entropy" /> **Drop of Entropy**. Each drop placed in the fuel slot is converted into **internal fuel** (no item NBT). The table holds a large buffer (configurable); a comparator reads **only** how full the physical fuel slot is, not the internal buffer.

## How to use

1. Place ingredients in the **input** grid (stack counts per slot matter).
2. Put **Drop of Entropy** in the **fuel** slot (one item is consumed only when the buffer can absorb a full fuel unit).
3. Take results from the **output** grid when crafting finishes.
4. Use the **redstone** button beside the fuel slot (aligned with the first input row) to control when the table runs (same modes as the Factory).

Scroll the input and output grids when you have more than nine visible slots.

A **comparator** on the block outputs **0–15** from the **fuel slot stack count only** (empty slot = 0, full stack = 15). It does **not** reflect internal fuel or input/output fill — useful to see when the fuel slot needs refilling.

## Tips

- Some crafts, such as <ItemImage id="iska_utils:entropy_crystal" /> **Entropy Crystal**, consume more internal fuel per operation than simpler ones.
