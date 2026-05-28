# GuideME documentation inventory (v24 + v26)

English-only guide pages under `assets/iska_utils/guides/iska_utils/guide/`.

## Classification

- **A (must-have)**: Custom behavior, GUI, toggles, Curios, energy, scanner, dollies, braziers, storage, machines, modules, structure tools.
- **B (materials hub)**: Intermediate resources in one shared page `materials/rubber_and_processing.md`.
- **C (no dedicated page)**: Decorative blocks, simple stairs/slabs, wither-proof building blocks, empty placeholders.

## Class A — item / block IDs (namespace `iska_utils:`)

| Topic | Item / block IDs |
|-------|------------------|
| Vector motion | `vector_charm`, `fanpack`, `slow_vect`, `moderate_vect`, `fast_vect`, `extreme_vect`, `ultra_vect`, `player_slow_vect`, `player_moderate_vect`, `player_fast_vect`, `player_extreme_vect`, `player_ultra_vect` |
| Fan upgrades | `fan`, `range_module`, `ghost_module`, `logic_module`, `capacitor_module` |
| Vector modules (crafting) | `base_module`, `slow_module`, `moderate_module`, `fast_module`, `extreme_module`, `ultra_module` |
| Travel / climb | `portable_dislocator`, `gauntlet_of_climbing` |
| Scanner | `scanner`, `scanner_chip`, `scanner_chip_ores`, `scanner_chip_mobs` |
| Artifacts and relics | `suspicious_delivery`, `drop_of_chaos`, `mining_equitizer`, `necrotic_crystal_heart`, `old_brick`, `chosen_cheese`, `ice_diamond`, `sharpened_bone`, `the_roots`, `totem_of_pain`, `cursed_candle`, `busted_crown`, `ritual_gauntlet`, `the_deception` — hub `hubs/artifacts.md` |
| Tools | `swiss_wrench`, `dolly`, `dolly_hard`, `dolly_creative`, `blueprint`, `structure_placer` |
| Rubber / tap | `treetap`, `electric_treetap`, `rubber_sap_extractor`, `sacred_rubber_sapling`, `rubber_boots`, `dye_berry` |
| World / RS | `weather_detector`, `sound_muffler`, `weather_alterer`, `time_alterer`, `smart_timer`, `angel_block`, `hellfire_igniter` |
| Machines | `temporal_overclocker`, `temporal_overclocker_chip`, `structure_placer_machine`, `structure_saver_machine`, `rubber_sap_extractor` |
| Shops | `shop`, `auto_shop` |
| Braziers / signals | `burning_brazier`, `ghost_brazier`, `redstone_activator` |
| Deep drawer (v24: `deep_drawers`; v26: `deep_drawer`) | main block + `deep_drawer_extractor`, `deep_drawer_interface`, `deep_drawer_extender` |
| Other | `potion_plate` (dynamic IDs — document category), `chaotic_tnt`, `raft` / `raft_no_drop` |

## Class B

- `materials/rubber_and_processing.md`: `sap`, `rubber`, `rubber_chunk`, `plastic_ingot`, `green_sludge`, `tar_slimeball`, rubber wood set as links in text without per-block pages.

## Class C (examples)

- Wither-proof set, netherite bars, smooth blackstone variants, plate base, rubber decor blocks, dye bush empty/filled blocks, filled/empty rubber logs, lapis ice cream food only.
- **`greedy_shield`**, **`gift`**: intentionally **excluded** from the in-game GuideME (no `guides/…/*.md` page); discover in-world only.

## Source signals used

- `ModItems.java` registrations
- `item/custom/*` with `appendHoverText`
- `tooltip.iska_utils.*` keys in `en_us.json`
