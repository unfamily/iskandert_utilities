---
navigation:
  parent: deep_drawer/deep_drawer-index.md
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
- **Dual Filter System**: 
  - **Allow Filter List**: Extracts only items that match the filters (whitelist mode). When this list is active, the button shows "Deny List" to switch to the deny filter
  - **Deny Filter List**: Extracts all items except those that match the filters (blacklist mode). When this list is active, the button shows "Allow List" to switch back to the allow filter
  - **Filter Priority**: When both filter lists have entries (hybrid cases), the Deny Filter List is applied first, then the Allow Filter List. This means items matching the Deny Filter List are blocked before checking the Allow Filter List
  - Switch between the two filter lists using the "Allow List" / "Deny List" button in the GUI
- **Redstone Control**: Control extraction with redstone signals
- **Optimized Performance**: Designed specifically for extraction to minimize performance impact
- **Network Discovery**: Automatically finds the drawer through connected modules up to 16 blocks away

## Use Cases

- **Selective Item Extraction**: Automatically extract specific items like enchanted gear, tools, or items from specific mods from the drawer to the Extractor's buffer
- **Production Lines**: Extract specific items needed for automated crafting or processing systems

Remember: The Extractor moves items from the drawer to its internal buffer, but you must connect item pipes or hoppers to the Extractor itself to actually get items from the buffer and route them to your destination.
