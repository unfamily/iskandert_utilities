---
navigation:
  title: Swiss Wrench
  icon: iska_utils:swiss_wrench
  parent: hubs/tools_and_scanner.md
  position: 25
item_ids:
  - iska_utils:swiss_wrench
categories:
  - Useful tools
---
# Swiss Wrench

<ItemImage id="iska_utils:swiss_wrench" />

A universal wrench for rotating and orienting blocks.

## Orientation menu

Hold the Swiss Wrench and look at a rotatable block, then press **R** (default keybind: Swiss Wrench Rotate).

The orientation menu opens over the world:

- **Icons around the center** show possible values for the current step (facing, local up, axle, and similar properties). Click an icon to choose it and continue.
- Multi-property blocks use **steps** (for example facing first, then a second property). The title in the center shows the current step.
- **← / →** in the center rotate the block immediately (counter-clockwise / clockwise) without closing the menu.
- **Done** keeps the current step value and advances to the next step (or applies and closes on the last step).
- **Right-click**, **Esc**, or **Backspace** go back one step; Esc on the first step closes the menu.
- Keyboard: movement **Left/Right** (or arrow keys) mirror the on-screen ← / → buttons.

Preview icons are oriented relative to where you are looking.

## Shift + Right Click

**Shift + Right Click** does not consume the interaction, so other mods can still pick up or interact with the block.

## Legacy modes (optional)

If legacy modes are enabled in the config, **Left Click** (block or air) cycles stored rotation modes (clockwise, counter-clockwise, absolute directions). With a non-menu mode selected, **R** applies that mode instead of opening the menu.
