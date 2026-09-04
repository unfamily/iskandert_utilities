package net.unfamily.iskautils.shop;

/**
 * Represents an entry in the shop system.
 * Within a category, entries are ordered by {@link #priority} (higher first), then by resource id.
 */
public class ShopEntry {
    public enum EntryType {
        ITEM,
        FLUID,
        GAS,
        OTHER
    }

    public String id;
    public String inCategory;
    /** Entry resource type. Default {@link EntryType#ITEM}. */
    public EntryType type = EntryType.ITEM;
    /** Item selector (id, components, or {@code #tag}). Used when {@code type == ITEM}. */
    public String item;
    /** Fluid selector (id or {@code #tag}). Used when {@code type == FLUID}. */
    public String fluid;
    /** Gas/chemical id (no tags). Used when {@code type == GAS}. */
    public String gas;
    /** Other resource id (e.g. {@code iska_utils:rf}). Used when {@code type == OTHER}. */
    public String other;
    /**
     * Generic quantity: item count, fluid mB, gas mB, or FE for RF other.
     * {@link #itemCount} mirrors this for legacy callers.
     */
    public int amount = 1;
    /** Legacy mirror of {@link #amount}. */
    public int itemCount;
    public String currency;
    public String valute;
    public double buy;
    public double sell;
    /** Display order within category: higher value = shown first. Default 0. */
    public int priority = 0;
    /** If true, item can be bought even when buy is 0; no currency is charged. */
    public boolean free = false;
    public ShopStage[] stages;
}
