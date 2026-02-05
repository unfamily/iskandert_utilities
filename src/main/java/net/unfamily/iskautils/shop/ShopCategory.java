package net.unfamily.iskautils.shop;

/**
 * Represents a category in the shop system.
 * Categories are ordered by {@link #priority} (higher first), then by {@link #id}.
 */
public class ShopCategory {
    public String id;
    public String name;
    public String description;
    public String item;
    /** Display order: higher value = shown first. Default 0. */
    public int priority = 0;
} 