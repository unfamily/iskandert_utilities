---
navigation:
  title: Smart Timer
  icon: iska_utils:smart_timer
  parent: hubs/world_and_machines.md
  position: 53
item_ids:
  - iska_utils:smart_timer
categories:
  - World and machines
---
# Smart Timer

<ItemImage id="iska_utils:smart_timer" />

## Purpose

The Smart Timer is a **repeating redstone source**. It alternates between:

1. **Cooldown** — no output (the block is **off**).
2. **Pulse** — full **strength 15** redstone output for a configurable duration.

So you get a train of pulses with a quiet gap between them, without building huge hopper clocks.

## GUI — two timers

Open the block to tune **two independent durations** (shown in hours, minutes, seconds, plus leftover ticks):

| In-game label | Meaning |
| ------------- | ------- |
| **Redstone off for:** | Time the block stays **off** between pulses — this is the **cooldown** before the next **on** phase. |
| **Redstone on for:** | How long each **pulse** stays **on** (full redstone strength). |

**Defaults** (if unchanged): about **5 seconds** cooldown, **3 seconds** pulse (server uses ticks internally).

### Adjusting values

- Rows of **+ / −** buttons step by **hours, minutes, seconds**, or fine steps (**10 ticks / 5 ticks** — 0.5 s / 0.25 s at 20 TPS).
- Each value also shows **total ticks** for precise builds.
- Minimum duration enforced on the server is **5 ticks** per phase.

While the pulse is **active**, redstone mode is ignored — the ON phase always runs to completion.

## Redstone output

- The block **emits strong power** when “on”: comparator-facing setups see **15** from the powered state.
- When “off”, output is **0**.

## Redstone control modes (next to the close button)

A small icon cycles **when the cooldown may advance** (the pause between pulses). **Left-click** steps forward; **right-click** steps backward.

| Icon (hint) | Mode | Behaviour during cooldown |
| ----------- | ---- | ------------------------- |
| Gunpowder | **Ignore redstone** | Cooldown **always** runs; timer never pauses. |
| Redstone dust | **Low** | Cooldown runs only while **no** adjacent redstone power is applied (default). |
| Redstone torch style | **High** | Cooldown runs only while the block **is** powered from neighbours. |
| Barrier | **Disabled** | Cooldown **never** advances — timer frozen unless you change mode. |

During the **pulse** (ON phase), this gating does not apply; the output length is fixed by **signal duration**.

## Tips

- Use **Low** mode so a lever can **pause** the clock when powered (invert your wiring if you need “run when powered”).
- Use **Ignore** for a clock that must run regardless of nearby dust or levers.
- Pair with **comparators** or **opaque blocks** to branch signals without loading the same face twice.

Recipe and exact labels match your installed language files; use **JEI** for crafting.
