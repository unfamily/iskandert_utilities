---
navigation:
  title: Pattern Crafter
  icon: iska_utils:improved_pattern_crafter
  parent: hubs/world_and_machines.md
  position: 37
item_ids:
  - iska_utils:pattern_crafter
  - iska_utils:improved_pattern_crafter
  - iska_utils:pattern_crafter_improver
categories:
  - World and machines
---
# Pattern Crafter

<ItemImage id="iska_utils:improved_pattern_crafter" />

## What it does

The **Pattern Crafter** (and **Improved Pattern Crafter**) is an automated crafting machine driven by **letter patterns**. You assign letters to a 3×3 grid, map each letter to an **item filter** (a “variable”), feed ingredients into the machine inventory, and the crafter repeatedly crafts matching recipes using RF/FE.

The **Improved** variant supports a larger upgrade budget and more concurrent work; both share the same GUI and controls.

## In-place upgrade

Craft a **Pattern Crafter Improver** (same gem frame as Improved, centre is any `#minecraft:planks`). **Shift+right-click** a placed normal Pattern Crafter with the Improver to convert it to Improved **without losing** inventory, patterns, filters, energy, or settings. The Improver is consumed.

## Patterns

- The left column holds the **current pattern**: mode controls, pattern browser, 3×3 letter grid, **Save**, and **Mark Input**.
- Each cell is a letter (or empty). Letters link to **variables** in the filter area.
- Cycle a cell with left/right click; **Shift+click** clears it. You can also place an item on a cell to assign a matching letter from your variables.
- Switch between stored patterns with the pattern arrows / label. **Shift+click** the pattern label clears the current pattern.
- **Save** writes pending grid edits to the selected pattern slot.

## Variables (filters)

Above the machine inventory sit the **variable** slots (paginated when you unlock many keys):

- Small **letter** buttons above/below each slot unlock and cycle the letter for that variable.
- While locked (no letter), the large button is inactive.
- When unlocked, click the large button to open the **inline editor** for that variable’s filter string.
- Filters use the same **Valid Keys** language as Deep Drawer filters (item id, mod id, tag, NBT, macros). Open **Valid Keys** in the editor for the full list.
- Arrow buttons page through variables when Logic Modules unlock more than one screen.

## Crafting modes

Cycle **Both / Only Shaped / Only Shapeless** to restrict which recipe shapes the machine accepts.

## Result routing (recursive outputs)

Controls where craft **results** go:

| Mode | Behavior |
| ---- | -------- |
| **Res. Eject** | Results go only to machine **output** slots. Crafting stops if there is no space. |
| **Res. Keep** | Results merge into **inputs** first; overflow goes to output. Stops if anything cannot fit. |
| **Res. Smart** | Merge as much of the result into inputs as fits; eject the rest. Stops if any part would overflow. |

## Unused ingredients (remainders)

When a craft leaves leftover inputs (or partial stacks):

| Mode | Behavior |
| ---- | -------- |
| **Ing. Keep** | Remainders try **input** slots first, then outputs. Crafting is blocked if they do not fit. |
| **Ing. Eject** | Remainders try **output** slots first. Crafting is blocked if they do not fit. |

## Tool safeguard

- **Tools: Protect** — tools that would break are ejected to output instead of being consumed.
- **Tools: Allow Break** — tools may be used until they break.

## Forbidden outputs

**Forbidden outputs** opens a list of craft results the machine must never produce. Edit entries with the same filter editor (Valid Keys, variants, apply/clear). Useful to block byproducts or unwanted recipe paths.

## Mark Input / Mark Output

Same idea as the Structure Placer “set inventory” marks:

- **Mark Input** — remembers which items belong in each machine input slot (ghosts on empty slots). Click / Shift / Ctrl-Alt variants clear or refresh marks.
- **Mark Output** — same for the paginated output grid. Empty marked slots show ghost previews.
- Double-click an empty marked slot to clear that slot’s mark.

Marks guide what hoppers and players should put where; they do not replace the letter filters.

## Inventory and automation

- **Machine input** (9×3): ingredients for crafting; hoppers can insert.
- **Output** (3×3, paginated): crafted results and ejected remainders/tools.
- **Player inventory** at the bottom of the GUI.
- Break the block to drop contents and modules.

## Redstone and power

- Needs **RF/FE** in the energy bar.
- Redstone mode button: ignore / low / high / disabled (same family as other machines).

## Modules

Three upgrade slots (ghost icons when empty):

| Slot | Module | Effect |
| ---- | ------ | ------ |
| Logic | <ItemImage id="iska_utils:logic_module" /> **Logic Module** | Unlocks more variable keys / pattern capacity. |
| Speed | **Vector modules** (**Slow** → **Ultra**) | Shortens crafting time (higher tiers are faster). |
| Production | <ItemImage id="iska_utils:production_module" /> **Production Module** | Increases how many crafts run concurrently. |

See **Modules** → **Logic Module**, **Vector modules**, and **Production Module**.

## JEI

With the Pattern Crafter GUI open, use JEI’s recipe transfer (**+**) on a crafting recipe:

- Related **variables** (filters / letters) are applied **immediately**.
- The **pattern** grid (and crafting mode from the transfer) stay **pending** until you confirm.
- **Save** confirms **only the pattern** into the selected pattern slot.

Requires free variable slots and unused letters for any new ingredient types; the pattern grid cells must be empty before transfer.

<ItemGrid>
  <ItemIcon id="iska_utils:pattern_crafter" />
  <ItemIcon id="iska_utils:improved_pattern_crafter" />
  <ItemIcon id="iska_utils:pattern_crafter_improver" />
  <ItemIcon id="iska_utils:logic_module" />
  <ItemIcon id="iska_utils:slow_module" />
  <ItemIcon id="iska_utils:production_module" />
</ItemGrid>

## Tips

- Unlock letters on variables **before** editing their filters.
- Keep Mark Input aligned with your letter filters so automation stays consistent.
- Use **Forbidden outputs** to stop annoying byproducts; use crafting mode to force shaped or shapeless-only lines.
- Pair Logic Modules with more complex multi-ingredient patterns; use Production when you need throughput.
