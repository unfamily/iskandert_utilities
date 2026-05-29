---
navigation:
  title: Entropy TNT
  icon: iska_utils:entropy_tnt
  parent: hubs/entropy_materials.md
  position: 4
item_ids:
  - iska_utils:entropy_tnt
categories:
  - Entropy materials
---
# Entropy TNT

<ItemImage id="iska_utils:entropy_tnt" />

A block of highly unstable entropy explosive. Handle with extreme care.

## Behavior

When placed, it detonates if the block receives **redstone** power (including when placed already powered). A neighbor signal change can also trigger it.

The blast is a **progressive ellipsoid**:

| Axis | Radius (blocks) |
|------|-----------------|
| Horizontal (X / Z) | **250** |
| Vertical (Y) | **50** |

It destroys terrain over that volume, including blocks that are normally unbreakable (such as bedrock). The explosion expands over time rather than instantly clearing the whole area.
