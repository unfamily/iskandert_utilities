package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.world.item.Item;

/**
 * Base class for cursed relics.
 * Concrete effects are implemented elsewhere (events / keybind integration).
 */
public class CursedRelicItem extends Item {
    public CursedRelicItem(Properties properties) {
        super(properties.stacksTo(1));
    }
}

