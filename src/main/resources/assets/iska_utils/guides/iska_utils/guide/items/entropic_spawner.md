---
navigation:
  title: Entropised Spawner
  icon: iska_utils:entropic_spawner
  parent: hubs/world_and_machines.md
  position: 37
item_ids:
  - iska_utils:entropic_spawner
  - iska_utils:production_module
categories:
  - World and machines
---
# Entropised Spawner

<ItemImage id="iska_utils:entropic_spawner" />

## What it does

Automated **mob spawner**. When conditions are met it summons the configured mob type **on the block directly above** the machine. Unlike a vanilla spawner, you choose the mob with a **spawn egg** and can upgrade timing, batch size, and fuel with module slots in the GUI.

## Setting the mob

Use a **spawn egg** on the block (same interaction as configuring a vanilla spawner). The egg is consumed unless you are in creative mode. Open the GUI to confirm the selected mob and the countdown to the next cycle.

While the machine is **active**, a small **rotating preview** of the configured mob appears inside the block.

## Spawn cycle

1. After a mob is set, the spawner waits a **random interval** before each cycle.
2. At the end of the interval it tries to spawn **one mob**, plus **one extra mob per stacked Production Module** in the module slot.
3. Spawning **pauses** while **too many living entities** are stacked above the block (server-side overcrowding cap). The timer may still advance depending on redstone mode, but no mobs appear until enough mobs leave.
4. If a **lifetime spawn limit** applies on your world, the GUI shows when the machine has reached it and spawning stops permanently until the block is replaced or the limit is cleared by other means.

Mobs spawn **centered above** the block. Only **solid collision** blocks placement; light level and vanilla spawn rules are ignored.

## Entropy fuel

Place **Drop of Entropy** (or other entropy fuel accepted by the Ancient Table) in the **fuel slot**. Items are converted into an **internal charge buffer** shown as a percentage under the slot.

- The spawner **spawns without charge** — fuel is **not** required for mob generation.
- While charge is present, the **countdown between spawn cycles runs faster** (stacks with Entropic Clock delay reduction).
- The physical fuel slot can be refilled by hand or automation while the buffer absorbs more charge.

## Upgrades

| Slot | Item | Effect |
| ---- | ---- | ------ |
| Clock | <ItemImage id="iska_utils:entropic_clock" /> **Entropic Clock** | Shortens the delay between spawn cycles (stacks in the slot). Also upgrades the **Temporal Overclocker** — see that page. |
| Module | <ItemImage id="iska_utils:production_module" /> **Production Module** | Adds **one extra mob** per spawn cycle for each module in the stack. |

Ghost icons in empty slots show valid items.

## GUI

| Control | Purpose |
| ------- | ------- |
| Center | Configured mob name and **time until next spawn** (or lifetime cap message). |
| Left column | Entropic Clock (top), Production Module (middle). |
| Fuel slot | Entropy fuel; percentage label below. |
| **Redstone** button (right) | Cycles control mode — left click forward, right click backward. |
| **✕** | Close |

## Redstone

Same family as other Iska machines, including **Pulse** on this block:

| Mode | Behaviour |
| ---- | --------- |
| **Ignore** | Always runs when other conditions are met. |
| **Low** | Runs only with **no** redstone signal. |
| **High** | Runs only **with** redstone signal. |
| **Pulse** | Timer advances while powered; **one spawn cycle** fires on a **rising** redstone edge. |
| **Disabled** | Never spawns. |

## Block states

- **Inactive** — no mob configured, redstone blocks operation, or lifetime cap reached.
- **Active** — ready to count down and spawn.
- **Spawning** — brief top texture flash when mobs are created.

## Automation

Module and fuel slots accept **hoppers and item pipes**. Breaking the block drops stored modules and fuel.

## Tips

- Leave **headroom** on the block directly above the spawner and keep it free of solid blocks.
- Pair with a **Mob Reaper** and **Collecting Crate** for unattended farms.
- Stack **Production Modules** for throughput; stack **Entropic Clocks** for faster cycles; optional **entropy fuel** shortens the delay further.
- Use **High** or **Pulse** redstone to gate farms behind buttons or clocks.
