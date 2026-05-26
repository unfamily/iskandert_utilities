---
navigation:
  title: Weather, time, and ambience
  icon: iska_utils:weather_detector
  parent: hubs/world_and_machines.md
  position: 50
item_ids:
  - iska_utils:weather_detector
  - iska_utils:weather_alterer
  - iska_utils:time_alterer
categories:
  - World and machines
---
# Weather, time, and ambience

These blocks read or change **world weather** and **time of day**. Player-facing audio is a separate concern (**Sound Muffler**, own guide).

## Weather Detector

<ItemImage id="iska_utils:weather_detector" />

- Outputs **redstone** according to the active mode (aligned with weather / sky state).
- **Right-click** the block to **cycle modes**.

## Weather Alterer

<ItemImage id="iska_utils:weather_alterer" />

- Changes **world weather** depending on the selected mode.
- **Right-click** the block to **cycle modes**.
- **Redstone input:** triggers the selected mode on a **rising edge** (unpowered → powered), like other redstone-controlled machines in this mod. Redstone dust connects to all faces.

## Time Alterer

<ItemImage id="iska_utils:time_alterer" />

- Changes **time of day** depending on the selected mode.
- **Right-click** the block to **cycle modes**.
- **Redstone input:** triggers the selected mode on a **rising edge** (unpowered → powered), like other redstone-controlled machines in this mod. Redstone dust connects to all faces.
