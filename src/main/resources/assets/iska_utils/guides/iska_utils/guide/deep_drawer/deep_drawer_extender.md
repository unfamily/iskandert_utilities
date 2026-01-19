---
navigation:
  parent: deep_drawers/deep_drawers-index.md
  title: Deep Drawer Extender
  icon: deep_drawer_extender
  position: 40
categories:
- storage
item_ids:
- iska_utils:deep_drawer_extender
---

# Deep Drawer Extender

<BlockImage id="deep_drawer_extender" scale="8" />

The Deep Drawer Extender is a module that extends the presence of a Deep Drawer, making it accessible in insertion only to hoppers, item pipes, and other automation blocks that need to interact directly with the drawer.

## Key Features

- **Extends Drawer Presence**: Makes the drawer visible to blocks that need direct interaction in insertion (hoppers, item pipes, etc.)
- **Item Insertion**: You can insert items directly into the drawer through the Extender
- **Extraction Blocked**: Only allows insertion
- **Network Discovery**: Automatically finds the drawer through connected modules up to 16 blocks away

## Use Cases

- **Hopper Integration**: Place an Extender next to a hopper to allow automatic item insertion into the drawer
- **Item Pipe Routing**: Connect item pipes to the Extender to route items into the drawer from multiple sources
- **Extended Reach**: Chain multiple Extenders to extend the drawer's accessible area
- **Input-Only Automation**: Perfect for scenarios where you only want to insert items
