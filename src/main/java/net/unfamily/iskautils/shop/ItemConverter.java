package net.unfamily.iskautils.shop;

import net.minecraft.world.item.ItemStack;

/**
 * Compatibility wrapper for moved library utility.
 *
 * <p>Prefer {@link net.unfamily.iskalib.item.ItemConverter} in new code.
 */
public final class ItemConverter {
    private ItemConverter() {}

    public static ItemStack parseItemString(String itemString, int count) {
        return net.unfamily.iskalib.item.ItemConverter.parseItemString(itemString, count);
    }

    public static ItemStack parseItemString(String itemString) {
        return net.unfamily.iskalib.item.ItemConverter.parseItemString(itemString);
    }

    public static boolean isValidItemString(String itemString) {
        return net.unfamily.iskalib.item.ItemConverter.isValidItemString(itemString);
    }

    public static String getItemDisplayName(String itemString) {
        return net.unfamily.iskalib.item.ItemConverter.getItemDisplayName(itemString);
    }

    /** Same shape as {@code /give} item argument and shop {@code item} fields. */
    public static String formatAsItemArgument(ItemStack stack) {
        return net.unfamily.iskalib.item.ItemConverter.formatAsItemArgument(stack);
    }
}

