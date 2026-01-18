---
navigation:
  parent: deep_drawers/deep_drawers-index.md
  title: Deep Drawer Extractor
  icon: deep_drawer_extractor
  position: 20
categories:
- storage
item_ids:
- iska_utils:deep_drawer_extractor
---

# Deep Drawer Extractor

<BlockImage id="deep_drawer_extractor" scale="8" />

The Deep Drawer Extractor is a specialized module designed to extract items from a Deep Drawer. It uses passive extraction: items are moved to its internal buffer, and you must connect item pipes or hoppers to extract items from the Extractor itself.

## Key Features

- **Passive Extraction**: Automatically moves items from the connected Deep Drawer to its internal buffer based on your filters. You must connect item pipes or hoppers to the Extractor to get items from the buffer
- **Advanced Filter System**: Supports multiple filter types including item IDs, tags, mod IDs, NBT data, and predefined macros. The complete list of valid filter keys is available in the GUI by clicking the "Valid Keys" button 
- **Deny List/Allow List Mode**: Configure the extractor to extract only specific items (deny) or exclude certain items (allow)
- **Dual Filter System**: 
  - **Normal Filter List**: The default filter that determines which items are extracted based on Allow/Deny mode
  - **Bypass List / Exclusion List**: An inverted filter that is applied before the normal filter:
    - When Extractor is in **Allow mode**: The inverted filter acts as **Exclusion List** (deny mode). If an item matches the exclusion list, it is blocked and will not be extracted, even if it matches the normal filter
    - When Extractor is in **Deny mode**: The inverted filter acts as **Bypass List** (allow mode). If an item matches the bypass list, it is extracted immediately without checking the normal filter
- **Redstone Control**: Control extraction with redstone signals
- **Optimized Performance**: Designed specifically for extraction to minimize performance impact
- **Network Discovery**: Automatically finds the drawer through connected modules up to 16 blocks away

## Use Cases

- **Selective Item Extraction**: Automatically extract specific items like enchanted gear, tools, or items from specific mods from the drawer to the Extractor's buffer
- **Production Lines**: Extract specific items needed for automated crafting or processing systems

Remember: The Extractor moves items from the drawer to its internal buffer, but you must connect item pipes or hoppers to the Extractor itself to actually get items from the buffer and route them to your destination.
