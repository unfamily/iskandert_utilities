package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.world.item.Item;

/**
 * The Roots relic.
 * Mining speed boost is handled via events.
 */
public class TheRootsItem extends Item {
    public TheRootsItem(Properties properties) {
        super(properties.stacksTo(1));
    }
}

