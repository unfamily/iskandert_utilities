package net.unfamily.iskautils.guide;

import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.item.custom.relic.TheRootsItem;

/**
 * Display names for The Roots guide pages, matching {@link TheRootsItem#getName(net.minecraft.world.item.ItemStack)}.
 */
public final class TheRootsGuideNames {
    private static final String KEY_PLURAL = "item.iska_utils.the_roots";
    private static final String KEY_UNIX = "item.iska_utils.the_roots.unix";

    private TheRootsGuideNames() {
    }

    public static Component displayComponent() {
        return Component.translatable(TheRootsItem.isUnixLike() ? KEY_UNIX : KEY_PLURAL);
    }

    public static String displayName() {
        return displayComponent().getString();
    }
}
