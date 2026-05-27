package net.unfamily.iskautils.item.custom.relic;

import net.minecraft.world.item.Item;

/**
 * Sharpened Bone relic.
 * Effects are implemented via event handlers; this item is just the carrier.
 */
public class SharpenedBoneItem extends Item {
    public SharpenedBoneItem(Properties properties) {
        super(properties.stacksTo(1));
    }
}

