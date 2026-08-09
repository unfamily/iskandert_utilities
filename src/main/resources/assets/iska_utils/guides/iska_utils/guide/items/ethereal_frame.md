---
navigation:
  title: Ethereal Frame
  icon: iska_utils:ethereal_frame
  parent: hubs/world_and_machines.md
  position: 61
item_ids:
  - iska_utils:ethereal_frame
categories:
  - World and machines
---
# Ethereal Frame

<ItemImage id="iska_utils:ethereal_frame" />

## What it does

The **Ethereal Frame** is an advanced filter block that controls **which entity types can pass through** based on an allow/deny list you set in its GUI.

- **Default**: Allow mode with only `minecraft:player` selected (players pass through; everything else is blocked).
- Adjacent frames automatically **share filter changes** across their connected network (up to 64 frames).
- When multiple networks exist nearby, the frame with the **newest filter update** wins during periodic sync and when placing new frames next to an existing network.

## Interactions

| Action | Result |
|--------|--------|
| **Right-click (empty hand)** | Opens the entity filter GUI |
| **Right-click (full block item, not camouflaged)** | Applies the block as camouflage |
| **Right-click (full block item, already camouflaged)** | Opens filter GUI |
| **Shift + Right-click (camouflaged)** | Returns the camouflage block |
| **Shift + Right-click (no camouflage, reinforced network)** | Removes reinforcement from the connected network and returns the materials |
| **Left-click (reinforcement material in hand)** | Reinforces as many frames in the network as the stack allows |

## Durability

By default the frame is **wood-like** (not wither / blast proof). It can be **reinforced** with materials such as <ItemImage id="iska_utils:wither_proof_block" /> **Wither Proof Block**, Obscure Glass, Ethereal Obscure Glass, or Netherite Bars (`#c:bars/netherite`) so the connected network becomes wither-proof like those blocks. Breaking a reinforced frame returns the reinforcement material (and camouflage if any).

## Camouflage

Right-click any **full opaque block** onto the frame to disguise it as that block. The frame keeps its filter behavior — only the visual changes. Shift-click to remove the camouflage and recover the block.

## Filter GUI

The filter screen shows a searchable list of entity types, common entity tags, and special filter keys.

Toggle **Allow list / Deny list** to flip the filter logic, select entries, and press **Apply**. After **Cancel**, the glass / tinted-glass icon toggles whether the frame **passes or blocks light**.

### Filter entries

| Entry kind | Example | Meaning |
|------------|---------|---------|
| Entity type | `minecraft:zombie` | Matches that entity type |
| Entity tag | `#minecraft:raiders` | Matches any entity in that tag |
| Special key | `$is_monster` | Matches entities meeting that condition |

Special keys: `$have_armor`, `$is_not_have_armor`, `$have_tool`, `$is_not_have_tool`, `$is_baby`, `$is_adult`, `$is_monster`, `$is_animal`, `$is_neutral`, `$on_fire`, `$is_not_on_fire`, `$is_crouching`, `$is_not_crouching`.

- `$is_animal` matches vanilla `Animal` entities (cows, wolves, axolotls, …).
- `$is_neutral` matches entities that implement `NeutralMob` (endermen, bees, piglins, iron golems, …).
- Baby/adult already cover each other’s opposite — there is no `$is_not_baby` / `$is_not_adult`.

An entity passes when **any** selected entry matches, then allow/deny mode is applied.
