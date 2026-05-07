#!/usr/bin/env python3
"""
Generate GuideME markdown pages for iskandert_utilities (v24) and iskandert_utilities_26 (v26).
Run from repo root: python3 scripts/generate_guideme_pages.py
"""
from __future__ import annotations

import os
from pathlib import Path

ROOTS = [
    Path(__file__).resolve().parents[1] / "src/main/resources/assets/iska_utils/guides/iska_utils/guide",
    Path(__file__).resolve().parents[2] / "iskandert_utilities_26/src/main/resources/assets/iska_utils/guides/iska_utils/guide",
]


def page_md(
    *,
    title: str,
    icon: str,
    parent: str,
    position: int,
    item_ids: list[str],
    category: str,
    body: str,
) -> str:
    ids_yaml = "\n".join(f"  - {i}" for i in item_ids)
    return f"""---
navigation:
  title: {title}
  icon: {icon}
  parent: {parent}
  position: {position}
item_ids:
{ids_yaml}
categories:
  - {category}
---
# {title}

<ItemImage id="{item_ids[0]}" />

{body}
"""


HUBS = [
    (
        "hubs/storage.md",
        "Storage",
        "iska_utils:deep_drawers",
        "index.md",
        10,
        """# Storage

Deep Drawer storage and related blocks.
""",
    ),
    (
        "hubs/rubber_and_processing.md",
        "Rubber and processing",
        "iska_utils:rubber",
        "index.md",
        12,
        """# Rubber and processing

Rubber trees, sap extraction, processing, and the main rubber materials used across the mod.
""",
    ),
    (
        "hubs/vector_motion.md",
        "Vector motion",
        "iska_utils:vector_charm",
        "index.md",
        20,
        """# Vector motion

Vector Charm, Fanpack, vector plates, and related crafting components.
""",
    ),
    (
        "hubs/fan_system.md",
        "Fan system",
        "iska_utils:fan",
        "index.md",
        25,
        """# Fan system

Modular Fan block and its upgrade modules.
""",
    ),
    (
        "hubs/tools_and_scanner.md",
        "Tools and scanner",
        "iska_utils:scanner",
        "index.md",
        30,
        """# Tools and scanner

Dollies, structure tools, Swiss Wrench, Mining Equitizer, and the Scanner.
""",
    ),
    (
        "hubs/world_and_machines.md",
        "World and machines",
        "iska_utils:temporal_overclocker",
        "index.md",
        40,
        """# World and machines

Weather and time blocks, Smart Timer, Angel Block, structure machines, Temporal Overclocker, shops, and Hellfire Igniter.
""",
    ),
    (
        "hubs/combat_and_travel.md",
        "Combat and travel",
        "iska_utils:portable_dislocator",
        "index.md",
        50,
        """# Combat and travel

Shields, hearts, braziers, climbing gauntlet, portable dislocator, and related gear.
""",
    ),
]

MATERIALS_ITEM_IDS = [
    "iska_utils:sap",
    "iska_utils:rubber",
    "iska_utils:rubber_chunk",
    "iska_utils:plastic_ingot",
    "iska_utils:green_sludge",
    "iska_utils:tar_slimeball",
    "iska_utils:dye_berry",
]


def materials_page_text() -> str:
    ids_yaml = "\n".join(f"  - {i}" for i in MATERIALS_ITEM_IDS)
    return f"""---
navigation:
  title: Materials (rubber and dye)
  parent: hubs/rubber_and_processing.md
  position: 30
item_ids:
{ids_yaml}
categories:
  - Rubber and processing
---
# Materials (rubber and dye)

Core rubber and dye materials used by rubber trees, processing, and various recipes.

<ItemGrid>
  <ItemIcon id="iska_utils:sap" />
  <ItemIcon id="iska_utils:rubber" />
  <ItemIcon id="iska_utils:rubber_chunk" />
  <ItemIcon id="iska_utils:plastic_ingot" />
  <ItemIcon id="iska_utils:green_sludge" />
  <ItemIcon id="iska_utils:tar_slimeball" />
  <ItemIcon id="iska_utils:dye_berry" />
</ItemGrid>

## Rubber flow

- Tap filled rubber logs with a **Tree Tap** or **Electric Tree Tap** to obtain **Sap**.
- For automation, use a **Rubber Sap Extractor**.
- Convert sap into **Rubber** and related intermediates used in many recipes (check JEI for exact steps).

## Tips

- If you are unsure which log variant can be tapped, start from the Rubber section pages and follow the item links.
- For throughput, place multiple logs and automate extraction + processing.

## Dye berries

- **Dye Berry** bushes refill over time; berries are used in dye recipes (see JEI in-game).
"""

# (path, title, icon, parent, position, item_id_24, item_id_26_or_none, category, body_md)
# item_id_26_or_none None => same id
PAGES: list[tuple[str, str, str, str, int, str, str | None, str, str]] = []

def add(
    path: str,
    title: str,
    icon: str,
    parent: str,
    pos: int,
    id24: str,
    id26: str | None,
    cat: str,
    body: str,
):
    PAGES.append((path, title, icon, parent, pos, id24, id26, cat, body))


add(
    "items/vector_charm.md",
    "Vector Charm",
    "iska_utils:vector_charm",
    "hubs/vector_motion.md",
    10,
    "iska_utils:vector_charm",
    None,
    "Vector motion",
    """## What it does

- Boosts movement on **Vector Plates** when held, worn, in Curios (if installed), or in inventory (priority: main hand, off-hand, Curios, then inventory).

## Energy

- Optional RF/FE buffer and per-direction consumption; see the in-game tooltip for the exact behavior.
""",
)

add(
    "items/fanpack.md",
    "Fanpack",
    "iska_utils:fanpack",
    "hubs/vector_motion.md",
    15,
    "iska_utils:fanpack",
    None,
    "Vector motion",
    """## What it does

- Extends the Vector Charm behavior and can grant **creative flight** for survival players when enough energy is available.

## Energy

- Uses energy for movement and (optionally) flight; low-energy warnings can appear on the action bar.
""",
)

add(
    "items/vector_plates.md",
    "Vector plates",
    "iska_utils:slow_vect",
    "hubs/vector_motion.md",
    20,
    "iska_utils:slow_vect",
    None,
    "Vector motion",
    """## Standard plates

- **Slow / Moderate / Fast / Extreme / Ultra** vector plates push entities along their facing direction.

## Player-only plates

- **Player_*** variants affect players the same way but ignore mobs.

## Crafting tiers

- Each speed tier is a different block. Higher tiers are crafted from lower tiers (see JEI for exact recipes).
""",
)

add(
    "items/acceleration_modules.md",
    "Acceleration modules",
    "iska_utils:slow_module",
    "hubs/vector_motion.md",
    30,
    "iska_utils:slow_module",
    None,
    "Vector motion",
    """## Use

- Crafting components used by Vector-motion recipes (for example **Vector Charm** requires the highest tier).
- They are not installed into vector plates.

## Items

<ItemGrid>
  <ItemIcon id="iska_utils:base_module" />
  <ItemIcon id="iska_utils:slow_module" />
  <ItemIcon id="iska_utils:moderate_module" />
  <ItemIcon id="iska_utils:fast_module" />
  <ItemIcon id="iska_utils:extreme_module" />
  <ItemIcon id="iska_utils:ultra_module" />
</ItemGrid>
""",
)

add(
    "items/modular_fan.md",
    "Modular Fan",
    "iska_utils:fan",
    "hubs/fan_system.md",
    5,
    "iska_utils:fan",
    None,
    "Fan system",
    """## What it does

- Pushes or pulls entities in a configurable box; upgrade with **Fan modules** in the GUI.

## GUI

- Adjust reach on each axis with **+ / −** around the grid; bottom bar shows forward depth (green = in bar, blue = extended beyond the visual cap).

## Redstone

- Multiple redstone modes (see in-game labels); integrates with automation via the fan GUI and redstone input.

## Modules

- See **Fan modules** in this guide for Range and Ghost upgrades.
""",
)

add(
    "items/fan_modules.md",
    "Fan modules",
    "iska_utils:range_module",
    "hubs/fan_system.md",
    10,
    "iska_utils:range_module",
    None,
    "Fan system",
    """## Modules

- **Range module**: increases max reach in all axes.
- **Ghost module**: changes how the airflow interacts with blocks.

<ItemGrid>
  <ItemIcon id="iska_utils:range_module" />
  <ItemIcon id="iska_utils:ghost_module" />
</ItemGrid>
""",
)

add(
    "items/portable_dislocator.md",
    "Portable Dislocator",
    "iska_utils:portable_dislocator",
    "hubs/combat_and_travel.md",
    20,
    "iska_utils:portable_dislocator",
    None,
    "Combat and travel",
    """## What it does

- Short-range teleport tool that jumps toward the target of a **search compass** you are holding (for example **Nature's Compass** or **Explorer's Compass**).

## Controls

- Use the bound key from Controls; destination depends on the held compass target and your look direction.
""",
)

add(
    "items/scanner_family.md",
    "Scanner and chips",
    "iska_utils:scanner",
    "hubs/tools_and_scanner.md",
    15,
    "iska_utils:scanner",
    None,
    "Tools and scanner",
    """## Scanner

- Highlights matching blocks or mobs for a limited time; supports Scanner Chips for storing and transferring targets.

<ItemGrid>
  <ItemIcon id="iska_utils:scanner" />
  <ItemIcon id="iska_utils:scanner_chip" />
  <ItemIcon id="iska_utils:scanner_chip_ores" />
  <ItemIcon id="iska_utils:scanner_chip_mobs" />
</ItemGrid>

## Scanner Chip (blank)

- **Shift + use on a block** to store a block target in the chip.
- To transfer the chip target into the **Scanner**, hold the scanner in your **main hand** and **use the chip**.

## Scanner Chip (Ores)

- Pre-set to scan **ores**.
- **Shift + use** to cycle the mining-level filter (shown in chat / tooltip).
- Hold the scanner in your **main hand** and **use the chip** to transfer the ore-scan target into the scanner.

## Scanner Chip (Mobs)

- Pre-set to scan **all mobs**.
- Hold the scanner in your **main hand** and **use the chip** to transfer the mob-scan target into the scanner.
""",
)

add(
    "items/dollies.md",
    "Dollies",
    "iska_utils:dolly",
    "hubs/tools_and_scanner.md",
    10,
    "iska_utils:dolly",
    None,
    "Tools and scanner",
    """## Dolly / Hard Dolly

- **Shift + use** on a block to pick it up (respecting mining level, blacklist/whitelist, and hardness rules per tier).
- Use again to place. Contents are preserved for supported blocks.

<ItemGrid>
  <ItemIcon id="iska_utils:dolly" />
  <ItemIcon id="iska_utils:dolly_hard" />
</ItemGrid>

## Tiers

- **Dolly**: standard limits.
- **Hard Dolly**: can include spawners where allowed.
""",
)

add(
    "items/swiss_wrench.md",
    "Swiss Wrench",
    "iska_utils:swiss_wrench",
    "hubs/tools_and_scanner.md",
    25,
    "iska_utils:swiss_wrench",
    None,
    "Tools and scanner",
    """## Use

- Repairs vector plates and interacts with supported Iska Utils blocks; see in-game tooltip for context actions.
""",
)

add(
    "items/mining_equitizer.md",
    "Mining Equitizer",
    "iska_utils:mining_equitizer",
    "hubs/tools_and_scanner.md",
    30,
    "iska_utils:mining_equitizer",
    None,
    "Tools and scanner",
    """## What it does

- Reduces or removes the mining speed penalty while flying/eligible movement states (as implemented for the version you play).
""",
)

add(
    "items/structure_tools.md",
    "Structure Placer and Blueprint",
    "iska_utils:structure_placer",
    "hubs/tools_and_scanner.md",
    40,
    "iska_utils:structure_placer",
    None,
    "Tools and scanner",
    """## Structure Placer

- Places saved structures from the mod's structure definitions / packs (see Structure Placer Machine for authoring workflow).

## Blueprint

- Records corner positions and related metadata for structure capture workflows.
""",
)

add(
    "rubber/rubber_overview.md",
    "Rubber: overview",
    "iska_utils:rubber",
    "hubs/rubber_and_processing.md",
    5,
    "iska_utils:rubber",
    None,
    "Rubber and processing",
    """## What this section covers

- Rubber trees and sap extraction.
- Automation options.
- Core materials used by rubber recipes.

## Quick start

1. Get a **Sacred Rubber Sapling** and grow a rubber tree.
2. Find a tappable rubber log variant and use a **Tree Tap** to extract **Sap**.
3. Process sap into **Rubber** (see JEI for the exact steps).

<ItemGrid>
  <ItemIcon id="iska_utils:sacred_rubber_sapling" />
  <ItemIcon id="iska_utils:treetap" />
  <ItemIcon id="iska_utils:sap" />
  <ItemIcon id="iska_utils:rubber" />
</ItemGrid>
""",
)

add(
    "rubber/sap_extraction.md",
    "Sap extraction",
    "iska_utils:sap",
    "hubs/rubber_and_processing.md",
    10,
    "iska_utils:sap",
    None,
    "Rubber and processing",
    """## Manual extraction

- Use a **Tree Tap** on a filled rubber log to extract **Sap**.
- The **Electric Tree Tap** is the powered variant (see item tooltip for usage).

## Automation

- The **Rubber Sap Extractor** can extract sap automatically from adjacent rubber logs.

<ItemGrid>
  <ItemIcon id="iska_utils:treetap" />
  <ItemIcon id="iska_utils:electric_treetap" />
  <ItemIcon id="iska_utils:rubber_sap_extractor" />
</ItemGrid>
""",
)

add(
    "rubber/processing_chain.md",
    "Processing chain",
    "iska_utils:rubber_chunk",
    "hubs/rubber_and_processing.md",
    20,
    "iska_utils:rubber_chunk",
    None,
    "Rubber and processing",
    """## From sap to rubber

- Rubber processing is recipe-driven. Use JEI to see the exact machines / steps for your pack.

## Common outputs

- **Rubber**: core ingredient used for tools/armor/plastic.
- **Plastic Ingot**: used in higher-tier crafting components.
- **Green Sludge** / **Tar Slimeball**: intermediates used by some recipes.

<ItemGrid>
  <ItemIcon id="iska_utils:sap" />
  <ItemIcon id="iska_utils:rubber" />
  <ItemIcon id="iska_utils:plastic_ingot" />
  <ItemIcon id="iska_utils:green_sludge" />
  <ItemIcon id="iska_utils:tar_slimeball" />
</ItemGrid>
""",
)

add(
    "items/rubber_boots.md",
    "Rubber Boots",
    "iska_utils:rubber_boots",
    "hubs/rubber_and_processing.md",
    8,
    "iska_utils:rubber_boots",
    None,
    "Rubber and processing",
    """## What they do

- Negate fall damage (boots take durability instead).
- Bounce when you land from a sufficient height unless you **crouch** (crouch = no bounce, still negates fall damage).
""",
)

add(
    "items/treetaps.md",
    "Tree Tap and Electric Tree Tap",
    "iska_utils:treetap",
    "hubs/rubber_and_processing.md",
    15,
    "iska_utils:treetap",
    None,
    "Rubber and processing",
    """## Tree Tap

- Durability tool used on filled rubber logs to extract **Sap**.

## Electric Tree Tap

- Powered variant; see tooltip for energy use.
""",
)

add(
    "items/rubber_sap_extractor.md",
    "Rubber Sap Extractor",
    "iska_utils:rubber_sap_extractor",
    "hubs/rubber_and_processing.md",
    20,
    "iska_utils:rubber_sap_extractor",
    None,
    "Rubber and processing",
    """## Automation

- Extracts sap from adjacent rubber logs; requires energy when configured.

## Output

- Routes output to adjacent inventories or drops (see block behavior in-world).
""",
)

add(
    "items/temporal_overclocker.md",
    "Temporal Overclocker",
    "iska_utils:temporal_overclocker",
    "hubs/world_and_machines.md",
    35,
    "iska_utils:temporal_overclocker",
    None,
    "World and machines",
    """## What it does

- Links to nearby blocks to accelerate their ticking / processing (exact behavior per supported block type).

## Chipset

- Use **Temporal Overclocker Chip** items to configure or boost the machine (see GUI).

<ItemGrid>
  <ItemIcon id="iska_utils:temporal_overclocker" />
  <ItemIcon id="iska_utils:temporal_overclocker_chip" />
</ItemGrid>
""",
)

add(
    "items/structure_machines.md",
    "Structure Placer / Saver machines",
    "iska_utils:structure_placer_machine",
    "hubs/world_and_machines.md",
    45,
    "iska_utils:structure_placer_machine",
    None,
    "World and machines",
    """## Placer machine

- Places structures from definitions with FE/RF costs per block where applicable.

## Saver machine

- Captures areas to structure definitions compatible with the placer.
""",
)

add(
    "items/world_utility_blocks.md",
    "Weather, time, and ambience",
    "iska_utils:weather_detector",
    "hubs/world_and_machines.md",
    50,
    "iska_utils:weather_detector",
    None,
    "World and machines",
    """## Blocks

- **Weather Detector** emits redstone based on weather.
- **Weather Alterer** / **Time Alterer** consume energy to change world state (see GUI).
- **Sound Muffler** reduces sound in an area.
- **Smart Timer** advanced redstone timing.
- **Angel Block** temporary placement helper for building.
""",
)

add(
    "items/shops.md",
    "Shop and Auto Shop",
    "iska_utils:shop",
    "hubs/world_and_machines.md",
    60,
    "iska_utils:shop",
    None,
    "World and machines",
    """## Shop

- Player-facing trading GUI bound to the block.

## Auto Shop

- Automated selling / restocking behavior (see block GUI).
""",
)

add(
    "items/redstone_activator.md",
    "Redstone Activator",
    "iska_utils:redstone_activator",
    "hubs/world_and_machines.md",
    55,
    "iska_utils:redstone_activator",
    None,
    "World and machines",
    """## Use

- Portable tool item that emits or configures redstone signals depending on mode (see in-game tooltip).
""",
)

add(
    "items/hellfire_igniter.md",
    "Hellfire Igniter",
    "iska_utils:hellfire_igniter",
    "hubs/world_and_machines.md",
    70,
    "iska_utils:hellfire_igniter",
    None,
    "World and machines",
    """## Use

- Ignites large areas safely per mod rules; see the in-game tooltip for usage details.
""",
)

add(
    "items/necrotic_crystal_heart.md",
    "Necrotic Crystal Heart",
    "iska_utils:necrotic_crystal_heart",
    "hubs/combat_and_travel.md",
    10,
    "iska_utils:necrotic_crystal_heart",
    None,
    "Combat and travel",
    """## What it does

- Prevents **lethal** damage when equipped, at the cost of reducing your **maximum health**.

## Details

- When an incoming hit would kill you, the heart cancels that damage and permanently reduces your max health (one heart at a time).
- Once your max health is reduced too far, lethal damage is no longer prevented and you will die normally.
- The max-health penalty resets after death, and also after sleeping (on wake-up).
""",
)

add(
    "items/burning_brazier.md",
    "Burning Brazier",
    "iska_utils:burning_brazier",
    "hubs/combat_and_travel.md",
    22,
    "iska_utils:burning_brazier",
    None,
    "Combat and travel",
    """## Use

- Places **Burning Flame** under you automatically (toggle with the key shown in the tooltip).
- Consumes durability when it places flames.
""",
)

add(
    "items/gauntlet_of_climbing.md",
    "Gauntlet of Climbing",
    "iska_utils:gauntlet_of_climbing",
    "hubs/combat_and_travel.md",
    24,
    "iska_utils:gauntlet_of_climbing",
    None,
    "Combat and travel",
    """## Use

- When enabled, climb walls while colliding horizontally.
- Toggle with the key shown in the tooltip.
""",
)

add(
    "items/ghost_brazier.md",
    "Ghost Brazier",
    "iska_utils:ghost_brazier",
    "hubs/combat_and_travel.md",
    30,
    "iska_utils:ghost_brazier",
    None,
    "Combat and travel",
    """## What it does

- Toggles or interacts with spectator-related movement rules (see version-specific tooltip and keybind).
""",
)

add(
    "items/sacred_rubber_sapling.md",
    "Sacred Rubber Sapling",
    "iska_utils:sacred_rubber_sapling",
    "hubs/rubber_and_processing.md",
    12,
    "iska_utils:sacred_rubber_sapling",
    None,
    "Rubber and processing",
    """## Worldgen / growth

- Special rubber tree variant; read tooltip for placement and growth hints.
""",
)

add(
    "items/potion_plates.md",
    "Potion plates",
    "iska_utils:potion_plate",
    "hubs/world_and_machines.md",
    80,
    "iska_utils:potion_plate",
    None,
    "World and machines",
    """## Dynamic items

- Potion plate blocks and items are registered dynamically per potion type. Use JEI to browse individual entries; behavior is shared across the family.
""",
)

add(
    "items/raft.md",
    "Raft blocks",
    "iska_utils:raft",
    "hubs/world_and_machines.md",
    95,
    "iska_utils:raft",
    None,
    "World and machines",
    """## Raft

- Floating platform blocks; see in-game tooltip / behavior.
""",
)


def hub_md(title: str, icon: str, parent: str, position: int, intro: str) -> str:
    return f"""---
navigation:
  title: {title}
  icon: {icon}
  parent: {parent}
  position: {position}
categories:
  - {title}
---
{intro}

<SubPages icons={{true}} alphabetical={{true}} />
"""


def write_hub(root: Path, rel: str, title: str, icon: str, parent: str, position: int, intro: str):
    full = root / rel
    full.parent.mkdir(parents=True, exist_ok=True)
    full.write_text(hub_md(title, icon, parent, position, intro), encoding="utf-8")


def write_materials(root: Path):
    full = root / "materials/rubber_and_processing.md"
    full.parent.mkdir(parents=True, exist_ok=True)
    full.write_text(materials_page_text(), encoding="utf-8")


def write_page(root: Path, is_v26: bool, entry: tuple):
    path, title, icon, parent, pos, id24, id26, cat, body = entry
    full = root / path
    full.parent.mkdir(parents=True, exist_ok=True)
    primary = id26 if is_v26 and id26 else id24
    item_ids = [primary]
    # multi-id pages already list one primary; scanner_family uses scanner only as primary - ok
    if path == "items/scanner_family.md":
        item_ids = [
            "iska_utils:scanner",
            "iska_utils:scanner_chip",
            "iska_utils:scanner_chip_ores",
            "iska_utils:scanner_chip_mobs",
        ]
    if path == "items/dollies.md":
        item_ids = ["iska_utils:dolly", "iska_utils:dolly_hard"]
    if path == "items/vector_plates.md":
        item_ids = [
            "iska_utils:slow_vect",
            "iska_utils:moderate_vect",
            "iska_utils:fast_vect",
            "iska_utils:extreme_vect",
            "iska_utils:ultra_vect",
            "iska_utils:player_slow_vect",
            "iska_utils:player_moderate_vect",
            "iska_utils:player_fast_vect",
            "iska_utils:player_extreme_vect",
            "iska_utils:player_ultra_vect",
        ]
    if path == "items/world_utility_blocks.md":
        item_ids = [
            "iska_utils:weather_detector",
            "iska_utils:weather_alterer",
            "iska_utils:time_alterer",
            "iska_utils:sound_muffler",
            "iska_utils:smart_timer",
            "iska_utils:angel_block",
        ]
    if path == "items/shops.md":
        item_ids = ["iska_utils:shop", "iska_utils:auto_shop"]
    if path == "items/structure_tools.md":
        item_ids = ["iska_utils:structure_placer", "iska_utils:blueprint"]
    if path == "items/structure_machines.md":
        item_ids = ["iska_utils:structure_placer_machine", "iska_utils:structure_saver_machine"]
    if path == "items/raft.md":
        item_ids = ["iska_utils:raft"]
    if path == "items/temporal_overclocker.md":
        item_ids = ["iska_utils:temporal_overclocker", "iska_utils:temporal_overclocker_chip"]

    content = page_md(
        title=title,
        icon=icon,
        parent=parent,
        position=pos,
        item_ids=item_ids,
        category=cat,
        body=body,
    )
    full.write_text(content, encoding="utf-8")


def main():
    for root in ROOTS:
        if not root.exists():
            print("skip missing", root)
            continue
        is_v26 = "iskandert_utilities_26" in str(root)
        write_materials(root)
        for rel, title, icon, parent, pos, intro in HUBS:
            write_hub(root, rel, title, icon, parent, pos, intro)
        for entry in PAGES:
            write_page(root, is_v26, entry)
        print("wrote hubs + pages ->", root)


if __name__ == "__main__":
    main()
