# Iska Utils 3.11.0.0.0

## Features

- **Ethereal Frame**: new transparent frame block with entity allow/deny filter (network-synced), optional light blocking, camouflage (mimics another full block), and wither-proof reinforcement. Left-click with a reinforcement material to reinforce the connected network; shift-right-click strips camouflage or reinforcement and returns the materials. Valid reinforcement materials: Wither Proof Block, Obscure Glass, Ethereal Obscure Glass, and `#c:bars/netherite` (Netherite Bars). Breaking a camouflaged/reinforced frame returns the frame plus the camouflage block item and reinforcement material.
- **Labeling Machine**: portable tool GUI to apply a formatted custom name and lore to an item. Multi-segment name/lore editors with styles (bold, italic, underline, strikethrough, obfuscated), HSV color picker, lore line list (configurable max lines / line length), Apply / Copy / Reset for name+lore together. Config: `020_labeling_max_line_length`, `021_labeling_max_lore_lines`, `022_labeling_force_italic_non_ops`. While the GUI is open, only the Labeling Machine that opened it is locked in the inventory (other Labeling Machines remain movable).
- **Obscure Glass**: It does not let light through and is wither proof block.
- **Wander Nullifier**: stop natural spawning of wandering traders.
- **Soul Nullifier**: require Forbidden Arcanus, stop natural spawning of Lost Souls.
- **Entropic Soil**: now does not spawn some mobs; can be blacklisted using the tag `#iska_utils:entropic_soil_no_spawn`.
- **Extended Scanner**: new chips & small tweaks.
- Added Ender Nullifier & Other Nullifiers to guideME.
- Added Recipe for Ender Nullifier.
- Extended & improved Auto Shop GUI.

## Tweaks

- Ender Nullifier modified texture.
- Changed Recipes for: Base Module, Wither Proof Block, Green sludge.
- Reduced Witherproof mining hardness.
- Entropic Soil: disabled by default max HP limit for spawning mobs.

## Bug Fixes

- Ghost Brazier: fixed a bug where the Ghost Brazier did not work in the Curio in version 26.1.2.
- Blazing Altar: no longer extinguishes the flames if it is broken.
- Auto Shop: Fixed a bug that was preventing the auto shop from working; it was no longer recognising the player’s team.
- Shop: Fixed a bug in version 26.1.2 that prevented users from purchasing large quantities of items.
- Wither Proof blocks (26.1.2): fixed missing item translations (`item.*` keys) by using the block description prefix.
- Ethereal Frame camouflage (26.1.2): fixed invisible camouflage rendering (client camouflage callback was not registered).
- Ethereal Frame camouflage drops: breaking a camouflaged frame now returns the camouflage block item itself instead of that block’s mining loot table (e.g. glass, grass, ores).
