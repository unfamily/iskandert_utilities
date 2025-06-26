package net.unfamily.iskautils.shop;

/**
 * Represents an entry in the shop system
 */
public class ShopEntry {
    public String id; // Unique identifier for the entry
    public String inCategory;
    public String item; // Also supports data components: "minecraft:diamond_sword[enchantments={...}]"
    public int itemCount;
    public String valute;
    public double buy;
    public double sell;
    public ShopStage[] stages;
} 