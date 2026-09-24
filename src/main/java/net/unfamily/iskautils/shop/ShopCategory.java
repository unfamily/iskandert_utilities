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
    /**
     * Parent category id (JSON {@code in_category}). {@code null} = root.
     */
    public String inCategory;
    /** Display order: higher value = shown first. Default 0. */
    public int priority = 0;
    /**
     * Stage gate requirements. If any fail the category (and all its children) are inaccessible.
     * Same semantics as {@link ShopEntry#stages}.
     */
    public ShopStage[] stages;
} 