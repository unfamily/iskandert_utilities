package net.unfamily.iskautils.shop;

/**
 * Represents an entry in the shop system.
 * Within a category, entries are ordered by {@link #priority} (higher first), then by {@link #item}.
 */
public class ShopEntry {
    public String id; // Unique identifier for the entry
    public String inCategory;
    public String item; // Also supports data components: "minecraft:diamond_sword[enchantments={...}]"
    public int itemCount;
    public String currency;
    public String valute; // Legacy field for backward compatibility
    public double buy;
    public double sell;
    /** Display order within category: higher value = shown first. Default 0. */
    public int priority = 0;
    /** If true, item can be bought even when buy is 0; no currency is charged. */
    public boolean free = false;
    public ShopStage[] stages;
} 